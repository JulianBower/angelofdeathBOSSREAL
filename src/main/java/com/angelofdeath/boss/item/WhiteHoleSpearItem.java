package com.angelofdeath.boss.item;

import com.angelofdeath.boss.entity.WhiteHoleSpearEntity;
import com.angelofdeath.boss.logic.SpearBlasts;
import com.angelofdeath.boss.registry.ModComponents;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Indestructible spear: charge it, throw it (always impales + carries a mob), loyalty-returns every time. */
public class WhiteHoleSpearItem extends Item {
    public static final int CHARGE_TICKS = 25;
    public static final int MIN_THROW_TICKS = 10;
    public static final float VANILLA_TRIDENT_SPEED = 2.5F;

    public WhiteHoleSpearItem(Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 8.0D,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.9D,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public static boolean isCharged(ItemStack stack) {
        return stack.getOrDefault(ModComponents.IS_CHARGED.get(), false);
    }

    public static boolean destroysBlocks(ItemStack stack) {
        return stack.getOrDefault(ModComponents.DESTROY_BLOCKS.get(), false);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isCharged(stack) || super.isFoil(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** Holding right-click for 25 ticks stores the charge on the stack. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        int used = getUseDuration(stack) - remainingUseDuration;
        if (used == CHARGE_TICKS && !level.isClientSide && !isCharged(stack)) {
            stack.set(ModComponents.IS_CHARGED.get(), true);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 1.0F, 1.4F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (getUseDuration(stack) - timeLeft < MIN_THROW_TICKS) {
            return;
        }
        boolean charged = isCharged(stack);
        int slot = player.getUsedItemHand() == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
        ItemStack thrown = stack.copy();
        thrown.set(ModComponents.IS_CHARGED.get(), false); // the charge is spent by this throw

        WhiteHoleSpearEntity spear = new WhiteHoleSpearEntity(level, player, thrown, slot, charged);
        Vec3 look = player.getLookAngle();
        spear.setPos(player.getX() + look.x * 0.6D, player.getEyeY() - 0.15D + look.y * 0.6D, player.getZ() + look.z * 0.6D);
        float speed = charged ? VANILLA_TRIDENT_SPEED * 3.0F : VANILLA_TRIDENT_SPEED;
        spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, 0.0F);
        level.addFreshEntity(spear);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PHANTOM_SWOOP,
                SoundSource.PLAYERS, 1.0F, charged ? 0.6F : 1.0F);
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.shrink(1); // flies off; the entity brings it back to this slot
    }

    /** Charged melee hit: spend the charge for an instant White-Hole burst. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (isCharged(stack) && attacker.level() instanceof ServerLevel level) {
            stack.set(ModComponents.IS_CHARGED.get(), false);
            SpearBlasts.meleeBurst(level, attacker, target, destroysBlocks(stack));
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(destroysBlocks(stack) ? "tooltip.angelofdeathboss.spear_griefing_on"
                : "tooltip.angelofdeathboss.spear_griefing_off"));
        if (isCharged(stack)) {
            tooltip.add(Component.translatable("tooltip.angelofdeathboss.spear_charged"));
        }
        tooltip.add(Component.translatable("tooltip.angelofdeathboss.spear_hint"));
    }
}
