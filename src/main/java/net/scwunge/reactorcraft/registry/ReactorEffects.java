package net.scwunge.reactorcraft.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.effect.RadiationEffect;

public final class ReactorEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ReactorCraft.MODID);

    /** Radiation sickness; its level is the radiation's intensity. */
    public static final DeferredHolder<MobEffect, RadiationEffect> RADIATION_EFFECT = EFFECTS.register("radiation", RadiationEffect::new);
    public static final Holder<MobEffect> RADIATION = RADIATION_EFFECT;

    private ReactorEffects() {
    }
}
