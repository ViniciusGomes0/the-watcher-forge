package com.thewatcher.watcher.entity.goal;

import com.thewatcher.watcher.entity.WatcherEntity;
import com.thewatcher.watcher.entity.WatcherMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Fase 1-2 — Observação / Presença.
 *
 * O Watcher aparece brevemente no campo periférico do jogador (ou, na fase 2,
 * em um ponto quente do heatmap), fica parado olhando por 1-2s e some. Se o
 * jogador vira e olha quase na direção dele, foge imediatamente. Entre
 * aparições há um cooldown próprio (não confundir com o cooldown de sustos).
 */
public class ObserveGoal extends Goal {

    private static final double SEARCH_RANGE = 48.0D;
    private static final double MIN_DIST = 10.0D;
    private static final double MAX_DIST = 22.0D;
    private static final int MAX_SHOW_TICKS = 40;   // ~2s
    private static final int COOLDOWN_TICKS = 200;  // ~10s base
    private static final double SPOTTED_DOT = 0.6D; // ~53° do centro da visão

    private final WatcherEntity watcher;
    private Player target;
    private int showTicks;
    private int cooldown;

    public ObserveGoal(WatcherEntity watcher) {
        this.watcher = watcher;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (watcher.getPhaseValue() > 2) {
            return false;
        }
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        Player p = watcher.level().getNearestPlayer(watcher, SEARCH_RANGE);
        if (p == null) {
            return false;
        }
        this.target = p;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return showTicks > 0 && target != null && target.isAlive() && !spotted();
    }

    @Override
    public void start() {
        Vec3 spot = chooseSpot();
        if (spot != null) {
            watcher.randomTeleport(spot.x, spot.y, spot.z, true);
        }
        this.showTicks = MAX_SHOW_TICKS / 2 + watcher.getRandom().nextInt(MAX_SHOW_TICKS / 2 + 1);
        watcher.getNavigation().stop();
    }

    @Override
    public void tick() {
        showTicks--;
        if (target != null) {
            watcher.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        fleeOutOfSight();
        this.cooldown = COOLDOWN_TICKS + watcher.getRandom().nextInt(COOLDOWN_TICKS);
        this.target = null;
    }

    private Vec3 chooseSpot() {
        if (target == null) {
            return null;
        }
        // Fase 2: prefere um ponto quente do heatmap se houver um por perto.
        if (watcher.getPhaseValue() == 2) {
            WatcherMemory mem = watcher.getMemory();
            if (mem != null) {
                BlockPos hot = mem.getHottestBlock();
                if (hot != null
                        && hot.distToCenterSqr(target.getX(), target.getY(), target.getZ()) < 40 * 40) {
                    return Vec3.atCenterOf(hot);
                }
            }
        }
        return peripheryPoint();
    }

    /** Um ponto ao lado/atrás do jogador, fora do centro da visão. */
    private Vec3 peripheryPoint() {
        double base = Math.toRadians(target.getYRot());
        double offsetDeg = 80.0D + watcher.getRandom().nextDouble() * 40.0D; // 80..120°
        double sign = watcher.getRandom().nextBoolean() ? 1.0D : -1.0D;
        double ang = base + Math.toRadians(offsetDeg) * sign;
        // Yaw do Minecraft: x = -sin(yaw), z = cos(yaw).
        double dx = -Math.sin(ang);
        double dz = Math.cos(ang);
        double d = MIN_DIST + watcher.getRandom().nextDouble() * (MAX_DIST - MIN_DIST);
        return new Vec3(target.getX() + dx * d, target.getY(), target.getZ() + dz * d);
    }

    /** O jogador está olhando quase na direção do Watcher e perto o bastante? */
    private boolean spotted() {
        if (target == null) {
            return false;
        }
        Vec3 toWatcher = watcher.position().subtract(target.getEyePosition()).normalize();
        Vec3 look = target.getViewVector(1.0F);
        double dot = look.dot(toWatcher);
        double dist = watcher.distanceTo(target);
        return dot > SPOTTED_DOT && dist < MAX_DIST * 1.5D;
    }

    /** Teleporta para longe: o "some" da observação. */
    private void fleeOutOfSight() {
        double ang = watcher.getRandom().nextDouble() * Math.PI * 2.0D;
        double d = 36.0D;
        double x = watcher.getX() + Math.cos(ang) * d;
        double z = watcher.getZ() + Math.sin(ang) * d;
        watcher.randomTeleport(x, watcher.getY(), z, true);
    }
}
