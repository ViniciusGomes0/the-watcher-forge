package com.thewatcher.watcher.data;

import com.thewatcher.watcher.entity.WatcherMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/**
 * Container persistente da {@link WatcherMemory}, salvo no armazenamento de
 * dados do nível (WorldSavedData / NBT em {@code data/thewatcher_watcher.dat}).
 *
 * Os dados ficam por dimensão. Use {@link #get(ServerLevel)} para obter (ou
 * criar) a instância do nível. Todos os mutadores aqui marcam o dado como
 * "dirty" para garantir a gravação.
 */
public class WatcherWorldData extends SavedData {

    /** Nome do arquivo de dados (sem extensão). */
    public static final String DATA_NAME = "thewatcher_watcher";

    private final WatcherMemory memory;

    public WatcherWorldData() {
        this.memory = new WatcherMemory();
    }

    /**
     * Obtém (ou cria) os dados do Watcher para o nível informado.
     * Deve ser chamado apenas no lado servidor.
     */
    public static WatcherWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                WatcherWorldData::load, WatcherWorldData::new,
                DATA_NAME);
    }

    /** Desserializador usado pela SavedData.Factory. */
    public static WatcherWorldData load(CompoundTag tag) {
        WatcherWorldData data = new WatcherWorldData();
        data.memory.load(tag);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        memory.save(tag);
        return tag;
    }

    // =====================================================================
    //  Acesso
    // =====================================================================

    /**
     * Acesso direto à memória para <em>leitura</em>. Para mutações, prefira os
     * atalhos abaixo (que marcam dirty) ou chame {@link #setDirty()} após editar.
     */
    public WatcherMemory getMemory() {
        return memory;
    }

    // =====================================================================
    //  Atalhos de mutação (marcam dirty automaticamente)
    // =====================================================================

    public void recordVisit(BlockPos pos) {
        memory.recordVisit(pos);
        setDirty();
    }

    public void addRouteSegment(Vec3[] segment) {
        memory.addRouteSegment(segment);
        setDirty();
    }

    public void addSleepPosition(BlockPos pos) {
        memory.addSleepPosition(pos);
        setDirty();
    }

    public void addMiningZone(BlockPos pos) {
        memory.addMiningZone(pos);
        setDirty();
    }

    public void recordLoginTime(long epochMillis) {
        memory.recordLoginTime(epochMillis);
        setDirty();
    }

    public void blendIdlePos(Vec3 pos, double weight) {
        memory.blendIdlePos(pos, weight);
        setDirty();
    }

    public void setAvgIdlePos(Vec3 pos) {
        memory.setAvgIdlePos(pos);
        setDirty();
    }

    public void incrementDaysTracked() {
        memory.incrementDaysTracked();
        setDirty();
    }

    public void setTotalDaysTracked(int days) {
        memory.setTotalDaysTracked(days);
        setDirty();
    }

    public void setLastDayIndex(long dayIndex) {
        memory.setLastDayIndex(dayIndex);
        setDirty();
    }

    public void setPhase(int phase) {
        memory.setPhase(phase);
        setDirty();
    }
}
