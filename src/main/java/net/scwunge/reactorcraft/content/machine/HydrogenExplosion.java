package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.RadiationShield;
import net.scwunge.reactorcraft.core.WorldSafety;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A hydrogen explosion (the original's HydrogenExplosion): an ordinary explosion, but first every solid block close by that
 * is not shielding is flung into the air as flying debris. Honours claims and mobGriefing for each block.
 */
public final class HydrogenExplosion {
    private HydrogenExplosion() {
    }

    public static void detonate(Level level, BlockPos center, float power, @Nullable UUID owner) {
        if (level.isClientSide || !WorldSafety.griefingAllowed(level)) {
            return;
        }
        int r = (int) power + 2;
        Vec3 middle = Vec3.atCenterOf(center);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (pos.getCenter().distanceTo(middle) > power + 0.5) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            RadiationShield shield = RadiationShield.of(state);
            if (shield != null && !Chance.of(level.random, shield.radiationDeflectChance)) {
                continue;
            }
            if (!canFling(level, pos, state) || !WorldSafety.mayChange(level, pos, owner)) {
                continue;
            }
            BlockPos at = pos.immutable();
            FallingBlockEntity debris = FallingBlockEntity.fall(level, at, state);
            // never settles quickly, and drops nothing
            debris.time = -10000;
            debris.dropItem = false;
            Vec3 away = debris.position().subtract(middle);
            debris.setDeltaMovement(away.x * 0.3 * level.random.nextDouble(), away.y * 0.3 * level.random.nextDouble() + 1.5,
                    away.z * 0.3 * level.random.nextDouble());
            debris.hurtMarked = true;
        }
        level.explode(null, middle.x, middle.y, middle.z, power, false, Level.ExplosionInteraction.BLOCK);
    }

    private static boolean canFling(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.is(Blocks.BEDROCK) || state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.getRenderShape() == RenderShape.MODEL && state.isSolidRender(level, pos) && state.getDestroySpeed(level, pos) >= 0;
    }
}
