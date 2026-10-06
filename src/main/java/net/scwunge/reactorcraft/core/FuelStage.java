package net.scwunge.reactorcraft.core;

import net.minecraft.world.item.ItemStack;
import net.scwunge.reactorcraft.registry.ReactorComponents;

/** How far a fuel stack has been used (the original kept this in the item's damage; 1.21 does not let a damageable item stack). */
public final class FuelStage {
    private FuelStage() {
    }

    public static int get(ItemStack stack) {
        return stack.getOrDefault(ReactorComponents.FUEL_STAGE.get(), 0);
    }

    /** Sets the stage; stage 0 carries no component, so fresh fuel stacks. */
    public static void set(ItemStack stack, int stage) {
        if (stage <= 0) {
            stack.remove(ReactorComponents.FUEL_STAGE.get());
        } else {
            stack.set(ReactorComponents.FUEL_STAGE.get(), stage);
        }
    }
}
