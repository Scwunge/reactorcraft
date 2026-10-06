package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;

import java.util.function.Supplier;

/** A magnetic pipe or gas duct: a thin pipe that joins others of its kind and the machines it deals with. */
public class ReactorPipeBlock extends SteamLineBlock {
    /** Which neighbouring block entities this kind of pipe joins to (besides its own kind). */
    public interface Joins {
        boolean test(LevelAccessor level, BlockPos pos, BlockEntity other);
    }

    private final Joins joins;

    public ReactorPipeBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type, Joins joins) {
        super(properties, type);
        this.joins = joins;
    }

    @Override
    protected boolean connects(LevelAccessor level, BlockPos neighborPos, BlockState neighbor, Direction dir) {
        if (neighbor.getBlock() == this) {
            return true;
        }
        BlockEntity other = level.getBlockEntity(neighborPos);
        return other != null && joins.test(level, neighborPos, other);
    }

    /** These carry fluid, not steam. */
    @Override
    public boolean connectsSteam(BlockState state, Direction side) {
        return false;
    }
}
