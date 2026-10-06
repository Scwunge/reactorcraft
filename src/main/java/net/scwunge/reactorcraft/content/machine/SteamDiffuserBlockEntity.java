package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.core.SteamTile;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Steam Diffuser (TileEntitySteamDiffuser): draws steam out of the steam line it faces and turns it into steam as a fluid (80 times as much of it, by the
 * ratio of the volumes of steam and of reactor steam), for other machines, which it gives out of its back.
 */
public class SteamDiffuserBlockEntity extends ReactorMachineBlockEntity implements SteamTile {
    /** calculateConversionRatio (Moran and Shapiro): ceil(1.696 / 0.01272 * 0.6) */
    public static final int RATIO = 80;
    public static final int CAPACITY = 2_500_000;

    private final FluidTank tank;
    private final IFluidHandler out;
    private int steam;

    public SteamDiffuserBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.STEAM_DIFFUSER.get(), pos, state, 0);
        tank = addTank("Steam", CAPACITY, s -> false);
        out = FluidAccess.drainOnly(tank);
    }

    /** The side the steam line is on. */
    public Direction facing() {
        return getBlockState().getValue(ReactorMachineBlock.LOOK);
    }

    @Override
    protected void tickServer() {
        takeSteam();
        convertSteam();
    }

    private void takeSteam() {
        if (level.getBlockEntity(worldPosition.relative(facing())) instanceof SteamLineBlockEntity line) {
            int difference = line.getSteam() - steam;
            if (difference > 0) {
                int taken = difference / 4 + 1;
                steam += (int) (taken * line.workingFluid().efficiency);
                line.removeSteam(taken);
            }
        }
    }

    private void convertSteam() {
        if (steam > 0) {
            int amount = Math.min(1 + steam / 4, (CAPACITY - tank.getFluidAmount()) / RATIO);
            if (amount > 0) {
                addLiquid(tank, ReactorFluids.STEAM.get(), amount * RATIO * 1000);
                steam -= amount;
            }
        }
    }

    public FluidTank steamTank() {
        return tank;
    }

    @Override
    public int getSteam() {
        return steam;
    }

    /** Steam comes out of the back only. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == null || side == facing().getOpposite() ? out : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", steam);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        steam = tag.getInt("Energy");
    }
}
