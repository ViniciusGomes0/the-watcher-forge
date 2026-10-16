package com.thewatcher.watcher.render;

import com.thewatcher.watcher.entity.WatcherEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Renderer GeckoLib do Watcher. A camada {@link AutoGlowingGeoLayer} usa o
 * glowmask (watcher_glowmask.png) para fazer os olhos brilharem no escuro.
 */
public class WatcherRenderer extends GeoEntityRenderer<WatcherEntity> {

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new WatcherGeoModel());
        this.shadowRadius = 0.55F;
        // A geometria mede 64 pixels (~4 blocos); mantém escala 1:1 para preservar as proporções.
        this.scaleWidth = 1.0F;
        this.scaleHeight = 1.0F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
