package com.thewatcher.watcher.scare.triggers;

import com.thewatcher.watcher.data.WatcherWorldData;
import com.thewatcher.watcher.scare.ScareFx;
import com.thewatcher.watcher.scare.ScareManager;
import com.thewatcher.watcher.scare.ScareType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Susto de login (fase 4): nos primeiros ~30s de sessão, o Watcher aparece e
 * desaparece. Usa a idade da entidade do jogador ({@code tickCount}), que zera
 * a cada login; dispara uma vez por sessão (resetado no logout via forget).
 */
public class LoginScareTrigger implements ScareTrigger {

    private static final int DURATION_TICKS = 20;
    private static final int WINDOW_START = 40;   // ~2s de carência (mundo carregando)
    private static final int WINDOW_END = 600;    // 30s
    private static final double SPAWN_AHEAD = 4.0D;

    private final Set<UUID> fired = new HashSet<>();

    @Override
    public ScareType type() {
        return ScareType.LOGIN;
    }

    @Override
    public boolean tick(ServerPlayer player, WatcherWorldData data) {
        if (fired.contains(player.getUUID())) {
            return false;
        }
        int age = player.tickCount;
        return age >= WINDOW_START && age <= WINDOW_END;
    }

    @Override
    public void execute(ServerPlayer player, WatcherWorldData data, ScareManager manager) {
        fired.add(player.getUUID());

        ServerLevel level = player.serverLevel();
        Vec3 dir = ScareFx.horizontalLook(player.getLookAngle());
        double sx = player.getX() + dir.x * SPAWN_AHEAD;
        double sz = player.getZ() + dir.z * SPAWN_AHEAD;
        BlockPos ground = ScareFx.findGround(level, sx, player.getBlockY(), sz);
        ScareFx.spawnWatcher(level, sx, ground.getY(), sz, player.position(), manager, DURATION_TICKS);
    }

    @Override
    public void forget(UUID playerId) {
        // Logout → libera o susto para a próxima sessão.
        fired.remove(playerId);
    }
}
