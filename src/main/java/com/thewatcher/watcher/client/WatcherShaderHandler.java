package com.thewatcher.watcher.client;

import com.thewatcher.watcher.TheWatcherMod;
import com.thewatcher.watcher.entity.WatcherEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Aplica o shader de aberração cromática/desfoque ao redor do Watcher.
 *
 * A cada frame, acha o Watcher mais próximo da câmera; se estiver dentro do
 * alcance, carrega o post-effect, calcula a intensidade (mais perto = mais
 * forte, com leve escala por fase) e projeta a posição da entidade na tela
 * para centrar o halo. Fora do alcance, descarrega o efeito.
 *
 * Os uniforms customizados (Intensity, WatcherPos) são setados buscando a lista
 * de passes do {@link PostChain} por tipo/instância — assim funciona tanto no
 * ambiente de dev quanto no jar obfuscado (sem depender de nome de campo).
 */
@Mod.EventBusSubscriber(modid = TheWatcherMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WatcherShaderHandler {

    private static final ResourceLocation EFFECT =
            new ResourceLocation(TheWatcherMod.MOD_ID, "shaders/post/watcher_aberration.json");
    private static final double MAX_DISTANCE = 24.0D;

    private static boolean loaded = false;
    private static Field passesField;

    private WatcherShaderHandler() {
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameRenderer == null) {
            return;
        }

        WatcherEntity nearest = findNearest(mc);
        if (nearest == null) {
            disable(mc);
            return;
        }

        double dist = mc.player.distanceTo(nearest);
        if (dist > MAX_DISTANCE) {
            disable(mc);
            return;
        }

        enable(mc);

        // Intensidade: mais perto = mais forte, com leve escala por fase.
        float proximity = (float) Mth.clamp(1.0D - dist / MAX_DISTANCE, 0.0D, 1.0D);
        float phaseScale = 0.5F + 0.5F * (nearest.getClientPhase() / 4.0F);
        float intensity = Mth.clamp(proximity * phaseScale, 0.0F, 1.0F);

        float[] screen = projectToScreen(mc, event, nearest);
        if (screen == null) {
            // Atrás da câmera: sem halo localizado, atenua e centraliza.
            intensity *= 0.25F;
            screen = new float[]{0.5F, 0.5F};
        }

        applyUniforms(mc, intensity, screen[0], screen[1]);
    }

    private static WatcherEntity findNearest(Minecraft mc) {
        AABB box = mc.player.getBoundingBox().inflate(MAX_DISTANCE);
        WatcherEntity best = null;
        double bestSqr = Double.MAX_VALUE;
        for (WatcherEntity watcher : mc.level.getEntitiesOfClass(WatcherEntity.class, box)) {
            double d = watcher.distanceToSqr(mc.player);
            if (d < bestSqr) {
                bestSqr = d;
                best = watcher;
            }
        }
        return best;
    }

    /** Projeta o centro do Watcher para coordenadas de tela 0..1, ou null se atrás da câmera. */
    private static float[] projectToScreen(Minecraft mc, RenderLevelStageEvent event, WatcherEntity watcher) {
        Vec3 cam = event.getCamera().getPosition();
        Vec3 target = watcher.position().add(0.0D, watcher.getBbHeight() * 0.6D, 0.0D);

        Vector4f p = new Vector4f(
                (float) (target.x - cam.x),
                (float) (target.y - cam.y),
                (float) (target.z - cam.z),
                1.0F);
        p.mul(event.getPoseStack().last().pose());
        p.mul(event.getProjectionMatrix());

        if (p.w() <= 0.0F) {
            return null;
        }
        float ndcX = p.x() / p.w();
        float ndcY = p.y() / p.w();
        return new float[]{ ndcX * 0.5F + 0.5F, ndcY * 0.5F + 0.5F };
    }

    private static void enable(Minecraft mc) {
        if (!loaded) {
            mc.gameRenderer.loadEffect(EFFECT);
            loaded = true;
        }
    }

    private static void disable(Minecraft mc) {
        if (loaded) {
            mc.gameRenderer.shutdownEffect();
            loaded = false;
            passesField = null;
        }
    }

    private static void applyUniforms(Minecraft mc, float intensity, float u, float v) {
        PostChain chain = mc.gameRenderer.currentEffect();
        if (chain == null) {
            return;
        }
        try {
            List<PostPass> passes = getPasses(chain);
            if (passes == null) {
                return;
            }
            for (PostPass pass : passes) {
                EffectInstance effect = pass.getEffect();
                // safeGetUniform nunca retorna null (no-op se o uniform não existir,
                // como no pass "blit"), então não precisa de checagem.
                effect.safeGetUniform("Intensity").set(intensity);
                effect.safeGetUniform("WatcherPos").set(u, v);
            }
        } catch (Exception ignored) {
            // Melhor-esforço: se a reflexão falhar, o shader roda com os defaults.
        }
    }

    /** Acha a lista de passes por tipo/instância (independente de nome de campo). */
    @SuppressWarnings("unchecked")
    private static List<PostPass> getPasses(PostChain chain) throws IllegalAccessException {
        if (passesField == null) {
            for (Field field : PostChain.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(chain);
                    if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof PostPass) {
                        passesField = field;
                        break;
                    }
                }
            }
        }
        if (passesField == null) {
            return null;
        }
        return (List<PostPass>) passesField.get(chain);
    }
}
