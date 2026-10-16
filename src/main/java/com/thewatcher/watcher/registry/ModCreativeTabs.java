package com.thewatcher.watcher.registry;

import com.thewatcher.watcher.TheWatcherMod;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Coloca os itens do mod nas abas do inventario criativo.
 */
@Mod.EventBusSubscriber(modid = TheWatcherMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabs {

    private ModCreativeTabs() {}

    @SubscribeEvent
    public static void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.WATCHER_SPAWN_EGG);
        }
    }
}
