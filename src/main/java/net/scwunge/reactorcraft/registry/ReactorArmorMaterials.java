package net.scwunge.reactorcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.reactorcraft.ReactorCraft;

import java.util.List;
import java.util.Map;

public final class ReactorArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, ReactorCraft.MODID);

    /** The hazmat suit protects against radiation, not blows: no armor value, and it never wears out (as in the original). */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HAZMAT = MATERIALS.register("hazmat", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 0, ArmorItem.Type.CHESTPLATE, 0, ArmorItem.Type.LEGGINGS, 0, ArmorItem.Type.BOOTS, 0),
            0, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(ReactorItems.SHIELDING_FABRIC.get()),
            List.of(new ArmorMaterial.Layer(ReactorCraft.id("hazmat"))), 0F, 0F));

    private ReactorArmorMaterials() {
    }
}
