package com.thewatcher.watcher.events;

import com.thewatcher.watcher.TheWatcherMod;
import com.thewatcher.watcher.scare.ScareManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handlers do FORGE event bus (eventos de jogo).
 *
 * Registrado automaticamente via {@link Mod.EventBusSubscriber}. Liga o
 * {@link ScareManager} ao loop do servidor e limpa o estado no logout.
 */
@Mod.EventBusSubscriber(modid = TheWatcherMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeEventHandler {

    private ForgeEventHandler() {
    }

    /** Alimenta o ScareManager uma vez por tick do servidor (fase END). */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ScareManager.get().serverTick(event.getServer());
        }
    }

    /** Esquece o estado de susto do jogador quando ele sai. */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ScareManager.get().onPlayerLogout(event.getEntity().getUUID());
    }
}
