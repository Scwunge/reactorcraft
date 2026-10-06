package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.content.multi.MultiController;
import net.scwunge.reactorcraft.content.multi.MultiPartBlock;
import net.scwunge.reactorcraft.content.multi.MultiStructure;
import net.scwunge.reactorcraft.content.multi.PowerStructures;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.ReactorTypeMix;
import net.scwunge.reactorcraft.core.WorkingFluid;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * Big Turbine (TileEntityHiPTurbine): the high-pressure turbine. Up to seven of these in a line, inside their casing, form a turbine that takes its
 * steam from the steam line behind the first stage (a steam line pointing at it), uses lubricant from the injector ring round that line, and gives
 * far more than the small turbine: up to 131 kN*m of torque and 131 krad/s, with a share of the steam's push that rises from 1.25% for one stage
 * to the whole for seven. It spins up only while its casing stands (and breaking the casing at speed wrecks the block), stops when any
 * injector is powered by redstone, and drips the water it condenses into any tank under its last stage.
 */
public class BigTurbineBlockEntity extends TurbineCoreBlockEntity implements MultiController {
    public static final int GEN_OMEGA = 131072;
    /** TileEntityReactorBoiler.WATER_PER_STEAM * 131 / 20 / 24 * 6 / 10, in millibuckets: what a stage drips into a tank under it. */
    public static final int FLUID_PER_RESERVOIR = 1000 * 131 / 20 / 24 * 6 / 10;
    private static final int MAX_STEAM = 3250;

    private WorkingFluid fluid = WorkingFluid.EMPTY;
    private int dripBuffer;
    private boolean formed;

