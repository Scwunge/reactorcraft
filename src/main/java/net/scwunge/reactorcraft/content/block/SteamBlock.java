package net.scwunge.reactorcraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.SteamTurbine;
import net.scwunge.reactorcraft.registry.ReactorBlocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Steam (BlockSteam): a hot gas that rises from a steam grate, drifting sideways when blocked and dying out as it climbs.
 * Directly under a turbine it is drawn through it. Its four flags are the original's metadata bits: {@link #PERSISTENT} (does
 * not die as it rises), {@link #POWERED} (can drive a turbine), {@link #AMMONIA} (ammonia gas: poisons), {@link #MOVED} (has
 * moved sideways). It burns what it touches.
 */
public class SteamBlock extends Block {
    public static final BooleanProperty PERSISTENT = BooleanProperty.create("persistent");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty AMMONIA = BooleanProperty.create("ammonia");
    public static final BooleanProperty MOVED = BooleanProperty.create("moved");
    private static final int TICK_RATE = 2;
    private static final AtomicLong CLEAR_UNTIL = new AtomicLong();

    public SteamBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PERSISTENT, false).setValue(POWERED, false).setValue(AMMONIA, false).setValue(MOVED, false));
    }

    /** What the grate makes: steam that can drive a turbine (the original's metadata 3, or 7 for ammonia). */
    public static BlockState grateSteam(boolean ammonia) {
        return ReactorBlocks.STEAM.get().defaultBlockState().setValue(PERSISTENT, true).setValue(POWERED, true).setValue(AMMONIA, ammonia);
    }

    /** The /clearsteam command: every steam block vanishes for the next second. */
    public static void clearAllSteam() {
        CLEAR_UNTIL.set(System.currentTimeMillis() + 1000);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PERSISTENT, POWERED, AMMONIA, MOVED);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, TICK_RATE);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (pos.getY() > level.getMaxBuildHeight() - 1 || System.currentTimeMillis() < CLEAR_UNTIL.get()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            return;
        }
        move(state, level, pos, random);
        if (level.getBlockState(pos).is(this)) {
            level.scheduleTick(pos, this, TICK_RATE);
        }
    }

    private void move(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(pos.above()).is(ReactorBlocks.STEAM_SCRUBBER.get())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            return;
        }
        if (level.getBlockEntity(pos.above()) instanceof SteamTurbine turbine) {
            Direction dir = turbine.steamMovement();
            int d = turbine.totalStages() - turbine.stage();
            BlockPos target = pos.offset(dir.getStepX() * d, dir.getStepY(), dir.getStepZ() * d);
            if (canMoveInto(level, target)) {
                level.setBlock(target, transmitted(state, dir), 2);
            }
            // steam that has nowhere to go past the turbine simply condenses away
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            return;
        }
        if (canMoveInto(level, pos.above())) {
            if (state.getValue(PERSISTENT) || Chance.of(random, 80)) {
                level.setBlock(pos.above(), transmitted(state, Direction.UP), 2);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            return;
        }
        List<Direction> sideways = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        Collections.shuffle(sideways, new java.util.Random(random.nextLong()));
        for (Direction dir : sideways) {
            BlockPos next = pos.relative(dir);
            if (canMoveInto(level, next)) {
                level.setBlock(next, transmitted(state, dir), 2);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                return;
            }
        }
    }

    /** getTransmittedMetadata: going up after a sideways move resets it to plain (or ammonia) steam; going sideways marks it moved. */
    private BlockState transmitted(BlockState state, Direction dir) {
        if (dir == Direction.UP) {
            return state.getValue(MOVED) ? state.setValue(MOVED, false).setValue(PERSISTENT, true).setValue(POWERED, false) : state;
        }
        return state.setValue(MOVED, true);
    }

    public boolean canMoveInto(Level level, BlockPos pos) {
        BlockState there = level.getBlockState(pos);
        if (there.isAir()) {
            return !there.is(this);
        }
        if (there.is(this) || !there.getFluidState().isEmpty()) {
            return false;
        }
        return there.canBeReplaced();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && !(entity instanceof ItemEntity || entity instanceof ExperienceOrb)) {
            entity.hurt(level.damageSources().inFire(), 1);
            if (state.getValue(AMMONIA) && entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
            }
        }
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
