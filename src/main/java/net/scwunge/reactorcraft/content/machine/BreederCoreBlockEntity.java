package net.scwunge.reactorcraft.content.machine;

import net.scwunge.reactorcraft.core.FuelStage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.content.item.BreederFuelItem;
import net.scwunge.reactorcraft.content.waste.WasteManager;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.CoolantState;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.StepTimer;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorItems;

/**
 * Breeder Reactor Core (TileEntityBreederCore): holds breeder fuel (depleted uranium wrapped around a fuel pellet) and plutonium. Neutrons
 * that hit it convert the fuel a stage at a time (5% of the time) into plutonium, otherwise just heat it; it passes heat on to the sodium
 * heaters beside it, and is cooled only by sodium.
 */
public class BreederCoreBlockEntity extends NuclearCoreBlockEntity {
    public static final int MAX_TEMPERATURE = 900;

    private final StepTimer sodiumTimer = new StepTimer(10);

    public BreederCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.BREEDER_CORE.get(), pos, state);
    }

    @Override
    protected void tickServer() {
        super.tickServer();
        sodiumTimer.update();
        if (sodiumTimer.checkCap()) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (level.getBlockEntity(worldPosition.relative(dir)) instanceof SodiumHeaterBlockEntity heater) {
                    int dTemp = temperature - heater.getTemperature();
                    if (dTemp > 0) {
                        temperature -= dTemp / 16;
                        heater.setTemperature(heater.getTemperature() + dTemp / 16);
                    }
                }
            }
        }
    }

    private int fuelSlot() {
        for (int i = 0; i < SLOTS; i++) {
            if (stack(i).is(ReactorItems.BREEDER_FUEL.get())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean isFissile() {
        return fuelSlot() != -1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!stack(slot).isEmpty() || slot >= FUEL_SLOTS) {
            return false;
        }
        return stack.is(ReactorItems.BREEDER_FUEL.get()) || stack.is(ReactorItems.PLUTONIUM.get());
    }

    @Override
    protected boolean canRemove(ItemStack stack) {
        return stack.is(ReactorItems.PLUTONIUM.get()) || WasteManager.isWaste(stack);
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        super.onNeutron(neutron, level, pos);
        if (level.isClientSide) {
            return false;
        }
        if (isPoisoned()) {
            return true;
        }
        if (Chance.of(level.random, 25 + temperature / 100) && isFissile()
                && Chance.of(level.random, neutron.neutronSpeed().interactionMultiplier())) {
            int slot = fuelSlot();
            if (slot != -1) {
                if (neutron.neutronType().canTriggerFuelConversion()
                        && Chance.of(level.random, 5 * neutron.neutronSpeed().wasteConversionMultiplier())) {
                    ItemStack fuel = stack(slot);
                    if (FuelStage.get(fuel) >= BreederFuelItem.STAGES - 1) {
                        setStack(slot, new ItemStack(ReactorItems.PLUTONIUM.get()));
                        pushSpentFuel(slot);
                    } else {
                        ItemStack next = fuel.copy();
                        FuelStage.set(next, FuelStage.get(fuel) + 1);
                        setStack(slot, next);
                    }
                    temperature += 50;
                } else {
                    temperature += temperature >= 700 ? 30 : 20;
                }
                spawnNeutronBurst();
                if (Chance.of(level.random, 10)) {
                    addWaste();
                }
                return true;
            }
        }
        return false;
    }

    /** Only sodium cools it. */
    @Override
    public boolean canDumpHeatInto(CoolantState coolant) {
        return coolant == CoolantState.SODIUM;
    }

    @Override
    public ReactorType getReactorType() {
        return ReactorType.BREEDER;
    }
}
