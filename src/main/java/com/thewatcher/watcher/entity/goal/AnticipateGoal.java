package com.thewatcher.watcher.entity.goal;

import com.thewatcher.watcher.entity.WatcherEntity;
import com.thewatcher.watcher.entity.WatcherMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Fase 4 — Conhece Você.
 *
 * O Watcher prevê para onde o jogador está indo e chega lá <em>antes</em>,
 * esperando. A previsão combina as memórias:
 * <ol>
 *   <li>Ponta de uma rota memorizada que fica à frente do jogador (na direção
 *       em que ele olha/anda);</li>
 *   <li>senão, o bloco mais quente do heatmap;</li>
 *   <li>senão, uma extrapolação simples à frente do jogador.</li>
 * </ol>
 * Reavalia o destino periodicamente para acompanhar mudanças de rumo.
 */
public class AnticipateGoal extends Goal {

    private static final double SEARCH_RANGE = 64.0D;
    private static final double SPEED = 1.1D;
    private static final double ARRIVAL_SQR = 4.0D;
    private static final int REPREDICT_TICKS = 60;
    private static final double AHEAD_DOT = 0.5D;   // "à frente" = dentro de ~60°
    private static final double MIN_AHEAD = 8.0D;
    private static final double MAX_AHEAD = 40.0D;

    private final WatcherEntity watcher;
    private Player target;
    private Vec3 destination;
    private int repredict;

    public AnticipateGoal(WatcherEntity watcher) {
        this.watcher = watcher;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (watcher.getPhaseValue() < 4) {
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
        return target != null && target.isAlive() && watcher.getPhaseValue() >= 4;
    }

    @Override
    public void start() {
        this.destination = predictDestination();
        this.repredict = REPREDICT_TICKS;
        moveToDestination();
    }

    @Override
    public void stop() {
        watcher.getNavigation().stop();
        this.target = null;
        this.destination = null;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        watcher.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (--repredict <= 0) {
            repredict = REPREDICT_TICKS;
            Vec3 updated = predictDestination();
            if (destination == null || updated.distanceToSqr(destination) > 9.0D) {
                destination = updated;
                moveToDestination();
            }
        }

        // Chegou antes do jogador: espera e observa.
        if (destination != null && watcher.position().distanceToSqr(destination) <= ARRIVAL_SQR) {
            watcher.getNavigation().stop();
        }
    }

    private void moveToDestination() {
        if (destination != null) {
            watcher.getNavigation().moveTo(destination.x, destination.y, destination.z, SPEED);
        }
    }

    private Vec3 predictDestination() {
        Vec3 playerPos = target.position();
        Vec3 heading = headingOf(target);
        WatcherMemory mem = watcher.getMemory();

        if (mem != null) {
            Vec3 routeDest = bestRouteEndpointAhead(mem, playerPos, heading);
            if (routeDest != null) {
                return routeDest;
            }
            BlockPos hot = mem.getHottestBlock();
            if (hot != null) {
                return Vec3.atCenterOf(hot);
            }
        }
        return playerPos.add(heading.scale(16.0D));
    }

    /** Direção aproximada do jogador (olhar horizontal). */
    private static Vec3 headingOf(Player player) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 h = new Vec3(look.x, 0.0D, look.z);
        return h.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : h.normalize();
    }

    /** Ponta de rota à frente do jogador, dentro de uma faixa de distância. */
    private static Vec3 bestRouteEndpointAhead(WatcherMemory mem, Vec3 playerPos, Vec3 heading) {
        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;
        for (Vec3[] seg : mem.getRouteSegments()) {
            if (seg.length == 0) {
                continue;
            }
            for (Vec3 endpoint : new Vec3[]{seg[0], seg[seg.length - 1]}) {
                Vec3 toEnd = endpoint.subtract(playerPos);
                double dist = toEnd.length();
                if (dist < MIN_AHEAD || dist > MAX_AHEAD) {
                    continue;
                }
                if (heading.dot(toEnd.normalize()) < AHEAD_DOT) {
                    continue;
                }
                if (dist < bestDist) {
                    bestDist = dist;
                    best = endpoint;
                }
            }
        }
        return best;
    }
}
