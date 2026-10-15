package com.thewatcher.watcher.scare;

import com.thewatcher.watcher.data.WatcherWorldData;
import com.thewatcher.watcher.entity.WatcherEntity;
import com.thewatcher.watcher.scare.triggers.BedScareTrigger;
import com.thewatcher.watcher.scare.triggers.CorridorScareTrigger;
import com.thewatcher.watcher.scare.triggers.LoginScareTrigger;
import com.thewatcher.watcher.scare.triggers.MiningScareTrigger;
import com.thewatcher.watcher.scare.triggers.PortalScareTrigger;
import com.thewatcher.watcher.scare.triggers.ScareTrigger;
import com.thewatcher.watcher.scare.triggers.WaterScareTrigger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orquestra todos os sustos do Watcher.
 *
 * Regras centrais:
 * <ul>
 *   <li>Cooldown global obrigatório por jogador: no mínimo 4 minutos entre sustos.</li>
 *   <li>A fase 1 (Observação) nunca assusta — só da fase 2 em diante.</li>
 *   <li>No máximo um susto por jogador por tick.</li>
 * </ul>
 *
 * Deve ser alimentado por um handler de {@code ServerTickEvent} chamando
 * {@link #serverTick(MinecraftServer)} uma vez por tick (a ligação ao event bus
 * é feita na etapa de wiring do mod).
 */
public final class ScareManager {

    /** Cooldown mínimo entre sustos: 4 minutos. */
    public static final int COOLDOWN_TICKS = 4 * 60 * 20; // 4800 ticks

    /** Fase mínima para qualquer susto (fase 1 só observa). */
    public static final int MIN_ACTIVE_PHASE = 2;

    private static final ScareManager INSTANCE = new ScareManager();

    public static ScareManager get() {
        return INSTANCE;
    }

    private final List<ScareTrigger> triggers = new ArrayList<>();
    /** Último tick (global do servidor) em que cada jogador foi assustado. */
    private final Map<UUID, Integer> lastScareTick = new HashMap<>();
    /** Sustos "ao vivo": entidades spawnadas que devem sumir em breve. */
    private final List<ActiveScare> activeScares = new ArrayList<>();

    /** Relógio global do servidor, atualizado a cada {@link #serverTick}. */
    private int currentTick;

    private ScareManager() {
        triggers.add(new MiningScareTrigger());
        triggers.add(new BedScareTrigger());
        triggers.add(new WaterScareTrigger());
        triggers.add(new CorridorScareTrigger());
        triggers.add(new PortalScareTrigger());
        triggers.add(new LoginScareTrigger());
    }

    /** Chamado uma vez por tick do servidor. */
    public void serverTick(MinecraftServer server) {
        currentTick = server.getTickCount();

        for (ServerLevel level : server.getAllLevels()) {
            WatcherWorldData data = WatcherWorldData.get(level);
            int phase = data.getMemory().getPhase();

            for (ServerPlayer player : level.players()) {
                for (ScareTrigger trigger : triggers) {
                    // tick() roda sempre, para o gatilho manter seu estado.
                    boolean wants = trigger.tick(player, data);
                    if (!wants) {
                        continue;
                    }
                    if (phase >= MIN_ACTIVE_PHASE
                            && trigger.type().availableInPhase(phase)
                            && canScare(player.getUUID())) {
                        trigger.execute(player, data, this);
                        markScared(player.getUUID());
                        // forget() é só o hook de logout; cada gatilho reseta seu
                        // próprio estado dentro de execute() quando necessário.
                        break; // um susto por jogador por tick
                    }
                }
            }
        }

        expireActiveScares();
    }

    // =====================================================================
    //  Cooldown
    // =====================================================================

    /** Se o jogador já pode ser assustado de novo (respeitando o cooldown). */
    public boolean canScare(UUID playerId) {
        Integer last = lastScareTick.get(playerId);
        return last == null || (currentTick - last) >= COOLDOWN_TICKS;
    }

    private void markScared(UUID playerId) {
        lastScareTick.put(playerId, currentTick);
    }

    /** Ticks restantes de cooldown para o jogador (0 se já liberado). */
    public int cooldownRemaining(UUID playerId) {
        Integer last = lastScareTick.get(playerId);
        if (last == null) {
            return 0;
        }
        return Math.max(0, COOLDOWN_TICKS - (currentTick - last));
    }

    /** Esquece o jogador (ex.: no logout). */
    public void onPlayerLogout(UUID playerId) {
        lastScareTick.remove(playerId);
        for (ScareTrigger t : triggers) {
            t.forget(playerId);
        }
    }

    // =====================================================================
    //  Sustos ativos (entidades temporárias)
    // =====================================================================

    /**
     * Registra uma entidade de susto para ser descartada após {@code durationTicks}.
     * Chamado pelos gatilhos dentro de {@link ScareTrigger#execute}.
     */
    public void registerScareEntity(WatcherEntity entity, int durationTicks) {
        activeScares.add(new ActiveScare(entity, currentTick + durationTicks));
    }

    private void expireActiveScares() {
        Iterator<ActiveScare> it = activeScares.iterator();
        while (it.hasNext()) {
            ActiveScare scare = it.next();
            if (!scare.entity.isAlive() || currentTick >= scare.expiryTick) {
                if (scare.entity.isAlive()) {
                    scare.entity.discard();
                }
                it.remove();
            }
        }
    }

    private static final class ActiveScare {
        final WatcherEntity entity;
        final int expiryTick;

        ActiveScare(WatcherEntity entity, int expiryTick) {
            this.entity = entity;
            this.expiryTick = expiryTick;
        }
    }
}
