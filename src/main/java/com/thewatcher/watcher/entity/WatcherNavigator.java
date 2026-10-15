package com.thewatcher.watcher.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Navegação customizada do Watcher (fase 3+).
 *
 * Em vez de só achar o caminho mais curto, o pathfinder é enviesado para os
 * caminhos que o <em>próprio jogador</em> já percorreu
 * ({@link WatcherMemory#getRouteSegments()}): nós fora dessas rotas recebem uma
 * penalidade de custo, então o A* prefere andar pelas trilhas familiares do
 * jogador — tornando a perseguição assustadoramente íntima.
 *
 * O viés só é aplicado na fase 3+. Fora disso (ou sem rotas registradas) a
 * navegação se comporta como {@link GroundPathNavigation} normal.
 */
public class WatcherNavigator extends GroundPathNavigation {

    private final WatcherEntity watcher;
    private RouteBiasNodeEvaluator routeEvaluator;

    public WatcherNavigator(WatcherEntity entity, Level level) {
        super(entity, level);
        this.watcher = entity;
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.routeEvaluator = new RouteBiasNodeEvaluator();
        this.routeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator = this.routeEvaluator;
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    /** Todas as sobrecargas de createPath convergem aqui — atualizamos o viés antes. */
    @Override
    protected Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward,
                              int accuracy, float followRange) {
        refreshRouteBias();
        return super.createPath(targets, regionOffset, offsetUpward, accuracy, followRange);
    }

    private void refreshRouteBias() {
        if (routeEvaluator == null) {
            return;
        }
        WatcherMemory mem = watcher.getMemory();
        if (mem != null && watcher.getPhaseValue() >= 3) {
            routeEvaluator.setRouteColumns(buildColumns(mem));
        } else {
            routeEvaluator.setRouteColumns(null); // desativa o viés
        }
    }

    /** Colunas (x,z) cobertas pelas rotas, com um halo de 1 bloco para a trilha não ficar rígida. */
    private static Set<Long> buildColumns(WatcherMemory mem) {
        Set<Long> columns = new HashSet<>();
        for (Vec3[] segment : mem.getRouteSegments()) {
            for (Vec3 point : segment) {
                int x = Mth.floor(point.x);
                int z = Mth.floor(point.z);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        columns.add(packColumn(x + dx, z + dz));
                    }
                }
            }
        }
        return columns;
    }

    static long packColumn(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    /**
     * WalkNodeEvaluator que soma uma penalidade a nós fora das colunas de rota.
     * Se nenhuma coluna estiver definida, é um WalkNodeEvaluator comum.
     */
    private static final class RouteBiasNodeEvaluator extends WalkNodeEvaluator {

        /** Penalidade de custo por nó fora da rota (acumula ao longo do caminho). */
        private static final float OFF_ROUTE_PENALTY = 6.0F;

        private Set<Long> routeColumns;

        void setRouteColumns(Set<Long> columns) {
            this.routeColumns = columns;
        }

        @Override
        protected Node findAcceptedNode(int x, int y, int z, int remainingJumpHeight,
                                        double floorLevel, Direction direction, BlockPathTypes pathType) {
            Node node = super.findAcceptedNode(x, y, z, remainingJumpHeight, floorLevel, direction, pathType);
            if (node != null && routeColumns != null && !routeColumns.isEmpty()
                    && !routeColumns.contains(packColumn(node.x, node.z))) {
                node.costMalus += OFF_ROUTE_PENALTY;
            }
            return node;
        }
    }
}
