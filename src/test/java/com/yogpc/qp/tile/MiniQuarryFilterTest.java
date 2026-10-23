package com.yogpc.qp.tile;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MiniQuarryFilterTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void emptyAllowAcceptsEverythingButDenyTakesPriority() {
        var filter = new MiniQuarryFilter();
        assertTrue(filter.allows(Blocks.STONE.getDefaultState()));
        filter.setInventorySlotContents(0, new ItemStack(Blocks.STONE));
        assertFalse(filter.allows(Blocks.DIRT.getDefaultState()));
        filter.setInventorySlotContents(9, new ItemStack(Blocks.STONE));
        assertFalse(filter.allows(Blocks.STONE.getDefaultState()));
    }

    @Test void samplesCopyWithoutConsumptionAndDistinguishBlockVariants() {
        var filter = new MiniQuarryFilter();
        ItemStack granite = new ItemStack(Blocks.STONE, 32, 1);
        filter.setInventorySlotContents(0, granite);
        assertEquals(32, granite.getCount());
        assertEquals(1, filter.getStackInSlot(0).getCount());
        assertTrue(filter.allows(Blocks.STONE.getStateFromMeta(1)));
        assertFalse(filter.allows(Blocks.STONE.getDefaultState()));
        granite.setItemDamage(2);
        assertTrue(filter.allows(Blocks.STONE.getStateFromMeta(1)));
        filter.setInventorySlotContents(0, new ItemStack(Items.DIAMOND));
        assertTrue(filter.getStackInSlot(0).isEmpty());
    }
}
