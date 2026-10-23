package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.yogpc.qp.tile.TileMiniQuarry;

final class MiniQuarryPanel {
    static ModularPanel build(MuiContext ctx) {
        TileMiniQuarry tile = (TileMiniQuarry) ctx.tile;
        ModularPanel panel = MuiWidgets.panel(ctx, "tile.mini_quarry.name", 230, 240);
        MuiWidgets.localizedText(ctx, panel, "mini_status", 8, 19, 150, tile::statusKey);
        MuiWidgets.button(ctx, panel, "mini_select", 166, 18, 56, 18, "qp.mini.select_area", packet -> tile.selectArea());
        panel.child(IKey.lang("qp.mini.tools").asWidget().pos(8, 32).width(140));
        MuiWidgets.button(ctx, panel, "mini_run", 104, 44, 60, 18, "qp.mini.toggle", packet -> tile.toggleRunning());
        MuiWidgets.button(ctx, panel, "mini_area", 166, 44, 56, 18, "qp.mini.show_area", packet -> tile.toggleAreaDisplay());
        panel.child(IKey.lang("qp.mini.allow").asWidget().pos(8, 68).width(210));
        panel.child(IKey.lang("qp.mini.deny").asWidget().pos(8, 100).width(210));
        panel.child(IKey.lang("container.inventory").asWidget().pos(34, 138));
        MuiWidgets.localizedText(ctx, panel, "mini_energy", 8, 228, 214, () -> "qp.mini.energy",
                () -> Long.toString(tile.getStoredEnergy() / 100000L));
        MuiWidgets.slots(ctx, panel);
        return panel;
    }
}
