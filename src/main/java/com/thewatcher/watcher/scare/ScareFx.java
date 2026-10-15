package com.thewatcher.watcher.scare;

import com.thewatcher.watcher.entity.WatcherEntity;
import com.thewatcher.watcher.registry.ModEntities;
import com.thewatcher.watcher.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Utilidades compartilhadas pelos gatilhos de susto: spawnar o Watcher por um
 * instante, encará-lo ao jogador, achar o chão e tocar o screech.
 */
public final class ScareFx {

    private ScareFx() {
    }

    /**
     * Spawna um Watcher temporário em (x,y,z), virado para {@code faceTarget}
     * (se não nulo), e registra para ser descartado após {@code durationTicks}.
     */
    @Nullable
    public static WatcherEntity spawnWatcher(ServerLevel level, double x, double y, double z,
                                             @Nullable Vec3 faceTarget, ScareManager manager,
                                             int durationTicks) {
        WatcherEntity watcher = ModEntities.WATCHER.get().create(level);
        if (watcher == null) {
            return null;
        }
        watcher.moveTo(x, y, z, 0.0F, 0.0F);
        if (faceTarget != null) {
            faceTarget(watcher, faceTarget);
        }
        level.addFreshEntity(watcher);
        manager.registerScareEntity(watcher, durationTicks);
        return watcher;
    }

    /** Toca o screech em uma posição. */
    public static void playScreech(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, ModSounds.WATCHER_SCREECH.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    /** Procura o chão perto de (x,z) a partir de {@code startY}, descendo. */
    public static BlockPos findGround(ServerLevel level, double x, int startY, double z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                Mth.floor(x), startY + 1, Mth.floor(z));
        for (int i = 0; i < 8; i++) {
            BlockPos below = cursor.below();
            if (!level.getBlockState(below).isAir() && level.getBlockState(cursor).isAir()) {
                return cursor.immutable();
            }
            cursor.move(0, -1, 0);
        }
        return new BlockPos(Mth.floor(x), startY, Mth.floor(z));
    }

    /** Orienta a entidade para encarar um alvo (apenas yaw horizontal). */
    public static void faceTarget(WatcherEntity entity, Vec3 target) {
        double dx = target.x - entity.getX();
        double dz = target.z - entity.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
    }

    /** Vetor de visão horizontal normalizado do jogador (fallback: +Z). */
    public static Vec3 horizontalLook(Vec3 look) {
        Vec3 h = new Vec3(look.x, 0.0D, look.z);
        return h.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : h.normalize();
    }
}
