package com.yogpc.qp.container;

import com.yogpc.qp.modules.ItemVoidModule;
import com.yogpc.qp.modules.VoidFilter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/** Ghost slots copy the cursor without consuming it; the edited module stays locked in the hotbar. */
public final class ContainerVoidModule extends Container {
    private final int heldSlot;
    private final ItemStack module;
    private final InventoryBasic samples;

    public ContainerVoidModule(EntityPlayer player) {
        heldSlot = player.inventory.currentItem;
        module = player.inventory.getCurrentItem();
        samples = new InventoryBasic("item.void_module.name", false, VoidFilter.SIZE);
        NonNullList<ItemStack> stored = VoidFilter.read(module);
        for (int i = 0; i < VoidFilter.SIZE; i++) {
            samples.setInventorySlotContents(i, stored.get(i));
            addSlotToContainer(new Slot(samples, i, 8 + i % 9 * 18, 30 + i / 9 * 18) {
                @Override public boolean isItemValid(ItemStack stack) { return false; }
                @Override public boolean canTakeStack(EntityPlayer player) { return false; }
                @Override public int getSlotStackLimit() { return 1; }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(player.inventory, 9 + row * 9 + col, 8 + col * 18, 94 + row * 18));
        for (int col = 0; col < 9; col++) {
            final int index = col;
            addSlotToContainer(new Slot(player.inventory, col, 8 + col * 18, 152) {
                @Override public boolean canTakeStack(EntityPlayer p) { return index != heldSlot; }
                @Override public boolean isItemValid(ItemStack stack) { return index != heldSlot; }
            });
        }
    }

    @Override public boolean canInteractWith(EntityPlayer player) {
        ItemStack current = player.inventory.getCurrentItem();
        return player.inventory.currentItem == heldSlot && current.getItem() instanceof ItemVoidModule
                && ((ItemVoidModule) current.getItem()).enabled()
                && (player.world.isRemote || current == module);
    }

    @Override public ItemStack slotClick(int slot, int button, ClickType type, EntityPlayer player) {
        if (!canInteractWith(player)) return ItemStack.EMPTY;
        if (type == ClickType.SWAP && button == heldSlot) return ItemStack.EMPTY;
        if (slot >= 0 && slot < VoidFilter.SIZE) {
            if (type == ClickType.PICKUP && (button == 0 || button == 1) && !player.world.isRemote) {
                ItemStack cursor = player.inventory.getItemStack();
                samples.setInventorySlotContents(slot, button == 1 ? ItemStack.EMPTY : VoidFilter.sample(cursor));
                NonNullList<ItemStack> stored = NonNullList.withSize(VoidFilter.SIZE, ItemStack.EMPTY);
                for (int i = 0; i < stored.size(); i++) stored.set(i, samples.getStackInSlot(i));
                VoidFilter.write(module, stored);
                player.inventory.markDirty();
                detectAndSendChanges();
            }
            return ItemStack.EMPTY;
        }
        if (slot == VoidFilter.SIZE + 27 + heldSlot) return ItemStack.EMPTY;
        return super.slotClick(slot, button, type, player);
    }

    @Override public ItemStack transferStackInSlot(EntityPlayer player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canMergeSlot(ItemStack stack, Slot slot) { return slot.inventory != samples && super.canMergeSlot(stack, slot); }
}
