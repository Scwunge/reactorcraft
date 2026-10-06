package net.scwunge.reactorcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** A part a heat pipe can move heat into or out of (the original's HeatConduction). */
public interface HeatConduction extends Temperatured {
    /** Heat per degree, in the same units as a pipe's (copper by the density of iron). */
    default double heatEnergyPerDegree() {
        return HeatPipes.CAPACITY;
    }

    /** Whether a pipe may push heat into it. */
    boolean allowExternalHeating();

    /** Whether a pipe may draw heat out of it. */
    boolean allowHeatExtraction();

    default int ambientTemperature(Level level, BlockPos pos) {
        return Thermal.ambient(level, pos);
    }
}
