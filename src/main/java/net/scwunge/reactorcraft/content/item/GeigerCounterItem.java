package net.scwunge.reactorcraft.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.scwunge.reactorcraft.content.entity.RadiationEntity;
import net.scwunge.reactorcraft.registry.ReactorComponents;
import net.scwunge.reactorcraft.registry.ReactorEffects;

import java.util.Comparator;
import java.util.List;

/**
 * Geiger Counter (ItemGeigerCounter): held in a hand it clicks, more often the nearer the nearest radiation within 20 blocks, and
 * constantly if you are already irradiated. It uses a little charge (kJ). RotaryCraft has no way to charge it yet, so it starts full.
 */
public class GeigerCounterItem extends Item {
    public static final int FULL_CHARGE = 32000;
    private static final int RANGE = 20;

    public GeigerCounterItem() {
        super(new Item.Properties().stacksTo(1).component(ReactorComponents.CHARGE.get(), FULL_CHARGE));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        int charge = stack.getOrDefault(ReactorComponents.CHARGE.get(), 0);
        if (level.isClientSide || !selected || charge <= 0) {
            return;
        }
        boolean sick = entity instanceof LivingEntity living && living.hasEffect(ReactorEffects.RADIATION);
        if (sick) {
            click(level, entity);
        } else {
            List<RadiationEntity> near = level.getEntitiesOfClass(RadiationEntity.class, new AABB(entity.blockPosition()).inflate(RANGE));
            near.stream().min(Comparator.comparingDouble(r -> r.distanceToSqr(entity))).ifPresent(nearest -> {
                double distanceSq = nearest.distanceToSqr(entity);
                if (level.random.nextDouble() * RANGE * 16 > distanceSq) {
                    click(level, entity);
                }
            });
        }
        if (level.random.nextInt(8) == 0) {
            stack.set(ReactorComponents.CHARGE.get(), charge - 1);
        }
    }

    private static void click(Level level, Entity entity) {
        level.playSound(null, entity.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1F, 2F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Charge: " + stack.getOrDefault(ReactorComponents.CHARGE.get(), 0) + " kJ"));
    }
}
