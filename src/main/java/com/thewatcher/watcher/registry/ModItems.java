package com.thewatcher.watcher.registry;

import com.thewatcher.watcher.TheWatcherMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registro de itens do mod.
 */
public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TheWatcherMod.MOD_ID);

    /** Ovo de spawn do Watcher. Cores: base quase preta, manchas vermelho-escuro. */
    public static final RegistryObject<ForgeSpawnEggItem> WATCHER_SPAWN_EGG =
            ITEMS.register("watcher_spawn_egg", () -> new ForgeSpawnEggItem(
                    ModEntities.WATCHER, 0x141414, 0x8B0000, new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
