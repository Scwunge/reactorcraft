package net.scwunge.reactorcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;

public final class ReactorEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ReactorCraft.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<NeutronEntity>> NEUTRON = ENTITIES.register("neutron",
            () -> EntityType.Builder.<NeutronEntity>of(NeutronEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).noSummon()
                    .clientTrackingRange(4).updateInterval(20).fireImmune().build("neutron"));

    private ReactorEntities() {
    }
}
