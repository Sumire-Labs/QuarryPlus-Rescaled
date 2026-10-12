package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.yogpc.qp.QuarryPlusI;
import com.yogpc.qp.container.ContainerListTemplate;
import com.yogpc.qp.container.ContainerMover;
import com.yogpc.qp.item.ItemTemplate;
import com.yogpc.qp.tile.*;
import com.yogpc.qp.utils.BlockData;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.LinkedList;
import java.util.List;

/** Assertions for the opt-in smoke world only; callbacks travel through the real MUI network. */
@SideOnly(Side.CLIENT)
final class GuiSmokeInteractions {
    private static final BlockPos POS = new BlockPos(2, 64, 2);
    private GuiSmokeInteractions() {}

    static void prepare(EntityPlayerMP player, int id) {
        QuarryModularContainer container = (QuarryModularContainer) player.openContainer;
        check(container.inventorySlots.size() == container.original().inventorySlots.size(), "Duplicate/missing native slots");
        for (int i = 0; i < player.inventory.getSizeInventory(); i++)
            if (id != 12 || i != 0) player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
        player.inventory.setItemStack(ItemStack.EMPTY);
        player.inventory.setInventorySlotContents(9, new ItemStack(Blocks.COBBLESTONE, 16));
        TileEntity tile = player.world.getTileEntity(POS);
        if (id == 1) player.inventory.setInventorySlotContents(9, ItemStack.EMPTY);
        else if (id == 2) {
            ContainerMover mover = (ContainerMover) container.original();
            ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
            pick.addEnchantment(Enchantments.EFFICIENCY, 3);
            mover.craftMatrix.setInventorySlotContents(0, pick);
            mover.craftMatrix.setInventorySlotContents(1, new ItemStack(QuarryPlusI.blockQuarry()));
        } else if (id == 3 || id == 4) {
            TileBasic basic = (TileBasic) tile;
            List<BlockData> list = id == 3 ? basic.fortuneList : basic.silktouchList;
            list.clear(); list.add(stone());
            if (id == 3) basic.fortuneInclude = false; else basic.silktouchInclude = false;
        } else if (id == 6) {
            ((TileAdvQuarry) tile).digRange_$eq(new TileAdvQuarry.DigRange(0, 64, 0, 15, 64, 15));
            ((TileAdvQuarry) tile).mode().set(TileAdvQuarry.NOT_NEED_BREAK());
        } else if (id == 7) ((TileAdvPump) tile).placeFrame_$eq(false);
        else if (id == 10) { ((TileQuarry) tile).yMin = 64; ((TileBasic) tile).setYLevel(0); }
        else if (id == 11) ((TileAdvQuarry) tile).yLevel_$eq(0);
        else if (id == 14) {
            TileQuarry2 quarry = (TileQuarry2) tile;
            quarry.area_$eq(new TileQuarry2.Area(0, 64, 0, 15, 64, 15)); quarry.yLevel_$eq(0);
        } else if (id == 12) ((ContainerListTemplate) container.original()).craftMatrix.setInventorySlotContents(0, new ItemStack(Blocks.STONE));
        else if (id >= 100 && id < 106) {
            TilePump pump = (TilePump) tile;
            pump.mapping.put(EnumFacing.byIndex(id - 100), new LinkedList<>(List.of("water", "lava")));
        } else if (id >= 120 && id < 126) {
            TileAdvQuarry quarry = (TileAdvQuarry) tile;
            quarry.fluidExtractFacings().apply(EnumFacing.byIndex(id - 120)).clear();
            quarry.fluidExtractFacings().apply(EnumFacing.byIndex(id - 120)).add(new FluidStack(FluidRegistry.WATER, 1000));
            quarry.fluidStacks().clear();
            FluidStack lava = new FluidStack(FluidRegistry.LAVA, 1000);
            quarry.fluidStacks().put(lava, new FluidTank(lava, 1000));
        } else if (id == 140) {
            player.world.setBlockState(POS.east(), Blocks.MOB_SPAWNER.getDefaultState());
        }
        container.detectAndSendChanges();
    }

