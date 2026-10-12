package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.factory.PosGuiData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

/** Retains the existing screen IDs while MUI supplies the window/network lifecycle. */
public final class QuarryGuiData extends PosGuiData {
    public final int guiId;

    public QuarryGuiData(EntityPlayer player, BlockPos pos, int guiId) {
        super(player, pos);
        this.guiId = guiId;
    }

    public int getGuiId() { return guiId; }

    @Override public net.minecraft.tileentity.TileEntity getTileEntity() {
        // PosGuiData uses a shared mutable position; client and integrated-server threads must not share it.
        return getWorld().getTileEntity(getBlockPos());
    }
}
