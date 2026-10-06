package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.core.StepTimer;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Centrifugal Fluid Extractor (TileEntityHeavyPump): driven from above or below with at least 512 Nm and 65536 W, it
 * pulls heavy water out of the sea floor (in an ocean, below y = 45, with 16 blocks of still water over it) or molten
 * lithium off the surface of a lava sea, from the source blocks around it. Fluid comes out of the sides.
 */
public class FluidExtractorBlockEntity extends ReactorMachineBlockEntity {
    public static final int MINPOWER = 65536;
    public static final int MINTORQUE = 512;
    /** HeavyWaterExtraction */
    public static final int HEAVY_WATER_MAXY = 45;
    public static final int MINDEPTH = 16;
    /**
     * MoltenLithiumExtraction: the extractor must sit in the top layer of a lava sea. The original's overworld lava level
     * was y = 10; in 1.21 the deep lava sea's top layer is y = -55. The Nether's is still y = 31.
     */
    public static final int OVERWORLD_LAVA_SURFACE = -55;
    public static final int NETHER_LAVA_SURFACE = 31;

    private final ShaftInput shaft = new ShaftInput();
    private final StepTimer timer = new StepTimer(20);
    private final FluidTank tank;
    private final IFluidHandler output;

    public FluidExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.FLUID_EXTRACTOR.get(), pos, state, 0);
        tank = addTank("Tank", 8000, s -> false);
        output = FluidAccess.drainOnly(tank);
    }

    @Override
    protected void tickServer() {
        int torque = shaft.torque();
        long power = shaft.power();
        shaft.read(level, worldPosition, Direction.DOWN, Direction.UP);
        if (torque != shaft.torque() || (power >= MINPOWER) != (shaft.power() >= MINPOWER)) {
            markForSync();
        }
        if (isWorking()) {
            timer.setCap(Math.max(1, 20 - 2 * log2(shaft.omega())));
            timer.update();
            Fluid source = sourceFluid();
            if (source != null) {
                if (timer.checkCap()) {
                    tryExtract(source);
                }
            } else {
                timer.reset();
            }
        }
    }

    /** ReikaMathLibrary.logbase(omega, 2), truncated. */
    private static int log2(int omega) {
        return omega <= 0 ? 0 : (int) (Math.log(omega) / Math.log(2));
    }

    public boolean isWorking() {
        return shaft.power() >= MINPOWER && shaft.torque() >= MINTORQUE;
    }

    @Override
    protected void tickClient() {
        spin(isWorking() ? 10 : 0);
    }

    /** getExtraction: the fluid of at least three of the four source blocks beside it, or null if they are mixed. */
    @Nullable
    private Fluid sourceFluid() {
        Fluid found = null;
        int count = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            FluidState fluid = level.getFluidState(worldPosition.relative(dir));
            if (!fluid.isEmpty() && fluid.isSource()) {
                Fluid type = fluid.getType();
                if (found == null || type == found) {
                    count++;
                    found = type;
                } else {
                    return null;
                }
            }
        }
        return found != null && count >= 3 && (found == Fluids.WATER || found == Fluids.LAVA) ? found : null;
    }

    private void tryExtract(Fluid source) {
        if (source == Fluids.WATER) {
            if (canExtractHeavyWater(level, worldPosition)) {
                tank.fill(new FluidStack(ReactorFluids.HEAVY_WATER.get(), 200), IFluidHandler.FluidAction.EXECUTE);
            }
        } else if (canExtractLithium(level, worldPosition)) {
            boolean nether = level.dimension() == Level.NETHER;
            int amount = nether ? 10 + level.random.nextInt(21) + level.random.nextInt(51) : 10 + level.random.nextInt(31);
            tank.fill(new FluidStack(ReactorFluids.LITHIUM.get(), amount), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public static boolean canExtractHeavyWater(Level level, BlockPos pos) {
        List<? extends String> dims = ReactorConfig.HEAVY_WATER_DIMENSIONS.get();
        if (!dims.isEmpty() && !dims.contains(level.dimension().location().toString())) {
            return false;
        }
        return pos.getY() < HEAVY_WATER_MAXY && level.getBiome(pos).is(BiomeTags.IS_OCEAN) && isOceanFloor(level, pos);
    }

    /** Still water on the four diagonals for 16 blocks up, and over the extractor itself. */
    private static boolean isOceanFloor(Level level, BlockPos pos) {
        for (int i = 0; i < MINDEPTH; i++) {
            for (int a = -1; a <= 1; a += 2) {
                for (int b = -1; b <= 1; b += 2) {
                    if (!isStillWater(level, pos.offset(a, i, b))) {
                        return false;
                    }
                }
            }
            if (i >= 1 && !isStillWater(level, pos.above(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isStillWater(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    public static boolean canExtractLithium(Level level, BlockPos pos) {
        int surface = level.dimension() == Level.OVERWORLD ? OVERWORLD_LAVA_SURFACE : level.dimension() == Level.NETHER ? NETHER_LAVA_SURFACE : Integer.MIN_VALUE;
        return pos.getY() == surface && level.getFluidState(pos.below()).is(FluidTags.LAVA) && !level.getFluidState(pos.above()).is(FluidTags.LAVA);
    }

    /** Fluid comes out of the four sides (and to buckets and canisters held against it). */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == null || side.getAxis().isHorizontal() ? output : null;
    }

    public FluidTank tank() {
        return tank;
    }

    public ShaftInput shaft() {
        return shaft;
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        shaft.save(tag);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        shaft.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        shaft.save(tag);
        tag.putInt("Timer", timer.getTick());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        shaft.load(tag);
        timer.setTick(tag.getInt("Timer"));
    }
}
