package net.scwunge.reactorcraft.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.reactorcraft.ReactorCraft;

public final class ReactorComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ReactorCraft.MODID);

    /** Which waste a nuclear waste item is: an isotope's number, or 1000 plus a mixed-waste group's (see WasteManager). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WASTE = COMPONENTS.register("waste",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    private ReactorComponents() {
    }
}
