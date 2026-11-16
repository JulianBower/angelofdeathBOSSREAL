package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.entity.AngelOfDeathBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Mounts the player-wing AngelOfDeathWingsModel on the boss' back at ~1.35x, textured per phase. */
public class BossWingsLayer extends RenderLayer<AngelOfDeathBossEntity, AngelOfDeathBossModel> {
    private static final ResourceLocation PHASE_1 =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/angel_wings.png");
    private static final ResourceLocation PHASE_2 =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/angel_wings_phase2.png");
    private static final float SCALE = 1.35F;

    private final AngelOfDeathWingsModel wings;

    public BossWingsLayer(RenderLayerParent<AngelOfDeathBossEntity, AngelOfDeathBossModel> parent,
                          EntityModelSet models) {
        super(parent);
        this.wings = new AngelOfDeathWingsModel(models.bakeLayer(ModModelLayers.WINGS));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AngelOfDeathBossEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        AngelOfDeathBossModel parent = getParentModel();
        poseStack.pushPose();
        parent.body.translateAndRotate(poseStack);
        // wing pivot (0.1, 13.7, 1.3 in the export) -> boss shoulder blades (0, 3, 3.3 body-local px)
        poseStack.translate(0.0F, 3.0F / 16.0F, 3.3F / 16.0F);
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(-WingGeometry.PIVOT_X / 16.0F, -WingGeometry.PIVOT_Y / 16.0F, -WingGeometry.PIVOT_Z / 16.0F);

        this.wings.setWingAngles(parent.wingUp, parent.wingSweep);
        ResourceLocation tex = entity.getPhase() >= AngelOfDeathBossEntity.PHASE_TWO ? PHASE_2 : PHASE_1;
        this.wings.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(tex)), packedLight,
                LivingEntityRenderer.getOverlayCoords(entity, 0.0F), -1);
        poseStack.popPose();
    }
}
