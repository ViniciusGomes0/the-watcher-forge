package com.thewatcher.watcher.client;

import com.thewatcher.watcher.TheWatcherMod;
import com.thewatcher.watcher.registry.ModEntities;
import com.thewatcher.watcher.render.WatcherRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registros do lado cliente (MOD event bus, apenas no cliente).
 * Liga o renderer GeckoLib à entidade.
 */
@Mod.EventBusSubscriber(modid = TheWatcherMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WATCHER.get(), WatcherRenderer::new);
    }
}
