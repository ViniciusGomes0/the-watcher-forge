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
        this.shadowRadius = 0.8F;
        // Estica o modelo: altissimo e esqueletico (~4.4 blocos).
        this.scaleWidth = 1.1F;
        this.scaleHeight = 1.6F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
