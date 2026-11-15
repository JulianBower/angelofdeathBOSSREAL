package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.item.AngelWingsItem;
import com.angelofdeath.boss.network.WingsBoostPayload;
import com.angelofdeath.boss.registry.ModAttachments;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** B-key boost request, plus the cosmic wing-trail particles that follow BOOST_GLOW_ALPHA. */
@EventBusSubscriber(modid = AngelOfDeathBoss.MOD_ID, value = Dist.CLIENT)
public final class WingsClientHandler {
    public static final KeyMapping WINGS_BOOST = new KeyMapping("key.angelofdeathboss.wings_boost",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.angelofdeathboss");

    private WingsClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (mc.player == null || level == null) {
            return;
        }
        while (WINGS_BOOST.consumeClick()) {
            if (mc.screen == null && AngelWingsItem.isWearing(mc.player)) {
                PacketDistributor.sendToServer(new WingsBoostPayload());
            }
        }
        if (mc.isPaused()) {
            return;
        }
        for (AbstractClientPlayer player : level.players()) {
            if (!AngelWingsItem.isWearing(player) || player.isInvisible()) {
                continue;
            }
            float glow = player.getData(ModAttachments.BOOST_GLOW_ALPHA);
            if (glow > 0.04F) {
                spawnTrail(level, player, glow);
            }
        }
    }

    private static void spawnTrail(ClientLevel level, AbstractClientPlayer player, float glow) {
        RandomSource random = level.random;
        float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
        double backX = Mth.sin(yaw);   // opposite of the facing vector (-sin, cos)
        double backZ = -Mth.cos(yaw);
        double leftX = Mth.cos(yaw);
        double leftZ = Mth.sin(yaw);
        int count = 1 + (int) (glow * 5.0F);
        for (int i = 0; i < count; i++) {
            for (int side = -1; side <= 1; side += 2) {
                double span = 0.5D + random.nextDouble() * 1.7D;
                double x = player.getX() + backX * (0.45D + random.nextDouble() * 0.4D) + leftX * side * span;
                double y = player.getY() + 0.7D + random.nextDouble() * 1.3D;
                double z = player.getZ() + backZ * (0.45D + random.nextDouble() * 0.4D) + leftZ * side * span;
                double vx = backX * 0.05D + (random.nextDouble() - 0.5D) * 0.02D;
                double vz = backZ * 0.05D + (random.nextDouble() - 0.5D) * 0.02D;
                switch (random.nextInt(3)) {
                    case 0 -> level.addParticle(ParticleTypes.END_ROD, x, y, z, vx, 0.01D, vz);
                    case 1 -> level.addParticle(ParticleTypes.SQUID_INK, x, y, z, vx * 0.5D, 0.0D, vz * 0.5D);
                    default -> level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, vx, 0.02D, vz);
                }
            }
        }
    }
}
