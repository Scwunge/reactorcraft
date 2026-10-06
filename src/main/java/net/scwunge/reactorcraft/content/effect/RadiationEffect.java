package net.scwunge.reactorcraft.content.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Difficulty;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.RadiationEffects;
import net.scwunge.reactorcraft.core.RadiationIntensity;

import java.util.List;

/**
 * Radiation sickness (PotionRadiation): once a second it may hurt, more often the healthier you are (so it wears you down but
 * does not finish you off quickly), starves players and slows them, and while it lasts it strips good effects and adds nausea,
 * poison and a weak jump. Milk does not cure it.
 */
public class RadiationEffect extends MobEffect {
    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x111111);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 5;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int level) {
        boolean peaceful = entity.level().getDifficulty() == Difficulty.PEACEFUL;
        double chance = peaceful ? 75 : 50;
        if (level >= RadiationIntensity.HIGHLEVEL.ordinal()) {
            chance *= 1.1;
        }
        if (level >= RadiationIntensity.LETHAL.ordinal()) {
            chance *= 1.25;
        }
        if (Chance.of(entity.getRandom(), entity.getHealth() / entity.getMaxHealth() * chance)) {
            entity.hurt(RadiationEffects.radiationDamage(entity.level()), peaceful ? 2 : 1);
        }
        for (var beneficial : List.of(MobEffects.REGENERATION, MobEffects.ABSORPTION, MobEffects.MOVEMENT_SPEED, MobEffects.DIG_SPEED,
                MobEffects.DAMAGE_BOOST, MobEffects.HEAL, MobEffects.JUMP, MobEffects.FIRE_RESISTANCE, MobEffects.DAMAGE_RESISTANCE,
                MobEffects.NIGHT_VISION)) {
            entity.removeEffect(beneficial);
        }
        if (!entity.hasEffect(MobEffects.CONFUSION)) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
        }
        entity.addEffect(new MobEffectInstance(MobEffects.POISON, 20, 0));
        // the original also cut a player walk speed and jump; slowness stands in for both
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        if (entity instanceof Player player) {
            int food = player.getFoodData().getFoodLevel();
            player.getFoodData().setFoodLevel(Math.min(food, Math.max(1, 8 - 2 * level)));
            player.getFoodData().setSaturation(Math.max(0, 4 - level));
        }
        return true;
    }

    /** Nothing cures it but time (creative players excepted). */
    @Override
    public void fillEffectCures(java.util.Set<net.neoforged.neoforge.common.EffectCure> cures, MobEffectInstance effect) {
        cures.clear();
    }
}
