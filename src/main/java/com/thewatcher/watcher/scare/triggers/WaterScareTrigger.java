package com.thewatcher.watcher.scare.triggers;

import com.thewatcher.watcher.data.WatcherWorldData;
import com.thewatcher.watcher.scare.ScareFx;
import com.thewatcher.watcher.scare.ScareManager;
import com.thewatcher.watcher.scare.ScareType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Susto de água: ao emergir da água e olhar para cima, o Watcher está na
 * superfície — por apenas 1 frame.
 */
public class WaterScareTrigger implements ScareTrigger {

    private static final int DURATION_TICKS = 2;        // "1 frame"
    private static final float LOOK_UP_PITCH = -20.0F;  // pitch negativo = olhando para cima
    private static final double SPAWN_AHEAD = 3.0D;

    private final Map<UUID, State> states = new HashMap<>();

    @Override
    public ScareType type() {
        return ScareType.WATER;
    }

    @Override
    public boolean tick(ServerPlayer player, WatcherWorldData data) {
        State s = states.computeIfAbsent(player.getUUID(), id -> new State());
        boolean inWater = player.isInWater();
        boolean emerged = s.wasInWater && !inWater;
        s.wasInWater = inWater;
        return emerged && player.getXRot() <= LOOK_UP_PITCH;
    }

    @Override
    public void execute(ServerPlayer player, WatcherWorldData data, ScareManager manager) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = ScareFx.horizontalLook(player.getLookAngle());
        double sx = player.getX() + dir.x * SPAWN_AHEAD;
        double sz = player.getZ() + dir.z * SPAWN_AHEAD;
        // Na superfície, ~nível do jogador que acabou de emergir.
        ScareFx.spawnWatcher(level, sx, player.getY(), sz, player.position(), manager, DURATION_TICKS);
    }

    @Override
    public void forget(UUID playerId) {
        states.remove(playerId);
    }

    private static final class State {
        boolean wasInWater;
    }
}
