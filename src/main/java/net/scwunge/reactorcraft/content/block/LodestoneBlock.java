package net.scwunge.reactorcraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.scwunge.reactorcraft.ReactorConfig;

/**
 * A block of lodestone. With a redstone signal it trickles energy into whatever sits on top: a little on each random tick
 * and a little whenever a neighbour changes (the original's RF trick, now FE).
 */
public class LodestoneBlock extends Block {
    public LodestoneBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        charge(level, pos, false);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            charge(level, pos, true);
        }
    }

    private static void charge(Level level, BlockPos pos, boolean forced) {
        if (!level.hasNeighborSignal(pos)) {
            return;
        }
        IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.above(), Direction.DOWN);
        if (target != null) {
            int amount = Mth.ceil(ReactorConfig.LODESTONE_FE_MULTIPLIER.get() * (forced ? 1 : 2));
            target.receiveEnergy(amount, false);
        }
    }
}
