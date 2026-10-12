package com.sumirelabs.quarryplus.gui.mui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class MuiClientInput {
    private MuiClientInput() {}
    static void write(PacketBuffer packet, int mouseButton) {
        packet.writeInt(mouseButton);
        packet.writeBoolean(GuiScreen.isShiftKeyDown());
        packet.writeBoolean(GuiScreen.isCtrlKeyDown());
    }
}
