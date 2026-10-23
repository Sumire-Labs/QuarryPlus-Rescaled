package com.yogpc.qp.render;

import com.yogpc.qp.tile.TileMiniQuarry;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public final class RenderMiniQuarry extends TileEntitySpecialRenderer<TileMiniQuarry> {
    @Override public void render(TileMiniQuarry tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (!tile.showArea() || tile.area() == null) return;
        GlStateManager.disableTexture2D();
        GlStateManager.glLineWidth(2);
        try {
            RenderGlobal.drawSelectionBoundingBox(tile.area().box().offset(x - tile.getPos().getX(), y - tile.getPos().getY(), z - tile.getPos().getZ()), 1, 1, 0, 1);
        } finally { GlStateManager.glLineWidth(1); GlStateManager.enableTexture2D(); }
    }
}
