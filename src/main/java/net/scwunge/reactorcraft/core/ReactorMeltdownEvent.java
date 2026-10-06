package net.scwunge.reactorcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/** Posted on the NeoForge event bus when a reactor core melts down (before anything is destroyed), for other mods to react to. */
public class ReactorMeltdownEvent extends Event {
    private final Level level;
    private final BlockPos pos;

    public ReactorMeltdownEvent(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    public Level level() {
        return level;
    }

    public BlockPos pos() {
        return pos;
    }
}
