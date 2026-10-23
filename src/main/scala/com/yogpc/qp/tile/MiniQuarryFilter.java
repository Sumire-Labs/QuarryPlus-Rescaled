package com.yogpc.qp.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/** Nine allow samples and nine deny samples. Deny wins; empty allow means all harvestable blocks. */
public final class MiniQuarryFilter extends InventoryBasic {
    public static final int ROW = 9;
    public MiniQuarryFilter() { super("qp.mini.filters", false, ROW * 2); }

    @Override public boolean isItemValidForSlot(int index, ItemStack stack) { return false; }
    @Override public int getInventoryStackLimit() { return 1; }

    @Override public void setInventorySlotContents(int index, ItemStack stack) {
        ItemStack sample = stack.getItem() instanceof ItemBlock ? stack.copy() : ItemStack.EMPTY;
        if (!sample.isEmpty()) sample.setCount(1);
        super.setInventorySlotContents(index, sample);
    }

    private boolean matches(int slot, IBlockState state) {
        ItemStack sample = getStackInSlot(slot);
        return !sample.isEmpty() && ((ItemBlock) sample.getItem()).getBlock() == state.getBlock()
                && sample.getMetadata() == state.getBlock().damageDropped(state);
    }

    public boolean allows(IBlockState state) {
        boolean anyAllow = false, allowed = false;
        for (int i = 0; i < ROW; i++) {
            anyAllow |= !getStackInSlot(i).isEmpty();
            allowed |= matches(i, state);
            if (matches(i + ROW, state)) return false;
        }
        return !anyAllow || allowed;
    }
}
