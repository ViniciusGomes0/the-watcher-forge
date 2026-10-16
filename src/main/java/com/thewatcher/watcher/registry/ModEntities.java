package com.thewatcher.watcher.registry;

import com.thewatcher.watcher.TheWatcherMod;
import com.thewatcher.watcher.entity.WatcherEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registro de entidades do mod.
 */
public final class ModEntities {

    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TheWatcherMod.MOD_ID);

    public static final RegistryObject<EntityType<WatcherEntity>> WATCHER =
            ENTITY_TYPES.register("watcher", () -> EntityType.Builder
                    .of(WatcherEntity::new, MobCategory.MONSTER)
                    // Stalker altissimo e magro: ~0.9 de largura, ~4.4 de altura.
                    .sized(0.9F, 4.4F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .fireImmune()
                    .build("watcher"));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::onAttributeCreate);
    }

    private static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(WATCHER.get(), WatcherEntity.createAttributes().build());
    }
}
