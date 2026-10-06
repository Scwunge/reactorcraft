package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.ReactorPart;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;

/**
 * Neutron Reflector (TileEntityNeutronReflector): slows a neutron to thermal speed, and then a quarter of the time sends it back the way
 * it came (sideways), otherwise soaks it up half the time.
 */
public class ReflectorBlockEntity extends ReactorMachineBlockEntity implements NeutronTile, ReactorPart {
    public ReflectorBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.REFLECTOR.get(), pos, state, 0);
    }

    @Override
    protected void tickServer() {
    }

    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        neutron.moderate();
        if (level.random.nextInt(4) == 0) {
            neutron.reverseHorizontally();
            return false;
        }
        return level.random.nextBoolean();
    }
}
