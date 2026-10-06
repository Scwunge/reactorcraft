package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.blockentity.CrystallizerBlockEntity;

/** Nuclear Waste Duct (TileEntityWastePipe): carries liquid nuclear waste from a thorium reactor to a crystallizer, and nothing else. */
public class WastePipeBlockEntity extends ReactorPipeBlockEntity implements NeutronTile {
    public WastePipeBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.WASTE_PIPE.get(), pos, state);
    }

    public static boolean interacts(BlockEntity other) {
        return other instanceof ThoriumCoreBlockEntity || other instanceof CrystallizerBlockEntity;
    }

    @Override
    public boolean isValidFluid(Fluid fluid) {
        return fluid == ReactorFluids.NUCLEAR_WASTE.get();
    }

    @Override
    protected boolean isInteractable(BlockEntity other) {
        return interacts(other);
    }

    @Override
    public boolean onNeutron(net.scwunge.reactorcraft.content.entity.NeutronEntity neutron, net.minecraft.world.level.Level level, BlockPos pos) {
        return false;
    }
}
