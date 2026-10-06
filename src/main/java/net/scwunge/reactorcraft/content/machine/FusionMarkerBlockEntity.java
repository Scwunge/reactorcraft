package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.core.ToroidAim;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;

import java.util.List;

/**
 * Fusion Marker (TileEntityFusionMarker): does nothing but show, while it has a redstone signal, where the fusion ring and the solenoid are meant to
 * go round it (drawn by its renderer): the ring starts 14 blocks south of the marker.
 */
public class FusionMarkerBlockEntity extends ReactorMachineBlockEntity {
    /** getAimPoints: the way each of the ring's 40 pieces points, going round from the injector. */
    public static final List<ToroidAim> AIM_POINTS = List.of(
            ToroidAim.N, ToroidAim.N, ToroidAim.NNW1, ToroidAim.NNW2, ToroidAim.NNW3, ToroidAim.NW, ToroidAim.WNW1, ToroidAim.WNW2, ToroidAim.WNW3,
            ToroidAim.W, ToroidAim.W, ToroidAim.W, ToroidAim.WSW1, ToroidAim.WSW2, ToroidAim.WSW3, ToroidAim.SW, ToroidAim.SSW1, ToroidAim.SSW2,
            ToroidAim.SSW3, ToroidAim.S, ToroidAim.S, ToroidAim.S, ToroidAim.SSE1, ToroidAim.SSE2, ToroidAim.SSE3, ToroidAim.SE, ToroidAim.ESE1,
            ToroidAim.ESE2, ToroidAim.ESE3, ToroidAim.E, ToroidAim.E, ToroidAim.E, ToroidAim.ENE1, ToroidAim.ENE2, ToroidAim.ENE3, ToroidAim.NE,
            ToroidAim.NNE1, ToroidAim.NNE2, ToroidAim.NNE3, ToroidAim.N);

    public FusionMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.FUSION_MARKER.get(), pos, state, 0);
    }

    @Override
    protected void tickServer() {
    }

    public boolean renderLines() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }
}
