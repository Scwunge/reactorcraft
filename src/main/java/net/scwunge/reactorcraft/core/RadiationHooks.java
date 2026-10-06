package net.scwunge.reactorcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;

/**
 * Where nuclear waste and the reactor's machines reach into the radiation systems (milestone 5), so for now those hooks do
 * nothing; each is listed in PLAN.md's deviations log so none is forgotten.
 */
public final class RadiationHooks {
    private RadiationHooks() {
    }

    /** A waste container gives off a waste neutron in {@code direction}. */
    public static void leakNeutron(Level level, BlockPos pos, Direction direction) {
        if (!level.isClientSide) {
            level.addFreshEntity(new NeutronEntity(level, pos, direction, NeutronType.WASTE));
        }
    }

    /** M5: a neutron hits a living thing (RadiationEffects.applyPulseEffects, MODERATE). */
    public static void pulse(LivingEntity target) {
        RadiationEffects.applyPulseEffects(target, RadiationIntensity.MODERATE);
    }

    /** M5: a neutron absorbed in a block may transform it (RadiationEffects.transformBlock, MODERATE). */
    public static void transformBlock(Level level, BlockPos pos) {
        RadiationEffects.transformBlock(level, pos, RadiationIntensity.MODERATE, null);
    }

    /** M5: a neutron absorbed in a block may leave a little radiation (contaminateArea, radius 1, LOWLEVEL), if there is little nearby already. */
    public static void spawnLowRadiation(Level level, BlockPos pos) {
        if (level.getEntitiesOfClass(net.scwunge.reactorcraft.content.entity.RadiationEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(8)).size() < 3) {
            RadiationEffects.contaminateArea(level, pos, 1, 1, 0, false, RadiationIntensity.LOWLEVEL);
        }
    }

    /** M5: carrying plutonium gives MODERATE radiation effects (ItemPlutonium.onUpdate). */
    public static void holdingPlutonium(Level level, net.minecraft.world.entity.player.Player player) {
        RadiationEffects.applyEffects(player, RadiationIntensity.MODERATE);
    }

    /** M5: a reactor meltdown contaminates the surroundings (RadiationEffects.contaminateArea, radius 32, LETHAL). */
    public static void contaminateReactor(Level level, BlockPos pos) {
        RadiationEffects.contaminateArea(level, pos, 32, 8, 2, true, RadiationIntensity.LETHAL);
    }

    /** M5: a waste storage block gives every living thing in sight within {@code range} blocks MODERATE radiation effects. */
    public static void sickenMobs(Level level, BlockPos pos, int range) {
        RadiationEffects.irradiateNearby(level, pos, range, RadiationIntensity.MODERATE);
    }

    /** M5: a waste container meltdown contaminates the area (RadiationEffects.contaminateArea, LETHAL, radius 9). */
    public static void contaminateArea(Level level, BlockPos pos) {
        RadiationEffects.contaminateArea(level, pos, 9, 4, 1.5, true, RadiationIntensity.LETHAL);
    }

    /** M5: carrying nuclear waste gives HIGHLEVEL radiation effects. */
    public static void holdingWaste(Level level, LivingEntity holder) {
        RadiationEffects.applyEffects(holder, RadiationIntensity.HIGHLEVEL);
    }
}
