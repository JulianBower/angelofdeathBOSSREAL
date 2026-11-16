package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.item.AngelWingsItem;
import com.angelofdeath.boss.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws the Wings of the Angel of Death on the back of a player wearing them: textured wings + boost glow mask. */
public class AngelOfDeathWingsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/angel_wings.png");
    public static final ResourceLocation GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/angel_wings_glow.png");

    private final AngelOfDeathWingsModel model;

    public AngelOfDeathWingsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                                  EntityModelSet models) {
        super(parent);
        this.model = new AngelOfDeathWingsModel(models.bakeLayer(ModModelLayers.WINGS));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (!AngelWingsItem.isWearing(player) || player.isInvisible()) {
            return;
        }
        float glow = Mth.clamp(player.getData(ModAttachments.BOOST_GLOW_ALPHA), 0.0F, 1.0F);
        boolean flying = player.getAbilities().flying || (!player.onGround() && !player.isInWater());

        poseStack.pushPose();
        // Attach to the torso so crouching / swimming tilts the wings with the body, then move the wing pivot
        // (13.7 px down in the export) up to the shoulder blades.
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, -9.7F / 16.0F, 0.0F);
        poseStack.translate(0.0F, 0.05F, 0.24F); // out onto the surface of the back

        this.model.pose(ageInTicks, flying, glow);
        this.model.setRootX(1.5F, -1.5F); // left_wing.x / right_wing.x, roots pulled toward the spine
        this.model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight,
                OverlayTexture.NO_OVERLAY, -1);

        if (glow > 0.01F) {
            int color = ((int) (glow * 255.0F) << 24) | 0x00FFFFFF;
            this.model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucentEmissive(GLOW_TEXTURE)),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
        }
        poseStack.popPose();
    }
}
