package com.yogpc.qp.modules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RepeatTickBudgetTest {
    @Test void noModulePreservesExistingBudget() {
        assertEquals(1, RepeatTickBudget.workLimit(1, 0));
        assertEquals(6, RepeatTickBudget.workLimit(6, 0));
    }

    @Test void stackSizeMultipliesExistingAcceleration() {
        assertEquals(2, RepeatTickBudget.workLimit(1, 1));
        assertEquals(65, RepeatTickBudget.workLimit(1, 64));
        assertEquals(390, RepeatTickBudget.workLimit(6, 64));
        assertEquals(260, RepeatTickBudget.workLimit(4, 64));
    }

    @Test void malformedCountsAreBoundedAndMultiplicationDoesNotOverflow() {
        assertEquals(1, RepeatTickBudget.repetitions(Integer.MIN_VALUE));
        assertEquals(65, RepeatTickBudget.repetitions(Integer.MAX_VALUE));
        assertEquals(0, RepeatTickBudget.workLimit(0, 64));
        assertEquals(0, RepeatTickBudget.workLimit(-1, 64));
        assertEquals(Integer.MAX_VALUE, RepeatTickBudget.workLimit(Integer.MAX_VALUE, 64));
    }
}
