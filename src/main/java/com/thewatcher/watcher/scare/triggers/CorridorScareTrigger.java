package com.thewatcher.watcher.scare.triggers;

import com.thewatcher.watcher.data.WatcherWorldData;
import com.thewatcher.watcher.scare.ScareFx;
import com.thewatcher.watcher.scare.ScareManager;
import com.thewatcher.watcher.scare.ScareType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Susto de corredor: quando o jogador está num túnel estreito (paredes dos dois
 * lados) e há um corredor aberto à frente, o Watcher aparece no fim do corredor
 * e recua para a escuridão.
 */
public class CorridorScareTrigger implements ScareTrigger {

    private static final int DURATION_TICKS = 24;
    private static final int MIN_CORRIDOR_LENGTH = 6;
    private static final int MAX_SCAN = 14;

    @Override
    public ScareType type() {
        return ScareType.CORRIDOR;
    }

    @Override
    public boolean tick(ServerPlayer player, WatcherWorldData data) {
        return corridorLength(player) >= MIN_CORRIDOR_LENGTH;
    }

    @Override
    public void execute(ServerPlayer player, WatcherWorldData data, ScareManager manager) {
        ServerLevel level = player.serverLevel();
        int length = corridorLength(player);
        Vec3 dir = ScareFx.horizontalLook(player.getLookAngle());

        double sx = player.getX() + dir.x * length;
        double sz = player.getZ() + dir.z * length;
        BlockPos ground = ScareFx.findGround(level, sx, player.getBlockY(), sz);
        ScareFx.spawnWatcher(level, sx, ground.getY(), sz, player.position(), manager, DURATION_TICKS);
    }

    /**
     * Comprimento do corredor aberto à frente, 0 se não for um túnel estreito.
     * "Estreito" = blocos sólidos à esquerda e à direita, na altura do jogador.
     */
    private static int corridorLength(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = ScareFx.horizontalLook(player.getLookAngle());
        BlockPos base = player.blockPosition();

        // Perpendicular à direção de visão.
        double px = -dir.z;
        double pz = dir.x;
        BlockPos left = BlockPos.containing(base.getX() + px, base.getY(), base.getZ() + pz);
        BlockPos right = BlockPos.containing(base.getX() - px, base.getY(), base.getZ() - pz);
        boolean narrow = !level.getBlockState(left).isAir() && !level.getBlockState(right).isAir();
        if (!narrow) {
            return 0;
        }

        int open = 0;
        for (int i = 1; i <= MAX_SCAN; i++) {
            BlockPos ahead = BlockPos.containing(base.getX() + dir.x * i, base.getY(), base.getZ() + dir.z * i);
            if (level.getBlockState(ahead).isAir() && level.getBlockState(ahead.above()).isAir()) {
                open = i;
            } else {
                break;
            }
        }
        return open;
    }
}