    static void click(Minecraft mc, int id) {
        QuarryModularContainer container = (QuarryModularContainer) mc.player.openContainer;
        check(container.inventorySlots.size() == container.original().inventorySlots.size(), "Client slot count mismatch");
        if ((id == 1 || id == 8) && net.minecraftforge.fml.common.Loader.isModLoaded("jei")) {
            mezz.jei.gui.recipes.RecipeClickableArea area = mezz.jei.Internal.getRuntime().getRecipeRegistry()
                    .getRecipeClickableArea((net.minecraft.client.gui.inventory.GuiContainer) mc.currentScreen,
                            id == 1 ? 8 : 80, id == 1 ? 78 : 36);
            boolean enabled = id == 1 || !com.yogpc.qp.Config.content().disableMapJ().get(com.yogpc.qp.block.BlockBookMover.SYMBOL);
            check((area != null) == enabled, "Incorrect JEI recipe click area registration");
        }
        if (id == 1) mc.playerController.windowClick(container.windowId, 0, 0, ClickType.QUICK_MOVE, mc.player);
        else if (id == 2) press(container, "mover_transfer");
        else if (id == 3 || id == 4) {
            press(container, "toggle"); press(container, "blocksSelect0"); press(container, "delete"); press(container, "delete");
        } else if (id == 6) press(container, "range0");
        else if (id == 7) press(container, "frame");
        else if (id == 10 || id == 11 || id == 14) press(container, "y_plus");
        else if (id == 12) {
            mc.playerController.windowClick(container.windowId, playerSlot(container, 0), 0, ClickType.PICKUP, mc.player);
            press(container, "add"); press(container, "modeToggle");
        }
        else if (id >= 100 && id < 106) {
            press(container, "fluidsSelect0"); press(container, "edit4");
            press(container, "fluidsSelect1"); press(container, "edit0"); press(container, "edit0");
        } else if (id >= 120 && id < 126) {
            press(container, "fluid0"); press(container, "remove");
            press(container, "add"); press(container, "fluid0"); press(container, "done");
        } else if (id == 140) { press(container, "entitiesSelect0"); press(container, "done"); }
        else if (!container.inventorySlots.isEmpty()) {
            int from = playerSlot(container, 9), to = playerSlot(container, 10);
            mc.playerController.windowClick(container.windowId, from, 0, ClickType.PICKUP, mc.player);
            mc.playerController.windowClick(container.windowId, to, 0, ClickType.PICKUP, mc.player);
        }
    }

