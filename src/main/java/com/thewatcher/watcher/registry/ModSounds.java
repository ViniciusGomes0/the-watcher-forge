package com.thewatcher.watcher.registry;

import com.thewatcher.watcher.TheWatcherMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registro dos SoundEvents customizados da entidade.
 *
 * Cada entrada precisa de um objeto correspondente em
 * {@code assets/thewatcher/sounds.json} e dos arquivos .ogg.
 */
public final class ModSounds {

    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TheWatcherMod.MOD_ID);

    /** Respiração leve, volume 0.05, raio ~30 blocos. */
    public static final RegistryObject<SoundEvent> WATCHER_AMBIENT = register("watcher.ambient");
    /** Passos lentos controlados por código, não pela engine. */
    public static final RegistryObject<SoundEvent> WATCHER_STEP = register("watcher.step");
    /** Stinger agudo + ruído branco, 1.2s, volume máximo. */
    public static final RegistryObject<SoundEvent> WATCHER_SCREECH = register("watcher.screech");
    /** Sussurro sub-limiar da fase 4. */
    public static final RegistryObject<SoundEvent> WATCHER_WHISPER = register("watcher.whisper");
    /** Som de bloco arrastado, volume 0.3. */
    public static final RegistryObject<SoundEvent> WATCHER_BLOCK_MOVE = register("watcher.block_move");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(TheWatcherMod.MOD_ID, name)));
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
