package net.scwunge.reactorcraft.registry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.scwunge.reactorcraft.ReactorCraft;

/** The chunk tickets active fission cores hold. They are not kept across restarts: a core asks again when its neutrons keep hitting it. */
@EventBusSubscriber(modid = ReactorCraft.MODID)
public final class ReactorTickets {
    public static final TicketController CONTROLLER = new TicketController(ReactorCraft.id("cores"),
            (level, ticketHelper) -> ticketHelper.getBlockTickets().keySet().forEach(ticketHelper::removeAllTickets));

    private ReactorTickets() {
    }

    @SubscribeEvent
    static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }
}
