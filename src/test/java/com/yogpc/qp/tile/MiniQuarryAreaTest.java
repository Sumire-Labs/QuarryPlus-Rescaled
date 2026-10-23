package com.yogpc.qp.tile;

import java.util.HashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MiniQuarryAreaTest {
    @Test void scansTopDownAndVisitsEachLayerTwice() {
        var area = new MiniQuarryArea(2, 10, 3, 3, 11, 4);
        assertEquals(16, area.size());
        assertEquals(new BlockPos(2, 11, 3), area.at(0));
        assertEquals(area.at(0), area.at(4));
        assertEquals(10, area.at(8).getY());
        var visits = new HashMap<BlockPos, Integer>();
        for (long i = 0; i < area.size(); i++) visits.merge(area.at(i), 1, Integer::sum);
        assertEquals(8, visits.size());
        assertTrue(visits.values().stream().allMatch(count -> count == 2));
        assertThrows(IndexOutOfBoundsException.class, () -> area.at(area.size()));
        assertThrows(IndexOutOfBoundsException.class, () -> area.at(-1));
    }

    @Test void persistsNegativeCoordinatesAndRejectsMalformedAreas() {
        var area = new MiniQuarryArea(-20, 1, -15, -2, 255, 0);
        assertEquals(area, MiniQuarryArea.read(area.write()));
        assertNull(MiniQuarryArea.read(new NBTTagCompound()));
        assertThrows(IllegalArgumentException.class, () -> new MiniQuarryArea(0, 0, 0, 1, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> new MiniQuarryArea(0, 1, 0, 256, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> new MiniQuarryArea(0, 2, 0, 1, 1, 1));
    }
}
