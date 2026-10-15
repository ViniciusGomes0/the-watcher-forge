package com.thewatcher.watcher.scare.triggers;

import com.thewatcher.watcher.data.WatcherWorldData;
import com.thewatcher.watcher.scare.ScareFx;
import com.thewatcher.watcher.scare.ScareManager;
import com.thewatcher.watcher.scare.ScareType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Susto de cama: ao acordar, 30% de chance de o Watcher estar na beira da cama,
 * parado, olhando. Silencioso — a presença é o susto.
 */
public class BedScareTrigger implements ScareTrigger {

    private static final float WAKE_CHANCE = 0.30F;
    private static final int DURATION_TICKS = 30;
    private static final double EDGE_DISTANCE = 1.5D;

    private final Map<UUID, State> states = new HashMap<>();

    @Override
    public ScareType type() {
        return ScareType.BED;
    }

    @Override
    public boolean tick(ServerPlayer player, WatcherWorldData data) {
        State s = states.computeIfAbsent(player.getUUID(), id -> new State());
        if (player.isSleeping()) {
            s.wasSleeping = true;
            return s.armed;
        }
        if (s.wasSleeping) {
            s.wasSleeping = false;
            if (player.getRandom().nextFloat() < WAKE_CHANCE) {
                s.armed = true;
            }
        }
        return s.armed;
    }

    @Override
    public void execute(ServerPlayer player, WatcherWorldData data, ScareManager manager) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = ScareFx.horizontalLook(player.getLookAngle());
        double sx = player.getX() + dir.x * EDGE_DISTANCE;
        double sz = player.getZ() + dir.z * EDGE_DISTANCE;
        BlockPos ground = ScareFx.findGround(level, sx, player.getBlockY(), sz);
        ScareFx.spawnWatcher(level, sx, ground.getY(), sz, player.position(), manager, DURATION_TICKS);

        State s = states.get(player.getUUID());
        if (s != null) {
            s.armed = false;
        }
    }

    @Override
    public void forget(UUID playerId) {
        states.remove(playerId);
    }

    private static final class State {
        boolean wasSleeping;
        boolean armed;
    }
}
