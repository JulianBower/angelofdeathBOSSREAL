package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.entity.VoidFeatherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Feather projectile: two crossed flat quads pointing along the flight direction (arrow-style orientation). When
 * stuck the entity's rotation no longer changes, so the feather simply stays embedded at its last angle.
 */
public class VoidFeatherRenderer extends EntityRenderer<VoidFeatherEntity> {
    private static final ResourceLocation BLACK =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/black_feather_entity.png");
    private static final ResourceLocation WHITE =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/white_feather_entity.png");
    private static final float HALF_LENGTH = 0.6F;
    private static final float HALF_WIDTH = 0.24F;

    public VoidFeatherRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(VoidFeatherEntity feather, float entityYaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, feather.yRotO, feather.getYRot()) - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, feather.xRotO, feather.getXRot())));
        // A little pulse while stuck telegraphs the coming detonation.
        float pulse = feather.isStuck() ? 1.0F + 0.08F * Mth.sin((feather.tickCount + partialTick) * 1.4F) : 1.0F;
        ps.scale(pulse, pulse, pulse);

        int light = feather.isWhite() ? 15728880 : packedLight;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(feather)));
        for (int plane = 0; plane < 2; plane++) {
            ps.pushPose();
            ps.mulPose(Axis.XP.rotationDegrees(plane * 90.0F));
            PoseStack.Pose pose = ps.last();
            vertex(consumer, pose, -HALF_LENGTH, -HALF_WIDTH, 0.0F, 1.0F, light);
            vertex(consumer, pose, HALF_LENGTH, -HALF_WIDTH, 1.0F, 1.0F, light);
            vertex(consumer, pose, HALF_LENGTH, HALF_WIDTH, 1.0F, 0.0F, light);
            vertex(consumer, pose, -HALF_LENGTH, HALF_WIDTH, 0.0F, 0.0F, light);
            ps.popPose();
        }
        ps.popPose();
        super.render(feather, entityYaw, partialTick, ps, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v,
                               int light) {
        consumer.addVertex(pose, x, y, 0.0F).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidFeatherEntity feather) {
        return feather.isWhite() ? WHITE : BLACK;
    }
}
