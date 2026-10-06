package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Steam Injector (TileEntitySteamInjector): one of the ring of casing blocks round the steam line that feeds a big turbine. It takes lubricant from a hose
 * or a bucket (never gives it back) and the first stage of the turbine draws on all the injectors of its ring.
 */
public class SteamInjectorBlockEntity extends ReactorMachineBlockEntity {
    public static final int CAPACITY = 1000;
    private static final TagKey<Fluid> LUBRICANT = FluidTags.create(net.minecraft.resources.ResourceLocation.parse("c:lubricant"));

    private final FluidTank lubricant;
    private final IFluidHandler lubricantIn;

    public SteamInjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.STEAM_INJECTOR.get(), pos, state, 0);
        lubricant = addTank("Lubricant", CAPACITY, s -> s.getFluid().is(LUBRICANT));
        lubricantIn = FluidAccess.fillOnly(lubricant);
    }

    @Override
    protected void tickServer() {
    }

    public int lubricant() {
        return lubricant.getFluidAmount();
    }

    public Fluid lubricantFluid() {
        return fluidOf(lubricant);
    }

    public void remove(int amount) {
        removeLiquid(lubricant, amount);
    }

    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return lubricantIn;
    }
}
