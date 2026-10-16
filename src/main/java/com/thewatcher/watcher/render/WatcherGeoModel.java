package com.thewatcher.watcher.render;

import com.thewatcher.watcher.TheWatcherMod;
import com.thewatcher.watcher.entity.WatcherEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Liga o modelo, a textura e as animações GeckoLib do Watcher.
 */
public class WatcherGeoModel extends GeoModel<WatcherEntity> {

    @Override
    public ResourceLocation getModelResource(WatcherEntity entity) {
        return new ResourceLocation(TheWatcherMod.MOD_ID, "geo/watcher.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WatcherEntity entity) {
        return new ResourceLocation(TheWatcherMod.MOD_ID, "textures/entity/watcher.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WatcherEntity entity) {
        return new ResourceLocation(TheWatcherMod.MOD_ID, "animations/watcher.animation.json");
    }
}
