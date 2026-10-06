package net.scwunge.reactorcraft.core;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** A block a steam line can join to. */
public interface SteamConnectable {
    /** Whether a steam line touching this block's {@code side} connects to it. */
    boolean connectsSteam(BlockState state, Direction side);
}
