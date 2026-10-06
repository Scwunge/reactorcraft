package net.scwunge.reactorcraft.core;

import net.minecraft.core.Direction;

/** A turbine that steam blocks pass through (what BlockSteam asks of the turbine core above it). */
public interface SteamTurbine {
    /** The way steam flows through the turbine. */
    Direction steamMovement();

    /** How many turbine blocks the shaft has in all. */
    int totalStages();

    /** Which of them this block is (0 is the first, where steam goes in). */
    int stage();
}
