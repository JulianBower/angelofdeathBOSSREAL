package com.angelofdeath.boss.client;

import com.angelofdeath.boss.item.AngelWingsItem;
import com.angelofdeath.boss.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Wings of the Angel of Death on the player's back. Folded while grounded, spread and flapping in the air /
 * creative flight, swept back and glowing after a boost (synced BOOST_GLOW_ALPHA).
 */
public class AngelOfDeathWingsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final float FOLDED_UP = 1.25F;
    private static final float FOLDED_FORWARD = -0.45F;

    /** Per-player smoothed values; the layer itself is stateless between frames. */
    private static final Map<AbstractClientPlayer, float[]> SMOOTH = new WeakHashMap<>();

    private final AngelWingsModel wings;

    public AngelOfDeathWingsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                                  EntityModelSet models) {
        super(parent);
        this.wings = new AngelWingsModel(models.bakeLayer(ModModelLayers.WINGS));
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffer, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!AngelWingsItem.isWearing(player) || player.isInvisible()) {
            return;
        }
        float now = player.level().getGameTime() + partialTick;
        float[] state = SMOOTH.computeIfAbsent(player, p -> new float[] {0.0F, 0.0F, now});
        float dt = Mth.clamp(now - state[2], 0.0F, 5.0F);
        state[2] = now;

        double dy = player.getY() - player.yo;
        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        float speed = (float) Math.sqrt(dx * dx + dz * dz);
        boolean airborne = player.getAbilities().flying || (!player.onGround() && !player.isInWater());

        float glowTarget = player.getData(ModAttachments.BOOST_GLOW_ALPHA);
        state[1] += (glowTarget - state[1]) * Mth.clamp(dt * 0.5F, 0.0F, 1.0F);
        float glow = state[1];

        float openTarget = airborne ? 1.0F : Mth.clamp(speed * 3.0F, 0.0F, 0.25F);
        state[0] += (openTarget - state[0]) * Mth.clamp(dt * 0.18F, 0.0F, 1.0F);
        float open = state[0];

        // Flapping: faster and wider while climbing, slow glide while falling.
        float flapSpeed = 0.55F + Mth.clamp((float) dy * 1.5F, 0.0F, 0.5F) + speed * 0.8F;
        float amplitude = 0.38F + Mth.clamp((float) dy * 1.8F, 0.0F, 0.4F);
        float flap = Mth.sin(now * flapSpeed) * amplitude;
        float openUp = 0.30F + flap;
        float openForward = 0.0F;
        float tilt = 0.0F;
        if (airborne && dy < -0.15D && !player.getAbilities().flying) {
            openUp = 0.75F + flap * 0.25F; // catching air while falling
        }
        float scale = 1.0F;
        if (glow > 0.01F) { // boost: swept back, spread long
            openUp = Mth.lerp(glow, openUp, 0.05F + flap * 0.2F);
            openForward = Mth.lerp(glow, openForward, -0.6F);
            tilt = glow * 0.2F;
            scale = 1.0F + glow * 0.1F;
        }
        float breathe = Mth.sin(now * 0.08F) * 0.04F;
        float up = Mth.lerp(open, FOLDED_UP + breathe, openUp);
        float forward = Mth.lerp(open, FOLDED_FORWARD, openForward);

        ps.pushPose();
        this.getParentModel().body.translateAndRotate(ps);
        this.wings.pose(up, forward, tilt, scale);
        VertexConsumer solid = buffer.getBuffer(RenderType.entityCutoutNoCull(WingTextures.ORIGINAL));
        this.wings.render(ps, solid, light, OverlayTexture.NO_OVERLAY, -1);

        if (glow > 0.01F) {
            float pulse = 0.75F + 0.25F * Mth.sin(now * 0.9F);
            VertexConsumer additive = buffer.getBuffer(RenderType.eyes(WingTextures.ORIGINAL));
            // White-hot wings...
            this.wings.render(ps, additive, 15728880, OverlayTexture.NO_OVERLAY, argb(glow * pulse, 255, 255, 255));
            // ...inside a wider violet-black halo.
            this.wings.pose(up, forward, tilt, scale * 1.14F);
            this.wings.render(ps, additive, 15728880, OverlayTexture.NO_OVERLAY,
                    argb(glow * 0.55F * pulse, 150, 110, 255));
        }
        ps.popPose();
    }

    private static int argb(float alpha, int r, int g, int b) {
        return (Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24) | (r << 16) | (g << 8) | b;
    }
}
