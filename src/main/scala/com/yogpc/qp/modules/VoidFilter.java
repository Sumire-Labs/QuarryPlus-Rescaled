package com.yogpc.qp.modules;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants;

/** Filter samples are copies, never inventories containing real items. */
public final class VoidFilter {
    public static final int SIZE = 18;
    private static final String KEY = "VoidFilter";

    private VoidFilter() {}

    public static NonNullList<ItemStack> read(ItemStack module) {
        NonNullList<ItemStack> samples = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        if (!module.hasTagCompound()) return samples;
        NBTTagList list = module.getTagCompound().getTagList(KEY, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int slot = entry.getInteger("Slot");
            if (slot >= 0 && slot < SIZE) samples.set(slot, sample(new ItemStack(entry)));
        }
        return samples;
    }

    public static void write(ItemStack module, List<ItemStack> samples) {
        NBTTagCompound tag = module.hasTagCompound() ? module.getTagCompound() : new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (int slot = 0; slot < Math.min(SIZE, samples.size()); slot++) {
            ItemStack sample = sample(samples.get(slot));
            if (sample.isEmpty()) continue;
            NBTTagCompound entry = sample.writeToNBT(new NBTTagCompound());
            entry.setInteger("Slot", slot);
            list.appendTag(entry);
        }
        if (list.isEmpty()) tag.removeTag(KEY);
        else tag.setTag(KEY, list);
        module.setTagCompound(tag.isEmpty() ? null : tag);
    }

    public static ItemStack sample(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof ItemVoidModule) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    public static boolean matches(List<ItemStack> samples, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return samples.stream().anyMatch(sample -> !sample.isEmpty()
                && ItemStack.areItemsEqual(sample, stack) && ItemStack.areItemStackTagsEqual(sample, stack));
    }
}