    static void verify(EntityPlayerMP player, int id) {
        if (id == 140) {
            net.minecraft.tileentity.TileEntityMobSpawner spawner = (net.minecraft.tileentity.TileEntityMobSpawner) player.world.getTileEntity(POS.east());
            net.minecraft.nbt.NBTTagCompound tag = spawner.getSpawnerBaseLogic().writeToNBT(new net.minecraft.nbt.NBTTagCompound());
            check(!tag.getCompoundTag("SpawnData").getString("id").equals("minecraft:pig"), "Spawner selection failed");
            return;
        }
        QuarryModularContainer container = (QuarryModularContainer) player.openContainer;
        TileEntity tile = player.world.getTileEntity(POS);
        if (id == 1) {
            int total = 0;
            for (ItemStack stack : player.inventory.mainInventory) if (stack.getItem() == Items.IRON_INGOT) total += stack.getCount();
            check(total == 1500 && ((TileWorkbench) tile).getStackInSlot(0).isEmpty(), "Large-stack shift transfer lost/duplicated items");
        } else if (id == 2) {
            ContainerMover mover = (ContainerMover) container.original();
            check(EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, mover.craftMatrix.getStackInSlot(0)) == 2
                    && EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, mover.craftMatrix.getStackInSlot(1)) == 1, "Enchantment transfer failed");
        } else if (id == 3 || id == 4) {
            TileBasic basic = (TileBasic) tile;
            check((id == 3 ? basic.fortuneList : basic.silktouchList).isEmpty()
                    && (id == 3 ? basic.fortuneInclude : basic.silktouchInclude), "Block filter toggle/delete failed");
        } else if (id == 6) check(((TileAdvQuarry) tile).digRange().minZ() == -16, "Range adjustment failed");
        else if (id == 7) check(((TileAdvPump) tile).placeFrame(), "Pump frame toggle failed");
        else if (id == 10) check(((TileBasic) tile).yLevel == 1, "Quarry Y setting failed");
        else if (id == 11) check(((TileAdvQuarry) tile).yLevel() == 1, "Advanced quarry Y setting failed");
        else if (id == 14) check(((TileQuarry2) tile).yLevel() == 1, "Quarry2 Y setting failed");
        else if (id == 12) {
            ItemTemplate.Template template = ItemTemplate.getTemplate(player.inventory.getCurrentItem());
            check(template.items().contains(stone()) && !template.include() && player.inventory.getItemStack().isEmpty(), "Template add/toggle/protected slot failed");
        } else if (id >= 100 && id < 106) check(((TilePump) tile).mapping.get(EnumFacing.byIndex(id - 100)).equals(List.of("lava")), "Pump filter reorder/confirmed removal failed");
        else if (id >= 120 && id < 126) {
            scala.collection.mutable.Set<FluidStack> fluids = ((TileAdvQuarry) tile).fluidExtractFacings().apply(EnumFacing.byIndex(id - 120));
            check(fluids.size() == 1 && fluids.iterator().next().getFluid() == FluidRegistry.LAVA, "Fluid filter remove/add failed");
        } else if (id != 140 && !container.inventorySlots.isEmpty()) check(player.inventory.getStackInSlot(9).isEmpty()
                && player.inventory.getStackInSlot(10).getCount() == 16 && player.inventory.getItemStack().isEmpty(), "Inventory pickup/place failed");
    }

    static void configuration(Minecraft mc) {
        MuiConfigScreen.validateSmokeValues();
        com.cleanroommc.modularui.api.IMuiScreen screen = (com.cleanroommc.modularui.api.IMuiScreen) mc.currentScreen;
        press(screen.getScreen().getMainPanel(), "config_44");
        press(screen.getScreen().getMainPanel(), "config_8");
        press(screen.getScreen().getMainPanel(), "config_194");
        check(!(mc.currentScreen instanceof com.cleanroommc.modularui.screen.GuiScreenWrapper), "Configuration save did not close");
        mc.displayGuiScreen(MuiConfigScreen.create(null));
        screen = (com.cleanroommc.modularui.api.IMuiScreen) mc.currentScreen;
        press(screen.getScreen().getMainPanel(), "config_288");
        check(!(mc.currentScreen instanceof com.cleanroommc.modularui.screen.GuiScreenWrapper), "Configuration cancel did not close");
    }

    private static BlockData stone() { return BlockData.apply(Blocks.STONE.getRegistryName(), 0); }
    private static int playerSlot(QuarryModularContainer container, int index) {
        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot source = ((QuarryModularSlot) container.inventorySlots.get(i)).original;
            if (source.inventory == container.getPlayer().inventory && source.getSlotIndex() == index) return i;
        }
        throw new IllegalStateException("Missing player slot " + index);
    }
    private static void press(QuarryModularContainer container, String name) {
        press(container.getScreen().getMainPanel(), name);
    }
    private static void press(IWidget parent, String name) {
        for (IWidget widget : parent.getChildren()) {
            if (widget.isName(name) && widget instanceof ButtonWidget<?> button) { button.onMousePressed(0); return; }
        }
        throw new IllegalStateException("Missing button " + name);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
