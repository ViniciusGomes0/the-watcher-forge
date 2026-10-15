package com.thewatcher.watcher;

import com.mojang.logging.LogUtils;
import com.thewatcher.watcher.registry.ModEntities;
import com.thewatcher.watcher.registry.ModSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Ponto de entrada de "The Watcher".
 *
 * Registra os DeferredRegisters no event bus do mod e expõe o id do mod
 * usado por toda a base de código.
 */
@Mod(TheWatcherMod.MOD_ID)
public class TheWatcherMod {

    /** Id do mod. Deve bater com {@code mod_id} em gradle.properties e mods.toml. */
    public static final String MOD_ID = "thewatcher";

    public static final Logger LOGGER = LogUtils.getLogger();

    public TheWatcherMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Registros do mod (entidades, sons, ...).
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);

        // Setup comum (cliente e servidor).
        modEventBus.addListener(this::commonSetup);

        LOGGER.info("[The Watcher] Ele sabe que voce esta aqui.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Trabalho de inicialização que precisa rodar de forma thread-safe
            // (ex.: registro de atributos de entidades é feito via evento próprio).
        });
    }
}
