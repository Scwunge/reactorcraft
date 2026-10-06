package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.content.entity.PlasmaEntity;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.StepTimer;
import net.scwunge.reactorcraft.core.ToroidAim;
import net.scwunge.reactorcraft.core.ToroidPart;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorTickets;
import net.scwunge.rotarycraft.power.Shockable;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import org.jetbrains.annotations.Nullable;


/**
 * Toroid Magnet (TileEntityToroidMagnet): one of the ring of magnets that hold fusion plasma on its way round. A screwdriver turns the way it steers.
 * Plasma inside it is bent towards the next magnet, but only while the ring is complete and has a solenoid, the magnet holds over 2500 charge (from
 * lightning or a Van de Graaff discharge, shared with the next magnet) and it has liquid nitrogen to cool it, ten mB for each bit of plasma. Plasma
 * that no magnet will take escapes the ring.
 */
public class ToroidMagnetBlockEntity extends ReactorMachineBlockEntity implements Shockable, ToroidPart, NeutronTile {
    public static final int MIN_CHARGE = 2500;
    public static final int NITROGEN_PER_PLASMA = 10;
    private static final int CHARGE_SHARE_DELAY = 4;

    private ToroidAim aim = ToroidAim.N;
    private boolean hasSolenoid;
    private boolean hasNext;
    private boolean active;
    private int charge;
    private int lastPlasma;
    private long age;
    private final StepTimer chargeTimer = new StepTimer(20);
    private final StepTimer recheckTimer = new StepTimer(20);
    private final FluidTank coolant;
    private final IFluidHandler nitrogenIn;

