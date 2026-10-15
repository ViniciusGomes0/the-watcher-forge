package com.thewatcher.watcher.events;

import com.thewatcher.watcher.TheWatcherMod;
import com.thewatcher.watcher.data.MemoryTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handlers do FORGE event bus que alimentam o {@link MemoryTracker}.
 *
 * Separado do {@link ForgeEventHandler} (que cuida dos sustos) para manter cada
 * sistema no seu arquivo. Os dois escutam {@code ServerTickEvent}: ambos disparam.
 */
@Mod.EventBusSubscriber(modid = TheWatcherMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MemoryEventHandler {

    private MemoryEventHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MemoryTracker.get().serverTick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MemoryTracker.get().onLogin(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MemoryTracker.get().onLogout(player);
        }
    }
}
