package com.thewatcher.watcher.scare.triggers;

import com.thewatcher.watcher.data.WatcherWorldData;
import com.thewatcher.watcher.scare.ScareManager;
import com.thewatcher.watcher.scare.ScareType;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Um gatilho de susto.
 *
 * O {@link ScareManager} chama {@link #tick} todo tick para cada jogador online
 * — mesmo durante o cooldown — para que o gatilho atualize seu estado interno.
 * Quando {@link #tick} devolve {@code true} e o cooldown/fase permitem, o
 * manager chama {@link #execute} e o gatilho realiza o susto.
 */
public interface ScareTrigger {

    /** O tipo deste susto (usado para o gate de fase). */
    ScareType type();

    /**
     * Atualiza o estado interno do gatilho e informa se a condição de susto
     * está satisfeita neste tick.
     *
     * @return {@code true} se o susto deveria disparar agora
     */
    boolean tick(ServerPlayer player, WatcherWorldData data);

    /**
     * Executa o susto. Chamado apenas quando {@link #tick} retornou {@code true}
     * e o manager liberou (cooldown e fase ok).
     */
    void execute(ServerPlayer player, WatcherWorldData data, ScareManager manager);

    /** Esquece o estado acumulado do jogador (ex.: logout ou após disparar). */
    default void forget(UUID playerId) {
    }
}
