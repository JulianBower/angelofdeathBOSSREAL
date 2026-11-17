package com.angelofdeath.boss;

import com.angelofdeath.boss.entity.AngelOfDeathBossEntity;
import com.angelofdeath.boss.entity.VoidCrowEntity;
import com.angelofdeath.boss.network.BoostGlowPayload;
import com.angelofdeath.boss.network.ToggleSpearGriefingPayload;
import com.angelofdeath.boss.network.WingsBoostPayload;
import com.angelofdeath.boss.registry.ModAttachments;
import com.angelofdeath.boss.registry.ModComponents;
import com.angelofdeath.boss.registry.ModEntities;
import com.angelofdeath.boss.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(AngelOfDeathBoss.MOD_ID)
public class AngelOfDeathBoss {
    public static final String MOD_ID = "angelofdeathboss";

    public AngelOfDeathBoss(IEventBus modBus) {
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModComponents.COMPONENTS.register(modBus);
        modBus.addListener(AngelOfDeathBoss::registerAttributes);
        modBus.addListener(AngelOfDeathBoss::registerPayloads);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ANGEL_OF_DEATH.get(), AngelOfDeathBossEntity.createAttributes().build());
        event.put(ModEntities.BLACK_CROW.get(), VoidCrowEntity.createAttributes().build());
        event.put(ModEntities.WHITE_CROW.get(), VoidCrowEntity.createAttributes().build());
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(WingsBoostPayload.TYPE, WingsBoostPayload.CODEC, WingsBoostPayload::handle);
        registrar.playToServer(ToggleSpearGriefingPayload.TYPE, ToggleSpearGriefingPayload.CODEC,
                ToggleSpearGriefingPayload::handle);
        registrar.playToClient(BoostGlowPayload.TYPE, BoostGlowPayload.CODEC, BoostGlowPayload::handle);
    }
}
