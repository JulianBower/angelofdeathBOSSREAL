package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.entity.VoidCrowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** 1.8x Allay-sized crows: shadowy black (glowing eyes) or radiant spectral white. */
public class VoidCrowRenderer extends MobRenderer<VoidCrowEntity, VoidCrowModel> {
    private static final ResourceLocation BLACK =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/black_crow.png");
    private static final ResourceLocation WHITE =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/white_crow.png");
    private static final ResourceLocation EYES =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/crow_eyes.png");

    public VoidCrowRenderer(EntityRendererProvider.Context context) {
        super(context, new VoidCrowModel(context.bakeLayer(ModModelLayers.CROW)), 0.35F);
        this.addLayer(new EyesLayer<VoidCrowEntity, VoidCrowModel>(this) {
            @Override
            public RenderType renderType() {
                return RenderType.eyes(EYES);
            }
        });
    }

    @Override
    protected void scale(VoidCrowEntity crow, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.8F, 1.8F, 1.8F);
    }

    @Override
    protected int getBlockLightLevel(VoidCrowEntity crow, net.minecraft.core.BlockPos pos) {
        return crow.isWhite() ? 15 : super.getBlockLightLevel(crow, pos);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidCrowEntity crow) {
        return crow.isWhite() ? WHITE : BLACK;
    }
}
