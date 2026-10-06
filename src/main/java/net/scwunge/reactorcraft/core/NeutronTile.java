package net.scwunge.reactorcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;

/** A block entity that reacts to neutrons entering its block. */
public interface NeutronTile {
    /** Returns true if the neutron is absorbed. */
    boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos);
}
