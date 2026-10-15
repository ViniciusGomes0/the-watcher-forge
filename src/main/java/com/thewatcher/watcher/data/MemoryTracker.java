package com.thewatcher.watcher.data;

import com.thewatcher.watcher.entity.WatcherMemory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Alimenta a {@link WatcherMemory} a partir do que o jogador faz.
 *
 * Por amostragem no tick do servidor:
 * <ul>
 *   <li><b>heatmap</b>: registra o bloco do jogador a cada ~1s;</li>
 *   <li><b>routeSegments</b>: acumula pontos conforme o jogador anda e fecha um
 *       segmento quando ele para por um tempo, quando o segmento enche, ou no
 *       logout;</li>
 *   <li><b>avgIdlePos</b>: mistura a posição quando o jogador fica parado;</li>
 *   <li><b>sleepPositions</b>: registra a cama uma vez por dormida;</li>
 *   <li><b>totalDaysTracked</b>: avança a cada virada de dia (tempo do Overworld),
 *       destravando as fases.</li>
 * </ul>
 * Os horários de login (<b>loginTimes</b>) entram via {@link #onLogin}.
 *
 * Observação: a memória é por dimensão. O avanço de dias usa o tempo do
 * Overworld e grava nos dados do Overworld; em outras dimensões a fase não
 * avança sozinha (consequência da memória por dimensão — centralizar numa
 * memória global é um passo isolado).
 */
public final class MemoryTracker {

    private static final int SAMPLE_INTERVAL = 10;       // ~0.5s entre amostras de rota
    private static final int HEATMAP_INTERVAL = 20;      // ~1s entre marcas de heatmap
    private static final double MIN_MOVE = 1.0D;         // blocos para um novo ponto de rota
    private static final int IDLE_SAMPLES_TO_FLUSH = 6;  // ~3s parado fecha o segmento
    private static final double IDLE_BLEND = 0.05D;      // peso da amostra de ociosidade
    private static final long TICKS_PER_DAY = 24000L;

    private static final MemoryTracker INSTANCE = new MemoryTracker();

    public static MemoryTracker get() {
        return INSTANCE;
    }

    private final Map<UUID, Tracked> tracked = new HashMap<>();
    private int currentTick;

    private MemoryTracker() {
    }

    /** Chamado uma vez por tick do servidor. */
    public void serverTick(MinecraftServer server) {
        currentTick = server.getTickCount();

        for (ServerLevel level : server.getAllLevels()) {
            WatcherWorldData data = WatcherWorldData.get(level);

            if (level.dimension().equals(Level.OVERWORLD)) {
                advanceDays(level, data);
            }

            for (ServerPlayer player : level.players()) {
                sample(player, data);
            }
        }
    }

    // =====================================================================
    //  Avanço de dias
    // =====================================================================

    private void advanceDays(ServerLevel level, WatcherWorldData data) {
        long dayIndex = level.getDayTime() / TICKS_PER_DAY;
        long last = data.getMemory().getLastDayIndex();

        if (last < 0) {
            // Primeira vez: ancora sem contar um dia.
            data.setLastDayIndex(dayIndex);
        } else if (dayIndex > last) {
            for (long d = last; d < dayIndex; d++) {
                data.incrementDaysTracked();
            }
            data.setLastDayIndex(dayIndex);
        }
    }

    // =====================================================================
    //  Amostragem por jogador
    // =====================================================================

    private void sample(ServerPlayer player, WatcherWorldData data) {
        Tracked t = tracked.computeIfAbsent(player.getUUID(), id -> new Tracked());
        Vec3 pos = player.position();

        // Sono: registra a cama uma vez por dormida.
        if (player.isSleeping()) {
            if (!t.recordedSleep) {
                player.getSleepingPos().ifPresent(data::addSleepPosition);
                t.recordedSleep = true;
            }
        } else {
            t.recordedSleep = false;
        }

        // Heatmap.
        if (currentTick - t.lastHeatmapTick >= HEATMAP_INTERVAL) {
            t.lastHeatmapTick = currentTick;
            data.recordVisit(player.blockPosition());
        }

        // Rotas + ociosidade.
        if (currentTick - t.lastSampleTick >= SAMPLE_INTERVAL) {
            t.lastSampleTick = currentTick;
            sampleRoute(t, data, pos);
        }
    }

    private void sampleRoute(Tracked t, WatcherWorldData data, Vec3 pos) {
        if (t.lastSamplePos == null) {
            t.lastSamplePos = pos;
            t.currentRoute.add(pos);
            return;
        }

        double moved = pos.distanceTo(t.lastSamplePos);
        if (moved >= MIN_MOVE) {
            t.currentRoute.add(pos);
            t.lastSamplePos = pos;
            t.idleSamples = 0;
            if (t.currentRoute.size() >= WatcherMemory.MAX_ROUTE_POINTS) {
                flushRoute(t, data, pos);
            }
        } else {
            // Praticamente parado: aprende o ponto de ociosidade e, se persistir,
            // fecha o segmento atual.
            t.idleSamples++;
            data.blendIdlePos(pos, IDLE_BLEND);
            if (t.idleSamples >= IDLE_SAMPLES_TO_FLUSH) {
                flushRoute(t, data, pos);
                t.idleSamples = 0;
            }
        }
    }

    private void flushRoute(Tracked t, WatcherWorldData data, Vec3 seed) {
        if (t.currentRoute.size() >= 2) {
            data.addRouteSegment(t.currentRoute.toArray(new Vec3[0]));
        }
        t.currentRoute.clear();
        t.currentRoute.add(seed);
        t.lastSamplePos = seed;
    }

    // =====================================================================
    //  Eventos de sessão
    // =====================================================================

    public void onLogin(ServerPlayer player) {
        WatcherWorldData data = WatcherWorldData.get(player.serverLevel());
        data.recordLoginTime(System.currentTimeMillis());
        tracked.remove(player.getUUID()); // nova sessão → nova trilha
    }

    public void onLogout(ServerPlayer player) {
        Tracked t = tracked.remove(player.getUUID());
        if (t != null && t.currentRoute.size() >= 2) {
            WatcherWorldData data = WatcherWorldData.get(player.serverLevel());
            data.addRouteSegment(t.currentRoute.toArray(new Vec3[0]));
        }
    }

    /** Estado de rastreamento por jogador. */
    private static final class Tracked {
        Vec3 lastSamplePos;
        final List<Vec3> currentRoute = new ArrayList<>();
        int idleSamples;
        int lastSampleTick;
        int lastHeatmapTick;
        boolean recordedSleep;
    }
}
