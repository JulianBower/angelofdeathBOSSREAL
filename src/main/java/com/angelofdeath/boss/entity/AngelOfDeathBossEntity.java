package com.angelofdeath.boss.entity;

import com.angelofdeath.boss.logic.SingularityActions;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Angel of Death. 600 HP / 16 armor, flying, two phases.
 *
 * <p>ATTACK_POSE: 0 idle, 1 grapple reach, 2 punch, 3 arms out (push+summon / scissors / true grey),
 * 4 aerial slam, 5 wing clap, 6 wing thrust, 7 wing guard, 8 feather barrage / grey shots.</p>
 */
public class AngelOfDeathBossEntity extends Monster {
    public static final int PHASE_ONE = 1;
    public static final int PHASE_TWO = 2;
    public static final int TRANSITION_LENGTH = 60;
    public static final int GUARD_LENGTH = 35;

    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(AngelOfDeathBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRANSITION_TICKS =
            SynchedEntityData.defineId(AngelOfDeathBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_POSE =
            SynchedEntityData.defineId(AngelOfDeathBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GLOWING_HAND =
            SynchedEntityData.defineId(AngelOfDeathBossEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int A_NONE = 0;
    private static final int A_GRAPPLE = 1;
    private static final int A_CROWS = 2;
    private static final int A_SLAM = 3;
    private static final int A_TORNADO = 4;
    private static final int A_THRUST = 5;
    private static final int A_BARRAGE = 6;
    private static final int A_SCISSORS = 7;
    private static final int A_GREY_TRUE = 8;
    private static final int A_GREY_PIERCE = 9;
    private static final int A_GUARD = 10;

    private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(new Vector3f(0.0F, 0.0F, 0.0F), 2.0F);

    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);

    private int attack = A_NONE;
    private int attackTick;
    private int stage;
    private int stageTick;
    private int cooldown = 60;
    private int guardCooldown;
    private int lastAttack = A_NONE;
    private boolean flag;
    private Vec3 lock = Vec3.ZERO;
    private Vec3 tornadoCenter = Vec3.ZERO;
    private int tornadoTicks;
    @Nullable
    private UUID holeA;
    @Nullable
    private UUID holeB;
    @Nullable
    private UUID backHole;
    private final List<UUID> summonHoles = new ArrayList<>();
    private final List<UUID> summonedCrows = new ArrayList<>();
    private int crowLockTicks;
    private double transitionBaseY;

    public AngelOfDeathBossEntity(EntityType<? extends AngelOfDeathBossEntity> type, Level level) {
        super(type, level);
        this.xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0D)
                .add(Attributes.ARMOR, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.90D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.FLYING_SPEED, 0.8D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D);
    }

    // ------------------------------------------------------------------ synced data

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    public int getTransitionTicks() {
        return this.entityData.get(TRANSITION_TICKS);
    }

    public int getAttackPose() {
        return this.entityData.get(ATTACK_POSE);
    }

    public boolean isGlowingHand() {
        return this.entityData.get(GLOWING_HAND);
    }

    private void setPhase(int phase) {
        this.entityData.set(PHASE, phase);
    }

    private void setTransitionTicks(int ticks) {
        this.entityData.set(TRANSITION_TICKS, Mth.clamp(ticks, 0, TRANSITION_LENGTH));
    }

    private void setAttackPose(int pose) {
        this.entityData.set(ATTACK_POSE, Mth.clamp(pose, 0, 8));
    }

    private void setGlowingHand(boolean glowing) {
        this.entityData.set(GLOWING_HAND, glowing);
    }

    public boolean isTransitioning() {
        return getTransitionTicks() > 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, PHASE_ONE);
        builder.define(TRANSITION_TICKS, 0);
        builder.define(ATTACK_POSE, 0);
        builder.define(GLOWING_HAND, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", getPhase());
        tag.putInt("TransitionTicks", getTransitionTicks());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPhase(Mth.clamp(tag.getInt("Phase"), PHASE_ONE, PHASE_TWO));
        this.entityData.set(TRANSITION_TICKS, Mth.clamp(tag.getInt("TransitionTicks"), 0, TRANSITION_LENGTH));
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // ------------------------------------------------------------------ body rules

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public boolean isNoGravity() {
        return isAirborne();
    }

    /** He walks on the ground; he only leaves it for the fusion, the crow phase and the aerial slam. */
    private boolean isAirborne() {
        return getTransitionTicks() > 0 || this.attack == A_CROWS || this.crowLockTicks > 0
                || (this.attack == A_SLAM && this.stage < 3);
    }

    /** True while a pending Power-1 summon hole or a summoned crow is still alive (400-tick safety timeout). */
    public boolean hasActiveCrows() {
        if (this.crowLockTicks <= 0 || !(this.level() instanceof ServerLevel level)) {
            return false;
        }
        for (UUID id : this.summonHoles) {
            if (hole(level, id) != null) {
                return true;
            }
        }
        for (UUID id : this.summonedCrows) {
            if (level.getEntity(id) instanceof VoidCrowEntity crow && crow.isAlive()) {
                return true;
            }
        }
        return false;
    }

    /** Called by a summon hole when it hatches. */
    public void registerCrow(UUID crowId) {
        this.summonedCrows.add(crowId);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FLY_INTO_WALL)
                || source.is(DamageTypes.CRAMMING) || super.isInvulnerableTo(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.level() instanceof ServerLevel level && !level.isClientSide) {
            discardHoles(level);
            discard(level, this.backHole);
            this.backHole = null;
        }
        super.remove(reason);
    }

    // ------------------------------------------------------------------ client animation state

    /** Smoothed per-pose weights (index = ATTACK_POSE 0-8), interpolated by the renderer with partialTick. */
    public final float[] poseWeight = new float[9];
    public final float[] poseWeightO = new float[9];
    public float glowWeight;
    public float glowWeightO;
    public float transitionWeight;
    public float transitionWeightO;
    /** Client: ticks the current ATTACK_POSE has been held (drives the pose-5 wing sequence). */
    public int poseTicks;
    private int lastClientPose;
    /** Client: where the pose-5 black tornado stands (captured when it spawns), null otherwise. */
    @Nullable
    public Vec3 clientTornadoCenter;

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            tickClientAnimation();
        }
    }

