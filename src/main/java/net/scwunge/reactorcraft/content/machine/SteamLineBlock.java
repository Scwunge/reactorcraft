package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;
import net.scwunge.reactorcraft.core.SteamConnectable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A steam line (BlockSteamLine): a thin wool-wrapped pipe that joins to other steam lines and to the boilers, grates and
 * turbines it can feed, with one block-state flag per side.
 */
public class SteamLineBlock extends ReactorMachineBlock implements SteamConnectable {
    private static final Map<Direction, BooleanProperty> SIDES = new EnumMap<>(Direction.class);
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        for (Direction dir : Direction.values()) {
            SIDES.put(dir, BooleanProperty.create(dir.getName()));
        }
        ARMS.put(Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5));
        ARMS.put(Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16));
        ARMS.put(Direction.WEST, Block.box(0, 5, 5, 5, 11, 11));
        ARMS.put(Direction.EAST, Block.box(11, 5, 5, 16, 11, 11));
        ARMS.put(Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11));
        ARMS.put(Direction.UP, Block.box(5, 11, 5, 11, 16, 11));
    }

    public SteamLineBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type) {
        super(properties, type, false, false);
        BlockState state = defaultBlockState();
        for (BooleanProperty side : SIDES.values()) {
            state = state.setValue(side, false);
        }
        registerDefaultState(state);
    }

    public static BooleanProperty side(Direction dir) {
        return SIDES.get(dir);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        SIDES.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        for (Direction dir : Direction.values()) {
            state = state.setValue(SIDES.get(dir), connects(context.getLevel().getBlockState(context.getClickedPos().relative(dir)), dir));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        return state.setValue(SIDES.get(direction), connects(neighborState, direction));
    }

    /** Whether a steam line joins to {@code neighbor}, which is on side {@code dir} of it. */
    private boolean connects(BlockState neighbor, Direction dir) {
        return neighbor.getBlock() instanceof SteamConnectable connectable && connectable.connectsSteam(neighbor, dir.getOpposite());
    }

    @Override
    public boolean connectsSteam(BlockState state, Direction side) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(SIDES.get(dir))) {
                shape = Shapes.or(shape, ARMS.get(dir));
            }
        }
        return shape;
    }
}
