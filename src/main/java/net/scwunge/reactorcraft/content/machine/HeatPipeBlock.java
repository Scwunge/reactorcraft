package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.core.HeatConduction;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;

import java.util.function.Supplier;

/** A heat pipe (BlockHeatPipe): a thin pipe that joins to other heat pipes and to parts that give or take heat, and burns what touches it when hot. */
public class HeatPipeBlock extends SteamLineBlock {
    public HeatPipeBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type) {
        super(properties, type);
    }

    @Override
    protected boolean connects(LevelAccessor level, BlockPos neighborPos, BlockState neighbor, Direction dir) {
        if (neighbor.getBlock() instanceof HeatPipeBlock) {
            return true;
        }
        return level.getBlockEntity(neighborPos) instanceof HeatConduction conduction
                && (conduction.allowExternalHeating() || conduction.allowHeatExtraction());
    }

    /** Heat pipes carry heat, not steam. */
    @Override
    public boolean connectsSteam(BlockState state, Direction side) {
        return false;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HeatPipeBlockEntity pipe && pipe.getTemperature() >= 100) {
            entity.hurt(level.damageSources().inFire(), pipe.getTemperature() / 100);
        }
    }
}
