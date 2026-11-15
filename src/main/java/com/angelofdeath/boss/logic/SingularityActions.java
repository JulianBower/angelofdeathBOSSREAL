package com.angelofdeath.boss.logic;

import com.angelofdeath.boss.entity.SingularityEntity;
import com.angelofdeath.boss.registry.ModEntities;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Gravity pull / repulsion helpers shared by holes, the boss and the crows. */
public final class SingularityActions {
    private SingularityActions() {
    }

    /** Black Hole void damage. */
    public static DamageSource voidSource(ServerLevel level, @Nullable Entity direct, @Nullable Entity owner) {
        return level.damageSources().source(DamageTypes.FELL_OUT_OF_WORLD, direct, owner);
    }

    /** Armor-bypassing true damage (indirect magic is in the minecraft:bypasses_armor tag). */
    public static DamageSource trueDamage(ServerLevel level, @Nullable Entity direct, @Nullable Entity owner) {
        return level.damageSources().indirectMagic(direct == null ? owner : direct, owner);
    }

    public static boolean canHarm(LivingEntity entity) {
        if (!entity.isAlive() || entity.isInvulnerable()) {
            return false;
        }
        return !(entity instanceof Player player) || (!player.isCreative() && !player.isSpectator());
    }

    /** Gravity pull toward a center, speed-clamped. */
    public static void pull(Entity target, Vec3 center, double strength, double maxSpeed) {
        Vec3 to = center.subtract(target.getBoundingBox().getCenter());
        double dist = to.length();
        if (dist < 0.01D) {
            return;
        }
        Vec3 velocity = target.getDeltaMovement().add(to.scale(strength / dist));
        applyVelocity(target, velocity, maxSpeed);
    }

    /** Repulsion away from a center with a small lift, speed-clamped. */
    public static void push(Entity target, Vec3 center, double strength, double lift, double maxSpeed) {
        Vec3 away = target.getBoundingBox().getCenter().subtract(center);
        double dist = away.length();
        Vec3 dir = dist < 0.01D ? new Vec3(0.0D, 1.0D, 0.0D) : away.scale(1.0D / dist);
        Vec3 velocity = target.getDeltaMovement().add(dir.scale(strength)).add(0.0D, lift, 0.0D);
        applyVelocity(target, velocity, maxSpeed);
    }

    /** Hard launch along a direction (used for punches and dives). */
    public static void launch(Entity target, Vec3 direction, double strength, double lift) {
        Vec3 dir = direction.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
        target.setDeltaMovement(dir.x * strength, lift, dir.z * strength);
        target.hurtMarked = true;
        target.fallDistance = 0.0F;
    }

    private static void applyVelocity(Entity target, Vec3 velocity, double maxSpeed) {
        double speed = velocity.length();
        if (speed > maxSpeed) {
            velocity = velocity.scale(maxSpeed / speed);
        }
        target.setDeltaMovement(velocity);
        target.hurtMarked = true;
    }

    public static void playSound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    public static SingularityEntity spawnHole(ServerLevel level, int mode, int power, @Nullable UUID owner,
            boolean attackVariant, int duration, Vec3 pos) {
        SingularityEntity hole = Objects.requireNonNull(ModEntities.SINGULARITY.get().create(level));
        hole.configure(mode, power, owner, attackVariant);
        if (duration > 0) {
            hole.setDuration(duration);
        }
        hole.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(hole);
        return hole;
    }
}
