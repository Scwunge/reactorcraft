package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.core.Thermal;
import net.scwunge.reactorcraft.core.WorldSafety;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The once-a-second temperature step the electrolyzer and the fluid synthesizer share (their updateTemperature): settle
 * one degree at a time towards the ambient temperature, which water beside the machine halves, ice quarters (and melts),
 * fire raises by 200 and lava by 600; snow and ice beside a machine over 100 C melt.
 */
final class MachineHeat {
    private MachineHeat() {
    }

    static int step(Level level, BlockPos pos, int temperature, int maxTemperature, @Nullable UUID owner) {
        boolean world = ReactorConfig.HOT_BLOCKS_AFFECT_WORLD.get();
        int ambient = Thermal.ambient(level, pos);
        if (adjacentFluid(level, pos, true)) {
            ambient /= 2;
        }
        Direction ice = adjacentBlock(level, pos, Blocks.ICE);
        if (ice != null) {
            if (ambient > 0) {
                ambient /= 4;
            }
            melt(level, pos.relative(ice), Blocks.WATER, owner, world);
        }
        if (adjacentFire(level, pos)) {
            ambient += 200;
        }
        if (adjacentFluid(level, pos, false)) {
            ambient += 600;
        }
        if (temperature > ambient) {
            temperature--;
        }
        if (temperature > ambient * 2) {
            temperature--;
        }
        if (temperature < ambient) {
            temperature++;
        }
        if (temperature * 2 < ambient) {
            temperature++;
        }
        if (temperature > maxTemperature) {
            temperature = maxTemperature;
        }
        if (temperature > 100) {
            Direction snow = adjacentBlock(level, pos, Blocks.SNOW_BLOCK);
            if (snow != null) {
                melt(level, pos.relative(snow), Blocks.AIR, owner, world);
            }
            Direction hotIce = adjacentBlock(level, pos, Blocks.ICE);
            if (hotIce != null) {
                melt(level, pos.relative(hotIce), Blocks.WATER, owner, world);
            }
        }
        return temperature;
    }

    private static void melt(Level level, BlockPos at, Block into, @Nullable UUID owner, boolean world) {
        if (world && !level.isClientSide && WorldSafety.mayChange(level, at, owner)) {
            level.setBlockAndUpdate(at, into.defaultBlockState());
        }
    }

    /** ReikaWorldHelper.checkForAdjBlock: the first side with that block. */
    @Nullable
    private static Direction adjacentBlock(Level level, BlockPos pos, Block block) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).is(block)) {
                return dir;
            }
        }
        return null;
    }

    private static boolean adjacentFire(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState state = level.getBlockState(pos.relative(dir));
            if (state.is(BlockTags.FIRE)) {
                return true;
            }
        }
        return false;
    }

    /** ReikaWorldHelper.checkForAdjMaterial for water or lava. */
    private static boolean adjacentFluid(Level level, BlockPos pos, boolean water) {
        for (Direction dir : Direction.values()) {
            if (level.getFluidState(pos.relative(dir)).is(water ? FluidTags.WATER : FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }
}
