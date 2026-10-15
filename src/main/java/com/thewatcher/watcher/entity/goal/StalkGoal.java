package com.thewatcher.watcher.entity.goal;

import com.thewatcher.watcher.entity.WatcherEntity;
import com.thewatcher.watcher.entity.WatcherMemory;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Fase 3 — Stalking.
 *
 * O Watcher segue o jogador percorrendo as rotas que o próprio jogador já
 * traçou ({@link WatcherMemory#getRouteSegments()}). Escolhe o segmento cujo
 * ponto mais próximo do jogador é o menor, orienta-o para terminar perto dele
 * e caminha ponto a ponto — chegando "pelo caminho de casa", assustadoramente
 * familiar. Ao ficar perto do jogador, para e apenas observa (não encosta).
 *
 * Observação: na fase 3+ a spec prevê um {@code WatcherNavigator} customizado
 * sobre o GroundPathNavigation; aqui usamos a navegação padrão para seguir os
 * waypoints da rota. Trocar pela navegação customizada é um passo isolado.
 */
public class StalkGoal extends Goal {

    private static final double SEARCH_RANGE = 64.0D;
    private static final double SPEED = 1.0D;
    private static final double MIN_STALK_DIST = 6.0D;
    private static final double WAYPOINT_REACHED = 2.5D;
    private static final int REPATH_TICKS = 10;

    private final WatcherEntity watcher;
    private Player target;
    private Vec3[] route;
    private int wpIndex;
    private int repath;

    public StalkGoal(WatcherEntity watcher) {
        this.watcher = watcher;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (watcher.getPhaseValue() < 3) {
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
        return target != null && target.isAlive()
                && watcher.getPhaseValue() >= 3
                && watcher.distanceTo(target) <= SEARCH_RANGE * 1.5D;
    }

    @Override
    public void start() {
        this.route = buildRoute();
        this.wpIndex = route != null ? nearestIndex(route, watcher.position()) : -1;
        this.repath = 0;
    }

    @Override
    public void stop() {
        watcher.getNavigation().stop();
        this.target = null;
        this.route = null;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        watcher.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Distância de conforto: não encosta no jogador.
        if (watcher.distanceTo(target) <= MIN_STALK_DIST) {
            watcher.getNavigation().stop();
            return;
        }

        // Avança o waypoint ao alcançá-lo.
        if (route != null && wpIndex >= 0 && wpIndex < route.length
                && watcher.position().distanceTo(route[wpIndex]) <= WAYPOINT_REACHED
                && wpIndex < route.length - 1) {
            wpIndex++;
        }

        if (--repath > 0) {
            return;
        }
        repath = REPATH_TICKS;

        Vec3 dest;
        if (route != null && wpIndex >= 0 && wpIndex < route.length - 1) {
            dest = route[wpIndex];          // ainda percorrendo a rota do jogador
        } else {
            dest = target.position();       // na ponta da rota: converge no jogador
        }
        watcher.getNavigation().moveTo(dest.x, dest.y, dest.z, SPEED);
    }

    /**
     * Escolhe o segmento de rota mais perto do jogador e o orienta para que o
     * último ponto seja o mais próximo dele.
     */
    private Vec3[] buildRoute() {
        WatcherMemory mem = watcher.getMemory();
        if (mem == null) {
            return null;
        }
        List<Vec3[]> segments = mem.getRouteSegments();
        Vec3 playerPos = target.position();

        Vec3[] best = null;
        double bestDist = Double.MAX_VALUE;
        for (Vec3[] seg : segments) {
            if (seg.length == 0) {
                continue;
            }
            double d = minDistSqrToPlayer(seg, playerPos);
            if (d < bestDist) {
                bestDist = d;
                best = seg;
            }
        }
        if (best == null) {
            return null;
        }

        // Orienta: o último ponto deve ser o mais próximo do jogador.
        if (best[0].distanceToSqr(playerPos) < best[best.length - 1].distanceToSqr(playerPos)) {
            Vec3[] reversed = new Vec3[best.length];
            for (int i = 0; i < best.length; i++) {
                reversed[i] = best[best.length - 1 - i];
            }
            return reversed;
        }
        return best;
    }

    private static double minDistSqrToPlayer(Vec3[] seg, Vec3 playerPos) {
        double min = Double.MAX_VALUE;
        for (Vec3 p : seg) {
            min = Math.min(min, p.distanceToSqr(playerPos));
        }
        return min;
    }

    private static int nearestIndex(Vec3[] route, Vec3 pos) {
        int idx = 0;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < route.length; i++) {
            double d = route[i].distanceToSqr(pos);
            if (d < best) {
                best = d;
                idx = i;
            }
        }
        return idx;
    }
}
