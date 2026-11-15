package com.angelofdeath.boss.logic;

/** Shared constants for the stasis (Grey Hole) freeze mechanic. */
public final class FreezeRules {
    /** Radius (blocks) in which a resting Grey Hole freezes everything. */
    public static final double GLOBAL_RADIUS = 12.0D;
    /** Stasis length of a True Grey Hole: 2 seconds. */
    public static final int FREEZE_TICKS = 40;

    private FreezeRules() {
    }
}
