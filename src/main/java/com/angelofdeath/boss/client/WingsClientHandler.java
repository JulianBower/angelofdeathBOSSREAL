package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.item.AngelWingsItem;
import com.angelofdeath.boss.network.WingsBoostPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** The B key: asks the server for the 12x directional wing boost (only while the wings are worn). */
@EventBusSubscriber(modid = AngelOfDeathBoss.MOD_ID, value = Dist.CLIENT)
public final class WingsClientHandler {
    public static final KeyMapping BOOST_KEY = new KeyMapping("key.angelofdeathboss.wings_boost",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.angelofdeathboss");

    private WingsClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (BOOST_KEY.consumeClick()) {
            if (minecraft.player != null && AngelWingsItem.isWearing(minecraft.player)) {
                PacketDistributor.sendToServer(new WingsBoostPayload());
            }
        }
    }
}
