package net.scwunge.reactorcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Where nuclear waste and the reactor's machines reach into the neutron and radiation systems. Those are milestones 4
 * and 5, so for now every hook does nothing; each is listed in PLAN.md's deviations log so none is forgotten.
 */
public final class RadiationHooks {
    private RadiationHooks() {
    }

    /** M4: decaying waste in a container fires a waste neutron out of {@code pos} in {@code direction} (EntityNeutron, NeutronType.WASTE). */
    public static void leakNeutron(Level level, BlockPos pos, Direction direction) {
    }

    /** M5: a waste storage block gives every living thing in sight within {@code range} blocks MODERATE radiation effects. */
    public static void sickenMobs(Level level, BlockPos pos, int range) {
    }

    /** M5: a waste container meltdown contaminates the area (RadiationEffects.contaminateArea, LETHAL, radius 9). */
    public static void contaminateArea(Level level, BlockPos pos) {
    }

    /** M5: carrying nuclear waste gives HIGHLEVEL radiation effects. */
    public static void holdingWaste(Level level, LivingEntity holder) {
    }
}
