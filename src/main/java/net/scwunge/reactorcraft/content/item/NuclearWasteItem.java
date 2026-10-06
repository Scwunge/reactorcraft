package net.scwunge.reactorcraft.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.scwunge.reactorcraft.content.waste.Isotope;
import net.scwunge.reactorcraft.content.waste.WasteManager;
import net.scwunge.reactorcraft.core.RadiationHooks;

import java.util.List;

/** Nuclear waste (ItemNuclearWaste): one isotope's worth, or a mixed lot of one element group. Dangerous to carry, and never despawns. */
public class NuclearWasteItem extends Item {
    public NuclearWasteItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            RadiationHooks.holdingWaste(level, living);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Isotope.ElementGroup group = WasteManager.group(stack);
        if (group != null) {
            tooltip.add(Component.literal("Mixed Waste: " + group.displayName));
            return;
        }
        Isotope isotope = WasteManager.isotope(stack);
        if (isotope != null) {
            tooltip.add(isotope.displayName());
            tooltip.add(Component.literal("Half Life: " + isotope.halfLifeText()));
        }
    }
}
