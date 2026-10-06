package net.scwunge.reactorcraft.content.item;

import net.scwunge.reactorcraft.core.FuelStage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Breeder Reactor Fuel (ItemReactorFuel, breeder): its damage is how much of it has been converted to plutonium, in twentieths. */
public class BreederFuelItem extends Item {
    public static final int STAGES = 20;

    public BreederFuelItem() {
        super(new Item.Properties());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).append(" (" + FuelStage.get(stack) * 5 + "% Converted)");
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }
}
