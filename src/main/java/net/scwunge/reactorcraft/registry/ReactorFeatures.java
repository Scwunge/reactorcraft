package net.scwunge.reactorcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.world.FluoriteVeinFeature;

public final class ReactorFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, ReactorCraft.MODID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> FLUORITE_VEIN = FEATURES.register("fluorite_vein", FluoriteVeinFeature::new);

    private ReactorFeatures() {
    }
}
