package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.item.WhiteHoleSpearItem;
import com.angelofdeath.boss.network.ToggleSpearGriefingPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** The X key: toggles Block Griefing on the White-Hole Spear in either hand. */
@EventBusSubscriber(modid = AngelOfDeathBoss.MOD_ID, value = Dist.CLIENT)
public final class SpearClientHandler {
    public static final KeyMapping TOGGLE_KEY = new KeyMapping("key.angelofdeathboss.spear_blast_toggle",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories.angelofdeathboss");

    private SpearClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (TOGGLE_KEY.consumeClick()) {
            if (minecraft.player == null) {
                continue;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                if (minecraft.player.getItemInHand(hand).getItem() instanceof WhiteHoleSpearItem) {
                    PacketDistributor.sendToServer(new ToggleSpearGriefingPayload());
                    break;
                }
            }
        }
    }
}
