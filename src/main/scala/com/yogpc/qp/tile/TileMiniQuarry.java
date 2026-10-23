package com.yogpc.qp.tile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.yogpc.qp.PowerManager;
import com.yogpc.qp.Config;
import com.yogpc.qp.QuarryPlus;
import com.yogpc.qp.QuarryPlusI;
import com.yogpc.qp.block.ADismCBlock;
import com.yogpc.qp.compat.InvUtils;
import com.yogpc.qp.modules.ItemFuelModule;
import com.yogpc.qp.modules.IModuleItem;
import com.yogpc.qp.utils.MarkerUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.item.ItemShears;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import scala.Symbol;

public final class TileMiniQuarry extends APowerTile implements IEnchantableTile, IDebugSender, IChunkLoadTile {
    public static final Symbol SYMBOL = Symbol.apply("MiniQuarry");
    public static final int SCAN_BUDGET = 256;
    public final InventoryBasic tools = new InventoryBasic("tile.mini_quarry.name", false, 5) {
        @Override public int getInventoryStackLimit() { return 1; }
        @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return isTool(stack) || stack.getItem() instanceof ItemFuelModule; }
        @Override public boolean isUsableByPlayer(EntityPlayer player) { return usable(player); }
    };
    public final MiniQuarryFilter filters = new MiniQuarryFilter();
    private final ItemStackHandler output = new ItemStackHandler(27) {
        @Override protected void onContentsChanged(int slot) { markDirty(); }
    };
    private final IItemHandler toolHandler = new InvWrapper(tools);
    private final IItemHandler itemHandler = new IItemHandler() {
        @Override public int getSlots() { return 5 + output.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return slot < 5 ? toolHandler.getStackInSlot(slot) : output.getStackInSlot(slot - 5); }
        @Override public int getSlotLimit(int slot) { return slot < 5 ? 1 : output.getSlotLimit(slot - 5); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return slot < 5 ? toolHandler.insertItem(slot, stack, simulate) : stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return slot < 5 ? toolHandler.extractItem(slot, amount, simulate) : output.extractItem(slot - 5, amount, simulate); }
    };
    private final Map<Integer, Integer> enchantments = new HashMap<>();
    private MiniQuarryArea area;
    private long next;
    private boolean running, redstone, showArea, loading;
    private String status = "qp.mini.idle";
    private ForgeChunkManager.Ticket ticket;
    private ChunkPos loadedTarget;

    public TileMiniQuarry() {
        G_ReInit();
        tools.addInventoryChangeListener(inv -> markDirty());
        filters.addInventoryChangeListener(inv -> changed());
    }

    public static boolean isTool(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof ItemTool || stack.getItem() instanceof ItemShears
                || !stack.getItem().getToolClasses(stack).isEmpty());
    }

    public boolean usable(EntityPlayer player) { return !isInvalid() && world.getTileEntity(pos) == this && player.getDistanceSq(pos) <= 64; }
    public MiniQuarryArea area() { return area; }
    public String statusKey() { return status; }
    public boolean isRunning() { return running; }
    public boolean showArea() { return showArea; }
    public void toggleAreaDisplay() { showArea = !showArea; changed(); }

    public void redstoneChanged(boolean powered) {
        boolean rising = powered && !redstone;
        redstone = powered;
        if (rising) toggleRunning();
        markDirty();
    }

    public void toggleRunning() {
        if (machineDisabled || world.isRemote) return;
        if (running) setRunning(false, "qp.mini.idle");
        else {
            if (area == null && !selectArea()) { changed(); return; }
            next = 0;
            setRunning(true, "qp.mini.running");
        }
    }

    public boolean selectArea() {
        if (world.isRemote || running) return false;
        EnumFacing facing = world.getBlockState(pos).getValue(ADismCBlock.FACING);
        var marker = MarkerUtil.searchMarker(world, pos, facing);
        if (marker.isEmpty()) { status = "qp.mini.no_area"; return false; }
        IMarker m = marker.get();
        BlockPos min = m.min(), max = m.max();
        int maxY = min.getY() == max.getY() ? Math.min(255, min.getY() + 3) : max.getY();
        try {
            MiniQuarryArea selected = new MiniQuarryArea(min.getX() + 1, Math.max(1, min.getY()), min.getZ() + 1,
                    max.getX() - 1, maxY, max.getZ() - 1);
            if (pos.getX() >= selected.minX() && pos.getX() <= selected.maxX()
                    && pos.getY() >= selected.minY() && pos.getY() <= selected.maxY()
                    && pos.getZ() >= selected.minZ() && pos.getZ() <= selected.maxZ()) {
                status = "qp.mini.invalid_area"; return false;
            }
            area = selected;
        } catch (IllegalArgumentException invalid) { status = "qp.mini.invalid_area"; return false; }
        m.removeFromWorldWithItem().forEach(this::store);
        next = 0;
        status = "qp.mini.idle";
        changed();
        return true;
    }

    /** Used by marker selection and the opt-in smoke world. */
    public void setArea(MiniQuarryArea area) {
        if (running) setRunning(false, "qp.mini.idle");
        this.area = area; next = 0; changed();
    }

    private void setRunning(boolean value, String message) {
        if (running != value) { if (value) startWork(); else finishWork(); }
        running = value; status = message;
        if (value) requestTicket();
        else releaseTicket();
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == QuarryPlusI.blockMiniQuarry()) world.setBlockState(pos, state.withProperty(ADismCBlock.ACTING, value), 2);
        changed();
    }

    public static int interval(int efficiency) {
        return switch (efficiency) { case 0 -> 40; case 1 -> 30; case 2 -> 20; case 3 -> 10; case 4 -> 5; case 5 -> 2; default -> efficiency < 0 ? 100 : 1; };
    }

    @Override public void update() {
        super.update();
        if (world.isRemote || machineDisabled) return;
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack stack = output.getStackInSlot(i);
            if (!stack.isEmpty()) { output.setStackInSlot(i, InvUtils.injectToNearTile(world, pos, stack)); break; }
        }
        if (!running || area == null) return;
        // Only one fuel module generates per real tick, regardless of the scan budget.
        for (int i = 0; i < tools.getSizeInventory(); i++) {
            ItemStack stack = tools.getStackInSlot(i);
            if (stack.getItem() instanceof ItemFuelModule fuel && fuel.enabled()) {
                ((IModuleItem) fuel).apply(stack, this).foreach(module -> { module.invoke(new IModule.Tick(this)); return scala.runtime.BoxedUnit.UNIT; });
                break;
            }
        }
        if (world.getTotalWorldTime() % interval(enchantments.getOrDefault(EfficiencyID, 0)) != 0) return;
        requestTicket();
        for (int checked = 0; checked < SCAN_BUDGET && next < area.size(); checked++) {
            BlockPos target = area.at(next);
            loadTarget(target);
            if (!world.isBlockLoaded(target)) { status = "qp.mini.unloaded"; break; }
            IBlockState state = world.getBlockState(target);
            if (target.equals(pos) || state.getBlock().isAir(state, world, target) || state.getMaterial().isLiquid() || TilePump.isLiquid(state)
                    || state.getBlockHardness(world, target) < 0 || !filters.allows(state)) { next++; continue; }
            var player = QuarryFakePlayer.get((WorldServer) world, target);
            int toolSlot = -1;
            try {
                player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
                boolean harvestable = false;
                for (int pass = 0; pass < 2 && !harvestable; pass++) for (int i = 0; i <= tools.getSizeInventory(); i++) {
                    ItemStack tool = i == tools.getSizeInventory() ? ItemStack.EMPTY : tools.getStackInSlot(i);
                    if (!tool.isEmpty() && !isTool(tool)) continue;
                    if (pass == 0 && (tool.isEmpty() || tool.getDestroySpeed(state) <= 1)) continue;
                    player.setHeldItem(EnumHand.MAIN_HAND, tool);
                    if (ForgeHooks.canHarvestBlock(state.getBlock(), player, world, target)) { toolSlot = i; harvestable = true; break; }
                }
                if (!harvestable) { next++; status = "qp.mini.no_tool"; continue; }
                ItemStack tool = player.getHeldItemMainhand();
                int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool);
                boolean silk = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0;
                long energy = Math.max(MJToMicroMJ, PowerManager.calcEnergyBreak(state.getBlockHardness(world, target), silk ? -1 : fortune,
                        enchantments.getOrDefault(UnbreakingID, 0)));
                if ((!Config.content().noEnergy() && getStoredEnergy() < energy)
                        || useEnergy(energy, energy, false, EnergyUsage.BREAK_BLOCK) != energy) { status = "qp.mini.no_energy"; break; }
                BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, target, state, player);
                if (!world.canMineBlockBody(player, target) || MinecraftForge.EVENT_BUS.post(event)) { next++; status = "qp.mini.protected"; continue; }
                var block = state.getBlock();
                var tile = world.getTileEntity(target);
                if (!block.removedByPlayer(state, world, target, player, true)) { next++; continue; }
                PowerManager.useEnergy(this, energy, EnergyUsage.BREAK_BLOCK);
                block.onPlayerDestroy(world, target, state);
                block.harvestBlock(world, player, target, state, tile, tool.copy());
                if (event.getExpToDrop() > 0) block.dropXpOnBlockBreak(world, target, event.getExpToDrop());
                if (!tool.isEmpty()) tool.onBlockDestroyed(world, state, target, player);
                if (toolSlot < tools.getSizeInventory() && tool.isEmpty()) tools.setInventorySlotContents(toolSlot, ItemStack.EMPTY);
                tools.markDirty();
                for (EntityItem item : world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(target).grow(1))) {
                    if (!item.isDead) { store(item.getItem()); item.setDead(); }
                }
                next++; status = "qp.mini.running";
                break;
            } finally { player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY); }
        }
        if (next >= area.size()) setRunning(false, "qp.mini.complete");
        else changed();
    }

    private void store(ItemStack stack) {
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(output, stack, false);
        if (!remainder.isEmpty()) InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY() + 1, pos.getZ(), remainder);
    }

    public void dropInventories() {
        InventoryHelper.dropInventoryItems(world, pos, tools);
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack stack = output.extractItem(i, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    private void changed() {
        if (loading) return;
        markDirty();
        if (world != null && !world.isRemote) { IBlockState state = world.getBlockState(pos); world.notifyBlockUpdate(pos, state, state, 2); }
    }

    private static NBTTagCompound inventoryTag(InventoryBasic inventory) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < stacks.size(); i++) stacks.set(i, inventory.getStackInSlot(i));
        return ItemStackHelper.saveAllItems(new NBTTagCompound(), stacks);
    }

    private static void readInventory(InventoryBasic inventory, NBTTagCompound tag) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(tag, stacks);
        for (int i = 0; i < stacks.size(); i++) inventory.setInventorySlotContents(i, stacks.get(i));
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (area != null) tag.setTag("area", area.write());
        tag.setLong("next", next); tag.setBoolean("running", running); tag.setBoolean("redstone", redstone); tag.setBoolean("showArea", showArea);
        tag.setString("status", status); tag.setTag("tools", inventoryTag(tools)); tag.setTag("filters", inventoryTag(filters));
        tag.setTag("output", output.serializeNBT());
        NBTTagCompound ench = new NBTTagCompound(); enchantments.forEach((id, level) -> ench.setInteger(id.toString(), level)); tag.setTag("ench", ench);
        return tag;
    }

    @Override public void readFromNBT(NBTTagCompound tag) {
        loading = true;
        try {
            super.readFromNBT(tag);
            area = MiniQuarryArea.read(tag.getCompoundTag("area"));
            next = area == null ? 0 : Math.max(0, Math.min(area.size(), tag.getLong("next")));
            running = tag.getBoolean("running") && area != null && next < area.size();
            redstone = tag.getBoolean("redstone"); showArea = tag.getBoolean("showArea");
            status = tag.hasKey("status") ? tag.getString("status") : "qp.mini.idle";
            readInventory(tools, tag.getCompoundTag("tools")); readInventory(filters, tag.getCompoundTag("filters"));
            if (tag.hasKey("output")) output.deserializeNBT(tag.getCompoundTag("output"));
            if (output.getSlots() != 27) output.setSize(27);
            enchantments.clear(); NBTTagCompound ench = tag.getCompoundTag("ench");
            for (String key : ench.getKeySet()) try { enchantments.put(Integer.parseInt(key), Math.max(0, ench.getInteger(key))); } catch (NumberFormatException ignored) { }
            G_ReInit();
        } finally { loading = false; }
    }

    @Override protected Symbol getSymbol() { return SYMBOL; }
    @Override protected boolean isWorking() { return running; }
    @Override public String getDebugName() { return "tile.mini_quarry.name"; }
    @Override public List<? extends ITextComponent> getDebugMessages() { return List.of(new TextComponentTranslation(status)); }
    @Override public Map<Integer, Integer> getEnchantments() { return Map.copyOf(enchantments); }
    @Override public void setEnchantment(short id, short level) { enchantments.put((int) id, Math.max(0, (int) level)); }
    @Override public void G_ReInit() { configure(100000000L, 10000000000L); }

    @Override public boolean hasCapability(Capability<?> capability, EnumFacing facing) { return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing); }
    @Override public <T> T getCapability(Capability<T> capability, EnumFacing facing) { return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandler) : super.getCapability(capability, facing); }
    @Override public AxisAlignedBB getRenderBoundingBox() { return showArea && area != null ? area.box().union(new AxisAlignedBB(pos)) : super.getRenderBoundingBox(); }

    @Override public void requestTicket() {
        if (ticket == null && world != null && !world.isRemote && running) {
            ticket = ForgeChunkManager.requestTicket(QuarryPlus.INSTANCE, world, ForgeChunkManager.Type.NORMAL);
            if (ticket != null) setTileData(ticket, pos);
        }
    }
    @Override public void forceChunkLoading(ForgeChunkManager.Ticket ticket) {
        if (!running) { ForgeChunkManager.releaseTicket(ticket); return; }
        this.ticket = ticket;
        ForgeChunkManager.forceChunk(ticket, new ChunkPos(pos));
    }
    private void loadTarget(BlockPos target) {
        if (ticket == null) return;
        ChunkPos chunk = new ChunkPos(target), machine = new ChunkPos(pos);
        if (!chunk.equals(loadedTarget)) {
            if (loadedTarget != null && !loadedTarget.equals(machine)) ForgeChunkManager.unforceChunk(ticket, loadedTarget);
            ForgeChunkManager.forceChunk(ticket, chunk); loadedTarget = chunk;
        }
    }
    private void releaseTicket() { if (ticket != null) { ForgeChunkManager.releaseTicket(ticket); ticket = null; loadedTarget = null; } }
    @Override public void invalidate() { releaseTicket(); super.invalidate(); }
    @Override public void onChunkUnload() { releaseTicket(); super.onChunkUnload(); }
}
