package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.entity.VoidCrowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders both crow entity types at 3.0x Allay scale with the black or white texture. */
public class VoidCrowRenderer extends MobRenderer<VoidCrowEntity, VoidCrowModel> {
    private static final ResourceLocation BLACK =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/angel_crow_black.png");
    private static final ResourceLocation WHITE =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/angel_crow_white.png");

    public VoidCrowRenderer(EntityRendererProvider.Context context) {
        super(context, new VoidCrowModel(context.bakeLayer(ModModelLayers.CROW)), 0.6F);
    }

    @Override
    protected void scale(VoidCrowEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(3.0F, 3.0F, 3.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidCrowEntity entity) {
        return entity.isWhite() ? WHITE : BLACK;
    }
}
