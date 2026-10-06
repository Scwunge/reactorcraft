package net.scwunge.reactorcraft.content.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;

/** Radiation Goggles (ItemRadiationGoggles): worn on the head; they let you see radiation as a hazy cloud. */
public class RadiationGogglesItem extends Item implements Equipable {
    public RadiationGogglesItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }
}
