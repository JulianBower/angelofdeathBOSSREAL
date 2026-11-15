package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.entity.VoidFeatherEntity;
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

/** Flying / embedded feather. Once stuck it pulses with an emissive glow that speeds up toward detonation. */
public class VoidFeatherRenderer extends EntityRenderer<VoidFeatherEntity> {
    private static final ResourceLocation BLACK =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/item/black_feather.png");
    private static final ResourceLocation WHITE =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/item/white_feather.png");

    public VoidFeatherRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(VoidFeatherEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        ResourceLocation texture = getTextureLocation(entity);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F)); // the icon's tip points up-right; aim it along +x
        float scale = 1.1F;
        if (entity.isStuck()) {
            float age = entity.getClientStuckAge() + partialTick;
            float progress = Mth.clamp(age / VoidFeatherEntity.FUSE_TICKS, 0.0F, 1.0F);
            float pulse = 0.5F + 0.5F * Mth.sin(age * (0.4F + progress * 1.6F));
            scale += 0.25F * progress * pulse;
            drawCross(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), packedLight, 255, scale);
            int alpha = (int) (255.0F * Mth.clamp(0.25F + 0.75F * pulse * (0.4F + progress), 0.0F, 1.0F));
            drawCross(poseStack, buffer.getBuffer(RenderType.entityTranslucentEmissive(texture)),
                    LightTexture.FULL_BRIGHT, alpha, scale * 1.08F);
        } else {
            drawCross(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), packedLight, 255, scale);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void drawCross(PoseStack ps, VertexConsumer vc, int light, int alpha, float scale) {
        quad(ps, vc, light, alpha, scale);
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(90.0F));
        quad(ps, vc, light, alpha, scale);
        ps.popPose();
    }

    private static void quad(PoseStack ps, VertexConsumer vc, int light, int alpha, float s) {
        PoseStack.Pose pose = ps.last();
        float h = s * 0.5F;
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
    public ResourceLocation getTextureLocation(VoidFeatherEntity entity) {
        return entity.isWhite() ? WHITE : BLACK;
    }
}
