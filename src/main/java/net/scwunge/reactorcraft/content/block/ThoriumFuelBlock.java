package net.scwunge.reactorcraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.RadiationEffects;
import net.scwunge.reactorcraft.core.RadiationIntensity;

/**
 * Spilled thorium fuel salt (BlockThoriumFuel): a pool of up to eight layers of radioactive molten salt that a fuel dump valve lets out of an
 * overheated core. It does not flow, is very hard to remove, and every so often contaminates the air around it.
 */
public class ThoriumFuelBlock extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 8);
    private static final VoxelShape[] SHAPES = new VoxelShape[9];

    static {
        for (int i = 1; i <= 8; i++) {
            SHAPES[i] = Block.box(0, 0, 0, 16, i * 2, 16);
        }
    }

    public ThoriumFuelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 8));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    /** canOverwrite: whether a dump valve may pour into a spot (nothing there that is not soft). */
    public static boolean canOverwrite(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LEVEL)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (Chance.of(random, 0.005)) {
            RadiationEffects.contaminateArea(level, pos.above(random.nextInt(2)), 2, 0.25F, 0, false, RadiationIntensity.MODERATE);
        }
    }
}
