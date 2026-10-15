package com.thewatcher.watcher.scare.triggers;

import com.thewatcher.watcher.data.WatcherWorldData;
import com.thewatcher.watcher.scare.ScareFx;
import com.thewatcher.watcher.scare.ScareManager;
import com.thewatcher.watcher.scare.ScareType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Susto de portal: ao atravessar para o Nether/End (ou de volta), o Watcher
 * aparece por 1 frame logo após a chegada.
 */
public class PortalScareTrigger implements ScareTrigger {

    private static final int DURATION_TICKS = 2;
    private static final double SPAWN_AHEAD = 3.0D;

    private final Map<UUID, ResourceKey<Level>> lastDimension = new HashMap<>();

    @Override
    public ScareType type() {
        return ScareType.PORTAL;
    }

    @Override
    public boolean tick(ServerPlayer player, WatcherWorldData data) {
        ResourceKey<Level> current = player.level().dimension();
        ResourceKey<Level> previous = lastDimension.put(player.getUUID(), current);
        return previous != null && !previous.equals(current);
    }

    @Override
    public void execute(ServerPlayer player, WatcherWorldData data, ScareManager manager) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = ScareFx.horizontalLook(player.getLookAngle());
        double sx = player.getX() + dir.x * SPAWN_AHEAD;
        double sz = player.getZ() + dir.z * SPAWN_AHEAD;
        BlockPos ground = ScareFx.findGround(level, sx, player.getBlockY(), sz);
        ScareFx.spawnWatcher(level, sx, ground.getY(), sz, player.position(), manager, DURATION_TICKS);
    }

    @Override
    public void forget(UUID playerId) {
        lastDimension.remove(playerId);
    }
}
