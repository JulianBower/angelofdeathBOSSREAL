package com.angelofdeath.boss.item;

import com.angelofdeath.boss.entity.VoidCrowEntity;
import com.angelofdeath.boss.entity.VoidFeatherEntity;
import com.angelofdeath.boss.registry.ModEntities;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Quick tap (< 20 ticks): infinite spectral feather, 10-tick cooldown, no ammo. Full charge (20 ticks, beacon chime):
 * summons a 3x-Allay allied crow for 25 seconds, 160-tick cooldown. Sneaking selects black, standing selects white.
 */
public class EclipseCrowcallerItem extends Item {
    public static final int CHARGE_TICKS = 20;
    public static final int TAP_COOLDOWN = 10;
    public static final int SUMMON_COOLDOWN = 160;

    public EclipseCrowcallerItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
	    return 72000;
	}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (getUseDuration(stack) - remainingUseDuration == CHARGE_TICKS && !level.isClientSide) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
            return;
        }
        boolean white = !player.isShiftKeyDown();
        int held = getUseDuration(stack) - timeLeft;
        if (held >= CHARGE_TICKS) {
            summonCrow(serverLevel, player, white);
            player.getCooldowns().addCooldown(this, SUMMON_COOLDOWN);
        } else {
            fireFeather(serverLevel, player, white);
            player.getCooldowns().addCooldown(this, TAP_COOLDOWN);
        }
    }

    private static void fireFeather(ServerLevel level, ServerPlayer player, boolean white) {
        VoidFeatherEntity feather = new VoidFeatherEntity(level, player, white);
        feather.setSpectral(true);
        Vec3 look = player.getLookAngle();
        feather.setPos(player.getX() + look.x * 0.5D, player.getEyeY() - 0.1D + look.y * 0.5D, player.getZ() + look.z * 0.5D);
        feather.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 0.4F);
        level.addFreshEntity(feather);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PHANTOM_SWOOP, SoundSource.PLAYERS,
                0.8F, white ? 1.6F : 1.1F);
    }

    private static void summonCrow(ServerLevel level, ServerPlayer player, boolean white) {
        VoidCrowEntity crow = (white ? ModEntities.WHITE_CROW : ModEntities.BLACK_CROW).get().create(level);
        if (crow == null) {
            return;
        }
        Vec3 look = player.getLookAngle();
        crow.moveTo(player.getX() + look.x * 2.0D, player.getY() + 2.0D, player.getZ() + look.z * 2.0D,
                player.getYRot(), 0.0F);
        crow.setWhite(white);
        crow.setSummonedAlly(player.getUUID()); // 500 ticks, fights for the owner, never drops feathers
        level.addFreshEntity(crow);
        level.sendParticles(white ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL, crow.getX(), crow.getY() + 1.0D,
                crow.getZ(), 40, 0.8D, 0.8D, 0.8D, 0.2D);
        level.playSound(null, crow.getX(), crow.getY(), crow.getZ(), SoundEvents.PARROT_AMBIENT, SoundSource.PLAYERS,
                2.0F, white ? 1.4F : 0.6F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.angelofdeathboss.crowcaller_tap"));
        tooltip.add(Component.translatable("tooltip.angelofdeathboss.crowcaller_charge"));
        tooltip.add(Component.translatable("tooltip.angelofdeathboss.crowcaller_sneak"));
    }
}
