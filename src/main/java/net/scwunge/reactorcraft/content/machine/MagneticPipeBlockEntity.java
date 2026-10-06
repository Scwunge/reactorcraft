package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.power.Shockable;

/**
 * Magnetic Pipe (TileEntityMagneticPipe): carries fusion plasma between the heater and the injector. It only holds together while it has charge
 * (from a Van de Graaff discharge, shared along the pipe): with none, plasma melts it into lava. Charge leaks away, and goes in one
 * flash into water next to the pipe.
 */
public class MagneticPipeBlockEntity extends ReactorPipeBlockEntity implements Shockable, NeutronTile {
    /** Fluid temperature (C) above which an uncharged pipe melts. */
    public static final int MELT_TEMPERATURE = 5000;

    private int charge;

    public MagneticPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.MAGNETIC_PIPE.get(), pos, state);
    }

    public static boolean interacts(BlockEntity other) {
        return other instanceof PlasmaPort;
    }

    @Override
    public boolean isValidFluid(Fluid fluid) {
        return fluid == ReactorFluids.FUSION_PLASMA.get();
    }

    @Override
    protected boolean isInteractable(BlockEntity other) {
        return interacts(other);
    }

    @Override
    protected void tickServer() {
        super.tickServer();
        shareCharge();
        decayCharge();
        if (charge <= 0) {
            charge = 0;
            if (!pipe.isEmpty() && pipe.getFluid().getFluid().getFluidType().getTemperature() > MELT_TEMPERATURE + 273) {
                melt();
            }
        }
    }

    private void melt() {
        if (ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get() && WorldSafety.mayChange(level, worldPosition, owner())) {
            level.setBlockAndUpdate(worldPosition, Blocks.LAVA.defaultBlockState());
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1F, 1F);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.LAVA, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    /** A quarter of the difference goes to each magnetic pipe beside it that has less. */
    private void shareCharge() {
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof MagneticPipeBlockEntity other) {
                int dq = charge - other.charge;
                if (dq > 0) {
                    other.charge += dq / 4;
                    charge -= dq / 4;
                }
            }
        }
    }

    private void decayCharge() {
        for (Direction dir : Direction.values()) {
            if (level.getFluidState(worldPosition.relative(dir)).is(FluidTags.WATER)) {
                if (charge > 0 && level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5 + dir.getStepX() * 0.5,
                            worldPosition.getY() + 0.5 + dir.getStepY() * 0.5, worldPosition.getZ() + 0.5 + dir.getStepZ() * 0.5, 6, 0.2, 0.2, 0.2, 0.1);
                }
                charge = 0;
                return;
            }
        }
        charge = charge > 1 ? (int) (charge * 0.99) : 0;
    }

    public int charge() {
        return charge;
    }

    public void setCharge(int charge) {
        this.charge = charge;
    }

    @Override
    public void onDischarge(int charge, double range) {
        this.charge += (int) Math.pow(charge, 1.1);
    }

    @Override
    public int getMinDischarge() {
        return 256;
    }

    @Override
    public boolean canDischargeLongRange() {
        return true;
    }

    @Override
    public float getAimX() {
        return 0.5F;
    }

    @Override
    public float getAimY() {
        return 0.5F;
    }

    @Override
    public float getAimZ() {
        return 0.5F;
    }

    @Override
    public boolean onNeutron(net.scwunge.reactorcraft.content.entity.NeutronEntity neutron, net.minecraft.world.level.Level level, BlockPos pos) {
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Charge", charge);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        charge = tag.getInt("Charge");
    }
}