    public ToroidMagnetBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.TOROID_MAGNET.get(), pos, state, 0);
        coolant = addTank("Coolant", 8000, s -> s.getFluid() == RotaryFluids.LIQUID_NITROGEN.source.get());
        nitrogenIn = net.scwunge.rotarycraft.pipe.FluidAccess.fillOnly(coolant);
    }

    @Override
    protected void onFirstTick() {
        if (!hasSolenoid) {
            copySolenoidState();
        }
        hasNext = isComplete();
    }

    @Override
    protected void tickServer() {
        age++;
        steerPlasma();
        pullNitrogen();
        chargeTimer.update();
        if (age % CHARGE_SHARE_DELAY == 0) {
            shareCharge();
        }
        if (chargeTimer.checkCap()) {
            charge = charge <= 1 ? 0 : (int) (charge * 0.8);
            markForSync();
        }
        if (hasSolenoid) {
            recheckTimer.update();
            if (recheckTimer.checkCap()) {
                boolean now = isComplete();
                if (now != hasNext) {
                    hasNext = now;
                    markForSync();
                }
            }
        }
        if (lastPlasma > 0 && --lastPlasma == 0) {
            setActive(false);
        }
    }

    private void steerPlasma() {
        BlockPos target = worldPosition.offset(aim.xOffset, 0, aim.zOffset);
        for (PlasmaEntity plasma : level.getEntitiesOfClass(PlasmaEntity.class, new AABB(worldPosition))) {
            if (canAffect(plasma)) {
                plasma.resetEscapeTimer();
                plasma.setTarget(target.getX(), target.getZ());
                plasma.magnetOrdinal = aim.ordinal();
                removeLiquid(coolant, NITROGEN_PER_PLASMA);
                lastPlasma = 20;
                setActive(true);
            }
        }
    }

    /** canAffect: the ring is whole, has a solenoid, enough charge and coolant, and the plasma came from this magnet or one just before it. */
    public boolean canAffect(PlasmaEntity plasma) {
        if (!hasNext || !hasSolenoid || charge <= MIN_CHARGE || coolant.isEmpty()) {
            return false;
        }
        int o = aim.ordinal();
        int p = plasma.magnetOrdinal;
        if (p == -1) {
            return o % 8 == 0;
        }
        if (o > 30) {
            return p > 28 || p < 1;
        }
        if (p > 30) {
            return o > 28 || o < 1;
        }
        return Math.abs(p - o) <= 2;
    }

    /** Liquid nitrogen may also come down a pipe two blocks above. */
    private void pullNitrogen() {
        IFluidHandler above = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.above(2), Direction.DOWN);
        if (above != null && coolant.getCapacity() > coolant.getFluidAmount()) {
            FluidStack offered = above.drain(new FluidStack(RotaryFluids.LIQUID_NITROGEN.source.get(), coolant.getCapacity() - coolant.getFluidAmount()),
                    IFluidHandler.FluidAction.SIMULATE);
            if (!offered.isEmpty()) {
                int taken = coolant.fill(offered, IFluidHandler.FluidAction.EXECUTE);
                above.drain(offered.copyWithAmount(taken), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    // ---- ring ----

    @Nullable
    @Override
    public ToroidPart nextPart(Level level) {
        return level.getBlockEntity(worldPosition.offset(aim.xOffset, 0, aim.zOffset)) instanceof ToroidPart part ? part : null;
    }

    /** checkCompleteness: following next parts for at most 60 steps comes back round to this magnet. */
    private boolean isComplete() {
        ToroidPart part = nextPart(level);
        int steps = 60;
        while (part != null && part != this && steps >= 0) {
            part = part.nextPart(level);
            steps--;
        }
        if (part != this && part instanceof ToroidMagnetBlockEntity other) {
            other.hasNext = false;
        }
        return part == this;
    }

    /** A magnet turned or placed before its solenoid is up takes the solenoid state of the magnet ahead of it (or past the injector). */
    private void copySolenoidState() {
        BlockPos next = worldPosition.offset(aim.xOffset, 0, aim.zOffset);
        if (level.getBlockEntity(next) instanceof ToroidMagnetBlockEntity magnet) {
            hasSolenoid = magnet.hasSolenoid;
            magnet.isComplete();
        } else if (level.getBlockEntity(next) instanceof FusionInjectorBlockEntity) {
            BlockPos at = next.offset(aim.xOffset, 0, aim.zOffset);
            while (level.getBlockEntity(at) instanceof FusionInjectorBlockEntity) {
                at = at.offset(aim.xOffset, 0, aim.zOffset);
            }
            if (level.getBlockEntity(at) instanceof ToroidMagnetBlockEntity magnet) {
                hasSolenoid = magnet.hasSolenoid;
            }
        }
    }

    public void setHasSolenoid(boolean has) {
        if (hasSolenoid != has) {
            hasSolenoid = has;
            markForSync();
        }
    }

    public boolean hasSolenoid() {
        return hasSolenoid;
    }

    // ---- charge ----

    /** A quarter of the difference goes to the next magnet; past an injector, to the magnet beyond it. */
    private void shareCharge() {
        BlockPos next = worldPosition.offset(aim.xOffset, 0, aim.zOffset);
        ToroidMagnetBlockEntity other = null;
        if (level.getBlockEntity(next) instanceof ToroidMagnetBlockEntity magnet) {
            other = magnet;
        } else if (level.getBlockEntity(next) instanceof FusionInjectorBlockEntity
                && level.getBlockEntity(next.offset(aim.xOffset, 0, aim.zOffset)) instanceof ToroidMagnetBlockEntity magnet) {
            other = magnet;
        }
        if (other != null) {
            int dC = charge - other.charge;
            if (dC > 0) {
                other.charge += dC / 4;
                charge -= dC / 4;
                if (level instanceof ServerLevel server && level.random.nextBoolean()) {
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 2.25, worldPosition.getZ() + 0.5,
                            2, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }
    }

    @Override
    public void onDischarge(int charge, double range) {
        this.charge += charge;
        markForSync();
    }

    @Override
    public int getMinDischarge() {
        return 8192;
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
        return -1.25F;
    }

    @Override
    public float getAimZ() {
        return 0.5F;
    }

    public int charge() {
        return charge;
    }

    public void setCharge(int charge) {
        this.charge = charge;
    }

    // ---- aim and activity ----

    public ToroidAim aim() {
        return aim;
    }

    public void setAim(ToroidAim aim) {
        this.aim = aim;
        if (!hasSolenoid && level != null) {
            copySolenoidState();
        }
        markForSync();
    }

    public boolean isActive() {
        return active;
    }

    public boolean ringComplete() {
        return hasNext;
    }

    public FluidTank coolantTank() {
        return coolant;
    }

    private void setActive(boolean now) {
        if (active != now) {
            active = now;
            if (level instanceof ServerLevel server && ReactorConfig.CHUNKLOADING.get()) {
                ChunkPos chunk = new ChunkPos(worldPosition);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        ReactorTickets.CONTROLLER.forceChunk(server, worldPosition, chunk.x + dx, chunk.z + dz, now, true);
                    }
                }
            }
            syncToClient();
        }
    }

    @Override
    public void setRemoved() {
        if (active && level instanceof ServerLevel) {
            active = false;
            ChunkPos chunk = new ChunkPos(worldPosition);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ReactorTickets.CONTROLLER.forceChunk((ServerLevel) level, worldPosition, chunk.x + dx, chunk.z + dz, false, true);
                }
            }
        }
        super.setRemoved();
    }

    /** Nitrogen in from anywhere. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return nitrogenIn;
    }

    @Override
    public boolean onNeutron(net.scwunge.reactorcraft.content.entity.NeutronEntity neutron, Level level, BlockPos pos) {
        return false;
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putInt("Aim", aim.ordinal());
        tag.putBoolean("Active", active);
        tag.putBoolean("Solenoid", hasSolenoid);
        tag.putBoolean("Next", hasNext);
        tag.putInt("Charge", charge);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        aim = ToroidAim.byOrdinal(tag.getInt("Aim"));
        active = tag.getBoolean("Active");
        hasSolenoid = tag.getBoolean("Solenoid");
        hasNext = tag.getBoolean("Next");
        charge = tag.getInt("Charge");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Aim", aim.ordinal());
        tag.putBoolean("Solenoid", hasSolenoid);
        tag.putInt("Charge", charge);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        aim = ToroidAim.byOrdinal(tag.getInt("Aim"));
        hasSolenoid = tag.getBoolean("Solenoid");
        charge = tag.getInt("Charge");
    }
}
