package com.angelofdeath.boss.entity;

import com.angelofdeath.boss.logic.SingularityActions;
import com.angelofdeath.boss.registry.ModEntities;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Delayed exploding feather: 4.0F armor-bypassing true damage, sticks into ground / the hit entity for 30 ticks
 * (STUCK is synced so the renderer can draw it embedded), then detonates into a tiny visual-only hole.
 */
public class VoidFeatherEntity extends Projectile {
    public static final int FUSE_TICKS = 30;
    public static final float HIT_DAMAGE = 4.0F;
    public static final float BLAST_DAMAGE = 6.0F;

    private static final EntityDataAccessor<Boolean> IS_WHITE =
            SynchedEntityData.defineId(VoidFeatherEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STUCK =
            SynchedEntityData.defineId(VoidFeatherEntity.class, EntityDataSerializers.BOOLEAN);

    private int stuckTicks;
    private int clientStuckAge;
    private int flightTicks;
    @Nullable
    private UUID stuckTarget;
    private Vec3 stuckOffset = Vec3.ZERO;

    public VoidFeatherEntity(EntityType<? extends VoidFeatherEntity> type, Level level) {
        super(type, level);
    }

    public VoidFeatherEntity(Level level, LivingEntity owner, boolean white) {
        this(ModEntities.VOID_FEATHER.get(), level);
        setOwner(owner);
        setWhite(white);
        setPos(owner.getX(), owner.getEyeY(), owner.getZ());
    }

    public boolean isWhite() {
        return this.entityData.get(IS_WHITE);
    }

    public void setWhite(boolean white) {
        this.entityData.set(IS_WHITE, white);
    }

    /** Client only: ticks since this feather was first seen stuck (0 while in flight). */
    public int getClientStuckAge() {
        return this.clientStuckAge;
    }

    public boolean isStuck() {
        return this.entityData.get(STUCK);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_WHITE, false);
        builder.define(STUCK, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("White", isWhite());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setWhite(tag.getBoolean("White"));
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof AngelOfDeathBossEntity)
                && !(entity instanceof VoidCrowEntity) && !(entity instanceof VoidFeatherEntity)
                && !(entity instanceof SingularityEntity);
    }

    @Override
    public void tick() {
        super.tick();
        if (isStuck()) {
            if (this.level().isClientSide) {
                this.clientStuckAge++;
            } else {
                tickStuck();
            }
            return;
        }
        Vec3 delta = getDeltaMovement();
        if (!this.level().isClientSide) {
            if (++this.flightTicks > 120) {
                discard();
                return;
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                this.hitTargetOrDeflectSelf(hit);
                if (isStuck() || isRemoved()) {
                    return;
                }
            }
        }
        setPos(getX() + delta.x, getY() + delta.y, getZ() + delta.z);
        this.updateRotation();
        setDeltaMovement(delta.add(0.0D, -0.012D, 0.0D).scale(0.995D));
    }

    private void tickStuck() {
        this.stuckTicks++;
        if (this.stuckTarget != null && this.level() instanceof ServerLevel level) {
            Entity target = level.getEntity(this.stuckTarget);
            if (target != null && target.isAlive()) {
                Vec3 p = target.position().add(this.stuckOffset);
                setPos(p.x, p.y, p.z);
            }
        }
        if (this.stuckTicks >= FUSE_TICKS) {
            detonate();
        }
    }

    private void stick() {
        this.entityData.set(STUCK, true);
        setDeltaMovement(Vec3.ZERO);
        this.stuckTicks = 0;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        Vec3 dir = getDeltaMovement().normalize();
        Vec3 p = result.getLocation().add(dir.scale(0.15D));
        setPos(p.x, p.y, p.z);
        stick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (this.level() instanceof ServerLevel level && entity instanceof LivingEntity living) {
            living.hurt(SingularityActions.trueDamage(level, this, getOwner()), HIT_DAMAGE);
        }
        this.stuckTarget = entity.getUUID();
        this.stuckOffset = position().subtract(entity.position());
        stick();
    }

    private void detonate() {
        if (this.level() instanceof ServerLevel level) {
            UUID owner = getOwner() != null ? getOwner().getUUID() : null;
            // Tiny visual-only hole: attack variant => no gravity pull, no push, just the 6.0F AoE pulse.
            SingularityEntity hole = SingularityActions.spawnHole(level, isWhite() ? 1 : 0, 1, owner, true, 8, position());
            hole.setBurst(BLAST_DAMAGE, 3.5D, 0.0D);
        }
        discard();
    }
}
