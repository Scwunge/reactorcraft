package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;

/**
 * Centrifugal Turbine (TileEntityCentrifugalTurbine): the small turbine of the original, which has six stages but takes in no steam and gives no
 * power of its own (its torque and speed limits are zero), so it only stands as a model of one.
 */
public class CentrifugalTurbineBlockEntity extends TurbineCoreBlockEntity {
    public CentrifugalTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.CENTRIFUGAL_TURBINE.get(), pos, state);
    }

    @Override
    protected boolean intakeSteam() {
        return false;
    }

    @Override
    protected int maxStage() {
        return 5;
    }

    @Override
    protected float torqueFactor() {
        return 0;
    }

    @Override
    protected int consumedLubricant() {
        return 4;
    }

    @Override
    public int maxSpeed() {
        return 0;
    }

    @Override
    public int maxTorque() {
        return 0;
    }
}
