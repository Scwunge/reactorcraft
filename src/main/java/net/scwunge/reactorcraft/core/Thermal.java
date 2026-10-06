package net.scwunge.reactorcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.rotarycraft.power.Ambient;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Temperature helpers from DragonAPI's ReikaWorldHelper: the temperature of the surroundings (shared with RotaryCraft so
 * heat balances between the two mods), whether a block can lose heat to open air, and what very hot or very cold blocks
 * do to the blocks around them.
 */
public final class Thermal {
    private Thermal() {
    }

    public static int ambient(Level level, BlockPos pos) {
        return Ambient.temperature(level, pos);
    }

    /** Ambient temperature for reactor parts: the original caps it at 95 C outside the Nether. */
    public static int reactorAmbient(Level level, BlockPos pos) {
        int t = ambient(level, pos);
        return level.dimensionType().ultraWarm() ? t : Math.min(t, 95);
    }

    /** True if any neighbour is air or has no collision box (the original also counted pipes and cables). */
    public static boolean isExposedToAir(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos at = pos.relative(dir);
            if (!level.isLoaded(at)) {
                continue;
            }
            BlockState state = level.getBlockState(at);
            if (state.isAir() || state.getCollisionShape(level, at).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Heats or chills the blocks up to three away along each axis: freezes water below 0 C, melts snow and ice, sets fire
     * to wool (600 C) and wood (450 C), glasses sand and soil (900 C) and melts stone into lava (1500 C, obsidian 1800 C,
     * metal 2000 C). Each change honours mobGriefing and claims; {@code owner} is who the change is made in the name of.
     */
    public static void heatEnvironment(Level level, BlockPos pos, int temperature, @Nullable UUID owner) {
        if (level.isClientSide || !ReactorConfig.HOT_BLOCKS_AFFECT_WORLD.get() || !WorldSafety.griefingAllowed(level)) {
            return;
        }
        if (temperature < 0) {
            for (Direction dir : Direction.values()) {
                BlockPos at = pos.relative(dir);
                if (level.isLoaded(at) && level.getFluidState(at).is(Fluids.WATER) && level.getFluidState(at).isSource()
                        && WorldSafety.mayChange(level, at, owner)) {
                    level.setBlockAndUpdate(at, Blocks.ICE.defaultBlockState());
                }
            }
        }
        for (Direction dir : Direction.values()) {
            for (int d = 1; d < 4; d++) {
                BlockPos at = pos.relative(dir, d);
                if (level.isLoaded(at)) {
                    apply(level, at, temperature, owner);
                }
            }
        }
    }

    private static void apply(Level level, BlockPos at, int temperature, @Nullable UUID owner) {
        BlockState state = level.getBlockState(at);
        if (state.isAir()) {
            return;
        }
        Block result = null;
        boolean ignite = false;
        if (state.is(BlockTags.ICE)) {
            result = temperature >= 0 ? Blocks.WATER : null;
        } else if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
            result = temperature >= 0 ? Blocks.AIR : null;
        } else if (state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS)) {
            ignite = temperature >= 600;
        } else if (isWood(state)) {
            ignite = temperature >= 450;
        } else if (state.is(BlockTags.SAND) || state.is(BlockTags.DIRT) || state.is(Blocks.GRAVEL)) {
            result = temperature >= 900 ? Blocks.GLASS : null;
        } else if (meltable(state, temperature)) {
            result = Blocks.LAVA;
        }
        if (result != null) {
            if (WorldSafety.mayChange(level, at, owner)) {
                level.setBlockAndUpdate(at, result.defaultBlockState());
            }
        } else if (ignite) {
            ignite(level, at, owner);
        }
    }

    private static boolean isWood(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS);
    }

    /** ReikaWorldHelper.isMeltable: obsidian at 1800 C, stone at 1500 C, metal blocks at 2000 C. */
    private static boolean meltable(BlockState state, int temperature) {
        if (state.is(Blocks.BEDROCK)) {
            return false;
        }
        if (state.is(Blocks.OBSIDIAN)) {
            return temperature >= 1800;
        }
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.COBBLESTONE)) {
            return temperature >= 1500;
        }
        if (state.is(Blocks.IRON_BLOCK) || state.is(Blocks.GOLD_BLOCK) || state.is(Blocks.COPPER_BLOCK)) {
            return temperature >= 2000;
        }
        return false;
    }

    /** ReikaWorldHelper.ignite: fire in every air block around a flammable block. */
    private static void ignite(Level level, BlockPos at, @Nullable UUID owner) {
        if (!level.getBlockState(at).isFlammable(level, at, Direction.UP)) {
            return;
        }
        for (Direction dir : Direction.values()) {
            BlockPos fire = at.relative(dir);
            if (level.isLoaded(fire) && level.getBlockState(fire).isAir() && WorldSafety.mayChange(level, fire, owner)) {
                level.setBlockAndUpdate(fire, Blocks.FIRE.defaultBlockState());
            }
        }
    }
}
