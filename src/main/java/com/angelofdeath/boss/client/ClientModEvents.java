package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.item.WhiteHoleSpearItem;
import com.angelofdeath.boss.registry.ModEntities;
import com.angelofdeath.boss.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Mod-bus client registration: renderers, model layers, the wings layer on players, the B keybind. */
@EventBusSubscriber(modid = AngelOfDeathBoss.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ANGEL_OF_DEATH.get(), AngelOfDeathBossRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_CROW.get(), VoidCrowRenderer::new);
        event.registerEntityRenderer(ModEntities.WHITE_CROW.get(), VoidCrowRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_FEATHER.get(), VoidFeatherRenderer::new);
        event.registerEntityRenderer(ModEntities.SINGULARITY.get(), SingularityRenderer::new);
        event.registerEntityRenderer(ModEntities.WHITE_HOLE_SPEAR.get(), WhiteHoleSpearRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.WINGS, AngelOfDeathWingsModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.BOSS, AngelOfDeathBossModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.CROW, VoidCrowModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new AngelOfDeathWingsLayer(renderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(WingsClientHandler.BOOST_KEY);
        event.register(SpearClientHandler.TOGGLE_KEY);
    }

    /** Lets the item model swap to the glow variant while the spear holds a stored charge. */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.WHITE_HOLE_SPEAR.get(),
                ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "charged"),
                (stack, level, entity, seed) -> WhiteHoleSpearItem.isCharged(stack) ? 1.0F : 0.0F));
    }
}