    public BigTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.BIG_TURBINE.get(), pos, state);
    }

    // ---- the structure ----

    @Override
    public MultiStructure structure() {
        return PowerStructures.TURBINE;
    }

    @Override
    public boolean isFormed() {
        return formed;
    }

    @Override
    public void setFormed(boolean formed) {
        if (this.formed && !formed && omega > 2048) {
            fail();
            return;
        }
        if (this.formed != formed) {
            this.formed = formed;
            if (!formed) {
                omega = 0;
                steam = 0;
            }
            markForSync();
        }
    }

    /** Breaking the casing round a turbine that is spinning fast wrecks it: the block goes, with an explosion. */
    private void fail() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean destroy = ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get() && WorldSafety.mayChange(level, worldPosition, owner());
        Level world = level;
        BlockPos at = worldPosition;
        world.removeBlock(at, false);
        world.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 4F, destroy ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    @Override
    protected boolean needsStructure() {
        return true;
    }

    @Override
    protected boolean structureStands() {
        return formed;
    }

    @Override
    protected boolean ignoredByBlades(BlockState state) {
        return state.getBlock() instanceof MultiPartBlock part && part.structure() == PowerStructures.TURBINE;
    }

    @Override
    public int totalStages() {
        return formed ? super.totalStages() : 0;
    }

    // ---- the turbine ----

    @Override
    public int maxTorque() {
        return fluid.efficiency > 1 ? 131072 : 65536;
    }

    @Override
    public int maxSpeed() {
        return GEN_OMEGA;
    }

    @Override
    protected int maxStage() {
        return 6;
    }

    @Override
    protected double efficiency() {
        return switch (totalStages()) {
            case 1 -> 0.0125;
            case 2 -> 0.025;
            case 3 -> 0.075;
            case 4 -> 0.125;
            case 5 -> 0.25;
            case 6 -> 0.5;
            case 7 -> 1;
            default -> 0;
        };
    }

    @Override
    protected int consumedLubricant() {
        return 100;
    }

    @Override
    protected boolean usesSteamBlocks() {
        return false;
    }

    @Override
    protected float torqueFactor() {
        float base = super.torqueFactor();
        if (steam < MAX_STEAM / 2) {
            // the original's cosInterpolation: the push falls off smoothly as the steam runs low
            float f = steam / (float) MAX_STEAM;
            base *= (float) (0.5 - 0.5 * Math.cos(Math.PI * Mth.clamp(f * 2, 0, 1)));
        }
        if (fluid.efficiency > 1) {
            base *= 1 + (fluid.efficiency - 1) * 0.25F;
        }
        return base;
    }

    public WorkingFluid fluid() {
        return fluid;
    }

    /** The injectors round the block behind the first stage. */
    private Iterable<SteamInjectorBlockEntity> injectors() {
        Direction behind = steamMovement().getOpposite();
        BlockPos middle = worldPosition.relative(behind);
        java.util.List<SteamInjectorBlockEntity> out = new java.util.ArrayList<>();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                BlockPos at = steamMovement().getAxis() == Direction.Axis.X ? middle.offset(0, a, b) : middle.offset(b, a, 0);
                if (level.getBlockEntity(at) instanceof SteamInjectorBlockEntity injector) {
                    out.add(injector);
                }
            }
        }
        return out;
    }

    @Override
    protected void intakeLubricant() {
        if (stage() != 0) {
            return;
        }
        for (SteamInjectorBlockEntity injector : injectors()) {
            int room = lubricantTank().getCapacity() - lubricantTank().getFluidAmount();
            int amount = Math.min(injector.lubricant(), room);
            if (amount > 0) {
                Fluid lubricant = injector.lubricantFluid();
                injector.remove(amount);
                addLiquid(lubricantTank(), lubricant, amount);
            }
        }
    }

    /** enabled(): it needs lubricant, and stops while any injector gets a redstone signal. */
    @Override
    protected boolean runnable() {
        if (lubricantTank().isEmpty() || !isEnabled()) {
            return false;
        }
        for (SteamInjectorBlockEntity injector : injectors()) {
            if (level.hasNeighborSignal(injector.getBlockPos())) {
                return false;
            }
        }
        return true;
    }

    /** intakeSteam: a steam line behind the first stage gives its steam, up to what the first stage holds. Returns whether to accelerate. */
    @Override
    protected boolean intakeSteam() {
        if (stage() != 0) {
            return false;
        }
        BlockEntity behind = level.getBlockEntity(worldPosition.relative(steamMovement().getOpposite()));
        boolean accelerate = false;
        if (behind instanceof SteamLineBlockEntity line) {
            int s = line.getSteam();
            if (s > 8 && canTakeIn(line.workingFluid())) {
                ReactorTypeMix source = line.sourceTypes();
                s = source.isEmpty() ? 0 : usable(s, source);
                if (s > 0) {
                    int rm = s / 8 + 1;
                    if (steam < MAX_STEAM) {
                        int taken = Math.min(rm, MAX_STEAM - steam);
                        steam += taken;
                        fluid = line.workingFluid();
                        line.removeSteam(taken);
                        dripBuffer += taken * 1000;
                    }
                    accelerate = s > rm + 32 && steam >= MAX_STEAM / 15;
                }
            }
        }
        if (steam == 0) {
            fluid = WorkingFluid.EMPTY;
        }
        return accelerate;
    }

    private boolean canTakeIn(WorkingFluid other) {
        return fluid == WorkingFluid.EMPTY || other == fluid;
    }

    /** getEffectiveUsable: steam from some kinds of reactor drives a high-pressure turbine better than from others. */
    private static int usable(int s, ReactorTypeMix source) {
        float total = 0;
        for (ReactorType type : source.types()) {
            if (type != null) {
                total += (float) (source.fraction(type) * s * type.hpTurbineMultiplier());
            }
        }
        return (int) total;
    }

    @Override
    protected void copyDataFrom(TurbineCoreBlockEntity behind) {
        if (behind instanceof BigTurbineBlockEntity other) {
            fluid = other.fluid;
            dripBuffer = other.dripBuffer;
            other.dripBuffer = 0;
        }
    }

    /** dumpSteam: the last stage lets the water it has made drip down into a tank (or reservoir) under it, or rain if there is none. */
    @Override
    protected void dumpSteam() {
        if (dripBuffer <= 0 || stage() != totalStages() - 1 || !(level instanceof ServerLevel server) || fluid.lowPressureFluid() == null) {
            return;
        }
        Direction s = steamMovement();
        Direction across = s.getClockWise();
        int th = (int) bladeRadius();
        Fluid low = fluid.lowPressureFluid();
        outer:
        for (int d = 0; d <= 1; d++) {
            for (int dy = 2; dy < 5; dy++) {
                int ty = worldPosition.getY() - th - dy;
                for (int i = -th; i <= th; i++) {
                    BlockPos at = new BlockPos(worldPosition.getX() + across.getStepX() * i + s.getStepX() * d, ty, worldPosition.getZ() + across.getStepZ() * i + s.getStepZ() * d);
                    IFluidHandler tank = server.getCapability(Capabilities.FluidHandler.BLOCK, at, Direction.UP);
                    if (tank != null) {
                        int accepted = tank.fill(new FluidStack(low, FLUID_PER_RESERVOIR), IFluidHandler.FluidAction.EXECUTE);
                        if (accepted > 0) {
                            dripBuffer -= accepted;
                            break outer;
                        }
                    }
                }
            }
        }
        if (dripBuffer < 0) {
            dripBuffer = 0;
        }
        if (level.getGameTime() % 4 == 0) {
            double x = worldPosition.getX() + 0.5 + across.getStepX() * (level.random.nextDouble() * 2 - 1) * th;
            double z = worldPosition.getZ() + 0.5 + across.getStepZ() * (level.random.nextDouble() * 2 - 1) * th;
            server.sendParticles(ParticleTypes.RAIN, x, worldPosition.getY() - th + 1 + level.random.nextInt(Math.max(1, th * 2)), z, 3, 0.2, 0.2, 0.2, 0);
        }
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putInt("Fluid", fluid.ordinal());
        tag.putBoolean("Formed", formed);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        fluid = WorkingFluid.byId(tag.getInt("Fluid"));
        formed = tag.getBoolean("Formed");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fluid", fluid.ordinal());
        tag.putInt("Drip", dripBuffer);
        tag.putBoolean("Formed", formed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluid = WorkingFluid.byId(tag.getInt("Fluid"));
        dripBuffer = tag.getInt("Drip");
        formed = tag.getBoolean("Formed");
    }
}
