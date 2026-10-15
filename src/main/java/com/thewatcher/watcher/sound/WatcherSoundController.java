package com.thewatcher.watcher.sound;

import com.thewatcher.watcher.entity.WatcherEntity;
import com.thewatcher.watcher.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Controla os sons do Watcher por código, conforme a fase e a situação.
 *
 * <ul>
 *   <li><b>Fase 1</b>: silêncio total (observação).</li>
 *   <li><b>Fase 2+</b>: respiração ambiente ocasional quando há jogador por
 *       perto, e passos cadenciados pela distância andada (não pela engine).</li>
 *   <li><b>Fase 4</b>: sussurros sub-limiares perto do jogador.</li>
 * </ul>
 *
 * O screech é disparado pelos sustos (ScareManager), não aqui.
 *
 * Uma instância por entidade — guarda timers e a última posição.
 */
public class WatcherSoundController {

    // Passos: distância (blocos) entre passos — passadas longas e lentas.
    private static final double STEP_DISTANCE = 2.2D;
    private static final float STEP_VOLUME = 0.5F;
    private static final float STEP_PITCH = 0.7F;

    // Respiração ambiente. Nota: no Minecraft o volume também define o alcance
    // (~16*volume blocos); o .ogg deve ser gravado baixo para soar "distante".
    private static final double AMBIENT_RANGE = 30.0D;
    private static final float AMBIENT_VOLUME = 0.9F;
    private static final float AMBIENT_PITCH = 0.8F;
    private static final int AMBIENT_MIN_TICKS = 80;   // ~4s
    private static final int AMBIENT_VAR_TICKS = 120;  // +0..6s

    // Sussurro (fase 4), tocado na posição do jogador.
    private static final float WHISPER_VOLUME = 0.4F;
    private static final float WHISPER_PITCH = 1.0F;
    private static final int WHISPER_MIN_TICKS = 200;  // ~10s
    private static final int WHISPER_VAR_TICKS = 400;  // +0..20s

    private int ambientTimer;
    private int whisperTimer;
    private double stepAccumulator;
    private Vec3 lastPos;

    /** Chamado a cada tick do servidor pela entidade. */
    public void tick(WatcherEntity watcher) {
        Level level = watcher.level();
        if (level.isClientSide) {
            return;
        }

        int phase = watcher.getPhaseValue();
        if (phase < 2) {
            // Fase 1: sem som. Mantém a posição atualizada para não disparar
            // uma rajada de passos ao entrar na fase 2.
            lastPos = watcher.position();
            return;
        }

        tickSteps(watcher, level);

        Player near = level.getNearestPlayer(watcher, AMBIENT_RANGE);
        tickAmbient(watcher, level, near);

        if (phase >= 4) {
            tickWhisper(watcher, level, near);
        }
    }

    private void tickSteps(WatcherEntity watcher, Level level) {
        Vec3 pos = watcher.position();
        if (lastPos != null) {
            double dx = pos.x - lastPos.x;
            double dz = pos.z - lastPos.z;
            double moved = Math.sqrt(dx * dx + dz * dz);
            if (moved > 0.01D) {
                stepAccumulator += moved;
                if (stepAccumulator >= STEP_DISTANCE) {
                    stepAccumulator = 0.0D;
                    playAtEntity(level, watcher, ModSounds.WATCHER_STEP.get(),
                            STEP_VOLUME, STEP_PITCH + jitter(watcher));
                }
            }
        }
        lastPos = pos;
    }

    private void tickAmbient(WatcherEntity watcher, Level level, Player near) {
        if (ambientTimer > 0) {
            ambientTimer--;
        }
        if (ambientTimer <= 0 && near != null) {
            playAtEntity(level, watcher, ModSounds.WATCHER_AMBIENT.get(),
                    AMBIENT_VOLUME, AMBIENT_PITCH);
            ambientTimer = AMBIENT_MIN_TICKS + watcher.getRandom().nextInt(AMBIENT_VAR_TICKS);
        }
    }

    private void tickWhisper(WatcherEntity watcher, Level level, Player near) {
        if (whisperTimer > 0) {
            whisperTimer--;
        }
        if (whisperTimer <= 0 && near != null) {
            // Na "orelha" do jogador: sub-limiar, íntimo.
            level.playSound(null, near.getX(), near.getY(), near.getZ(),
                    ModSounds.WATCHER_WHISPER.get(), SoundSource.HOSTILE,
                    WHISPER_VOLUME, WHISPER_PITCH);
            whisperTimer = WHISPER_MIN_TICKS + watcher.getRandom().nextInt(WHISPER_VAR_TICKS);
        }
    }

    private static void playAtEntity(Level level, WatcherEntity watcher, SoundEvent sound,
                                     float volume, float pitch) {
        level.playSound(null, watcher.getX(), watcher.getY(), watcher.getZ(),
                sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static float jitter(WatcherEntity watcher) {
        return (watcher.getRandom().nextFloat() - 0.5F) * 0.1F;
    }
}
