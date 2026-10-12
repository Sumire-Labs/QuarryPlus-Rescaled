package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ProgressWidget;
import com.yogpc.qp.QuarryPlus;
import com.yogpc.qp.QuarryPlusI;
import com.yogpc.qp.container.ContainerMover;
import com.yogpc.qp.gui.TranslationKeys;
import com.yogpc.qp.tile.TileBookMover;
import com.yogpc.qp.tile.TilePlacer;
import com.yogpc.qp.tile.TileSolidQuarry;
import com.yogpc.qp.tile.TileWorkbench;
import net.minecraft.util.ResourceLocation;

/** Inventory screens retain the original containers' slot rules and domain operations. */
public final class InventoryPanels {
    private InventoryPanels() {}

    public static ModularPanel build(MuiContext ctx) {
        int id = ctx.data.guiId;
        if (id == QuarryPlusI.guiIdMover()) return mover(ctx);
        if (id == QuarryPlusI.guiIdMoverFromBook()) return bookMover(ctx);
        if (id == QuarryPlusI.guiIdSolidQuarry()) return solidQuarry(ctx);
        if (id == QuarryPlusI.guiIdPlacer()) return placer(ctx);
        if (id == QuarryPlusI.guiIdWorkbench()) return workbench(ctx);
        return null;
    }

    private static ModularPanel mover(MuiContext ctx) {
        ContainerMover mover = (ContainerMover) ctx.legacy;
        ModularPanel panel = MuiWidgets.panel(ctx, TranslationKeys.mover, 176, 166);
        StringSyncValue enchantment = new StringSyncValue(() -> mover.getEnchantment().map(e -> e.getName()).orElse(""));
        ctx.sync.syncValue("mover_enchantment", 0, enchantment);
        MuiWidgets.button(ctx, panel, "mover_up", 27, 18, 122, 18, TranslationKeys.UP,
                packet -> mover.setAvail(ContainerMover.D.UP));
        MuiWidgets.actionButton(ctx, panel, "mover_transfer", 27, 36, 122, 18,
                IKey.lang(enchantment::getStringValue), packet -> {
                    mover.detectAndSendChanges();
                    mover.moveEnchant();
                    mover.detectAndSendChanges();
                });
        MuiWidgets.button(ctx, panel, "mover_down", 27, 54, 122, 18, TranslationKeys.DOWN,
                packet -> mover.setAvail(ContainerMover.D.DOWN));
        inventoryLabel(panel, 72);
        MuiWidgets.slots(ctx, panel);
        return panel;
    }

    private static ModularPanel bookMover(MuiContext ctx) {
        TileBookMover tile = (TileBookMover) ctx.tile;
        ModularPanel panel = MuiWidgets.panel(ctx, TranslationKeys.moverfrombook, 176, 166);
        DoubleSyncValue progress = new DoubleSyncValue(() -> tile.isWorking() && tile.getMaxStored() > 0
                ? Math.min(1.0D, (double) tile.getStoredEnergy() / tile.getMaxStored()) : 0.0D);
        ctx.sync.syncValue("book_mover_progress", 0, progress);
        panel.child(new ProgressWidget().value(progress).texture(null,
                texture("bookmover").getSubArea(176f / 256, 14f / 256, 200f / 256, 30f / 256), 24)
                .pos(79, 35).size(24, 16));
        inventoryLabel(panel, 72);
        MuiWidgets.slots(ctx, panel);
        return panel;
    }

    private static ModularPanel solidQuarry(MuiContext ctx) {
        TileSolidQuarry tile = (TileSolidQuarry) ctx.tile;
        ModularPanel panel = MuiWidgets.panel(ctx, TranslationKeys.solidquarry, 176, 166);
        MuiWidgets.text(ctx, panel, "solid_quarry_fuel", 110, 6, 62, () -> "Fuel: " + tile.getFuelCount());
        inventoryLabel(panel, 72);
        MuiWidgets.slots(ctx, panel);
        return panel;
    }

    private static ModularPanel placer(MuiContext ctx) {
        TilePlacer tile = (TilePlacer) ctx.tile;
        ModularPanel panel = MuiWidgets.panel(ctx, tile.getName(), 176, 166);
        inventoryLabel(panel, 72);
        MuiWidgets.slots(ctx, panel);
        return panel;
    }

    private static ModularPanel workbench(MuiContext ctx) {
        TileWorkbench tile = (TileWorkbench) ctx.tile;
        ModularPanel panel = MuiWidgets.panel(ctx, TranslationKeys.workbench, 176, 222);
        DoubleSyncValue progress = new DoubleSyncValue(tile::getWorkProgress);
        IntSyncValue selection = new IntSyncValue(() -> tile.isWorking() ? tile.getRecipeIndex() : -1);
        IntSyncValue repeat = new IntSyncValue(() -> tile.workContinue ? 1 : 0);
        ctx.sync.syncValue("workbench_progress", 0, progress);
        ctx.sync.syncValue("workbench_selection", 0, selection);
        ctx.sync.syncValue("workbench_repeat", 0, repeat);
        panel.child(new ProgressWidget().value(progress).texture(null,
                texture("workbench").getSubArea(0, 222f / 256, 160f / 256, 226f / 256), 160)
                .pos(8, 78).size(160, 4));
        // The original recipe-slot clicks select, repeat, and cancel via ContainerWorkbench.slotClick.
        inventoryLabel(panel, 128);
        MuiWidgets.slots(ctx, panel);
        return panel;
    }

    static com.cleanroommc.modularui.api.drawable.IDrawable recipeBackground(MuiContext ctx, int recipe) {
        IntSyncValue selection = (IntSyncValue) ctx.sync.findSyncHandlerNullable("workbench_selection", 0);
        IntSyncValue repeat = (IntSyncValue) ctx.sync.findSyncHandlerNullable("workbench_repeat", 0);
        UITexture once = texture("workbench").getSubArea(176f / 256, 0, 192f / 256, 16f / 256);
        UITexture continuous = texture("workbench").getSubArea(192f / 256, 0, 208f / 256, 16f / 256);
        return new com.cleanroommc.modularui.drawable.DynamicDrawable(() -> selection.getIntValue() == recipe
                ? (repeat.getIntValue() == 0 ? once : continuous)
                : com.cleanroommc.modularui.drawable.GuiTextures.SLOT_ITEM);
    }

    private static void inventoryLabel(ModularPanel panel, int y) {
        panel.child(IKey.lang(TranslationKeys.CONTAINER_INVENTORY).asWidget().pos(8, y));
    }

    private static UITexture texture(String name) {
        return UITexture.fullImage(new ResourceLocation(QuarryPlus.modID, "textures/gui/" + name + ".png"));
    }
}
