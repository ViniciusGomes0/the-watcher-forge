package com.thewatcher.watcher.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thewatcher.watcher.TheWatcherMod;
import com.thewatcher.watcher.entity.WatcherEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer do Watcher.
 *
 * Desenha o modelo sempre translúcido, com a opacidade variando por fase
 * (15% na Observação → mais sólido conforme evolui). A fase vem sincronizada
 * do servidor via {@link WatcherEntity#getClientPhase()}.
 */
public class WatcherRenderer extends MobRenderer<WatcherEntity, WatcherModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TheWatcherMod.MOD_ID, "textures/entity/watcher.png");

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new WatcherModel(context.bakeLayer(WatcherModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(WatcherEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(WatcherEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.model.alpha = alphaForPhase(entity.getClientPhase());
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected RenderType getRenderType(WatcherEntity entity, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        // Sempre translúcido: o blending é o que deixa a opacidade por fase visível.
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }

    private static float alphaForPhase(int phase) {
        return switch (phase) {
            case 2 -> 0.25F;  // Presença
            case 3 -> 0.45F;  // Stalking
            case 4 -> 0.65F;  // Conhece Você
            default -> 0.15F; // Observação (fase 1)
        };
    }
}
