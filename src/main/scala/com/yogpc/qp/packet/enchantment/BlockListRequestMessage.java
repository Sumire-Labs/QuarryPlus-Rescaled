package com.yogpc.qp.packet.enchantment;

import com.yogpc.qp.QuarryPlus;
import com.yogpc.qp.container.ContainerEnchList;
import com.yogpc.qp.packet.IMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * To server only.
 * For container player opening.
 */
public class BlockListRequestMessage implements IMessage {

    int containerId;

    public static BlockListRequestMessage create(int containerId) {
        BlockListRequestMessage message = new BlockListRequestMessage();
        message.containerId = containerId;
        return message;
    }

    @Override
    public void fromBytes(PacketBuffer buffer) {
        containerId = buffer.readInt();
    }

    @Override
    public void toBytes(PacketBuffer buffer) {
        buffer.writeInt(containerId);
    }

    @Override
    public IMessage onReceive(IMessage message, MessageContext ctx) {
        EntityPlayer player = QuarryPlus.proxy.getPacketPlayer(ctx.netHandler);
        net.minecraft.inventory.Container opened = com.sumirelabs.quarryplus.gui.mui.QuarryModularContainer.unwrap(player.openContainer);
        if (opened.windowId == containerId && opened instanceof ContainerEnchList) {
            ContainerEnchList container = (ContainerEnchList) opened;
            return DiffMessage.create(container, container.tile.fortuneList, container.tile.silktouchList);
        }
        return null;
    }
}
