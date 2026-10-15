package com.thewatcher.watcher.scare.triggers;

import com.thewatcher.watcher.data.WatcherWorldData;
import com.thewatcher.watcher.entity.WatcherEntity;
import com.thewatcher.watcher.registry.ModEntities;
import com.thewatcher.watcher.registry.ModSounds;
import com.thewatcher.watcher.scare.ScareManager;
import com.thewatcher.watcher.scare.ScareType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Susto de mineração.
 *
 * Condição: o jogador está minerando (em local escuro/subterrâneo) e passa
 * 3+ minutos "sem olhar para trás" — ou seja, mantendo aproximadamente a mesma
 * direção de visão. Quando dispara, o Watcher aparece atrás do jogador com um
 * screech e some logo depois.
 *
 * Como não dependemos (ainda) de eventos de quebra de bloco, usamos uma proxy
 * de "postura de mineração": o jogador está com pouca/nenhuma luz do céu acima
 * (ou abaixo de Y=50). Quando o jogador vira significativamente (olha para
 * trás), o contador zera.
 */
public class MiningScareTrigger implements ScareTrigger {

    /** 3 minutos de mineração focada. */
    private static final int MINING_THRESHOLD_TICKS = 3 * 60 * 20; // 3600
    /** Giro (em graus) que conta como "olhou para trás" e zera o contador. */
    private static final float LOOK_BACK_DEGREES = 120.0F;
    /** Y abaixo do qual consideramos subterrâneo mesmo com alguma luz. */
    private static final int UNDERGROUND_Y = 50;
    /** Distância (blocos) que o Watcher aparece atrás do jogador. */
    private static final double SPAWN_DISTANCE = 3.0D;
    /** Duração do susto antes da entidade sumir (~2s). */
    private static final int SCARE_DURATION_TICKS = 40;

    private final Map<UUID, MiningState> states = new HashMap<>();

    @Override
    public ScareType type() {
        return ScareType.MINING;
    }

    @Override
    public boolean tick(ServerPlayer player, WatcherWorldData data) {
        MiningState state = states.computeIfAbsent(player.getUUID(), id -> new MiningState());
        float yaw = player.getYRot();

        if (!state.initialized) {
            state.referenceYaw = yaw;
            state.initialized = true;
        }

        // Virou para trás? Zera e redefine a referência de direção.
        float fromReference = Math.abs(Mth.degreesDifference(state.referenceYaw, yaw));
        if (fromReference > LOOK_BACK_DEGREES) {
            state.miningTicks = 0;
            state.referenceYaw = yaw;
            return false;
        }

        if (isMiningPosture(player)) {
            state.miningTicks++;
        } else {
            // Fora de postura de mineração: esfria o contador aos poucos.
            state.miningTicks = Math.max(0, state.miningTicks - 2);
        }

        return state.miningTicks >= MINING_THRESHOLD_TICKS;
    }

    private static boolean isMiningPosture(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        boolean dark = level.getBrightness(LightLayer.SKY, pos) == 0;
        return dark || player.getY() < UNDERGROUND_Y;
    }

    @Override
    public void execute(ServerPlayer player, WatcherWorldData data, ScareManager manager) {
        ServerLevel level = player.serverLevel();

        // Posição atrás do jogador (oposta à direção de visão horizontal).
        Vec3 look = player.getLookAngle();
        double horiz = Math.sqrt(look.x * look.x + look.z * look.z);
        double dirX = horiz > 1.0E-4D ? look.x / horiz : 0.0D;
        double dirZ = horiz > 1.0E-4D ? look.z / horiz : 1.0D;

        double spawnX = player.getX() - dirX * SPAWN_DISTANCE;
        double spawnZ = player.getZ() - dirZ * SPAWN_DISTANCE;
        BlockPos ground = findGround(level, spawnX, player.getBlockY(), spawnZ);

        WatcherEntity watcher = ModEntities.WATCHER.get().create(level);
        if (watcher == null) {
            return;
        }

        watcher.moveTo(spawnX, ground.getY(), spawnZ, 0.0F, 0.0F);
        faceTarget(watcher, player.position());
        level.addFreshEntity(watcher);

        level.playSound(null, player.blockPosition(),
                ModSounds.WATCHER_SCREECH.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

        manager.registerScareEntity(watcher, SCARE_DURATION_TICKS);

        // Esse local vira uma zona de mineração memorizada.
        data.addMiningZone(player.blockPosition());

        // Reseta o estado para não re-disparar assim que o cooldown acabar.
        states.remove(player.getUUID());
    }

    /** Procura o chão perto de (x,z) a partir de {@code startY}, descendo. */
    private static BlockPos findGround(ServerLevel level, double x, int startY, double z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                Mth.floor(x), startY + 1, Mth.floor(z));
        for (int i = 0; i < 6; i++) {
            BlockPos below = cursor.below();
            if (!level.getBlockState(below).isAir() && level.getBlockState(cursor).isAir()) {
                return cursor.immutable();
            }
            cursor.move(0, -1, 0);
        }
        return new BlockPos(Mth.floor(x), startY, Mth.floor(z));
    }

    /** Orienta a entidade para encarar um alvo (apenas yaw horizontal). */
    private static void faceTarget(WatcherEntity entity, Vec3 target) {
        double dx = target.x - entity.getX();
        double dz = target.z - entity.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
    }

    @Override
    public void forget(UUID playerId) {
        states.remove(playerId);
    }

    /** Estado por jogador deste gatilho. */
    private static final class MiningState {
        boolean initialized = false;
        float referenceYaw = 0.0F;
        int miningTicks = 0;
    }
}
