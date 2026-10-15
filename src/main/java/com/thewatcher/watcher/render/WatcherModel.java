package com.thewatcher.watcher.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thewatcher.watcher.TheWatcherMod;
import com.thewatcher.watcher.entity.WatcherEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Modelo do Watcher — humanoide alto e emaciado, de acordo com a referência:
 * cabeça pequena, lisa e sem rosto; pescoço fino; tronco estreito; braços
 * muito longos (quase até os joelhos); pernas longas e finas.
 *
 * Unidades do Minecraft: 16 = 1 bloco. O modelo tem ~45 unidades (~2.8 blocos).
 */
public class WatcherModel extends HierarchicalModel<WatcherEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(TheWatcherMod.MOD_ID, "watcher"), "main");

    /** Opacidade aplicada no render (0..1), ajustada por fase pelo renderer. */
    public float alpha = 1.0F;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public WatcherModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Cabeça pequena, ovalada, sem rosto.
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.5F, -6.0F, -2.5F, 5.0F, 6.0F, 5.0F),
                PartPose.offset(0.0F, -3.0F, 0.0F));

        // Pescoço fino + tronco estreito e alongado.
        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 20).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F)     // pescoço
                        .texOffs(0, 40).addBox(-3.0F, 0.0F, -1.5F, 6.0F, 18.0F, 3.0F),    // tronco
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Braços muito longos (descem até perto das pernas).
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 0)
                        .addBox(-1.5F, -1.0F, -1.5F, 3.0F, 26.0F, 3.0F),
                PartPose.offset(-4.0F, 1.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(60, 0).mirror()
                        .addBox(-1.5F, -1.0F, -1.5F, 3.0F, 26.0F, 3.0F),
                PartPose.offset(4.0F, 1.0F, 0.0F));

        // Pernas longas e finas.
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(40, 40)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 20.0F, 3.0F),
                PartPose.offset(-1.5F, 18.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(60, 40).mirror()
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 20.0F, 3.0F),
                PartPose.offset(1.5F, 18.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(WatcherEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // A cabeça acompanha o olhar.
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F);

        // Caminhada lenta e perturbadora.
        float speed = 0.4F;
        float degree = 0.9F;
        this.rightLeg.xRot = Mth.cos(limbSwing * speed) * degree * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * speed + (float) Math.PI) * degree * limbSwingAmount;
        this.rightArm.xRot = Mth.cos(limbSwing * speed + (float) Math.PI) * degree * 0.5F * limbSwingAmount;
        this.leftArm.xRot = Mth.cos(limbSwing * speed) * degree * 0.5F * limbSwingAmount;

        // Oscilação sutil quando parado (a quietude é parte do terror).
        float idle = Mth.cos(ageInTicks * 0.05F) * 0.02F;
        this.rightArm.zRot = 0.05F + idle;
        this.leftArm.zRot = -0.05F - idle;
    }

    /** Multiplica a opacidade pelo fator de fase antes de desenhar. */
    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vertexConsumer, int light, int overlay,
                               float r, float g, float b, float a) {
        super.renderToBuffer(pose, vertexConsumer, light, overlay, r, g, b, a * this.alpha);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
