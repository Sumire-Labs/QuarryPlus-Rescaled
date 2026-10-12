package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Exposes an existing slot to MUI without losing its validation or custom inventory. */
public final class QuarryModularSlot extends ModularSlot {
    public final Slot original;
    public final int originalIndex;

    public QuarryModularSlot(Slot original, int index) {
        super(original.inventory instanceof net.minecraft.entity.player.InventoryPlayer playerInventory
                        ? new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(playerInventory) : new SlotInventory(original),
                original.inventory instanceof net.minecraft.entity.player.InventoryPlayer ? original.getSlotIndex() : 0);
        this.original = original;
        this.originalIndex = index;
    }

    @Override public ItemStack getStack() { return original.getStack(); }
    @Override public void putStack(ItemStack stack) { original.putStack(stack); }
    @Override public boolean isItemValid(ItemStack stack) { return original.isItemValid(stack); }
    @Override public boolean canTakeStack(EntityPlayer player) { return original.canTakeStack(player); }
    @Override public int getSlotStackLimit() { return original.getSlotStackLimit(); }
    @Override public int getItemStackLimit(ItemStack stack) { return original.getItemStackLimit(stack); }
    @Override public ItemStack decrStackSize(int amount) { return original.decrStackSize(amount); }
    @Override public ItemStack onTake(EntityPlayer player, ItemStack stack) { return original.onTake(player, stack); }

    private record SlotInventory(Slot slot) implements IItemHandlerModifiable {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int index) { return slot.getStack(); }
        @Override public void setStackInSlot(int index, ItemStack stack) { slot.putStack(stack); }
        @Override public int getSlotLimit(int index) { return slot.getSlotStackLimit(); }
        @Override public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
            // Container click/transfer logic handles mutations; this is used for slot validation.
            return stack;
        }
        @Override public ItemStack extractItem(int index, int amount, boolean simulate) {
            if (simulate) {
                ItemStack copy = slot.getStack().copy();
                copy.setCount(Math.min(amount, copy.getCount()));
                return copy;
            }
            return slot.decrStackSize(amount);
        }
    }
}
