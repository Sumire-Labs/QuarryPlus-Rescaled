package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.api.UIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.yogpc.qp.QuarryPlus;
import com.yogpc.qp.QuarryPlusI;
import com.yogpc.qp.gui.GuiHandler;
import com.yogpc.qp.block.BlockController;
import com.yogpc.qp.tile.*;
import com.yogpc.qp.container.ContainerQuarryModule.HasModuleInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** All game GUI entry points share one server-authoritative MUI factory. */
public final class QuarryGuiFactory implements UIFactory<QuarryGuiData> {
    public static final QuarryGuiFactory INSTANCE = new QuarryGuiFactory();
    public static final int PUMP_FILTER_BASE = 100;
    public static final int ADV_FLUID_BASE = 120;
    public static final int CONTROLLER = 140;
    private static boolean registered;

    public static void register() {
        if (!registered) { GuiManager.registerFactory(INSTANCE); registered = true; }
    }

    public static void open(EntityPlayer player, int id, World world, int x, int y, int z) {
        open(player, id, world, new BlockPos(x, y, z));
    }

    public static void open(EntityPlayer player, int id, World world, BlockPos pos) {
        if (player instanceof FakePlayer || world != player.world) return;
        QuarryGuiData data = new QuarryGuiData(player, pos, id);
        if (!INSTANCE.canInteractWith(player, data)) return;
        if (world.isRemote) requestClient(data);
        else if (player instanceof EntityPlayerMP serverPlayer) GuiManager.open(INSTANCE, data, serverPlayer);
    }

    @SideOnly(Side.CLIENT)
    private static void requestClient(QuarryGuiData data) { GuiManager.openFromClient(INSTANCE, data); }

    @Override public String getFactoryName() { return "quarryplus:machine"; }

    @Override public ModularPanel createPanel(QuarryGuiData data, PanelSyncManager sync, UISettings settings) {
        if (!canInteractWith(data.getPlayer(), data)) throw new IllegalArgumentException("Invalid QuarryPlus GUI target: id="
                + data.guiId + ", pos=" + data.getBlockPos() + ", tile=" + data.getTileEntity()
                + ", distance=" + data.getSquaredDistance(data.getPlayer()) + ", client=" + data.isClient());
        Container original = data.guiId == QuarryPlusI.guiIdStatus() ? new StatusInventory(data.getPlayer()) : data.guiId >= PUMP_FILTER_BASE
                ? new EmptyContainer()
                : GuiHandler.instance().getServerGuiElement(data.guiId, data.getPlayer(), data.getWorld(), data.getX(), data.getY(), data.getZ());
        if (original == null) throw new IllegalArgumentException("Unknown QuarryPlus GUI: " + data.guiId);
        MuiContext ctx = new MuiContext(data, original, sync, settings);
        // The legacy container already supplies player slots (or intentionally has none).
        // Register the group so MUI does not append another 36 hidden inventory slots.
        sync.registerSlotGroup(new com.cleanroommc.modularui.widgets.slot.PlayerSlotGroup(
                com.cleanroommc.modularui.widgets.slot.PlayerSlotGroup.NAME));
        settings.customContainer(() -> new QuarryModularContainer(original));
        settings.canInteractWith(player -> canInteract(ctx));
        ModularPanel panel = CorePanels.build(ctx);
        if (panel == null) panel = InventoryPanels.build(ctx);
        if (panel == null) panel = MachinePanels.build(ctx);
        if (panel == null) panel = FilterPanels.build(ctx);
        if (panel == null) throw new IllegalArgumentException("No ModularUI panel for GUI " + data.guiId);
        return panel;
    }

    public static boolean canInteract(MuiContext ctx) {
        return INSTANCE.canInteractWith(ctx.player, ctx.data) && ctx.legacy.canInteractWith(ctx.player)
                && ctx.data.getTileEntity() == ctx.tile;
    }

