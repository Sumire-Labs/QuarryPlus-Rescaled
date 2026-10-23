package com.yogpc.qp.modules;

/** A stack adds at most 64 extra work passes; each pass still pays its normal energy cost. */
public final class RepeatTickBudget {
    public static final int MAX_MODULES = 64;

    private RepeatTickBudget() {}

    public static int repetitions(int count) {
        return 1 + Math.max(0, Math.min(MAX_MODULES, count));
    }

    public static int workLimit(int base, int count) {
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, base) * repetitions(count));
    }
}
