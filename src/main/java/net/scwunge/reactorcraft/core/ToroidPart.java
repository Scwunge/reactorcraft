package net.scwunge.reactorcraft.core;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** A piece of the fusion ring (FusionReactorToroidPart): knows which piece plasma goes on to. */
public interface ToroidPart {
    @Nullable
    ToroidPart nextPart(Level level);
}
