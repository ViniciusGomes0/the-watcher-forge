package com.thewatcher.watcher.entity;

import com.thewatcher.watcher.data.WatcherWorldData;
import com.thewatcher.watcher.entity.goal.AnticipateGoal;
import com.thewatcher.watcher.entity.goal.ObserveGoal;
import com.thewatcher.watcher.entity.goal.StalkGoal;
import com.thewatcher.watcher.sound.WatcherSoundController;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * A entidade "The Watcher".
 *
 * Humanoide alto (2.8 blocos), excessivamente magro e sem rosto. Não ataca
 * diretamente — causa terror psicológico. O comportamento muda por fase, lida
 * da {@link WatcherMemory}:
 * <ul>
 *   <li>Fase 1-2 → {@link ObserveGoal} (observa, aparece e some)</li>
 *   <li>Fase 3   → {@link StalkGoal} (segue pelas rotas memorizadas)</li>
 *   <li>Fase 4   → {@link AnticipateGoal} (antecipa o destino do jogador)</li>
 * </ul>
 *
 * A fase é sincronizada para o cliente via {@link #DATA_PHASE} (usada depois
 * pelo render para controlar opacidade/efeitos).
 */
public class WatcherEntity extends Monster {

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(WatcherEntity.class, EntityDataSerializers.INT);

    private final WatcherSoundController soundController = new WatcherSoundController();

    public WatcherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // Persistente por natureza: nunca despawna sozinho.
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, 1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // As três Goals compartilham as flags MOVE+LOOK → só uma roda por vez.
        // A prioridade (menor = preferida) resolve o conflito por fase.
        this.goalSelector.addGoal(2, new ObserveGoal(this));
        this.goalSelector.addGoal(3, new AnticipateGoal(this));
        this.goalSelector.addGoal(4, new StalkGoal(this));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        // Navegação enviesada pelas rotas do jogador (ativa na fase 3+).
        return new WatcherNavigator(this, level);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // Mantém a fase sincronizada a partir da memória do mundo.
        if (this.tickCount % 20 == 0) {
            WatcherMemory mem = getMemory();
            if (mem != null) {
                this.entityData.set(DATA_PHASE, mem.getPhase());
            }
        }
        // Sons por código (ambient/step/whisper), conforme fase e situação.
        this.soundController.tick(this);
    }

    // Passos são controlados por código (WatcherSoundController), não pela engine.
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // no-op
    }

    /** Memória do mundo (lado servidor). {@code null} no cliente. */
    @Nullable
    public WatcherMemory getMemory() {
        if (this.level() instanceof ServerLevel serverLevel) {
            return WatcherWorldData.get(serverLevel).getMemory();
        }
        return null;
    }

    /** Fase atual: da memória no servidor, do dado sincronizado no cliente. */
    public int getPhaseValue() {
        WatcherMemory mem = getMemory();
        return mem != null ? mem.getPhase() : this.entityData.get(DATA_PHASE);
    }

    /** Fase vista pelo cliente (para o render). */
    public int getClientPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // Os sons são disparados por código (ScareManager/ambiente), não pela engine.
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
