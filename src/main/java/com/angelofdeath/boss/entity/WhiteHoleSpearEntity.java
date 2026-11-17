package com.angelofdeath.boss.entity;

import com.angelofdeath.boss.logic.SpearBlasts;
import com.angelofdeath.boss.item.WhiteHoleSpearItem;
import com.angelofdeath.boss.registry.ModComponents;
import com.angelofdeath.boss.registry.ModEntities;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Thrown White-Hole Spear. Always impales the first living entity it hits (1 target) and carries it until it strikes a
 * block. Uncharged: trident-like arc, 7.0F to the carried mob at the wall, no explosion. Charged: zero-gravity laser at
 * 3x trident speed and 10x range, detonates on impact. In every case it then flies back to the owner (loyalty), and
 * teleports back if it falls into the void.
 */
public class WhiteHoleSpearEntity extends Projectile {
    public static final double BASE_RANGE = 32.0D;
    public static final double CHARGED_RANGE = BASE_RANGE * 10.0D;
    public static final float WALL_DAMAGE = 7.0F;

    private static final EntityDataAccessor<Boolean> CHARGED =
            SynchedEntityData.defineId(WhiteHoleSpearEntity.class, EntityDataSerializers.BOOLEAN);

    private ItemStack spear = ItemStack.EMPTY;
    private int slot = -1;
    @Nullable
    private UUID ownerId;
    @Nullable
    private UUID impaledId;
    private boolean returning;
    private int returnTicks;
    private double traveled;

    public WhiteHoleSpearEntity(EntityType<? extends WhiteHoleSpearEntity> type, Level level) {
        super(type, level);
    }

    public WhiteHoleSpearEntity(Level level, LivingEntity owner, ItemStack spear, int slot, boolean charged) {
        this(ModEntities.WHITE_HOLE_SPEAR.get(), level);
        setOwner(owner);
        this.ownerId = owner.getUUID();
        this.spear = spear;
        this.slot = slot;
        this.entityData.set(CHARGED, charged);
        setPos(owner.getX(), owner.getEyeY(), owner.getZ());
    }

