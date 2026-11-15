package com.angelofdeath.boss.client;

import com.angelofdeath.boss.entity.AngelOfDeathBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Pure-white fullbright overlay on his right hand while GLOWING_HAND is set. */
public class BossHandGlowLayer extends RenderLayer<AngelOfDeathBossEntity, AngelOfDeathBossModel> {
    public BossHandGlowLayer(RenderLayerParent<AngelOfDeathBossEntity, AngelOfDeathBossModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AngelOfDeathBossEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        float glow = Mth.clamp(Mth.lerp(partialTick, entity.glowWeightO, entity.glowWeight), 0.0F, 1.0F);
        if (glow < 0.02F) {
            return;
        }
        AngelOfDeathBossModel model = getParentModel();
        int color = ((int) (glow * 255.0F) << 24) | 0x00FFFFFF;
        model.setHandOnly(true);
        model.renderToBuffer(poseStack,
                buffer.getBuffer(RenderType.entityTranslucentEmissive(SingularityVisuals.WHITE_TEXTURE)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
        model.setHandOnly(false);
    }
}
