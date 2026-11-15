package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.entity.AngelOfDeathBossEntity;
import com.angelofdeath.boss.entity.SingularityEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Boss renderer. Three layers sit on top of the body: wings (black L / white R in phase 1, luminous grey in phase 2),
 * held singularities (palm orbs + the grey hole floating behind his back) and the glowing right hand. The
 * phase-transition arcs to the spiralling holes are drawn in {@link #render}.
 */
public class AngelOfDeathBossRenderer extends MobRenderer<AngelOfDeathBossEntity, AngelOfDeathBossModel> {
    private static final ResourceLocation PHASE_1 =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/angel_of_death_phase1.png");
    private static final ResourceLocation PHASE_2 =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/angel_of_death_phase2.png");
    static final float MODEL_SCALE = 1.8F;

    public AngelOfDeathBossRenderer(EntityRendererProvider.Context context) {
        super(context, new AngelOfDeathBossModel(context.bakeLayer(ModModelLayers.BOSS)), 1.2F);
        this.addLayer(new WingsLayer(this, context));
        this.addLayer(new HeldSingularityLayer(this));
        this.addLayer(new GlowingHandLayer(this));
    }

    @Override
    protected void scale(AngelOfDeathBossEntity boss, PoseStack poseStack, float partialTick) {
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(AngelOfDeathBossEntity boss) {
        return boss.getPhase() >= 2 ? PHASE_2 : PHASE_1;
    }

    @Override
    public void render(AngelOfDeathBossEntity boss, float entityYaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        super.render(boss, entityYaw, partialTick, ps, buffer, packedLight);
        if (boss.isTransitioning()) {
            renderFusionArcs(boss, partialTick, ps, buffer);
        }
    }

    // ------------------------------------------------------------------ fusion arcs

    private void renderFusionArcs(AngelOfDeathBossEntity boss, float partialTick, PoseStack ps, MultiBufferSource buffer) {
        double bx = Mth.lerp(partialTick, boss.xo, boss.getX());
        double by = Mth.lerp(partialTick, boss.yo, boss.getY());
        double bz = Mth.lerp(partialTick, boss.zo, boss.getZ());
        List<SingularityEntity> holes = boss.level().getEntitiesOfClass(SingularityEntity.class,
                boss.getBoundingBox().inflate(24.0D),
                h -> !h.isOwnerRendered() && h.getPower() == 5 && boss.getUUID().equals(h.getOwnerUUID()));
        if (holes.isEmpty()) {
            return;
        }
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(bx, by, bz);
        float time = boss.tickCount + partialTick;
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = ps.last();
        Vec3 chest = new Vec3(0.0D, 2.3D, 0.0D);
        for (SingularityEntity hole : holes) {
            Vec3 target = new Vec3(Mth.lerp(partialTick, hole.xo, hole.getX()) - bx,
                    Mth.lerp(partialTick, hole.yo, hole.getY()) - by, Mth.lerp(partialTick, hole.zo, hole.getZ()) - bz);
            boolean white = hole.getMode() == 1;
            for (int arc = 0; arc < 3; arc++) {
                drawArc(consumer, pose, chest, target, cam, time, boss.getId() * 31L + arc * 7L + (white ? 1L : 0L),
                        white);
            }
        }
    }

    private static void drawArc(VertexConsumer consumer, PoseStack.Pose pose, Vec3 from, Vec3 to, Vec3 cam, float time,
                                long seed, boolean white) {
        RandomSource random = RandomSource.create(seed + (long) (time * 0.5F) * 7919L); // re-rolls every 2 ticks
        float pulse = 0.55F + 0.45F * Mth.sin(time * 0.7F + seed);
        int segments = 10;
        Vec3 previous = from;
        for (int i = 1; i <= segments; i++) {
            Vec3 point = from.lerp(to, i / (double) segments);
            if (i < segments) {
                double jitter = 0.5D * Math.sin(Math.PI * i / segments);
                point = point.add((random.nextDouble() - 0.5D) * jitter, (random.nextDouble() - 0.5D) * jitter,
                        (random.nextDouble() - 0.5D) * jitter);
            }
            Vec3 dir = point.subtract(previous);
            Vec3 toCam = cam.subtract(previous.add(point).scale(0.5D));
            Vec3 side = dir.cross(toCam);
            if (side.lengthSqr() > 1.0E-8D) {
                side = side.normalize();
                if (white) {
                    segment(consumer, pose, previous, point, side.scale(0.28D), 255, 255, 255, (int) (50 * pulse));
                    segment(consumer, pose, previous, point, side.scale(0.09D), 255, 255, 255, (int) (230 * pulse));
                } else {
                    segment(consumer, pose, previous, point, side.scale(0.28D), 80, 30, 170, (int) (60 * pulse));
                    segment(consumer, pose, previous, point, side.scale(0.09D), 175, 110, 255, (int) (230 * pulse));
                }
            }
            previous = point;
        }
    }

    private static void segment(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 side, int r, int g,
                                int bl, int alpha) {
        consumer.addVertex(pose, (float) (a.x - side.x), (float) (a.y - side.y), (float) (a.z - side.z)).setColor(r, g, bl, alpha);
        consumer.addVertex(pose, (float) (a.x + side.x), (float) (a.y + side.y), (float) (a.z + side.z)).setColor(r, g, bl, alpha);
        consumer.addVertex(pose, (float) (b.x + side.x), (float) (b.y + side.y), (float) (b.z + side.z)).setColor(r, g, bl, alpha);
        consumer.addVertex(pose, (float) (b.x - side.x), (float) (b.y - side.y), (float) (b.z - side.z)).setColor(r, g, bl, alpha);
    }

    // ------------------------------------------------------------------ layers

    /** Wings from the shared Blockbench model, animated from the synced pose weights. */
    private static final class WingsLayer extends RenderLayer<AngelOfDeathBossEntity, AngelOfDeathBossModel> {
        private static final float N = AngelOfDeathBossModel.N;
        //                                       0    1      2      3      4       5      6      7      8
        private static final float[] UP = {N, 0.45F, 0.45F, 0.55F, 1.35F, 0.25F, 0.10F, 0.10F, 0.55F};
        private static final float[] FORWARD = {N, 0.0F, 0.0F, 0.10F, -0.25F, 1.75F, 1.25F, 2.0F, 0.90F};
        private static final float[] TILT = {N, 0.0F, 0.0F, 0.0F, 0.40F, 0.0F, 0.25F, 0.0F, -0.10F};
        private static final float[] SCALE = {N, 1.0F, 1.0F, 1.0F, 0.90F, 1.0F, 1.0F, 0.95F, 1.15F};

        private final AngelWingsModel wings;

        WingsLayer(RenderLayerParent<AngelOfDeathBossEntity, AngelOfDeathBossModel> parent,
                   EntityRendererProvider.Context context) {
            super(parent);
            this.wings = new AngelWingsModel(context.bakeLayer(ModModelLayers.WINGS));
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buffer, int light, AngelOfDeathBossEntity boss,
                           float limbSwing, float limbSwingAmount, float partialTick, float age, float netHeadYaw,
                           float headPitch) {
            float[] w = AngelOfDeathBossModel.weights(boss, partialTick);
            float trans = Mth.lerp(partialTick, boss.transitionWeightO, boss.transitionWeight);
            float calm = 1.0F - Mth.clamp(w[5] + w[6] + w[7], 0.0F, 1.0F);
            float flap = Mth.sin(age * 0.14F) * 0.30F * calm; // graceful idle sweeps

            float up = AngelOfDeathBossModel.blend(0.35F + flap, w, UP);
            float forward = AngelOfDeathBossModel.blend(flap * 0.15F, w, FORWARD);
            float tilt = AngelOfDeathBossModel.blend(0.0F, w, TILT);
            float scale = AngelOfDeathBossModel.blend(1.0F, w, SCALE) * 1.35F;
            float pulse = 0.5F + 0.5F * Mth.sin(age * 0.6F);
            up = Mth.lerp(trans, up, 0.95F + pulse * 0.15F);       // fusion: wings flare wide
            forward = Mth.lerp(trans, forward, -0.10F);
            scale *= 1.0F + trans * 0.15F;

            boolean phaseTwo = boss.getPhase() >= 2;
            ps.pushPose();
            this.getParentModel().translateToBody(ps);
            this.wings.pose(up, forward, tilt, scale);
            int overlay = OverlayTexture.NO_OVERLAY;
            if (phaseTwo) {
                VertexConsumer grey = buffer.getBuffer(RenderType.entityCutoutNoCull(WingTextures.GREY));
                this.wings.render(ps, grey, light, overlay, -1);
            } else {
                VertexConsumer black = buffer.getBuffer(RenderType.entityCutoutNoCull(WingTextures.BLACK));
                this.wings.renderLeft(ps, black, light, overlay, -1);
                VertexConsumer white = buffer.getBuffer(RenderType.entityCutoutNoCull(WingTextures.WHITE));
                this.wings.renderRight(ps, white, light, overlay, -1);
            }

            VertexConsumer additive = buffer.getBuffer(RenderType.eyes(phaseTwo ? WingTextures.GREY : WingTextures.ORIGINAL));
            if (phaseTwo) { // ascended: steady luminous halo
                this.wings.pose(up, forward, tilt, scale * 1.07F);
                this.wings.render(ps, additive, 15728880, overlay, argb(0.30F + 0.10F * pulse, 200, 215, 255));
            }
            if (trans > 0.02F) { // fusion: wings blaze black-and-white
                this.wings.pose(up, forward, tilt, scale * 1.05F);
                this.wings.render(ps, additive, 15728880, overlay, argb(trans * (0.5F + 0.4F * pulse), 255, 255, 255));
            }
            ps.popPose();
        }

        private static int argb(float alpha, int r, int g, int b) {
            return (Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24) | (r << 16) | (g << 8) | b;
        }
    }

    /** Palm singularities (black on the grapple reach, white on the push) and the grey hole between his wings. */
    private static final class HeldSingularityLayer extends RenderLayer<AngelOfDeathBossEntity, AngelOfDeathBossModel> {
        HeldSingularityLayer(RenderLayerParent<AngelOfDeathBossEntity, AngelOfDeathBossModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buffer, int light, AngelOfDeathBossEntity boss,
                           float limbSwing, float limbSwingAmount, float partialTick, float age, float netHeadYaw,
                           float headPitch) {
            float[] w = AngelOfDeathBossModel.weights(boss, partialTick);
            Quaternionf camera = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
            float time = boss.tickCount + partialTick;
            float black = Mth.clamp(w[1] * 1.4F, 0.0F, 1.0F);
            float white = Mth.clamp(w[3] * 1.4F, 0.0F, 1.0F);
            float shimmer = 1.0F + 0.06F * Mth.sin(time * 0.5F);

            if (black > 0.05F || white > 0.05F) {
                ps.pushPose();
                this.getParentModel().translateToLeftHand(ps);
                if (black > 0.05F) {
                    HeldSingularityRenderer.renderAtModelPoint(ps, buffer, camera, SingularityVisuals.MODE_BLACK_HOLE,
                            0.55F * black * shimmer, time, 0.0F, 6.0F / 16.0F, 0.0F);
                }
                if (white > 0.05F) {
                    HeldSingularityRenderer.renderAtModelPoint(ps, buffer, camera, SingularityVisuals.MODE_WHITE_HOLE,
                            0.60F * white * shimmer, time, 0.0F, 6.0F / 16.0F, 0.0F);
                }
                ps.popPose();
            }

            float trans = Mth.lerp(partialTick, boss.transitionWeightO, boss.transitionWeight);
            if (boss.getPhase() >= 2 && !boss.isTransitioning()) {
                float grow = 1.0F - trans; // eases in after the fusion blast
                if (grow > 0.05F) {
                    ps.pushPose();
                    this.getParentModel().translateToBody(ps);
                    HeldSingularityRenderer.renderAtModelPoint(ps, buffer, camera, SingularityVisuals.MODE_GREY_HOLE,
                            0.70F * grow * shimmer, time, 0.0F, 6.0F / 16.0F, 11.0F / 16.0F);
                    ps.popPose();
                }
            }
        }
    }

    /** Fullbright white aura around the right hand and forearm while GLOWING_HAND is set. */
    private static final class GlowingHandLayer extends RenderLayer<AngelOfDeathBossEntity, AngelOfDeathBossModel> {
        GlowingHandLayer(RenderLayerParent<AngelOfDeathBossEntity, AngelOfDeathBossModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buffer, int light, AngelOfDeathBossEntity boss,
                           float limbSwing, float limbSwingAmount, float partialTick, float age, float netHeadYaw,
                           float headPitch) {
            float glow = Mth.lerp(partialTick, boss.glowWeightO, boss.glowWeight);
            if (glow < 0.02F) {
                return;
            }
            float pulse = 0.8F + 0.2F * Mth.sin(age * 0.8F);
            VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(SingularityVisuals.WHITE_TEXTURE));
            int overlay = OverlayTexture.NO_OVERLAY;
            this.getParentModel().renderRightHandGlow(ps, consumer, overlay, argb(glow * pulse, 255, 255, 255), 1.0F);
            this.getParentModel().renderRightHandGlow(ps, consumer, overlay, argb(glow * 0.35F * pulse, 190, 215, 255), 1.45F);
        }

        private static int argb(float alpha, int r, int g, int b) {
            return (Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24) | (r << 16) | (g << 8) | b;
        }
    }
}