    public boolean isChargedThrow() {
        return this.entityData.get(CHARGED);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CHARGED, false);
    }

    @Override
    public boolean isNoGravity() {
        return true; // gravity is applied by hand (uncharged arc only)
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return this.impaledId == null && !this.returning && entity instanceof LivingEntity && super.canHitEntity(entity);
    }

    // ------------------------------------------------------------------ flight

    @Override
    public void tick() {
        super.tick();
        Vec3 delta = getDeltaMovement();
        if (this.level().isClientSide) {
            setPos(getX() + delta.x, getY() + delta.y, getZ() + delta.z);
            updateRotation();
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (this.returning) {
            tickReturn(level);
            return;
        }
        if (getY() < level.getMinBuildHeight() - 8) {
            beginReturn(level, true); // void: skip the flight home
            return;
        }

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() == HitResult.Type.ENTITY) {
            impale(((EntityHitResult) hit).getEntity());
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 at = ((BlockHitResult) hit).getLocation();
            setPos(at.x, at.y, at.z);
            carry();
            impact(level, at);
            return;
        }

        this.traveled += delta.length();
        setPos(getX() + delta.x, getY() + delta.y, getZ() + delta.z);
        updateRotation();
        if (!isChargedThrow()) {
            setDeltaMovement(delta.add(0.0D, -0.05D, 0.0D).scale(0.99D));
        }
        carry();
        if (this.traveled >= (isChargedThrow() ? CHARGED_RANGE : BASE_RANGE)) {
            beginReturn(level, false);
        }
    }

    private void impale(Entity entity) {
        this.impaledId = entity.getUUID();
        SoundHelper.play(this, SoundEvents.SHIELD_BLOCK);
    }

    @Nullable
    private LivingEntity impaled() {
        if (this.impaledId != null && this.level() instanceof ServerLevel level
                && level.getEntity(this.impaledId) instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        return null;
    }

    /** Drags the impaled mob along, hanging just behind the spearhead. */
    private void carry() {
        LivingEntity mob = impaled();
        if (mob == null) {
            return;
        }
        Vec3 dir = getDeltaMovement();
        dir = dir.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : dir.normalize();
        Vec3 p = position().subtract(dir.scale(0.5D + mob.getBbWidth() * 0.5D));
        double y = p.y - mob.getBbHeight() * 0.5D;
        if (mob instanceof ServerPlayer player) {
            player.connection.teleport(p.x, y, p.z, player.getYRot(), player.getXRot());
        } else {
            mob.setPos(p.x, y, p.z);
        }
        mob.setDeltaMovement(Vec3.ZERO);
        mob.hurtMarked = true;
        mob.fallDistance = 0.0F;
    }

    private void impact(ServerLevel level, Vec3 at) {
        LivingEntity mob = impaled();
        Entity owner = getOwner();
        boolean destroy = WhiteHoleSpearItem.destroysBlocks(this.spear);
        if (isChargedThrow()) {
            if (mob != null) {
                SpearBlasts.mobDetonation(level, this, owner, mob, at, destroy);
            } else {
                SpearBlasts.groundShockwave(level, this, owner, at, destroy);
            }
        } else if (mob != null) {
            mob.invulnerableTime = 0;
            mob.hurt(level.damageSources().thrown(this, owner), WALL_DAMAGE);
        }
        beginReturn(level, false);
    }

    // ------------------------------------------------------------------ loyalty return

    @Nullable
    private ServerPlayer ownerPlayer(ServerLevel level) {
        return this.ownerId == null ? null : level.getServer().getPlayerList().getPlayer(this.ownerId);
    }

    private void beginReturn(ServerLevel level, boolean fromVoid) {
        this.impaledId = null;
        this.returning = true;
        this.noPhysics = true;
        this.returnTicks = 0;
        setDeltaMovement(Vec3.ZERO);
        if (fromVoid) {
            ServerPlayer owner = ownerPlayer(level);
            if (owner != null) {
                setPos(owner.getX(), owner.getEyeY(), owner.getZ()); // teleport out of the void
            }
        }
    }

    private void tickReturn(ServerLevel level) {
        ServerPlayer owner = ownerPlayer(level);
        if (owner == null || !owner.isAlive()) {
            dropAndDiscard(level);
            return;
        }
        if (owner.level() != level || ++this.returnTicks > 200) {
            giveBack(level, owner);
            return;
        }
        Vec3 to = owner.getEyePosition().subtract(position());
        double d = to.length();
        if (d < 1.5D) {
            giveBack(level, owner);
            return;
        }
        double speed = Math.min(4.0D, 0.8D + this.returnTicks * 0.25D);
        Vec3 step = to.scale(Math.min(speed, d) / d);
        setDeltaMovement(step);
        setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
        updateRotation();
    }

    private void giveBack(ServerLevel level, ServerPlayer owner) {
        ItemStack stack = this.spear.copy();
        Inventory inventory = owner.getInventory();
        if (this.slot >= 0 && this.slot < inventory.getContainerSize() && inventory.getItem(this.slot).isEmpty()) {
            inventory.setItem(this.slot, stack);
        } else if (!inventory.add(stack)) {
            owner.drop(stack, false);
        }
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS,
                1.0F, 1.0F);
        discard();
    }

    /** Owner is gone (offline / dead): leave the spear as an item that cannot despawn, never below the void. */
    private void dropAndDiscard(ServerLevel level) {
        ItemEntity item = new ItemEntity(level, getX(), Math.max(getY(), level.getMinBuildHeight() + 2.0D), getZ(),
                this.spear.copy());
        item.setUnlimitedLifetime();
        item.setNoGravity(true);
        item.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(item);
        discard();
    }

    // ------------------------------------------------------------------ save data

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Charged", isChargedThrow());
        tag.putInt("Slot", this.slot);
        if (this.ownerId != null) {
            tag.putUUID("SpearOwner", this.ownerId);
        }
        if (!this.spear.isEmpty()) {
            tag.put("Spear", this.spear.save(registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(CHARGED, tag.getBoolean("Charged"));
        this.slot = tag.getInt("Slot");
        if (tag.hasUUID("SpearOwner")) {
            this.ownerId = tag.getUUID("SpearOwner");
        }
        if (tag.contains("Spear")) {
            this.spear = ItemStack.parse(registryAccess(), tag.get("Spear")).orElse(ItemStack.EMPTY);
        }
    }

    /** Tiny helper so impale() can play a sound without a ServerLevel cast at the call site. */
    private static final class SoundHelper {
        static void play(Entity e, net.minecraft.sounds.SoundEvent sound) {
            e.level().playSound(null, e.getX(), e.getY(), e.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
