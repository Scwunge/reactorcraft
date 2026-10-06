package net.scwunge.reactorcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.content.entity.NuclearWasteEntity;
import net.scwunge.reactorcraft.content.entity.PlasmaEntity;
import net.scwunge.reactorcraft.content.entity.RadiationEntity;

public final class ReactorEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ReactorCraft.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<NeutronEntity>> NEUTRON = ENTITIES.register("neutron",
            () -> EntityType.Builder.<NeutronEntity>of(NeutronEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).noSummon()
                    .clientTrackingRange(4).updateInterval(20).fireImmune().build("neutron"));

    public static final DeferredHolder<EntityType<?>, EntityType<RadiationEntity>> RADIATION = ENTITIES.register("radiation",
            () -> EntityType.Builder.<RadiationEntity>of(RadiationEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).noSummon()
                    .clientTrackingRange(8).updateInterval(Integer.MAX_VALUE).fireImmune().build("radiation"));

    public static final DeferredHolder<EntityType<?>, EntityType<NuclearWasteEntity>> NUCLEAR_WASTE_ITEM = ENTITIES.register("nuclear_waste_item",
            () -> EntityType.Builder.<NuclearWasteEntity>of(NuclearWasteEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(6)
                    .updateInterval(20).fireImmune().build("nuclear_waste_item"));

    public static final DeferredHolder<EntityType<?>, EntityType<PlasmaEntity>> PLASMA = ENTITIES.register("plasma",
            () -> EntityType.Builder.<PlasmaEntity>of(PlasmaEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).noSummon()
                    .clientTrackingRange(16).updateInterval(1).fireImmune().build("plasma"));

    private ReactorEntities() {
    }
}
