package com.angelofdeath.boss.entity;

import com.angelofdeath.boss.logic.SingularityActions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Summoned crow (~1.8x Allay size, see ModEntities). Black: swoops down and lifts the player into the air while
 * dealing Black Hole void damage. White: dives with heavy knockback.
 */
public class VoidCrowEntity extends Monster {
    private static final EntityDataAccessor<Boolean> IS_WHITE =
            SynchedEntityData.defineId(VoidCrowEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int HOVER = 0;
    private static final int SWOOP = 1;
    private static final int CARRY = 2;
    private static final int DIVE = 3;
    private static final int RECOVER = 4;
    private static final int CARRY_TICKS = 25;

    private int state = HOVER;
    private int stateTick;
    private int cooldown = 20;

    public VoidCrowEntity(EntityType<? extends VoidCrowEntity> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.8D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D);
    }

    public boolean isWhite() {
        return this.entityData.get(IS_WHITE);
    }

    public void setWhite(boolean white) {
        this.entityData.set(IS_WHITE, white);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_WHITE, false);
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
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.core.BlockPos pos) {
    }

    private void setState(int newState) {
        this.state = newState;
        this.stateTick = 0;
    }

    private void flyTo(Vec3 pos, double speed) {
        Vec3 to = pos.subtract(position());
        double d = to.length();
        if (d < 0.2D) {
            setDeltaMovement(getDeltaMovement().scale(0.6D));
            return;
        }
        Vec3 want = to.scale(speed / d);
        setDeltaMovement(getDeltaMovement().scale(0.75D).add(want.scale(0.25D)));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            setState(HOVER);
            setDeltaMovement(getDeltaMovement().scale(0.9D));
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        this.stateTick++;
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        Vec3 center = target.getBoundingBox().getCenter();

        switch (this.state) {
            case HOVER -> {
                double a = this.tickCount * 0.08D + (this.getId() % 8);
                flyTo(target.position().add(Math.cos(a) * 6.0D, 5.0D, Math.sin(a) * 6.0D), 0.4D);
                if (this.cooldown <= 0 && distanceTo(target) < 28.0F) {
                    setState(isWhite() ? DIVE : SWOOP);
                }
            }
            case SWOOP -> {
                flyTo(center, 0.9D);
                if (distanceTo(target) < 2.0F && SingularityActions.canHarm(target)) {
                    setState(CARRY);
                } else if (this.stateTick > 80) {
                    this.cooldown = 40;
                    setState(RECOVER);
                }
            }
            case CARRY -> {
                if (!SingularityActions.canHarm(target)) {
                    setState(RECOVER);
                    break;
                }
                // Lift the player: strong upward velocity, little horizontal drift, crow rides on their head.
                Vec3 v = target.getDeltaMovement();
                target.setDeltaMovement(v.x * 0.5D, 0.42D, v.z * 0.5D);
                target.hurtMarked = true;
                target.fallDistance = 0.0F;
                this.setPos(target.getX(), target.getY() + target.getBbHeight() + 0.1D, target.getZ());
                setDeltaMovement(Vec3.ZERO);
                if (this.stateTick % 5 == 0) {
                    target.invulnerableTime = 0;
                    target.hurt(SingularityActions.voidSource(level, this, this), 2.0F);
                }
                if (this.stateTick >= CARRY_TICKS) {
                    this.cooldown = 90;
                    setState(RECOVER);
                }
            }
            case DIVE -> {
                flyTo(center, 1.3D);
                if (getBoundingBox().inflate(0.4D).intersects(target.getBoundingBox())) {
                    if (SingularityActions.canHarm(target)) {
                        target.invulnerableTime = 0;
                        target.hurt(this.damageSources().mobAttack(this), 6.0F);
                        Vec3 away = new Vec3(target.getX() - getX(), 0.0D, target.getZ() - getZ());
                        SingularityActions.launch(target, away, 2.4D, 0.5D);
                    }
                    this.cooldown = 70;
                    setState(RECOVER);
                } else if (this.stateTick > 70) {
                    this.cooldown = 40;
                    setState(RECOVER);
                }
            }
            default -> {
                flyTo(target.position().add(0.0D, 7.0D, 0.0D), 0.7D);
                if (this.stateTick > 20) {
                    setState(HOVER);
                }
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }
}
