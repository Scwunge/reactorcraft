package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.reactorcraft.content.multi.MultiController;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;

import java.util.function.Supplier;

/**
 * The flywheel's and the generator's block: a plain block until the casing is built round it, then the model. A screwdriver turns it (a flywheel inside
 * its casing only turns to face the other way; a generator turns to face the side that is clicked, as the original did).
 */
public class FlywheelBlock extends TurbineCoreBlock {
    private final boolean faceClicked;

    public FlywheelBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type, boolean faceClicked) {
        super(properties, type);
        this.faceClicked = faceClicked;
        registerDefaultState(defaultBlockState().setValue(SolenoidBlock.FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SolenoidBlock.FORMED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(SolenoidBlock.FORMED) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Override
    protected BlockState turnedBy(BlockState state, Level level, BlockPos pos, BlockHitResult hit) {
        if (faceClicked) {
            return hit.getDirection().getAxis().isHorizontal() ? state.setValue(LOOK, hit.getDirection()) : state;
        }
        if (level.getBlockEntity(pos) instanceof MultiController controller && controller.isFormed()) {
            return state.setValue(LOOK, state.getValue(LOOK).getOpposite());
        }
        return super.turnedBy(state, level, pos, hit);
    }

    /** Shows the formed look (the model) or the plain block. */
    public static void showFormed(Level level, BlockPos pos, boolean formed) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(SolenoidBlock.FORMED) && state.getValue(SolenoidBlock.FORMED) != formed) {
            level.setBlock(pos, state.setValue(SolenoidBlock.FORMED, formed), 2);
        }
    }
}
