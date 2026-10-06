package net.scwunge.reactorcraft.core;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/** A reactor part that a Central Control block (CPU) can take over (the original's LinkableReactorCore). */
public interface Linkable {
    /** Remembers the CPU that controls it. */
    void link(BlockPos cpu);

    /** The CPU that controls it, or null. */
    @Nullable
    BlockPos cpu();
}
