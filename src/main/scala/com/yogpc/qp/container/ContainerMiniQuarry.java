package com.yogpc.qp.container;

import com.yogpc.qp.tile.TileMiniQuarry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class ContainerMiniQuarry extends Container {
    private final TileMiniQuarry tile;
    public ContainerMiniQuarry(EntityPlayer player, TileMiniQuarry tile) {
        this.tile = tile;
        for (int i = 0; i < 5; i++) addSlotToContainer(new SlotTile(tile.tools, i, 8 + i * 18, 45));
        for (int i = 0; i < 18; i++) addSlotToContainer(new Slot(tile.filters, i, 8 + i % 9 * 18, i < 9 ? 82 : 114) {
            @Override public boolean isItemValid(ItemStack stack) { return false; }
            @Override public boolean canTakeStack(EntityPlayer player) { return false; }
            @Override public int getSlotStackLimit() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(player.inventory, 9 + row * 9 + col, 34 + col * 18, 150 + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player.inventory, col, 34 + col * 18, 208));
    }
    @Override public boolean canInteractWith(EntityPlayer player) { return tile.usable(player); }
    @Override public ItemStack slotClick(int slot, int button, ClickType type, EntityPlayer player) {
        if (!canInteractWith(player)) return ItemStack.EMPTY;
        if (slot >= 5 && slot < 23) {
            if (!player.world.isRemote && type == ClickType.PICKUP && (button == 0 || button == 1)) {
                tile.filters.setInventorySlotContents(slot - 5, button == 1 ? ItemStack.EMPTY : player.inventory.getItemStack());
                detectAndSendChanges();
            }
            return ItemStack.EMPTY;
        }
        return super.slotClick(slot, button, type, player);
    }
    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size() || !canInteractWith(player) || (index >= 5 && index < 23)) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        ItemStack stack = slot.getStack(), before = stack.copy();
        if (index < 5) {
            if (!mergeItemStack(stack, 23, inventorySlots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!tile.tools.isItemValidForSlot(0, stack)) return ItemStack.EMPTY;
            if (!mergeItemStack(stack, 0, 5, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
        slot.onTake(player, stack);
        return before;
    }
    @Override public boolean canMergeSlot(ItemStack stack, Slot slot) { return slot.inventory != tile.filters && super.canMergeSlot(stack, slot); }
}
