package net.scwunge.reactorcraft.content.item;

import net.scwunge.reactorcraft.core.FuelStage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A TRISO fuel pellet for pebble bed reactors: its damage is how much has been used, in 25 stages. */
public class TrisoPelletItem extends Item {
    public static final int STAGES = 25;

    public TrisoPelletItem() {
        super(new Item.Properties());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).append(" (" + FuelStage.get(stack) * 100 / STAGES + "% Depleted)");
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }
}
