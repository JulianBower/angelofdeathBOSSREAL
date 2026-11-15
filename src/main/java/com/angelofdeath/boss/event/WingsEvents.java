package com.angelofdeath.boss.event;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.item.AngelWingsItem;
import com.angelofdeath.boss.network.BoostGlowPayload;
import com.angelofdeath.boss.registry.ModAttachments;
import com.angelofdeath.boss.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Wings behaviour: free flight (mayfly), fall immunity, explosive directional boost, synced glow decay. */
@EventBusSubscriber(modid = AngelOfDeathBoss.MOD_ID)
public final class WingsEvents {
    /** Base creative flight speed (~0.25 blocks/tick); the boost is 12x that = 3.0 blocks/tick. */
    public static final double BASE_FLIGHT_SPEED = 0.25D;
    public static final double BOOST_MULTIPLIER = 12.0D;
    public static final int BOOST_COOLDOWN_TICKS = 40;
    private static final float GLOW_DECAY = 0.04F;
    private static final String GRANTED_FLIGHT = "aod_wings_flight";

    private WingsEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean wearing = AngelWingsItem.isWearing(player);
        var data = player.getPersistentData();
        if (wearing) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                data.putBoolean(GRANTED_FLIGHT, true);
                player.onUpdateAbilities();
            }
            player.fallDistance = 0.0F;
        } else if (data.getBoolean(GRANTED_FLIGHT)) {
            data.putBoolean(GRANTED_FLIGHT, false);
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }

        float alpha = player.getData(ModAttachments.BOOST_GLOW_ALPHA);
        if (alpha > 0.0F) {
            setGlow(player, Math.max(0.0F, alpha - GLOW_DECAY));
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && AngelWingsItem.isWearing(player)) {
            event.setCanceled(true);
        }
    }

    public static void tryBoost(ServerPlayer player) {
        if (!AngelWingsItem.isWearing(player) || player.isSpectator()
                || player.getCooldowns().isOnCooldown(ModItems.ANGEL_OF_DEATH_WINGS.get())) {
            return;
        }
        player.getCooldowns().addCooldown(ModItems.ANGEL_OF_DEATH_WINGS.get(), BOOST_COOLDOWN_TICKS);

        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.scale(BASE_FLIGHT_SPEED * BOOST_MULTIPLIER));
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        setGlow(player, 1.0F);

        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_FLAP,
                    SoundSource.PLAYERS, 2.5F, 0.9F);
            Vec3 back = player.position().add(0.0D, player.getBbHeight() * 0.6D, 0.0D);
            level.sendParticles(ParticleTypes.EXPLOSION, back.x, back.y, back.z, 3, 0.4D, 0.4D, 0.4D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, back.x, back.y, back.z, 40, 0.5D, 0.5D, 0.5D, 0.3D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, back.x, back.y, back.z, 30, 0.5D, 0.5D, 0.5D, 0.3D);
        }
    }

    /** Stores the glow on the server and mirrors it to the player and everyone tracking them. */
    private static void setGlow(ServerPlayer player, float value) {
        player.setData(ModAttachments.BOOST_GLOW_ALPHA, value);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new BoostGlowPayload(player.getId(), value));
    }
}
