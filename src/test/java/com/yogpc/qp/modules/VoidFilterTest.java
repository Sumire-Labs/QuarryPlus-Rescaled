package com.yogpc.qp.modules;

import java.util.List;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VoidFilterTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void matchesMetadataAndNbtButIgnoresCount() {
        ItemStack sample = new ItemStack(Items.DYE, 1, 4);
        sample.setStackDisplayName("Keep metadata and NBT distinct");
        ItemStack same = sample.copy();
        same.setCount(64);
        assertTrue(VoidFilter.matches(List.of(sample), same));
        assertFalse(VoidFilter.matches(List.of(sample), new ItemStack(Items.DYE, 1, 4)));
        ItemStack otherMeta = sample.copy();
        otherMeta.setItemDamage(5);
        assertFalse(VoidFilter.matches(List.of(sample), otherMeta));
        assertFalse(VoidFilter.matches(List.of(sample), new ItemStack(Items.DIAMOND)));
        assertFalse(VoidFilter.matches(List.of(sample), ItemStack.EMPTY));
    }

    @Test void roundTripPreservesSlotsAndCopiesSamplesWithoutConsumingItems() {
        ItemStack module = new ItemStack(Items.PAPER);
        module.setStackDisplayName("Existing module tags");
        ItemStack input = new ItemStack(Items.DYE, 32, 4);
        input.setStackDisplayName("Blue");
        NonNullList<ItemStack> samples = NonNullList.withSize(VoidFilter.SIZE, ItemStack.EMPTY);
        samples.set(17, input);
        VoidFilter.write(module, samples);
        assertEquals(32, input.getCount());
        input.setStackDisplayName("Changed after registration");
        var restored = VoidFilter.read(module.copy());
        assertEquals(VoidFilter.SIZE, restored.size());
        assertTrue(restored.get(0).isEmpty());
        assertEquals(1, restored.get(17).getCount());
        assertEquals("Blue", restored.get(17).getDisplayName());
        restored.get(17).setStackDisplayName("Changed returned copy");
        assertEquals("Blue", VoidFilter.read(module).get(17).getDisplayName());
        VoidFilter.write(module, NonNullList.withSize(VoidFilter.SIZE, ItemStack.EMPTY));
        assertEquals("Existing module tags", module.getDisplayName());
        assertTrue(VoidFilter.read(module).stream().allMatch(ItemStack::isEmpty));
    }

    @Test void ignoresInvalidSlotsAndEmptyFiltersNeverDeleteAnything() {
        ItemStack module = new ItemStack(Items.PAPER);
        NBTTagList entries = new NBTTagList();
        NBTTagCompound invalid = new ItemStack(Items.DIAMOND).writeToNBT(new NBTTagCompound());
        invalid.setInteger("Slot", -1);
        entries.appendTag(invalid);
        NBTTagCompound tooLarge = invalid.copy();
        tooLarge.setInteger("Slot", VoidFilter.SIZE);
        entries.appendTag(tooLarge);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("VoidFilter", entries);
        module.setTagCompound(tag);
        assertFalse(VoidFilter.matches(VoidFilter.read(module), new ItemStack(Items.DIAMOND)));
    }
}
