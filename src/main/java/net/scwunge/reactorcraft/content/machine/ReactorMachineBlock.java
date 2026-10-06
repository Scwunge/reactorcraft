package net.scwunge.reactorcraft.content.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A ReactorCraft machine block. {@link #LOOK} is the way the placer was looking (the original's
 * get4SidedMetadataFromPlayerLook), which turns the model and picks the output side. Buckets and canisters fill and
 * empty the machine's tanks; otherwise right-clicking opens its GUI.
 */
public class ReactorMachineBlock extends BaseEntityBlock {
    public static final DirectionProperty LOOK = HorizontalDirectionalBlock.FACING;

    private final Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type;
    private final VoxelShape shape;
    private final boolean modelled;
    private final boolean clientTicks;

    public ReactorMachineBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type,
                               boolean modelled, boolean clientTicks) {
        this(properties, type, Shapes.block(), modelled, clientTicks);
    }

    public ReactorMachineBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type,
                               VoxelShape shape, boolean modelled, boolean clientTicks) {
        super(properties);
        this.type = type;
        this.shape = shape;
        this.modelled = modelled;
        this.clientTicks = clientTicks;
        registerDefaultState(stateDefinition.any().setValue(LOOK, Direction.SOUTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        throw new UnsupportedOperationException("machine blocks are not data-driven");
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOOK);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(LOOK, context.getHorizontalDirection());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof ReactorBlockEntity machine) {
            machine.setOwner(player.getUUID());
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(LOOK, rotation.rotate(state.getValue(LOOK)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(LOOK)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return modelled ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return clientTicks ? createTickerHelper(blockEntityType, type.get(), ReactorBlockEntity::clientTick) : null;
        }
        return createTickerHelper(blockEntityType, type.get(), ReactorBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof ReactorMachineBlockEntity machine) {
            IFluidHandler tanks = machine.fluidHandler(null);
            if (tanks != null && FluidUtil.getFluidHandler(stack).isPresent()) {
                if (FluidUtil.interactWithFluidHandler(player, hand, tanks)) {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ReactorMachineBlockEntity machine && machine.hasMenu()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                ReactorMenu.open(serverPlayer, machine);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ReactorMachineBlockEntity machine) {
            machine.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
