package com.sumirelabs.quarryplus.gui.mui;

import com.yogpc.qp.tile.MiniQuarryArea;
import com.yogpc.qp.tile.TileMiniQuarry;
import com.yogpc.qp.tile.TileMarker;
import com.yogpc.qp.QuarryPlusI;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/** Real-world harvesting and persistence checks, only in the opt-in disposable smoke world. */
final class MiniQuarrySmokeChecks {
    private static void check(boolean valid, String message) { if (!valid) throw new IllegalStateException(message); }
    private static void tick(TileMiniQuarry tile) {
        var world = tile.getWorld();
        world.setTotalWorldTime((world.getTotalWorldTime() / 40 + 1) * 40);
        tile.update();
    }
    private static void stop(TileMiniQuarry tile) { if (tile.isRunning()) tile.toggleRunning(); }
    private static IItemHandler handler(TileMiniQuarry tile) { return tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP); }
    private static int outputCount(TileMiniQuarry tile, Item item) {
        int count = 0;
        IItemHandler handler = handler(tile);
        for (int slot = 5; slot < handler.getSlots(); slot++) if (handler.getStackInSlot(slot).getItem() == item) count += handler.getStackInSlot(slot).getCount();
        return count;
    }
    private static void reset(TileMiniQuarry tile, BlockPos target) {
        stop(tile);
        tile.setArea(new MiniQuarryArea(target.getX(), target.getY(), target.getZ(), target.getX(), target.getY(), target.getZ()));
        for (int slot = 0; slot < 18; slot++) tile.filters.setInventorySlotContents(slot, ItemStack.EMPTY);
        tile.tools.clear();
        IItemHandler handler = handler(tile);
        for (int slot = 5; slot < handler.getSlots(); slot++) handler.extractItem(slot, Integer.MAX_VALUE, false);
        tile.setStoredEnergy(10000000000L);
    }

    static void verify(EntityPlayerMP player, TileMiniQuarry tile) {
        var world = player.getServerWorld();
        BlockPos target = new BlockPos(5, 64, 2), power = tile.getPos().west();
        stop(tile);
        BlockPos[] markers = {tile.getPos().south(), tile.getPos().south().east(5),
                tile.getPos().south(6), tile.getPos().south(6).east(5)};
        for (BlockPos marker : markers) {
            world.setBlockState(marker.down(), Blocks.STONE.getDefaultState());
            world.setBlockState(marker, QuarryPlusI.blockMarker().getDefaultState());
        }
        ((TileMarker) world.getTileEntity(markers[0])).S_tryConnection();
        check(tile.selectArea() && tile.area().minX() == 3 && tile.area().maxX() == 6
                && tile.area().minZ() == 4 && tile.area().maxZ() == 7, "Mini Quarry did not select the inside of linked markers");
        for (BlockPos marker : markers) check(world.isAirBlock(marker), "Mini Quarry did not return consumed markers");
        tile.redstoneChanged(false);
        world.setBlockState(power, Blocks.REDSTONE_BLOCK.getDefaultState());
        check(tile.isRunning(), "Redstone rising edge did not start Mini Quarry");
        tile.redstoneChanged(true);
        check(tile.isRunning(), "Held redstone signal toggled Mini Quarry repeatedly");
        world.setBlockToAir(power);
        check(tile.isRunning(), "Redstone falling edge stopped Mini Quarry");
        world.setBlockState(power, Blocks.REDSTONE_BLOCK.getDefaultState());
        check(!tile.isRunning(), "Second redstone pulse did not stop Mini Quarry");
        world.setBlockToAir(power);

        reset(tile, target);
        world.setBlockState(target, Blocks.STONE.getDefaultState());
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        pick.addEnchantment(Enchantments.SILK_TOUCH, 1);
        tile.tools.setInventorySlotContents(0, pick);
        tile.toggleRunning(); tick(tile);
        check(world.isAirBlock(target), "Mini Quarry did not harvest stone");
        check(pick.getItemDamage() == 1 && outputCount(tile, Item.getItemFromBlock(Blocks.STONE)) == 1, "Silk Touch or tool durability failed");
        check(tile.getStoredEnergy() < 10000000000L, "Mini Quarry did not consume energy");
        NBTTagCompound saved = tile.writeToNBT(new NBTTagCompound());
        TileMiniQuarry restored = new TileMiniQuarry();
        restored.setWorld(world); restored.readFromNBT(saved);
        check(restored.area().equals(tile.area()) && restored.isRunning() == tile.isRunning()
                && restored.tools.getStackInSlot(0).getItemDamage() == 1, "Mini Quarry save/load lost progress or tools");
        restored.tools.getStackInSlot(0).setItemDamage(12);
        check(pick.getItemDamage() == 1, "Loaded tool stacks share mutable state");

        reset(tile, target);
        world.setBlockState(target, Blocks.STONE.getDefaultState());
        tile.tools.setInventorySlotContents(0, new ItemStack(Items.DIAMOND_PICKAXE));
        tile.setStoredEnergy(0);
        tile.toggleRunning(); tick(tile);
        check(!world.isAirBlock(target) && tile.tools.getStackInSlot(0).getItemDamage() == 0, "No-energy work damaged tools or blocks");

        reset(tile, target);
        world.setBlockState(target, Blocks.OBSIDIAN.getDefaultState());
        tile.tools.setInventorySlotContents(0, new ItemStack(Items.WOODEN_PICKAXE));
        tile.toggleRunning(); tick(tile);
        check(world.getBlockState(target).getBlock() == Blocks.OBSIDIAN, "Low-tier tool harvested obsidian");

        reset(tile, target);
        world.setBlockState(target, Blocks.STONE.getDefaultState());
        tile.tools.setInventorySlotContents(0, new ItemStack(Items.DIAMOND_PICKAXE));
        Protection protection = new Protection(target);
        MinecraftForge.EVENT_BUS.register(protection);
        try {
            tile.toggleRunning(); tick(tile);
            check(!world.isAirBlock(target) && tile.getStoredEnergy() == 10000000000L
                    && tile.tools.getStackInSlot(0).getItemDamage() == 0, "Protected block consumed energy, tool or block");
        } finally { MinecraftForge.EVENT_BUS.unregister(protection); }

        reset(tile, target);
        world.setBlockState(target, Blocks.BEDROCK.getDefaultState());
        tile.tools.setInventorySlotContents(0, new ItemStack(Items.DIAMOND_PICKAXE));
        tile.filters.setInventorySlotContents(0, new ItemStack(Blocks.BEDROCK));
        tile.toggleRunning(); tick(tile);
        check(world.getBlockState(target).getBlock() == Blocks.BEDROCK, "Allow list bypassed unbreakable blocks");

        reset(tile, target);
        world.setBlockState(target, Blocks.DIAMOND_ORE.getDefaultState());
        pick = new ItemStack(Items.DIAMOND_PICKAXE);
        pick.addEnchantment(Enchantments.SILK_TOUCH, 1);
        pick.setItemDamage(pick.getMaxDamage());
        tile.tools.setInventorySlotContents(0, pick);
        tile.toggleRunning(); tick(tile);
        check(tile.tools.getStackInSlot(0).isEmpty(), "Broken Mini Quarry tool was retained");
        check(outputCount(tile, Item.getItemFromBlock(Blocks.DIAMOND_ORE)) == 1, "Ore Silk Touch drop was lost");
        BlockPos chestPos = tile.getPos().east();
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState());
        tile.update();
        TileEntityChest chest = (TileEntityChest) world.getTileEntity(chestPos);
        check(chest.getStackInSlot(0).getItem() == Item.getItemFromBlock(Blocks.DIAMOND_ORE), "Mini Quarry did not export to adjacent chest");
        stop(tile);
    }

    public static final class Protection {
        private final BlockPos target;
        Protection(BlockPos target) { this.target = target; }
        @SubscribeEvent public void deny(BlockEvent.BreakEvent event) { if (event.getPos().equals(target)) event.setCanceled(true); }
    }
}
