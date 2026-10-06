package net.scwunge.reactorcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.scwunge.reactorcraft.ReactorCraft;

/** Dirty bombs: a creeper that has been irradiated spreads radiation when it explodes. */
@EventBusSubscriber(modid = ReactorCraft.MODID)
public final class RadiationEvents {
    private RadiationEvents() {
    }

    @SubscribeEvent
    static void creeperExplodes(ExplosionEvent.Detonate event) {
        if (event.getExplosion().getDirectSourceEntity() instanceof Creeper creeper && creeper.getPersistentData().getBoolean("radioactive")
                && !event.getLevel().isClientSide) {
            RadiationEffects.contaminateArea(event.getLevel(), BlockPos.containing(event.getExplosion().center()), 4, 3, 1.5, true,
                    RadiationIntensity.MODERATE);
        }
    }
}
