package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public final class MuiContext {
    public final QuarryGuiData data;
    public final EntityPlayer player;
    public final TileEntity tile;
    public final Container legacy;
    public final PanelSyncManager sync;
    public final UISettings settings;

    public MuiContext(QuarryGuiData data, Container legacy, PanelSyncManager sync, UISettings settings) {
        this.data = data;
        this.player = data.getPlayer();
        this.tile = data.getTileEntity();
        this.legacy = legacy;
        this.sync = sync;
        this.settings = settings;
    }

    public EnumFacing facing() {
        int base = data.guiId >= QuarryGuiFactory.ADV_FLUID_BASE
                ? QuarryGuiFactory.ADV_FLUID_BASE : QuarryGuiFactory.PUMP_FILTER_BASE;
        return EnumFacing.byIndex(data.guiId - base);
    }
}
