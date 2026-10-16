package com.thewatcher.watcher.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.thewatcher.watcher.TheWatcherMod;
import com.thewatcher.watcher.entity.WatcherEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sobreposição de horror desenhada por cima da tela (HUD): vinheta fechando as
 * bordas, grão de filme animado e um véu frio escuro. Confiável (não mexe no
 * post-processing do jogo). A intensidade sobe à noite e perto do Watcher.
 */
@Mod.EventBusSubscriber(modid = TheWatcherMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WatcherShaderHandler {

    /** Liga/desliga o clima de horror. */
    private static final boolean ENABLED = true;

    private static final float BASE = 0.08F;      // quase nada de dia (não incomoda)
    private static final float NIGHT = 0.50F;     // escurece à noite
    private static final float PROXIMITY = 0.35F; // aperta perto do Watcher
    private static final double MAX_DISTANCE = 24.0D;

    private static final ResourceLocation VIGNETTE =
            new ResourceLocation(TheWatcherMod.MOD_ID, "textures/misc/dread_vignette.png");
    private static final ResourceLocation GRAIN =
            new ResourceLocation(TheWatcherMod.MOD_ID, "textures/misc/dread_grain.png");

    private WatcherShaderHandler() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }

        float night = Mth.clamp(mc.level.getStarBrightness(1.0F) * 2.0F, 0.0F, 1.0F);
        float prox = 0.0F;
        WatcherEntity nearest = findNearest(mc);
        if (nearest != null) {
            double dist = mc.player.distanceTo(nearest);
            prox = (float) Mth.clamp(1.0D - dist / MAX_DISTANCE, 0.0D, 1.0D);
        }
        float intensity = Mth.clamp(BASE + NIGHT * night + PROXIMITY * prox, 0.0F, 1.0F);
        if (intensity <= 0.02F) {
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Véu frio escuro (tom doentio). O alpha do fill ESCALA com a intensidade
        // de forma confiável (é a variação dia/noite/proximidade).
        int veilA = (int) (intensity * 90.0F);
        g.fill(0, 0, w, h, (veilA << 24) | 0x0A1310);

        // Vinheta discreta (alpha assado na textura). Sem grão: o chiado incomodava.
        g.blit(VIGNETTE, 0, 0, w, h, 0.0F, 0.0F, 512, 512, 512, 512);

        RenderSystem.disableBlend();
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
}
