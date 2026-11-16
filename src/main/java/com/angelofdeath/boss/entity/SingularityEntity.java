package com.angelofdeath.boss.entity;

import com.angelofdeath.boss.logic.FreezeRules;
import com.angelofdeath.boss.logic.FreezeTracker;
import com.angelofdeath.boss.logic.SingularityActions;
import com.angelofdeath.boss.registry.ModEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A placed singularity. Mode 0 = Black Hole, 1 = White Hole, 2 = Grey Hole.
 *
 * <p>When {@link #isAttackVariant()} is true the hole runs NO arena physics (no pull, no repulsion field, no
 * block push, no stasis). It is visual and only triggers its combo effects: an opening burst, an expiry
 * detonation / crow burst, or (grey) a fast piercing shot.</p>
 */
public class SingularityEntity extends Entity {
    public static final int BLACK_DURATION = 100;
    public static final int WHITE_DURATION = 100;
    public static final int GREY_DURATION = 120;
    private static final int MAX_PUSHED_BLOCKS = 2500;

    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_POWER =
            SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_ATTACK_VARIANT =
            SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_DURATION =
            SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.INT);
    /** True when the owner's renderer draws this hole itself (palm / back-mounted), so the entity stays invisible. */
    private static final EntityDataAccessor<Boolean> DATA_OWNER_RENDERED =
            SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.BOOLEAN);

    /** The living entity this singularity is latched onto; the hole follows its bounding-box center. */
    private static final EntityDataAccessor<Optional<UUID>> ATTACHED_TARGET =
            SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(new Vector3f(0.0F, 0.0F, 0.0F), 2.0F);
    private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 2.0F);
    private static final DustParticleOptions GREY_DUST = new DustParticleOptions(new Vector3f(0.53F, 0.53F, 0.57F), 2.0F);

    private int life;
    private boolean discardOnLoad;
    @Nullable
    private Entity clientTarget;
    private final FreezeTracker freezeTracker = new FreezeTracker();

    // Combo effects (server only).
    private float burstDamage;
    private double burstRadius;
    private double burstKnockback;
    private float expireDamage;
    private double expireRadius;
    private boolean spawnCrowOnExpire;
    private Vec3 shotVelocity = Vec3.ZERO;
    private float shotDamage;
    private final Set<UUID> shotHit = new HashSet<>();

    public SingularityEntity(EntityType<? extends SingularityEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void configure(int mode, int power, @Nullable UUID owner) {
        configure(mode, power, owner, false);
    }

    public void configure(int mode, int power, @Nullable UUID owner, boolean isAttackVariant) {
        this.entityData.set(DATA_MODE, Mth.clamp(mode, 0, 2));
        this.entityData.set(DATA_POWER, Mth.clamp(power, 1, 5));
        this.entityData.set(DATA_OWNER, Optional.ofNullable(owner));
        this.entityData.set(DATA_ATTACK_VARIANT, isAttackVariant);
    }

    public boolean isOwnerRendered() {
        return this.entityData.get(DATA_OWNER_RENDERED);
    }

    public void setOwnerRendered(boolean value) {
        this.entityData.set(DATA_OWNER_RENDERED, value);
    }

    public void setDuration(int ticks) {
        this.entityData.set(DATA_DURATION, Math.max(1, ticks));
    }

    /** Damage/knockback pulse applied once on the first tick. */
    public void setBurst(float damage, double radius, double knockback) {
        this.burstDamage = damage;
        this.burstRadius = radius;
        this.burstKnockback = knockback;
    }

    /** Detonation applied when the hole expires (True Grey Hole). */
    public void setExpire(float damage, double radius) {
        this.expireDamage = damage;
        this.expireRadius = radius;
    }

    public void setSpawnCrowOnExpire(boolean value) {
        this.spawnCrowOnExpire = value;
    }

    /** Turns a grey attack-variant hole into a fast piercing shot. */
    public void setShot(Vec3 velocity, float damage) {
        this.shotVelocity = velocity;
        this.shotDamage = damage;
    }

    public int getMode() {
        return this.entityData.get(DATA_MODE);
    }

    public int getPower() {
        return this.entityData.get(DATA_POWER);
    }

    public boolean isAttackVariant() {
        return this.entityData.get(DATA_ATTACK_VARIANT);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER).orElse(null);
    }

    public void setAttachedTarget(@Nullable UUID target) {
        this.entityData.set(ATTACHED_TARGET, Optional.ofNullable(target));
    }

    @Nullable
    public UUID getAttachedTargetUUID() {
        return this.entityData.get(ATTACHED_TARGET).orElse(null);
    }

    /** Client-side: the latched entity, if it is currently loaded (used by the renderer for smooth following). */
    @Nullable
    public Entity getClientTarget() {
        return this.clientTarget != null && !this.clientTarget.isRemoved() ? this.clientTarget : null;
    }

    /** Server-side: lock to the center of the latched mob's bounding box every tick, detach if it is gone. */
    private void followAttachedTarget(ServerLevel level) {
        UUID id = getAttachedTargetUUID();
        if (id == null) {
            return;
        }
        Entity target = level.getEntity(id);
        if (target == null || target.isRemoved() || !target.isAlive()) {
            setAttachedTarget(null);
            return;
        }
        Vec3 center = target.getBoundingBox().getCenter();
        this.setPos(center.x, center.y, center.z);
    }

    private void tickClientAttachment() {
        UUID id = getAttachedTargetUUID();
        if (id == null) {
            this.clientTarget = null;
            return;
        }
        if (this.clientTarget == null || this.clientTarget.isRemoved() || !id.equals(this.clientTarget.getUUID())) {
            this.clientTarget = null;
            List<LivingEntity> found = this.level().getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(48.0D), e -> id.equals(e.getUUID()));
            if (!found.isEmpty()) {
                this.clientTarget = found.get(0);
            }
        }
        if (this.clientTarget != null) {
            Vec3 center = this.clientTarget.getBoundingBox().getCenter();
            this.setPos(center.x, center.y, center.z);
        }
    }

    public int getDuration() {
        int custom = this.entityData.get(DATA_DURATION);
        if (custom > 0) {
            return custom;
        }
        return switch (getMode()) {
            case 1 -> WHITE_DURATION;
            case 2 -> GREY_DURATION;
            default -> BLACK_DURATION;
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODE, 0);
        builder.define(DATA_POWER, 1);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_ATTACK_VARIANT, false);
        builder.define(DATA_DURATION, 0);
        builder.define(DATA_OWNER_RENDERED, false);
        builder.define(ATTACHED_TARGET, Optional.empty());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_MODE, Mth.clamp(tag.getInt("Mode"), 0, 2));
        this.entityData.set(DATA_POWER, Mth.clamp(tag.getInt("Power"), 1, 5));
        this.entityData.set(DATA_OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        this.entityData.set(DATA_ATTACK_VARIANT, tag.getBoolean("Attack"));
        this.entityData.set(DATA_DURATION, tag.getInt("Duration"));
        this.life = tag.getInt("Life");
        this.entityData.set(ATTACHED_TARGET, tag.hasUUID("Attached") ? Optional.of(tag.getUUID("Attached")) : Optional.empty());
        this.expireDamage = tag.getFloat("ExpireDamage");
        this.expireRadius = tag.getDouble("ExpireRadius");
        this.freezeTracker.load(tag, "Frozen");
        // Attack variants are transient combat props: drop them if the world reloads mid-fight.
        this.discardOnLoad = tag.getBoolean("Attack");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Mode", getMode());
        tag.putInt("Power", getPower());
        UUID owner = getOwnerUUID();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putBoolean("Attack", isAttackVariant());
        tag.putInt("Duration", this.entityData.get(DATA_DURATION));
        tag.putInt("Life", this.life);
        UUID attached = getAttachedTargetUUID();
        if (attached != null) {
            tag.putUUID("Attached", attached);
        }
        tag.putFloat("ExpireDamage", this.expireDamage);
        tag.putDouble("ExpireRadius", this.expireRadius);
        this.freezeTracker.save(tag, "Frozen");
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 400.0D * 400.0D;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(0.8D + 0.32D * getPower() * 2.5D);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel && reason.shouldDestroy()) {
            this.freezeTracker.releaseAll(serverLevel);
        }
        super.remove(reason);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            tickClientAttachment();
            return;
        }
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.discardOnLoad) {
            this.discard();
            return;
        }
        followAttachedTarget(level);
        this.life++;
        if (isAttackVariant()) {
            tickAttackVariant(level);
        } else {
            switch (getMode()) {
                case 1 -> tickWhiteHole(level);
                case 2 -> tickGreyHole(level);
                default -> tickBlackHole(level);
            }
        }
        if (this.life >= getDuration()) {
            finish(level);
            this.discard();
        }
    }

    private boolean isOwner(Entity entity) {
        UUID owner = getOwnerUUID();
        return owner != null && owner.equals(entity.getUUID());
    }

    @Nullable
    private Entity getOwnerEntity(ServerLevel level) {
        UUID owner = getOwnerUUID();
        return owner == null ? null : level.getEntity(owner);
    }

    private boolean isAffectable(Entity entity) {
        return entity != this && !isOwner(entity) && !(entity instanceof SingularityEntity)
                && !entity.isSpectator() && !(entity instanceof HangingEntity)
                && !(entity instanceof AngelOfDeathBossEntity) && !(entity instanceof VoidCrowEntity)
                && !(entity instanceof VoidFeatherEntity);
    }

    // ------------------------------------------------------------------ Attack variant (combo effects only)

    private void tickAttackVariant(ServerLevel level) {
        int power = getPower();
        Vec3 center = position();
        int mode = getMode();

        if (this.life == 1) {
            if (this.burstDamage > 0.0F) {
                applyBurst(level, center, this.burstDamage, this.burstRadius, this.burstKnockback);
            }
            SingularityActions.playSound(level, center, mode == 1 ? SoundEvents.LIGHTNING_BOLT_IMPACT
                    : SoundEvents.PORTAL_AMBIENT, 1.0F + 0.4F * power, mode == 1 ? 1.2F : 0.5F);
        }
        if (mode == 2 && this.shotVelocity.lengthSqr() > 0.0D) {
            tickGreyShot(level);
            center = position();
        }

        double spread = 0.3D + 0.2D * power;
        switch (mode) {
            case 1 -> {
                level.sendParticles(WHITE_DUST, center.x, center.y, center.z, 6, spread, spread, spread, 0.0D);
                level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 4, spread, spread, spread, 0.1D);
            }
            case 2 -> {
                level.sendParticles(GREY_DUST, center.x, center.y, center.z, 6, spread, spread, spread, 0.0D);
                level.sendParticles(ParticleTypes.ASH, center.x, center.y, center.z, 8, spread * 2, spread * 2, spread * 2, 0.02D);
            }
            default -> {
                level.sendParticles(BLACK_DUST, center.x, center.y, center.z, 6, spread, spread, spread, 0.0D);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 12, 1.0D + power,
                        1.0D + power, 1.0D + power, 0.2D);
            }
        }
    }

    private void tickGreyShot(ServerLevel level) {
        Vec3 next = position().add(this.shotVelocity);
        this.setPos(next.x, next.y, next.z);
        Entity owner = getOwnerEntity(level);
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.2D),
                e -> isAffectable(e) && SingularityActions.canHarm(e));
        for (LivingEntity living : hit) {
            if (this.shotHit.add(living.getUUID())) {
                living.invulnerableTime = 0;
                living.hurt(SingularityActions.trueDamage(level, this, owner), this.shotDamage);
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                SingularityActions.launch(living, this.shotVelocity, 0.9D, 0.25D);
            }
        }
        BlockPos bp = BlockPos.containing(next);
        if (!level.getBlockState(bp).getCollisionShape(level, bp).isEmpty()) {
            level.sendParticles(ParticleTypes.ASH, next.x, next.y, next.z, 30, 0.6D, 0.6D, 0.6D, 0.1D);
            this.life = getDuration();
        }
    }

    /** Damage (and optional knockback) pulse around a point. Never touches blocks. */
    private void applyBurst(ServerLevel level, Vec3 center, float damage, double radius, double knockback) {
        Entity owner = getOwnerEntity(level);
        DamageSource source = getMode() == 1 ? level.damageSources().explosion(this, owner)
                : SingularityActions.voidSource(level, this, owner);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> isAffectable(e) && SingularityActions.canHarm(e));
        for (LivingEntity living : targets) {
            Vec3 away = living.getBoundingBox().getCenter().subtract(center);
            double distance = away.length();
            if (distance > radius) {
                continue;
            }
            living.invulnerableTime = 0;
            living.hurt(source, damage);
            if (knockback > 0.0D) {
                SingularityActions.launch(living, away, knockback * (1.0D - 0.5D * distance / radius), 0.35D);
            }
        }
        if (radius >= 6.0D) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        } else {
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 3, 0.6D, 0.6D, 0.6D, 0.0D);
        }
        switch (getMode()) {
            case 1 -> {
                level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 60, 1.0D, 1.0D, 1.0D, 0.5D);
            }
            case 2 -> level.sendParticles(ParticleTypes.ASH, center.x, center.y, center.z, 80, 1.5D, 1.5D, 1.5D, 0.3D);
            default -> level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 60, 1.0D, 1.0D, 1.0D, 0.5D);
        }
    }

    /** Runs once when the hole expires: detonation and/or crow burst. */
    private void finish(ServerLevel level) {
        Vec3 center = position();
        if (this.expireDamage > 0.0F) {
            applyBurst(level, center, this.expireDamage, this.expireRadius, 0.0D);
            SingularityActions.playSound(level, center, SoundEvents.LIGHTNING_BOLT_IMPACT, 6.0F, 0.7F);
        }
        if (this.spawnCrowOnExpire) {
            VoidCrowEntity crow = (getMode() == 1 ? ModEntities.WHITE_CROW : ModEntities.BLACK_CROW).get().create(level);
            if (crow != null) {
                crow.moveTo(center.x, center.y, center.z, this.random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(crow);
                if (getOwnerUUID() != null && level.getEntity(getOwnerUUID()) instanceof AngelOfDeathBossEntity boss) {
                    boss.registerCrow(crow.getUUID());
                }
            }
            level.sendParticles(getMode() == 1 ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL,
                    center.x, center.y, center.z, 40, 0.6D, 0.6D, 0.6D, 0.3D);
        }
    }

    // ------------------------------------------------------------------ Black Hole

    private void tickBlackHole(ServerLevel level) {
        int power = getPower();
        double radius = 14.0D + power * 7.0D;
        Vec3 center = position();
        boolean damageTick = this.life % 10 == 0;
        DamageSource source = SingularityActions.voidSource(level, this, getOwnerEntity(level));

        List<Entity> targets = level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(radius),
                e -> isAffectable(e) && (e instanceof LivingEntity || e instanceof ItemEntity));
        for (Entity target : targets) {
            double distance = target.getBoundingBox().getCenter().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            double falloff = 1.0D - distance / radius;
            SingularityActions.pull(target, center, (0.35D + 0.15D * power) * (0.4D + 0.6D * falloff), 3.0D);
            if (target instanceof LivingEntity living) {
                living.fallDistance = 0.0F;
                if (damageTick && distance < 3.0D && SingularityActions.canHarm(living)) {
                    living.invulnerableTime = 0;
                    living.hurt(source, 4.0F * power);
                }
            }
        }

        level.sendParticles(BLACK_DUST, center.x, center.y, center.z, 10, 0.5D, 0.5D, 0.5D, 0.0D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 30, 3.0D + power, 3.0D + power,
                3.0D + power, 0.2D);
        if (this.life == 1) {
            SingularityActions.playSound(level, center, SoundEvents.ENDER_DRAGON_GROWL, 3.0F, 0.5F);
        }
        if (this.life % 20 == 0) {
            SingularityActions.playSound(level, center, SoundEvents.PORTAL_AMBIENT, 2.0F, 0.5F);
        }
    }

    // ------------------------------------------------------------------ White Hole

    private void tickWhiteHole(ServerLevel level) {
        int power = getPower();
        Vec3 center = position();

        if (this.life == 1) {
            shockwave(level, power, center);
            blockPush(level, power);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 150, 1.5D, 1.5D, 1.5D, 0.8D);
            SingularityActions.playSound(level, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 10.0F, 0.6F);
            SingularityActions.playSound(level, center, SoundEvents.LIGHTNING_BOLT_IMPACT, 10.0F, 0.8F);
        } else {
            // Repulsive barrier field.
            double radius = 10.0D + power * 5.0D;
            List<Entity> targets = level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(radius),
                    e -> isAffectable(e) && (e instanceof LivingEntity || e instanceof ItemEntity));
            for (Entity target : targets) {
                double distance = target.getBoundingBox().getCenter().distanceTo(center);
                if (distance > radius) {
                    continue;
                }
                double inverse = Math.max(0.2D, 1.0D - distance / radius);
                SingularityActions.push(target, center, (0.75D + 0.36D * power) * inverse, 0.06D, 8.0D);
            }
        }

        level.sendParticles(WHITE_DUST, center.x, center.y, center.z, 10, 0.5D, 0.5D, 0.5D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 8, 0.6D, 0.6D, 0.6D, 0.25D);
        if (this.life % 20 == 0) {
            SingularityActions.playSound(level, center, SoundEvents.BEACON_AMBIENT, 2.0F, 1.5F);
        }
    }

    private void shockwave(ServerLevel level, int power, Vec3 center) {
        double blast = 8.0D * power; // Power 5 reaches 40 blocks.
        DamageSource source = level.damageSources().explosion(this, getOwnerEntity(level));
        List<Entity> targets = level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(blast),
                e -> isAffectable(e) && (e instanceof LivingEntity || e instanceof ItemEntity
                        || e instanceof Projectile || e instanceof PrimedTnt));
        for (Entity target : targets) {
            Vec3 away = target.getBoundingBox().getCenter().subtract(center);
            double distance = away.length();
            if (distance > blast) {
                continue;
            }
            double factor = 1.0D - distance / blast;
            Vec3 direction = distance < 0.01D ? new Vec3(0.0D, 1.0D, 0.0D) : away.scale(1.0D / distance);
            double speed = (2.25D + 2.0D * power) * (0.4D + 0.6D * factor);
            Vec3 velocity = direction.scale(speed).add(0.0D, 0.35D + 0.1D * power * factor, 0.0D);
            target.setDeltaMovement(velocity);
            target.hurtMarked = true;
            target.fallDistance = 0.0F;
            if (target instanceof LivingEntity living && SingularityActions.canHarm(living)) {
                living.invulnerableTime = 0;
                living.hurt(source, (float) ((2.0D + 6.0D * power) * factor));
            }
        }
    }

    private void blockPush(ServerLevel level, int power) {
        int radius = 4 + 2 * power;
        BlockPos origin = this.blockPosition();
        Vec3 originCenter = position();
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.isOutsideBuildHeight(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()
                            || state.getDestroySpeed(level, cursor) < 0.0F
                            || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                            || state.hasProperty(BlockStateProperties.BED_PART)) {
                        continue;
                    }
                    candidates.add(cursor.immutable());
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(originCenter)));
        int limit = Math.min(candidates.size(), MAX_PUSHED_BLOCKS);
        for (int i = 0; i < limit; i++) {
            BlockPos pos = candidates.get(i);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
                continue;
            }
            Vec3 blockCenter = Vec3.atCenterOf(pos);
            Vec3 away = blockCenter.subtract(originCenter);
            double distance = away.length();
            Vec3 direction = distance < 0.001D ? new Vec3(0.0D, 1.0D, 0.0D) : away.scale(1.0D / distance);
            double factor = 1.0D - Math.min(1.0D, distance / (radius + 1.0D));

            FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
            double speed = (0.5D + 0.25D * power) * (0.5D + 0.5D * factor) * (0.85D + this.random.nextDouble() * 0.3D);
            double upward = 0.35D + 0.45D * factor + this.random.nextDouble() * 0.2D;
            falling.setDeltaMovement(direction.x * speed, upward, direction.z * speed);
            falling.hurtMarked = true;
        }
    }

    // ------------------------------------------------------------------ Grey Hole

    private boolean isFreezable(Entity entity) {
        return isAffectable(entity) && !entity.isPassenger() && !(entity instanceof Marker)
                && !(entity instanceof Display) && !(entity instanceof LightningBolt);
    }

    private void tickGreyHole(ServerLevel level) {
        Vec3 center = position();
        double radius = FreezeRules.GLOBAL_RADIUS;
        double radiusSqr = radius * radius;

        Set<UUID> keep = new HashSet<>();
        List<Entity> targets = level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(radius),
                e -> isFreezable(e) && e.distanceToSqr(center) <= radiusSqr);
        for (Entity target : targets) {
            this.freezeTracker.freeze(target);
            keep.add(target.getUUID());
        }
        this.freezeTracker.keepOnly(level, keep);

        level.sendParticles(GREY_DUST, center.x, center.y, center.z, 8, 0.6D, 0.6D, 0.6D, 0.0D);
        level.sendParticles(ParticleTypes.ASH, center.x, center.y, center.z, 20, 4.0D, 4.0D, 4.0D, 0.02D);
        if (this.life == 1) {
            SingularityActions.playSound(level, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 6.0F, 0.5F);
        }
        if (this.life % 20 == 0) {
            SingularityActions.playSound(level, center, SoundEvents.BEACON_AMBIENT, 4.0F, 0.6F);
        }
    }
}
