package net.scwunge.reactorcraft.core;

import net.minecraft.util.RandomSource;

/** The original's dice (DragonAPI's ReikaRandomHelper), with the same quirks so the original's numbers mean the same. */
public final class Chance {
    private Chance() {
    }

    /**
     * doWithChance: values above 1 are percentages, values up to 1 are fractions (so exactly 1 always succeeds).
     */
    public static boolean of(RandomSource random, double chance) {
        if (chance >= 100) {
            return true;
        }
        if (chance > 1) {
            chance /= 100D;
        }
        if (chance >= 1) {
            return true;
        }
        if (chance <= 0) {
            return false;
        }
        return random.nextDouble() < chance;
    }

    /** doWithPercentChance: always a percentage. */
    public static boolean percent(RandomSource random, double percent) {
        if (percent >= 100) {
            return true;
        }
        if (percent <= 0) {
            return false;
        }
        return random.nextDouble() * 100 < percent;
    }
}
