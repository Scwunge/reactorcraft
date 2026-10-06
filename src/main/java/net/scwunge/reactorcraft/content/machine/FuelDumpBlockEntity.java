package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.content.block.ThoriumFuelBlock;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorFluids;

/**
 * Fuel Dump Valve (TileEntityFuelDump): the thorium core's safety valve. Under a core that has reached 1100 C it drains the core's fuel salt
 * into its own 2000 mB tank and lets it out below as pools of spilled fuel, 125 mB to a layer. If it stays full for ten seconds it blows.
 */
public class FuelDumpBlockEntity extends ReactorMachineBlockEntity {
    public static final int CAPACITY = 2000;
    public static final int PER_LAYER = 125;

    private final FluidTank tank;
    private int fullTicks;

    public FuelDumpBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.FUEL_DUMP.get(), pos, state, 0);
        tank = addTank("Tank", CAPACITY, s -> false);
    }

    @Override
    protected void tickServer() {
        if (level.getBlockEntity(worldPosition.above()) instanceof ThoriumCoreBlockEntity core
                && core.getTemperature() >= ThoriumCoreBlockEntity.FUEL_DUMP_TEMPERATURE && core.hasFuel()) {
            int room = CAPACITY - tank.getFluidAmount();
            if (room > 0) {
                int fuel = core.dumpFuel(room);
                if (fuel > 0) {
                    addLiquid(tank, ReactorFluids.LIFBE_FUEL.get(), fuel);
                }
                fullTicks = 0;
            } else {
                fullTicks++;
                if (fullTicks > 200) {
                    overload();
                    return;
                }
            }
        }
        if (tank.getFluidAmount() >= PER_LAYER && canDumpAt(worldPosition.below())) {
            dump();
        }
    }

    private boolean canDumpAt(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return ThoriumFuelBlock.canOverwrite(level, pos) || state.is(ReactorBlocks.THORIUM_FUEL.get()) && state.getValue(ThoriumFuelBlock.LEVEL) < 8;
    }

    private void dump() {
        BlockPos below = worldPosition.below();
        BlockState there = level.getBlockState(below);
        int layers = Math.min(8, tank.getFluidAmount() / PER_LAYER);
        int existing = there.is(ReactorBlocks.THORIUM_FUEL.get()) ? there.getValue(ThoriumFuelBlock.LEVEL) : 0;
        layers = Math.min(layers, 8 - existing);
        if (layers <= 0 || !WorldSafety.mayChange(level, below, owner())) {
            return;
        }
        removeLiquid(tank, layers * PER_LAYER);
        level.setBlockAndUpdate(below, ReactorBlocks.THORIUM_FUEL.get().defaultBlockState().setValue(ThoriumFuelBlock.LEVEL, existing + layers));
        fullTicks = 0;
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1F, 1F);
    }

    private void overload() {
        if (ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get() && WorldSafety.mayChange(level, worldPosition, owner())) {
            level.setBlockAndUpdate(worldPosition, ReactorBlocks.CORIUM_BLOCK.get().defaultBlockState());
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 3F, true, Level.ExplosionInteraction.BLOCK);
        } else {
            fullTicks = 0;
        }
    }

    public FluidTank tank() {
        return tank;
    }
}
