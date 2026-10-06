package net.scwunge.reactorcraft.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
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

    /** The CPU a remote control is linked to. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> LINKED_CPU = COMPONENTS.register("linked_cpu",
            () -> DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());
    /** What is left of a remote control's charge, in kJ. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE = COMPONENTS.register("charge",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    /** Water in a radiation cleanup tool, in mB. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WATER = COMPONENTS.register("water",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    private ReactorComponents() {
    }
}
