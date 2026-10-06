package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.StepTimer;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.reactorcraft.registry.ReactorItems;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Isotope Centrifuge (TileEntityCentrifuge): spun from below at 262144 rad/s or more, it splits uranium hexafluoride
 * into enriched (9% of the time) or depleted uranium dust, 50 mB per dust. Faster spinning shortens the cycle.
 */
public class IsotopeCentrifugeBlockEntity extends ReactorMachineBlockEntity {
    public static final int MINSPEED = 262144;
    public static final int UF6_PER_DUST = 50;
    /** Percent chance a cycle gives enriched rather than depleted dust. */
    public static final int FUEL_CHANCE = 9;
    public static final int SLOT_ENRICHED = 0;
    public static final int SLOT_DEPLETED = 1;

    private final ShaftInput shaft = new ShaftInput();
    private final StepTimer timer = new StepTimer(900);
    private final FluidTank tank;
    private final IFluidHandler pipeInput;

    public IsotopeCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.ISOTOPE_CENTRIFUGE.get(), pos, state, 2);
        tank = addTank("Tank", 12000, s -> s.is(ReactorFluids.URANIUM_HEXAFLUORIDE.get()));
        pipeInput = FluidAccess.fillOnly(tank);
    }

    @Override
    protected void tickServer() {
        int omega = shaft.omega();
        shaft.read(level, worldPosition, Direction.DOWN);
        if (omega != shaft.omega()) {
            markForSync();
        }
        timer.setCap(cycleTime(shaft.omega()));
        if (shaft.power() > 0 && shaft.omega() >= MINSPEED && !tank.isEmpty()) {
            if (tank.getFluidAmount() >= UF6_PER_DUST && hasInventorySpace()) {
                timer.update();
                if (timer.checkCap()) {
                    make();
                }
            } else {
                timer.reset();
            }
        } else {
            timer.reset();
        }
    }

    /** setTimer: ticks per dust at a given speed. */
    public static int cycleTime(int omega) {
        if (omega >= 67108864) {
            return 8;
        } else if (omega >= 33554432) {
            return 20;
        } else if (omega >= 16777216) {
            return 50;
        } else if (omega >= 8388608) {
            return 100;
        } else if (omega >= 4194304) {
            return 240;
        } else if (omega >= 2097152) {
            return 400;
        } else if (omega >= 1048576) {
            return 600;
        } else if (omega >= 524288) {
            return 800;
        }
        return 900;
    }

    private void make() {
        removeLiquid(tank, UF6_PER_DUST);
        if (Chance.percent(level.random, FUEL_CHANCE)) {
            output(SLOT_ENRICHED, new ItemStack(ReactorItems.ENRICHED_URANIUM_DUST.get()));
        } else {
            output(SLOT_DEPLETED, new ItemStack(ReactorItems.DEPLETED_URANIUM_DUST.get()));
        }
    }

    private boolean hasInventorySpace() {
        return canOutput(SLOT_ENRICHED, new ItemStack(ReactorItems.ENRICHED_URANIUM_DUST.get()))
                && canOutput(SLOT_DEPLETED, new ItemStack(ReactorItems.DEPLETED_URANIUM_DUST.get()));
    }

    /** animateWithTick */
    @Override
    protected void tickClient() {
        int omega = shaft.omega();
        float d = 0;
        if (omega >= 262144) {
            d += 40;
        } else if (omega >= 65536) {
            d += 30;
        } else if (omega >= 16384) {
            d += 20;
        } else if (omega >= 4096) {
            d += 15;
        }
        if (omega >= 1024) {
            d += 10;
        }
        if (omega >= 256) {
            d += 7;
        } else if (omega > 0) {
            d += 5;
        }
        spin(d);
    }

    // ---- automation: UF6 in from the top, dust out of the top and bottom ----

    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side == null) {
            return tank;
        }
        return side == Direction.UP ? pipeInput : null;
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return side == null || side.getAxis() == Direction.Axis.Y;
    }

    public FluidTank tank() {
        return tank;
    }

    public ShaftInput shaft() {
        return shaft;
    }

    // ---- GUI ----

    @Override
    public boolean hasMenu() {
        return true;
    }

    @Override
    public void addMenuSlots(ReactorMenu menu) {
        menu.addOutputSlot(SLOT_ENRICHED, 44, 62);
        menu.addOutputSlot(SLOT_DEPLETED, 116, 62);
    }

    @Override
    protected int guiValueCount() {
        return 2;
    }

    @Override
    protected int getGuiValue(int index) {
        return index == 0 ? timer.getTick() : timer.getCap();
    }

    // ---- saving ----

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