    private void tickClientAnimation() {
        int pose = getAttackPose();
        if (pose != this.lastClientPose) {
            this.lastClientPose = pose;
            this.poseTicks = 0;
        } else {
            this.poseTicks++;
        }
        if (pose != 5) {
            this.clientTornadoCenter = null;
        } else if (this.poseTicks == 10) {
            Vec3 fwd = Vec3.directionFromRotation(0.0F, getYRot());
            double cx = getX() + fwd.x * 6.0D;
            double cz = getZ() + fwd.z * 6.0D;
            double base = getY() - 4.0D;
            for (int i = 0; i < 18; i++) {
                BlockPos below = BlockPos.containing(cx, getY() - i, cz);
                if (!this.level().getBlockState(below).isAir()) {
                    base = below.getY() + 1.0D;
                    break;
                }
            }
            this.clientTornadoCenter = new Vec3(cx, base, cz);
        }
        for (int i = 0; i < this.poseWeight.length; i++) {
            this.poseWeightO[i] = this.poseWeight[i];
            float target = i == pose ? 1.0F : 0.0F;
            // The punch (2) and the wing snaps (5, 8) are meant to be sudden; everything else eases in.
            float rate = (i == 2 || i == 5 || i == 8) && target > 0.0F ? 0.7F : 0.22F;
            this.poseWeight[i] += Mth.clamp(target - this.poseWeight[i], -rate, rate);
        }
        this.glowWeightO = this.glowWeight;
        this.glowWeight += Mth.clamp((isGlowingHand() ? 1.0F : 0.0F) - this.glowWeight, -0.15F, 0.3F);
        this.transitionWeightO = this.transitionWeight;
        this.transitionWeight += Mth.clamp((isTransitioning() ? 1.0F : 0.0F) - this.transitionWeight, -0.08F, 0.12F);

        if (isGlowingHand() && this.random.nextInt(2) == 0) {
            this.level().addParticle(ParticleTypes.END_ROD, getX() + (this.random.nextDouble() - 0.5D) * 1.4D,
                    getY() + 2.6D + this.random.nextDouble() * 1.2D, getZ() + (this.random.nextDouble() - 0.5D) * 1.4D,
                    0.0D, -0.05D, 0.0D);
        }
        if (isTransitioning()) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL, getX() + (this.random.nextDouble() - 0.5D) * 3.0D,
                    getY() + 1.0D + this.random.nextDouble() * 2.5D, getZ() + (this.random.nextDouble() - 0.5D) * 3.0D,
                    0.0D, 0.0D, 0.0D);
        } else if (getPhase() == PHASE_TWO && this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.ASH, getX() + (this.random.nextDouble() - 0.5D) * 2.5D,
                    getY() + 1.0D + this.random.nextDouble() * 2.5D, getZ() + (this.random.nextDouble() - 0.5D) * 2.5D,
                    0.0D, 0.02D, 0.0D);
        }
    }

    // ------------------------------------------------------------------ damage / wing guard

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) {
            return super.hurt(source, amount);
        }
        if (isTransitioning() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (isFrontalMeleeOrArrow(source)) {
            if (this.attack == A_GUARD) {
                blockSound();
                return false;
            }
            if (this.attack == A_NONE && this.guardCooldown <= 0 && getTarget() != null && !hasActiveCrows()
                    && this.random.nextFloat() < 0.30F) {
                startGuard();
                blockSound();
                return false;
            }
        }
        if (getPhase() == PHASE_ONE && amount >= getHealth()) {
            amount = Math.max(0.0F, getHealth() - 1.0F); // must reach the fusion phase before dying
        }
        return super.hurt(source, amount);
    }

    private boolean isFrontalMeleeOrArrow(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        boolean arrow = source.is(DamageTypeTags.IS_PROJECTILE);
        boolean melee = source.getDirectEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity();
        if (!arrow && !melee) {
            return false;
        }
        Vec3 from = source.getSourcePosition();
        if (from == null) {
            return false;
        }
        Vec3 to = new Vec3(from.x - getX(), 0.0D, from.z - getZ());
        return to.lengthSqr() < 1.0E-4D || to.normalize().dot(forward()) > 0.3D;
    }

    private void blockSound() {
        if (this.level() instanceof ServerLevel level) {
            SingularityActions.playSound(level, position(), SoundEvents.SHIELD_BLOCK, 1.5F, 0.8F);
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.8D, getZ(), 8, 0.6D, 0.6D, 0.6D, 0.05D);
        }
    }

    private void startGuard() {
        this.attack = A_GUARD;
        this.attackTick = 0;
        this.stage = 0;
        this.guardCooldown = 160;
        setAttackPose(7);
    }

    // ------------------------------------------------------------------ geometry helpers

    private Vec3 forward() {
        return Vec3.directionFromRotation(0.0F, getYRot());
    }

    private Vec3 left() {
        Vec3 f = forward();
        return new Vec3(f.z, 0.0D, -f.x);
    }

    private Vec3 handLeft() {
        return position().add(0.0D, 1.8D, 0.0D).add(forward().scale(2.2D)).add(left().scale(1.5D));
    }

    private void face(Vec3 pos) {
        float yaw = (float) (Mth.atan2(pos.z - getZ(), pos.x - getX()) * 57.29577951308232D) - 90.0F;
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private void flyTo(Vec3 pos, double speed) {
        if (!isAirborne()) {
            // grounded: stride horizontally toward the point, gravity handles the rest
            Vec3 flat = new Vec3(pos.x - getX(), 0.0D, pos.z - getZ());
            double fd = flat.length();
            Vec3 cur = getDeltaMovement();
            if (fd < 0.5D) {
                setDeltaMovement(cur.x * 0.6D, cur.y, cur.z * 0.6D);
            } else {
                Vec3 want = flat.scale(Math.min(speed, 0.35D) / fd);
                setDeltaMovement(cur.x * 0.7D + want.x * 0.3D, cur.y, cur.z * 0.7D + want.z * 0.3D);
            }
            return;
        }
        Vec3 to = pos.subtract(position());
        double d = to.length();
        if (d < 0.3D) {
            setDeltaMovement(getDeltaMovement().scale(0.6D));
            return;
        }
        Vec3 want = to.scale(speed / d);
        if (d < speed * 4.0D) {
            want = want.scale(d / (speed * 4.0D));
        }
        setDeltaMovement(getDeltaMovement().scale(0.7D).add(want.scale(0.3D)));
    }

    /** Grounded chase: path toward the target until he is in striking range. */
    private void walkTo(LivingEntity target) {
        if (distanceTo(target) > 7.0F) {
            getNavigation().moveTo(target, 1.0D);
        } else {
            getNavigation().stop();
            brake();
        }
    }

    private double groundY(ServerLevel level) {
        for (int i = 0; i < 24; i++) {
            BlockPos below = BlockPos.containing(getX(), getY() - i, getZ());
            if (!level.getBlockState(below).isAir()) {
                return below.getY() + 1.0D;
            }
        }
        return getY() - 4.0D;
    }

    private void brake() {
        setDeltaMovement(getDeltaMovement().scale(0.75D));
    }

    @Nullable
    private static SingularityEntity hole(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof SingularityEntity s && !s.isRemoved() ? s : null;
    }

    private static void discard(ServerLevel level, @Nullable UUID id) {
        SingularityEntity s = hole(level, id);
        if (s != null) {
            s.discard();
        }
    }

    private void discardHoles(ServerLevel level) {
        discard(level, this.holeA);
        discard(level, this.holeB);
        this.holeA = null;
        this.holeB = null;
    }

    private SingularityEntity spawn(ServerLevel level, int mode, int power, int duration, Vec3 pos) {
        return SingularityActions.spawnHole(level, mode, power, getUUID(), true, duration, pos);
    }

    // ------------------------------------------------------------------ main AI loop

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(getHealth() / getMaxHealth());
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.guardCooldown > 0) {
            this.guardCooldown--;
        }
        if (this.crowLockTicks > 0) {
            this.crowLockTicks--;
            if (!hasActiveCrows()) {
                this.crowLockTicks = 0;
                this.summonHoles.clear();
                this.summonedCrows.clear();
            }
        }
        if (getPhase() == PHASE_ONE && !isTransitioning() && getHealth() <= getMaxHealth() * 0.5F) {
            startTransition(level);
        }
        if (isTransitioning()) {
            tickTransition(level);
            return;
        }
        if (getPhase() == PHASE_TWO) {
            maintainBackHole(level);
        }
        LivingEntity target = getTarget();
        if (hasActiveCrows() && this.attack != A_CROWS) {
            // strict lock: no attack of any kind until both crows are dead
            if (this.attack != A_NONE) {
                endAttack(level, 30);
            }
            if (target != null && target.isAlive()) {
                face(target.position());
            }
            flyTo(new Vec3(getX(), groundY(level) + 4.0D, getZ()), 0.3D);
            return;
        }
        if (target == null || !target.isAlive()) {
            if (this.attack != A_NONE) {
                endAttack(level, 40);
            }
            brake();
            return;
        }
        face(target.position());

        if (this.attack == A_NONE) {
            walkTo(target);
            if (this.cooldown > 0) {
                this.cooldown--;
                return;
            }
            startAttack();
        } else {
            this.attackTick++;
            if (tickAttack(level, target)) {
                endAttack(level, getPhase() == PHASE_ONE ? 45 + this.random.nextInt(25) : 30 + this.random.nextInt(20));
            }
        }
    }

    private void startAttack() {
        int[] pool = getPhase() == PHASE_ONE
                ? new int[] {A_GRAPPLE, A_CROWS, A_SLAM, A_TORNADO, A_THRUST, A_BARRAGE, A_SCISSORS}
                : new int[] {A_SLAM, A_TORNADO, A_THRUST, A_BARRAGE, A_SCISSORS, A_GREY_TRUE, A_GREY_PIERCE,
                        A_GREY_TRUE, A_GREY_PIERCE};
        int pick = pool[this.random.nextInt(pool.length)];
        for (int i = 0; i < 4 && pick == this.lastAttack; i++) {
            pick = pool[this.random.nextInt(pool.length)];
        }
        this.attack = pick;
        this.lastAttack = pick;
        this.attackTick = 0;
        this.stage = 0;
        this.stageTick = 0;
        this.flag = false;
        this.tornadoTicks = 0;
    }

    private void endAttack(ServerLevel level, int nextCooldown) {
        discardHoles(level);
        setAttackPose(0);
        setGlowingHand(false);
        this.attack = A_NONE;
        this.attackTick = 0;
        this.stage = 0;
        this.stageTick = 0;
        this.flag = false;
        this.tornadoTicks = 0;
        this.cooldown = nextCooldown;
    }

    private boolean tickAttack(ServerLevel level, LivingEntity target) {
        return switch (this.attack) {
            case A_GRAPPLE -> tickGrapple(level, target);
            case A_CROWS -> tickCrowSummon(level, target);
            case A_SLAM -> tickSlam(level, target);
            case A_TORNADO -> tickTornado(level, target);
            case A_THRUST -> tickThrust(level, target);
            case A_BARRAGE -> tickBarrage(level, target);
            case A_SCISSORS -> tickScissors(level, target);
            case A_GREY_TRUE -> tickGreyTrue(level, target);
            case A_GREY_PIERCE -> tickGreyPierce(level, target);
            case A_GUARD -> tickGuard();
            default -> true;
        };
    }

    // ------------------------------------------------------------------ guard

    private boolean tickGuard() {
        setAttackPose(7);
        brake();
        return this.attackTick >= GUARD_LENGTH;
    }

    // ------------------------------------------------------------------ 1. Black Hole Grapple-Punch

    private boolean tickGrapple(ServerLevel level, LivingEntity target) {
        brake();
        if (this.stage == 0) {
            Vec3 hand = handLeft();
            if (this.attackTick == 1) {
                setAttackPose(1);
                SingularityEntity palm = spawn(level, 0, 1, 50, hand); // visual-only, drawn in his palm by the renderer
                palm.setOwnerRendered(true);
                this.holeA = palm.getUUID();
            }
            SingularityEntity h = hole(level, this.holeA);
            if (h != null) {
                h.setPos(hand.x, hand.y, hand.z);
            }
            SingularityActions.pull(target, hand, 0.28D, 1.2D);
            target.fallDistance = 0.0F;
            double d = target.getBoundingBox().getCenter().distanceTo(hand);
            if (d < 2.5D || this.attackTick >= 35) {
                discardHoles(level);
                setAttackPose(2);
                this.stage = 1;
                this.stageTick = 0;
            }
            return false;
        }
        this.stageTick++;
        if (this.stageTick == 5) {
            if (distanceTo(target) < 5.5F && SingularityActions.canHarm(target)) {
                target.invulnerableTime = 0;
                target.hurt(this.damageSources().mobAttack(this), 14.0F);
                SingularityActions.launch(target, forward(), 3.0D, 0.7D);
                level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.0D, target.getZ(), 3,
                        0.3D, 0.3D, 0.3D, 0.0D);
                SingularityActions.playSound(level, position(), SoundEvents.PLAYER_ATTACK_STRONG, 2.0F, 0.6F);
            }
        }
        return this.stageTick >= 16;
    }

    // ------------------------------------------------------------------ 2. False White Hole Push & Crow Summon

    private boolean tickCrowSummon(ServerLevel level, LivingEntity target) {
        flyTo(new Vec3(getX(), groundY(level) + 4.0D, getZ()), 0.3D); // lifts off for the summon
        Vec3 hand = handLeft();
        if (this.attackTick == 1) {
            setAttackPose(3);
            SingularityEntity palm = spawn(level, 1, 1, 14, hand); // visual-only push, drawn in his palm
            palm.setOwnerRendered(true);
            this.holeA = palm.getUUID();
        }
        if (this.attackTick <= 12) {
            SingularityEntity h = hole(level, this.holeA);
            if (h != null) {
                h.setPos(hand.x, hand.y, hand.z);
            }
            if (distanceTo(target) < 16.0F) {
                SingularityActions.push(target, hand, 1.1D * (1.0D - this.attackTick / 14.0D), 0.15D, 3.0D);
            }
        }
        if (this.attackTick == 10) {
            this.summonHoles.clear();
            this.summonedCrows.clear();
            for (int i = 0; i < 2; i++) {
                Vec3 pos = position().add(0.0D, 2.5D, 0.0D).add(left().scale(i == 0 ? 4.0D : -4.0D));
                SingularityEntity h = spawn(level, i, 1, 20, pos); // 1 Power-1 black hole, 1 Power-1 white hole
                h.setSpawnCrowOnExpire(true);
                this.summonHoles.add(h.getUUID());
            }
            this.crowLockTicks = 400;
            SingularityActions.playSound(level, position(), SoundEvents.PORTAL_AMBIENT, 3.0F, 0.6F);
        }
        return this.attackTick >= 45;
    }

    // ------------------------------------------------------------------ 3. White-Hand Aerial Slam

    private boolean tickSlam(ServerLevel level, LivingEntity target) {
        setAttackPose(4);
        setGlowingHand(this.stage < 3);
        switch (this.stage) {
            case 0 -> { // ascend
                flyTo(target.position().add(0.0D, 8.0D, 0.0D), 1.0D);
                if (getY() >= target.getY() + 6.0D || this.attackTick >= 30) {
                    this.stage = 1;
                    this.stageTick = 0;
                }
            }
            case 1 -> { // charge
                brake();
                this.stageTick++;
                level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.0D, getZ(), 6, 0.5D, 0.5D, 0.5D, 0.05D);
                if (this.stageTick >= 8) {
                    this.lock = target.position();
                    this.stage = 2;
                    this.stageTick = 0;
                }
            }
            case 2 -> { // dive
                this.stageTick++;
                Vec3 dir = this.lock.subtract(position());
                if (dir.lengthSqr() > 1.0E-4D) {
                    setDeltaMovement(dir.normalize().scale(2.2D));
                }
                boolean impact = this.verticalCollision || this.onGround() || getY() <= this.lock.y + 1.5D
                        || this.stageTick > 40;
                if (impact) {
                    setDeltaMovement(Vec3.ZERO);
                    // Power-2 White Hole, attack variant: no block push, no field, 12.0F AoE pulse only.
                    SingularityEntity h = spawn(level, 1, 2, 12, position());
                    h.setBurst(12.0F, 9.0D, 1.4D);
                    setGlowingHand(false);
                    this.stage = 3;
                    this.stageTick = 0;
                }
            }
            default -> {
                brake();
                this.stageTick++;
                return this.stageTick >= 14;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ 4. Wing-Clap Black Tornado

    private boolean tickTornado(ServerLevel level, LivingEntity target) {
        setAttackPose(5);
        if (this.attackTick < 10) {
            flyTo(target.position().add(forward().scale(-8.0D)).add(0.0D, 4.0D, 0.0D), 0.6D);
            return false;
        }
        brake();
        if (this.attackTick == 10) {
            SingularityActions.playSound(level, position(), SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 1.0F);
            this.tornadoCenter = new Vec3(getX() + forward().x * 6.0D, target.getY(), getZ() + forward().z * 6.0D);
            this.tornadoTicks = 55;
        }
        if (this.tornadoTicks > 0) {
            int age = 55 - this.tornadoTicks;
            this.tornadoTicks--;
            tickTornadoEffects(level, age);
        }
        return this.attackTick >= 10 + 55 + 8;
    }

    private void tickTornadoEffects(ServerLevel level, int age) {
        Vec3 c = this.tornadoCenter;
        for (int h = 0; h < 10; h++) {
            double r = 0.8D + h * 0.5D;
            double a = age * 0.55D + h * 0.9D;
            level.sendParticles(BLACK_DUST, c.x + Math.cos(a) * r, c.y + h * 0.9D, c.z + Math.sin(a) * r, 2, 0.1D, 0.1D,
                    0.1D, 0.0D);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x + Math.cos(a + 3.0D) * r, c.y + h * 0.9D,
                    c.z + Math.sin(a + 3.0D) * r, 1, 0.1D, 0.1D, 0.1D, 0.0D);
        }
        AABB box = new AABB(c.x - 16.0D, c.y - 3.0D, c.z - 16.0D, c.x + 16.0D, c.y + 14.0D, c.z + 16.0D);
        for (Player p : level.getEntitiesOfClass(Player.class, box, SingularityActions::canHarm)) {
            Vec3 to = new Vec3(c.x - p.getX(), 0.0D, c.z - p.getZ());
            double d = to.length();
            if (d < 0.1D) {
                continue;
            }
            Vec3 in = to.scale(1.0D / d);
            Vec3 tangent = new Vec3(-in.z, 0.0D, in.x);
            Vec3 v = p.getDeltaMovement().add(in.scale(0.16D)).add(tangent.scale(0.08D)).add(0.0D, d < 3.0D ? 0.12D : 0.0D, 0.0D);
            double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
            if (horizontal > 1.0D) {
                v = new Vec3(v.x / horizontal, v.y, v.z / horizontal);
            }
            p.setDeltaMovement(v);
            p.hurtMarked = true;
            p.fallDistance = 0.0F;
            if (d < 4.0D && age % 10 == 0) {
                p.invulnerableTime = 0;
                p.hurt(SingularityActions.voidSource(level, this, this), 4.0F);
            }
        }
        if (age % 20 == 0) {
            SingularityActions.playSound(level, c, SoundEvents.PORTAL_AMBIENT, 2.0F, 0.5F);
        }
    }

    // ------------------------------------------------------------------ 5. Piercing Wing Thrust

    private boolean tickThrust(ServerLevel level, LivingEntity target) {
        setAttackPose(6);
        if (this.attackTick < 14) {
            flyTo(target.position().add(forward().scale(-7.0D)).add(0.0D, 1.5D, 0.0D), 0.6D);
            return false;
        }
        if (this.attackTick == 14) {
            this.lock = target.getBoundingBox().getCenter().subtract(position().add(0.0D, 1.5D, 0.0D)).normalize();
            SingularityActions.playSound(level, position(), SoundEvents.PHANTOM_SWOOP, 3.0F, 0.6F);
        }
        if (this.attackTick < 25) {
            setDeltaMovement(this.lock.scale(2.0D));
            if (!this.flag && getBoundingBox().inflate(1.5D).intersects(target.getBoundingBox())) {
                this.flag = true;
                if (SingularityActions.canHarm(target)) {
                    target.invulnerableTime = 0;
                    target.hurt(SingularityActions.trueDamage(level, this, this), 11.0F); // armor-bypassing
                    SingularityActions.launch(target, this.lock, 1.6D, 0.4D);
                }
            }
        } else {
            brake();
        }
        return this.attackTick >= 34;
    }

    // ------------------------------------------------------------------ 6. Exploding Feather Barrage

    private boolean tickBarrage(ServerLevel level, LivingEntity target) {
        setAttackPose(8);
        flyTo(target.position().add(forward().scale(-9.0D)).add(0.0D, 4.0D, 0.0D), 0.5D);
        int shot = (this.attackTick - 12) / 3;
        if (this.attackTick >= 12 && (this.attackTick - 12) % 3 == 0 && shot < 6) {
            Vec3 origin = position().add(0.0D, 2.0D, 0.0D).add(forward().scale(1.5D));
            Vec3 aim = target.getBoundingBox().getCenter().subtract(origin).normalize();
            Vec3 dir = aim.yRot((float) Math.toRadians((shot - 2.5D) * 6.0D));
            VoidFeatherEntity feather = new VoidFeatherEntity(level, this, shot % 2 == 1); // 3 black, 3 white
            feather.setPos(origin.x, origin.y, origin.z);
            feather.shoot(dir.x, dir.y, dir.z, 2.0F, 1.0F);
            level.addFreshEntity(feather);
            SingularityActions.playSound(level, origin, SoundEvents.PHANTOM_SWOOP, 1.5F, 1.4F);
        }
        return this.attackTick >= 45;
    }

    // ------------------------------------------------------------------ 7. Eclipse Scissors

    private boolean tickScissors(ServerLevel level, LivingEntity target) {
        setAttackPose(3);
        brake();
        if (this.attackTick == 1) {
            Vec3 look = target.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0D, look.z);
            flat = flat.lengthSqr() < 1.0E-4D ? forward() : flat.normalize();
            Vec3 targetLeft = new Vec3(flat.z, 0.0D, -flat.x);
            Vec3 mid = target.position().add(0.0D, 1.0D, 0.0D);
            this.holeA = spawn(level, 0, 2, 50, mid.add(targetLeft.scale(5.0D))).getUUID();
            this.holeB = spawn(level, 1, 2, 50, mid.subtract(targetLeft.scale(5.0D))).getUUID();
        }
        SingularityEntity black = hole(level, this.holeA);
        SingularityEntity white = hole(level, this.holeB);
        if (black != null && white != null && this.attackTick <= 50) {
            // Black on the player's left pulls, white on the right pushes: both drag them sideways.
            SingularityActions.pull(target, black.position(), 0.18D, 1.2D);
            SingularityActions.push(target, white.position(), 0.22D, 0.0D, 1.2D);
            if (this.attackTick % 10 == 0 && target.getBoundingBox().getCenter().distanceTo(black.position()) < 2.5D
                    && SingularityActions.canHarm(target)) {
                target.invulnerableTime = 0;
                target.hurt(SingularityActions.voidSource(level, black, this), 3.0F);
            }
        }
        return this.attackTick >= 55;
    }

    // ------------------------------------------------------------------ Phase 2: Grey attacks

    private boolean tickGreyTrue(ServerLevel level, LivingEntity target) {
        setAttackPose(3);
        if (this.attackTick < 15) {
            if (distanceTo(target) > 8.0F) {
                flyTo(target.position().add(0.0D, 2.0D, 0.0D), 1.6D); // swift dash
            } else {
                brake();
            }
            return false;
        }
        if (this.attackTick == 16) {
            // True Grey Hole: arena stasis freeze for 2 s, latched onto the target, detonates on expiry.
            SingularityEntity grey = SingularityActions.spawnHole(level, 2, 3, getUUID(), false, 40,
                    target.getBoundingBox().getCenter());
            grey.setAttachedTarget(target.getUUID());
            grey.setExpire(14.0F, 8.0D);
        }
        if (this.attackTick > 20) {
            flyTo(target.position().add(forward().scale(-12.0D)).add(0.0D, 5.0D, 0.0D), 0.7D);
        } else {
            brake();
        }
        return this.attackTick >= 55;
    }

    private boolean tickGreyPierce(ServerLevel level, LivingEntity target) {
        setAttackPose(8);
        flyTo(target.position().add(forward().scale(-10.0D)).add(0.0D, 4.0D, 0.0D), 0.5D);
        if (this.attackTick == 15 || this.attackTick == 25 || this.attackTick == 35) {
            Vec3 origin = position().add(0.0D, 1.8D, 0.0D).subtract(forward().scale(1.4D));
            Vec3 dir = target.getBoundingBox().getCenter().subtract(origin).normalize();
            SingularityEntity shot = spawn(level, 2, 2, 40, origin); // attack variant: fast piercing impact shot
            shot.setShot(dir.scale(2.4D), 9.0F);
            SingularityActions.playSound(level, origin, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 1.6F);
        }
        return this.attackTick >= 55;
    }

    /** Large inert Grey Hole floating behind his back between the grey wings. */
    private void maintainBackHole(ServerLevel level) {
        Vec3 pos = position().add(0.0D, 1.8D, 0.0D).subtract(forward().scale(1.4D));
        SingularityEntity h = hole(level, this.backHole);
        if (h == null) {
            h = spawn(level, 2, 3, 400, pos);
            h.setOwnerRendered(true); // the boss renderer floats it between the grey wings
            this.backHole = h.getUUID();
        }
        h.setPos(pos.x, pos.y, pos.z);
    }

    // ------------------------------------------------------------------ Phase 1 -> 2 fusion

    private void startTransition(ServerLevel level) {
        endAttack(level, 40);
        this.transitionBaseY = getY();
        setTransitionTicks(1);
        SingularityActions.playSound(level, position(), SoundEvents.ENDER_DRAGON_GROWL, 6.0F, 0.5F);
    }

    private void tickTransition(ServerLevel level) {
        int t = getTransitionTicks();
        setDeltaMovement(getDeltaMovement().scale(0.5D));
        if (getY() < this.transitionBaseY + 4.0D) {
            setDeltaMovement(getDeltaMovement().add(0.0D, 0.12D, 0.0D));
        }
        setAttackPose(0);

        // One Power-5 Black Hole and one Power-5 White Hole (visual only) spiral inward and collide.
        double angle = t * 0.45D;
        double radius = 9.0D * (1.0D - t / (double) TRANSITION_LENGTH) + 0.8D;
        double y = getY() + 1.6D + Math.sin(t * 0.2D) * 0.8D;
        Vec3 blackPos = new Vec3(getX() + Math.cos(angle) * radius, y, getZ() + Math.sin(angle) * radius);
        Vec3 whitePos = new Vec3(getX() - Math.cos(angle) * radius, y, getZ() - Math.sin(angle) * radius);
        SingularityEntity black = hole(level, this.holeA);
        if (black == null) {
            black = spawn(level, 0, 5, 90, blackPos);
            this.holeA = black.getUUID();
        }
        SingularityEntity white = hole(level, this.holeB);
        if (white == null) {
            white = spawn(level, 1, 5, 90, whitePos);
            this.holeB = white.getUUID();
        }
        black.setPos(blackPos.x, blackPos.y, blackPos.z);
        white.setPos(whitePos.x, whitePos.y, whitePos.z);

        if (t >= TRANSITION_LENGTH) {
            finishTransition(level);
            return;
        }
        setTransitionTicks(t + 1);
    }

    private void finishTransition(ServerLevel level) {
        discardHoles(level);
        Vec3 c = position().add(0.0D, 1.6D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 3, 2.0D, 2.0D, 2.0D, 0.0D);
        level.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 250, 3.0D, 3.0D, 3.0D, 0.9D);
        level.sendParticles(ParticleTypes.ASH, c.x, c.y, c.z, 200, 4.0D, 4.0D, 4.0D, 0.2D);
        SingularityActions.playSound(level, c, SoundEvents.LIGHTNING_BOLT_THUNDER, 12.0F, 0.5F);
        SingularityActions.playSound(level, c, SoundEvents.LIGHTNING_BOLT_IMPACT, 12.0F, 0.6F);

        DamageSource source = level.damageSources().explosion(this, this);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(16.0D),
                e -> e != this && !(e instanceof VoidCrowEntity) && SingularityActions.canHarm(e));
        for (LivingEntity living : victims) {
            Vec3 away = living.getBoundingBox().getCenter().subtract(c);
            if (away.length() > 16.0D) {
                continue;
            }
            living.invulnerableTime = 0;
            living.hurt(source, 20.0F);
            SingularityActions.launch(living, away, 2.5D, 0.7D);
        }

        setTransitionTicks(0);
        setPhase(PHASE_TWO);
        this.cooldown = 40;
    }
}
