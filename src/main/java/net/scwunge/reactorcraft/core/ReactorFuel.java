package net.scwunge.reactorcraft.core;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.scwunge.reactorcraft.registry.ReactorItems;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * What fuel does in a fuel rod (the original's ReactorFuel). Fuel pellets last {@link #STAGES} fissions, shown as the item's
 * damage; the last one turns uranium into depleted uranium and plutonium into nothing.
 */
public enum ReactorFuel {
    URANIUM(ReactorItems.FUEL::get, 25, 3, 5, 20, 0F),
    PLUTONIUM(ReactorItems.PLUTONIUM::get, 30, 4, 10, 30, 0.025F);

    /** Fissions a pellet lasts. */
    public static final int STAGES = 100;

    private final Supplier<? extends Item> item;
    /** Percent chance a neutron causes a fission, before the void coefficient. */
    public final int fissionChance;
    /** Percent chance a fission uses up one stage of the pellet. */
    public final int consumeChance;
    /** Percent chance using up a stage leaves a piece of nuclear waste. */
    public final int wasteChance;
    /** Degrees a fission heats the rod. */
    public final int temperatureStep;
    /** Extra fission chance per degree above 100 C (negative feedback, or positive for plutonium). */
    public final float voidCoefficient;

    ReactorFuel(Supplier<? extends Item> item, int fissionChance, int consumeChance, int wasteChance, int temperatureStep, float voidCoefficient) {
        this.item = item;
        this.fissionChance = fissionChance;
        this.consumeChance = consumeChance;
        this.wasteChance = wasteChance;
        this.temperatureStep = temperatureStep;
        this.voidCoefficient = voidCoefficient;
    }

    public ItemStack fuelItem() {
        return new ItemStack(item.get());
    }

    /** What a pellet becomes when one stage is used up. */
    public ItemStack fissionProduct(ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (FuelStage.get(input) >= STAGES - 1) {
            return this == URANIUM ? new ItemStack(ReactorItems.DEPLETED.get()) : ItemStack.EMPTY;
        }
        ItemStack next = input.copy();
        FuelStage.set(next, FuelStage.get(input) + 1);
        return next;
    }

    @Nullable
    public static ReactorFuel of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (ReactorFuel fuel : values()) {
            if (stack.is(fuel.item.get())) {
                return fuel;
            }
        }
        return null;
    }
}
