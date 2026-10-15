package com.thewatcher.watcher.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A memória adaptativa de "The Watcher".
 *
 * Guarda tudo o que a entidade aprende sobre o jogador. É um simples
 * container de dados: a serialização NBT vive aqui, mas quem decide
 * <em>quando</em> salvar (marcar dirty) é o {@link com.thewatcher.watcher.data.WatcherWorldData}.
 *
 * Todas as coleções têm limites para o NBT salvo não crescer sem controle;
 * ao estourar o limite, descarta-se a entrada mais antiga (ou a de menor peso,
 * no caso do heatmap).
 */
public class WatcherMemory {

    // --- Limites de capacidade ---
    public static final int MAX_HEATMAP_ENTRIES = 4096;
    public static final int MAX_ROUTE_SEGMENTS = 128;
    public static final int MAX_ROUTE_POINTS = 64;
    public static final int MAX_SLEEP_POSITIONS = 64;
    public static final int MAX_MINING_ZONES = 256;
    public static final int MAX_LOGIN_TIMES = 128;

    // --- Chaves NBT ---
    private static final String KEY_HEATMAP = "heatmap";
    private static final String KEY_ROUTES = "routeSegments";
    private static final String KEY_SLEEP = "sleepPositions";
    private static final String KEY_MINING = "miningZones";
    private static final String KEY_LOGINS = "loginTimes";
    private static final String KEY_IDLE = "avgIdlePos";
    private static final String KEY_PHASE = "phase";
    private static final String KEY_DAYS = "totalDaysTracked";
    private static final String KEY_LAST_DAY = "lastDayIndex";

    // --- Dados aprendidos ---
    /** Frequência de visita por bloco. */
    private final Map<BlockPos, Integer> heatmap = new HashMap<>();
    /** Rotas que o jogador percorre (cada segmento é uma sequência de pontos). */
    private final List<Vec3[]> routeSegments = new ArrayList<>();
    /** Onde o jogador dormiu. */
    private final List<BlockPos> sleepPositions = new ArrayList<>();
    /** Onde minerou por mais de 2 minutos. */
    private final List<BlockPos> miningZones = new ArrayList<>();
    /** Horários de login (padrão de sessão), em epoch millis. */
    private long[] loginTimes = new long[0];
    /** Onde o jogador costuma ficar parado. */
    private Vec3 avgIdlePos = Vec3.ZERO;
    /** Fase atual (1 a 4). */
    private int phase = 1;
    /** Dias reais de jogo rastreados. */
    private int totalDaysTracked = 0;
    /** Índice do último dia já contabilizado (para detectar virada de dia). -1 = ainda não inicializado. */
    private long lastDayIndex = -1L;

    // =====================================================================
    //  Acesso de leitura
    // =====================================================================

    /** Heatmap somente-leitura. */
    public Map<BlockPos, Integer> getHeatmap() {
        return Collections.unmodifiableMap(heatmap);
    }

    /** Quantas vezes o bloco foi visitado (0 se nunca). */
    public int getHeat(BlockPos pos) {
        return heatmap.getOrDefault(pos, 0);
    }