    @Override public boolean canInteractWith(EntityPlayer player, QuarryGuiData data) {
        if (player != data.getPlayer() || player.isDead || !player.isEntityAlive()) return false;
        if (data.guiId == QuarryPlusI.guiIdListTemplate()) {
            return player.inventory.getCurrentItem().getItem() == QuarryPlusI.itemTemplate();
        }
        if (!data.getWorld().isBlockLoaded(data.getBlockPos()) || data.getSquaredDistance(player) > 64) return false;
        if (data.guiId == QuarryPlusI.guiIdMover()) return data.getWorld().getBlockState(data.getBlockPos()).getBlock() == QuarryPlusI.blockMover();
        if (data.guiId == CONTROLLER) return data.getWorld().getBlockState(data.getBlockPos()).getBlock() instanceof BlockController;
        TileEntity tile = data.getTileEntity();
        if (tile == null || tile.isInvalid()) return false;
        int id = data.guiId;
        if (id == QuarryPlusI.guiIdWorkbench()) return tile instanceof TileWorkbench;
        if (id == QuarryPlusI.guiIdFList() || id == QuarryPlusI.guiIdSList() || id == QuarryPlusI.guiIdQuarryYLevel()) return tile instanceof TileBasic;
        if (id == QuarryPlusI.guiIdPlacer()) return tile instanceof TilePlacer;
        if (id == QuarryPlusI.guiIdAdvQuarry() || id == QuarryPlusI.guiIdAdvQuarryYLevel() || (id >= ADV_FLUID_BASE && id < ADV_FLUID_BASE + 6)) return tile instanceof TileAdvQuarry;
        if (id == QuarryPlusI.guiIdAdvPump()) return tile instanceof TileAdvPump;
        if (id == QuarryPlusI.guiIdMoverFromBook()) return tile instanceof TileBookMover;
        if (id == QuarryPlusI.guiIdSolidQuarry()) return tile instanceof TileSolidQuarry;
        if (id == QuarryPlusI.guiIdQuarry2YLevel() || id == QuarryPlusI.guiIdStatus()) return tile instanceof TileQuarry2;
        if (id == QuarryPlusI.guiIdQuarryModule()) return tile instanceof HasModuleInventory;
        if (id == QuarryPlusI.guiIdFiller()) return tile instanceof TileFiller;
        return id >= PUMP_FILTER_BASE && id < PUMP_FILTER_BASE + 6 && tile instanceof TilePump;
    }

    @SideOnly(Side.CLIENT)
    @Override public ModularScreen createScreen(QuarryGuiData data, ModularPanel panel) {
        return new ModularScreen(QuarryPlus.modID, panel);
    }

    @SideOnly(Side.CLIENT)
    @Override public com.cleanroommc.modularui.api.IMuiScreen createScreenWrapper(
            com.cleanroommc.modularui.screen.ModularContainer container, ModularScreen screen) {
        int id = ((QuarryGuiData) container.getGuiData()).guiId;
        if (id == QuarryPlusI.guiIdWorkbench()) return new MuiRecipeGui.Workbench(container, screen);
        if (id == QuarryPlusI.guiIdMoverFromBook()) return new MuiRecipeGui.BookMover(container, screen);
        return new com.cleanroommc.modularui.screen.GuiContainerWrapper(container, screen);
    }

    @Override public void writeGuiData(QuarryGuiData data, PacketBuffer packet) {
        packet.writeBlockPos(data.getBlockPos());
        packet.writeVarInt(data.guiId);
    }

    @Override public QuarryGuiData readGuiData(EntityPlayer player, PacketBuffer packet) {
        return new QuarryGuiData(player, packet.readBlockPos(), packet.readVarInt());
    }

    private static final class EmptyContainer extends Container {
        @Override public boolean canInteractWith(EntityPlayer player) { return true; }
    }

    private static final class StatusInventory extends Container {
        StatusInventory(EntityPlayer player) {
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
                addSlotToContainer(new Slot(player.inventory, 9 + row * 9 + col, 8 + col * 18, 144 + row * 18));
            for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player.inventory, col, 8 + col * 18, 202));
        }
        @Override public boolean canInteractWith(EntityPlayer player) { return true; }
        @Override public net.minecraft.item.ItemStack transferStackInSlot(EntityPlayer player, int index) { return net.minecraft.item.ItemStack.EMPTY; }
    }
}
