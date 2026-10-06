package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;

/** Gas Duct (TileEntityGasDuct): carries gases, and only gases, from machine to machine; it does not join ordinary pipes. */
public class GasDuctBlockEntity extends ReactorPipeBlockEntity {
    public GasDuctBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.GAS_DUCT.get(), pos, state);
    }

    /** Whether a duct at {@code pos} deals with {@code other}: a machine that handles fluid, but not another kind of pipe. */
    public static boolean interacts(Level level, BlockPos pos, BlockEntity other) {
        return !(other instanceof ReactorPipeBlockEntity) && !(other instanceof net.scwunge.rotarycraft.blockentity.PipeBlockEntity)
                && level.getCapability(Capabilities.FluidHandler.BLOCK, other.getBlockPos(), null) != null;
    }

    @Override
    public boolean isValidFluid(Fluid fluid) {
        for (ReactorFluids.Entry entry : ReactorFluids.ALL) {
            if (entry.source.get() == fluid) {
                return entry.gas;
            }
        }
        return fluid.getFluidType().isLighterThanAir();
    }

    @Override
    protected boolean isInteractable(BlockEntity other) {
        return interacts(level, worldPosition, other);
    }
}
