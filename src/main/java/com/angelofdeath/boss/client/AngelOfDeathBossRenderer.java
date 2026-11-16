package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.entity.AngelOfDeathBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class AngelOfDeathBossRenderer extends MobRenderer<AngelOfDeathBossEntity, AngelOfDeathBossModel> {
    private static final ResourceLocation PHASE_1 =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/angel_boss_phase1.png");
    private static final ResourceLocation PHASE_2 =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/angel_boss_phase2.png");

    public AngelOfDeathBossRenderer(EntityRendererProvider.Context context) {
        super(context, new AngelOfDeathBossModel(context.bakeLayer(ModModelLayers.BOSS)), 1.4F);
        addLayer(new BossHandGlowLayer(this));
        addLayer(new BossWingsLayer(this, context.getModelSet()));
    }

    @Override
    protected void scale(AngelOfDeathBossEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(2.0F, 2.0F, 2.0F);
    }

    @Override
    public boolean shouldRender(AngelOfDeathBossEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true; // the tornado / shockwave / orbs can be on screen while the body is not
    }

    @Override
    public ResourceLocation getTextureLocation(AngelOfDeathBossEntity entity) {
        return entity.getPhase() >= AngelOfDeathBossEntity.PHASE_TWO ? PHASE_2 : PHASE_1;
    }

    @Override
    public void render(AngelOfDeathBossEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        float time = entity.tickCount + partialTick;
        Vec3 pos = entity.getPosition(partialTick);
        float yaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        Vec3 left = new Vec3(fwd.z, 0.0D, -fwd.x);

        // palm orbs: black hole (grapple) / white hole (push), exactly where the server spawns its visual-only holes
        Vec3 hand = pos.add(0.0D, 1.8D, 0.0D).add(fwd.scale(2.2D)).add(left.scale(1.5D));
        float w1 = Mth.lerp(partialTick, entity.poseWeightO[1], entity.poseWeight[1]);
        float w3 = Mth.lerp(partialTick, entity.poseWeightO[3], entity.poseWeight[3]);
        if (w1 > 0.05F) {
            HeldSingularityRenderer.queueOrb(hand, SingularityVisuals.MODE_BLACK_HOLE, 0.7F * w1, time);
        }
        if (w3 > 0.05F && !entity.isTransitioning() && entity.getPhase() == AngelOfDeathBossEntity.PHASE_ONE) {
            HeldSingularityRenderer.queueOrb(hand, SingularityVisuals.MODE_WHITE_HOLE, 0.7F * w3, time);
        }

        // phase transition: the two massive Black & White spheres spiral together in front of him
        if (entity.isTransitioning()) {
            Vec3 focal = pos.add(0.0D, 2.6D, 0.0D).add(fwd.scale(1.5D));
            HeldSingularityRenderer.queueFusion(focal, 1.9F, entity.getTransitionTicks() + partialTick, fwd, time);
        } else if (entity.getPhase() == AngelOfDeathBossEntity.PHASE_TWO) {
            // back-mounted Phase 2 Grey Hole floating between the grey wings
            HeldSingularityRenderer.queueOrb(pos.add(0.0D, 1.8D, 0.0D).subtract(fwd.scale(1.4D)),
                    SingularityVisuals.MODE_GREY_HOLE, 1.3F, time);
        }

        // wing attack 5: traveling shockwave at the clap, then the spinning funnel
        if (entity.getAttackPose() == 5) {
            float pTicks = entity.poseTicks + partialTick;
            if (pTicks >= 9.0F && pTicks < 34.0F) {
                poseStack.pushPose();
                poseStack.translate(0.0D, entity.clientTornadoCenter != null ? entity.clientTornadoCenter.y - pos.y : 0.0D, 0.0D);
                BossEffects.renderShockwave(poseStack, buffer, (pTicks - 9.0F) / 25.0F);
                poseStack.popPose();
            }
            Vec3 c = entity.clientTornadoCenter;
            if (c != null && pTicks >= 10.0F && pTicks < 73.0F) {
                float fade = Mth.clamp(Math.min((pTicks - 10.0F) / 6.0F, (66.0F - pTicks) / 8.0F), 0.0F, 1.0F);
                poseStack.pushPose();
                poseStack.translate(c.x - pos.x, c.y - pos.y, c.z - pos.z);
                BossEffects.renderTornado(poseStack, buffer, time, fade);
                poseStack.popPose();
            }
        }
    }
}
