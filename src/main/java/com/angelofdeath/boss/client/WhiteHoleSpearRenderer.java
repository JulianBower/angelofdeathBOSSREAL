package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.entity.WhiteHoleSpearEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Thrown spear: the 32x32 pixel-art spear as crossed quads, plus the soft translucent rim glow at 0.55 alpha. */
public class WhiteHoleSpearRenderer extends EntityRenderer<WhiteHoleSpearEntity> {
    private static final ResourceLocation BASE =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/item/white_hole_spear.png");
    private static final ResourceLocation GLOW =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/item/white_hole_spear_glow.png");
    private static final float SIZE = 2.2F;

    public WhiteHoleSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WhiteHoleSpearEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F)); // the art's tip points up-right; aim it along +x

        drawCross(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(BASE)), packedLight, 255);
        float alpha = 0.55F;
        if (entity.isChargedThrow()) {
            alpha += 0.1F * Mth.sin((entity.tickCount + partialTick) * 0.6F);
        }
        drawCross(poseStack, buffer.getBuffer(RenderType.entityTranslucentEmissive(GLOW)),
                LightTexture.pack(12, 12), (int) (alpha * 255.0F));
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void drawCross(PoseStack ps, VertexConsumer vc, int light, int alpha) {
        quad(ps, vc, light, alpha);
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(90.0F));
        quad(ps, vc, light, alpha);
        ps.popPose();
    }

    private static void quad(PoseStack ps, VertexConsumer vc, int light, int alpha) {
        PoseStack.Pose pose = ps.last();
        float h = SIZE * 0.5F;
        vertex(vc, pose, -h, -h, 0.0F, 1.0F, light, alpha);
        vertex(vc, pose, h, -h, 1.0F, 1.0F, light, alpha);
        vertex(vc, pose, h, h, 1.0F, 0.0F, light, alpha);
        vertex(vc, pose, -h, h, 0.0F, 0.0F, light, alpha);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v, int light,
                               int alpha) {
        vc.addVertex(pose, x, y, 0.0F).setColor(255, 255, 255, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(WhiteHoleSpearEntity entity) {
        return BASE;
    }
}