    /** Bloco mais visitado, ou {@code null} se o heatmap estiver vazio. */
    public BlockPos getHottestBlock() {
        BlockPos best = null;
        int bestCount = Integer.MIN_VALUE;
        for (Map.Entry<BlockPos, Integer> e : heatmap.entrySet()) {
            if (e.getValue() > bestCount) {
                bestCount = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    public List<Vec3[]> getRouteSegments() {
        return Collections.unmodifiableList(routeSegments);
    }

    public List<BlockPos> getSleepPositions() {
        return Collections.unmodifiableList(sleepPositions);
    }

    public List<BlockPos> getMiningZones() {
        return Collections.unmodifiableList(miningZones);
    }

    public long[] getLoginTimes() {
        return loginTimes.clone();
    }

    public Vec3 getAvgIdlePos() {
        return avgIdlePos;
    }

    public int getPhase() {
        return phase;
    }

    public int getTotalDaysTracked() {
        return totalDaysTracked;
    }

    public long getLastDayIndex() {
        return lastDayIndex;
    }

    public void setLastDayIndex(long dayIndex) {
        this.lastDayIndex = dayIndex;
    }

    // =====================================================================
    //  Mutadores (chamados pelo WatcherWorldData, que marca dirty)
    // =====================================================================

    /** Registra uma visita ao bloco (incrementa o heatmap). */
    public void recordVisit(BlockPos pos) {
        BlockPos key = pos.immutable();
        heatmap.merge(key, 1, Integer::sum);
        if (heatmap.size() > MAX_HEATMAP_ENTRIES) {
            evictColdestBlock();
        }
    }

    private void evictColdestBlock() {
        BlockPos coldest = null;
        int coldestCount = Integer.MAX_VALUE;
        for (Map.Entry<BlockPos, Integer> e : heatmap.entrySet()) {
            if (e.getValue() < coldestCount) {
                coldestCount = e.getValue();
                coldest = e.getKey();
            }
        }
        if (coldest != null) {
            heatmap.remove(coldest);
        }
    }

    /** Adiciona uma rota percorrida pelo jogador (segmento de pontos). */
    public void addRouteSegment(Vec3[] segment) {
        if (segment == null || segment.length == 0) {
            return;
        }
        Vec3[] stored = segment;
        if (segment.length > MAX_ROUTE_POINTS) {
            // Reduz por amostragem uniforme mantendo início e fim.
            stored = downsample(segment, MAX_ROUTE_POINTS);
        }
        routeSegments.add(stored);
        while (routeSegments.size() > MAX_ROUTE_SEGMENTS) {
            routeSegments.remove(0);
        }
    }

    private static Vec3[] downsample(Vec3[] src, int target) {
        Vec3[] out = new Vec3[target];
        double step = (double) (src.length - 1) / (target - 1);
        for (int i = 0; i < target; i++) {
            out[i] = src[(int) Math.round(i * step)];
        }
        return out;
    }

    /** Registra onde o jogador dormiu. */
    public void addSleepPosition(BlockPos pos) {
        sleepPositions.add(pos.immutable());
        while (sleepPositions.size() > MAX_SLEEP_POSITIONS) {
            sleepPositions.remove(0);
        }
    }

    /** Registra uma zona de mineração prolongada. */
    public void addMiningZone(BlockPos pos) {
        miningZones.add(pos.immutable());
        while (miningZones.size() > MAX_MINING_ZONES) {
            miningZones.remove(0);
        }
    }

    /** Registra um horário de login (epoch millis), mantendo os mais recentes. */
    public void recordLoginTime(long epochMillis) {
        long[] next = Arrays.copyOf(loginTimes, loginTimes.length + 1);
        next[loginTimes.length] = epochMillis;
        if (next.length > MAX_LOGIN_TIMES) {
            next = Arrays.copyOfRange(next, next.length - MAX_LOGIN_TIMES, next.length);
        }
        loginTimes = next;
    }

    /** Define diretamente a posição média de ociosidade. */
    public void setAvgIdlePos(Vec3 pos) {
        this.avgIdlePos = pos;
    }

    /**
     * Mistura uma nova amostra de ociosidade na média corrente.
     *
     * @param pos    amostra observada
     * @param weight peso da nova amostra em [0,1] (ex.: 0.05 = média bem suave)
     */
    public void blendIdlePos(Vec3 pos, double weight) {
        double w = Math.max(0.0D, Math.min(1.0D, weight));
        if (avgIdlePos.equals(Vec3.ZERO)) {
            avgIdlePos = pos;
        } else {
            avgIdlePos = avgIdlePos.scale(1.0D - w).add(pos.scale(w));
        }
    }

    public void setPhase(int phase) {
        this.phase = Math.max(1, Math.min(4, phase));
    }

    /** Incrementa o contador de dias e recalcula a fase. */
    public void incrementDaysTracked() {
        totalDaysTracked++;
        recomputePhase();
    }

    public void setTotalDaysTracked(int days) {
        this.totalDaysTracked = Math.max(0, days);
        recomputePhase();
    }

    /**
     * Recalcula a fase a partir dos dias rastreados:
     * 1-3 → Observação, 4-7 → Presença, 8-14 → Stalking, 15+ → Conhece Você.
     */
    public void recomputePhase() {
        if (totalDaysTracked >= 15) {
            phase = 4;
        } else if (totalDaysTracked >= 8) {
            phase = 3;
        } else if (totalDaysTracked >= 4) {
            phase = 2;
        } else {
            phase = 1;
        }
    }

    // =====================================================================
    //  Serialização NBT
    // =====================================================================

    /** Escreve toda a memória dentro de {@code tag}. */
    public void save(CompoundTag tag) {
        // heatmap
        ListTag heat = new ListTag();
        for (Map.Entry<BlockPos, Integer> e : heatmap.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("p", e.getKey().asLong());
            c.putInt("c", e.getValue());
            heat.add(c);
        }
        tag.put(KEY_HEATMAP, heat);

        // routeSegments
        ListTag routes = new ListTag();
        for (Vec3[] seg : routeSegments) {
            ListTag pts = new ListTag();
            for (Vec3 v : seg) {
                pts.add(vec3ToTag(v));
            }
            CompoundTag c = new CompoundTag();
            c.put("pts", pts);
            routes.add(c);
        }
        tag.put(KEY_ROUTES, routes);

        // listas de BlockPos → long[]
        tag.putLongArray(KEY_SLEEP, toLongArray(sleepPositions));
        tag.putLongArray(KEY_MINING, toLongArray(miningZones));

        // loginTimes
        tag.putLongArray(KEY_LOGINS, loginTimes);

        // avgIdlePos
        tag.put(KEY_IDLE, vec3ToTag(avgIdlePos));

        // escalares
        tag.putInt(KEY_PHASE, phase);
        tag.putInt(KEY_DAYS, totalDaysTracked);
        tag.putLong(KEY_LAST_DAY, lastDayIndex);
    }

    /** Recarrega toda a memória a partir de {@code tag}. */
    public void load(CompoundTag tag) {
        // heatmap
        heatmap.clear();
        ListTag heat = tag.getList(KEY_HEATMAP, Tag.TAG_COMPOUND);
        for (int i = 0; i < heat.size(); i++) {
            CompoundTag c = heat.getCompound(i);
            heatmap.put(BlockPos.of(c.getLong("p")), c.getInt("c"));
        }

        // routeSegments
        routeSegments.clear();
        ListTag routes = tag.getList(KEY_ROUTES, Tag.TAG_COMPOUND);
        for (int i = 0; i < routes.size(); i++) {
            ListTag pts = routes.getCompound(i).getList("pts", Tag.TAG_LIST);
            Vec3[] seg = new Vec3[pts.size()];
            for (int j = 0; j < pts.size(); j++) {
                seg[j] = tagToVec3(pts.getList(j));
            }
            routeSegments.add(seg);
        }

        // listas de BlockPos
        sleepPositions.clear();
        for (long l : tag.getLongArray(KEY_SLEEP)) {
            sleepPositions.add(BlockPos.of(l));
        }
        miningZones.clear();
        for (long l : tag.getLongArray(KEY_MINING)) {
            miningZones.add(BlockPos.of(l));
        }

        // loginTimes
        loginTimes = tag.getLongArray(KEY_LOGINS).clone();

        // avgIdlePos
        if (tag.contains(KEY_IDLE, Tag.TAG_LIST)) {
            avgIdlePos = tagToVec3(tag.getList(KEY_IDLE, Tag.TAG_DOUBLE));
        } else {
            avgIdlePos = Vec3.ZERO;
        }

        // escalares
        phase = tag.contains(KEY_PHASE, Tag.TAG_INT) ? tag.getInt(KEY_PHASE) : 1;
        totalDaysTracked = tag.getInt(KEY_DAYS);
        lastDayIndex = tag.contains(KEY_LAST_DAY, Tag.TAG_LONG) ? tag.getLong(KEY_LAST_DAY) : -1L;
        if (phase < 1 || phase > 4) {
            recomputePhase();
        }
    }

    // --- helpers NBT ---

    private static ListTag vec3ToTag(Vec3 v) {
        ListTag l = new ListTag();
        l.add(DoubleTag.valueOf(v.x));
        l.add(DoubleTag.valueOf(v.y));
        l.add(DoubleTag.valueOf(v.z));
        return l;
    }

    private static Vec3 tagToVec3(ListTag l) {
        if (l.size() < 3) {
            return Vec3.ZERO;
        }
        return new Vec3(l.getDouble(0), l.getDouble(1), l.getDouble(2));
    }

    private static long[] toLongArray(List<BlockPos> list) {
        long[] out = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = list.get(i).asLong();
        }
        return out;
    }
}
