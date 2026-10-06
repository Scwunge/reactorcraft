package net.scwunge.reactorcraft.content.item;

import net.scwunge.reactorcraft.core.FuelStage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.scwunge.reactorcraft.core.ReactorFuel;
import net.scwunge.reactorcraft.core.RadiationHooks;

/** A fuel pellet (ItemReactorFuel): a damageable item whose damage is how much of it has been used. Plutonium is radioactive to carry. */
public class FuelPelletItem extends Item {
    private final boolean radioactive;

    public FuelPelletItem(boolean radioactive) {
        super(new Item.Properties());
        this.radioactive = radioactive;
    }

    @Override
    public Component getName(ItemStack stack) {
        int depleted = FuelStage.get(stack) * 100 / ReactorFuel.STAGES;
        return Component.translatable(getDescriptionId(stack)).append(" (" + depleted + "% Depleted)");
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (radioactive && !level.isClientSide && entity instanceof Player player && !player.isCreative()) {
            RadiationHooks.holdingPlutonium(level, player);
        }
    }
}
