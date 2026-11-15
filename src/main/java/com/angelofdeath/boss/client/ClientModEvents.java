package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.registry.ModEntities;
import com.angelofdeath.boss.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** All mod-bus client wiring: renderers, model layers, player layer, key mapping, textures, item extensions. */
@EventBusSubscriber(modid = AngelOfDeathBoss.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SINGULARITY.get(), SingularityRenderer::new);
        event.registerEntityRenderer(ModEntities.ANGEL_OF_DEATH.get(), AngelOfDeathBossRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_CROW.get(), VoidCrowRenderer::new);
        event.registerEntityRenderer(ModEntities.WHITE_CROW.get(), VoidCrowRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_FEATHER.get(), VoidFeatherRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.WINGS, AngelWingsModel::createLayer);
        event.registerLayerDefinition(ModModelLayers.BOSS, AngelOfDeathBossModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.CROW, VoidCrowModel::createBodyLayer);
    }

    /** Wearable wings: one layer on each player skin renderer (wide and slim arms). */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new AngelOfDeathWingsLayer(renderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(WingsClientHandler.WINGS_BOOST);
    }

    /** Rebuilds the black / white / grey wing variants from wings.png whenever resources (re)load. */
    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) WingTextures::rebuild);
    }

    /** The Singularity item gently levitates in the first-person hand. */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                                   ItemStack itemInHand, float partialTick, float equipProcess,
                                                   float swingProcess) {
                poseStack.translate(0.0F, Mth.sin((player.tickCount + partialTick) * 0.12F) * 0.012F, 0.0F);
                return false;
            }
        }, ModItems.THE_SINGULARITY.get());
    }
}
