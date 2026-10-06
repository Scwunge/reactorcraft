package net.scwunge.reactorcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/** Posted on the NeoForge event bus when a Central Control block drops all its control rods. */
public class ScramEvent extends Event {
    private final Level level;
    private final BlockPos cpu;
    private final int temperature;

    public ScramEvent(Level level, BlockPos cpu, int temperature) {
        this.level = level;
        this.cpu = cpu;
        this.temperature = temperature;
    }

    public Level level() {
        return level;
    }

    public BlockPos cpu() {
        return cpu;
    }

    public int temperature() {
        return temperature;
    }
}
