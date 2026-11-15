package com.angelofdeath.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Procedural black-and-charcoal funnel tornado and the black/white traveling shockwave ring. */
public final class BossEffects {
    private static final int SEGMENTS = 24;
    private static final int FUNNEL_RINGS = 12;

    private BossEffects() {
    }

    /** Pose stack at the tornado base. age in ticks (with partial tick). */
    public static void renderTornado(PoseStack ps, MultiBufferSource buffer, float age, float fade) {
        VertexConsumer vc = buffer.getBuffer(RenderType.beaconBeam(SingularityVisuals.WHITE_TEXTURE, true));
        PoseStack.Pose pose = ps.last();
        float ringHeight = 1.0F;
        int alpha = (int) (215 * fade);
        for (int h = 0; h < FUNNEL_RINGS; h++) {
            float r0 = 0.8F + h * 0.55F;
            float r1 = 0.8F + (h + 1) * 0.55F;
            float y0 = h * ringHeight;
            float y1 = y0 + ringHeight;
            float twist0 = age * 0.55F + h * 0.9F;
            float twist1 = age * 0.55F + (h + 1) * 0.9F;
            for (int s = 0; s < SEGMENTS; s++) {
                double a0 = Math.PI * 2.0D * s / SEGMENTS;
                double a1 = Math.PI * 2.0D * (s + 1) / SEGMENTS;
                boolean dark = (s + h) % 2 == 0;
                int c = dark ? 15 : 42;
                quad(vc, pose, r0, y0, a0 + twist0, a1 + twist0, r1, y1, a0 + twist1, a1 + twist1, c, alpha);
            }
        }
    }

    /** Expanding ground ring; two stacked bands, one white, one black. progress 0..1. */
    public static void renderShockwave(PoseStack ps, MultiBufferSource buffer, float progress) {
        VertexConsumer vc = buffer.getBuffer(RenderType.beaconBeam(SingularityVisuals.WHITE_TEXTURE, true));
        PoseStack.Pose pose = ps.last();
        float radius = 1.5F + progress * 24.0F;
        int alpha = (int) (230 * (1.0F - progress));
        if (alpha <= 0) {
            return;
        }
        for (int s = 0; s < 64; s++) {
            double a0 = Math.PI * 2.0D * s / 64;
            double a1 = Math.PI * 2.0D * (s + 1) / 64;
            quad(vc, pose, radius, 0.1F, a0, a1, radius, 1.4F, a0, a1, 245, alpha);
            quad(vc, pose, radius - 0.9F, 0.1F, a0, a1, radius - 0.9F, 1.1F, a0, a1, 15, alpha);
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float rBottom, float yBottom, double b0, double b1,
                             float rTop, float yTop, double t0, double t1, int gray, int alpha) {
        float[][] p = {
                {(float) (Math.cos(b0) * rBottom), yBottom, (float) (Math.sin(b0) * rBottom)},
                {(float) (Math.cos(t0) * rTop), yTop, (float) (Math.sin(t0) * rTop)},
                {(float) (Math.cos(t1) * rTop), yTop, (float) (Math.sin(t1) * rTop)},
                {(float) (Math.cos(b1) * rBottom), yBottom, (float) (Math.sin(b1) * rBottom)}};
        for (int i = 0; i < 4; i++) {
            vertex(vc, pose, p[i], gray, alpha);
        }
        for (int i = 3; i >= 0; i--) {
            vertex(vc, pose, p[i], gray, alpha);
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float[] p, int gray, int alpha) {
        vc.addVertex(pose, p[0], p[1], p[2]).setColor(gray, gray, gray, alpha).setUv(0.5F, 0.5F)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
