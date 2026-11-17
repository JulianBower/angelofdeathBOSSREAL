package com.angelofdeath.boss.logic;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The White-Hole Spear's instant bursts. None of them leaves a persistent White Hole entity behind. */
public final class SpearBlasts {
    public static final float BURST_DAMAGE = 45.0F;
    public static final float GROUND_DAMAGE = 6.0F;
    public static final double SHOCKWAVE_RANGE = 60.0D;

    private SpearBlasts() {
    }

    /** Charged melee hit: instant burst centered on the struck mob. */
    public static void meleeBurst(ServerLevel level, LivingEntity attacker, LivingEntity target, boolean destroyBlocks) {
        burst(level, attacker, attacker, target, target.getBoundingBox().getCenter(), 4.0D, 1.6D, destroyBlocks, 3);
    }

    /** Charged throw that carried a mob into a block: the mob takes 45.0F, everything close takes half. */
    public static void mobDetonation(ServerLevel level, Entity source, @Nullable Entity owner, LivingEntity mob,
                                     Vec3 pos, boolean destroyBlocks) {
        burst(level, source, owner, mob, pos, 4.0D, 1.2D, destroyBlocks, 3);
    }

    /** Charged throw that hit the ground with nobody impaled: 6.0F close up + a 60-block repulsion shockwave. */
    public static void groundShockwave(ServerLevel level, Entity source, @Nullable Entity owner, Vec3 pos,
                                       boolean destroyBlocks) {
        AABB area = new AABB(pos, pos).inflate(SHOCKWAVE_RANGE);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, x -> x != owner && x.isAlive())) {
            double d = e.getBoundingBox().getCenter().distanceTo(pos);
            if (d > SHOCKWAVE_RANGE) {
                continue;
            }
            if (d <= 6.0D && SingularityActions.canHarm(e)) {
                e.invulnerableTime = 0;
                e.hurt(level.damageSources().explosion(source, owner), GROUND_DAMAGE);
            }
            double strength = 0.4D + 3.2D * (1.0D - d / SHOCKWAVE_RANGE);
            SingularityActions.push(e, pos, strength, 0.35D, 3.5D);
        }
        effects(level, pos, 1.6F);
        if (destroyBlocks) {
            destroyBlocks(level, pos, 5, source);
        }
    }

    private static void burst(ServerLevel level, Entity source, @Nullable Entity owner, LivingEntity main, Vec3 pos,
                              double radius, double knock, boolean destroyBlocks, int blockRadius) {
        AABB area = new AABB(pos, pos).inflate(radius);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area,
                x -> x != owner && SingularityActions.canHarm(x))) {
            e.invulnerableTime = 0;
            e.hurt(SingularityActions.voidSource(level, source, owner), e == main ? BURST_DAMAGE : BURST_DAMAGE * 0.5F);
            SingularityActions.launch(e, e.getBoundingBox().getCenter().subtract(pos), knock, 0.45D);
        }
        effects(level, pos, 1.0F);
        if (destroyBlocks) {
            destroyBlocks(level, pos, blockRadius, source);
        }
    }

    private static void effects(ServerLevel level, Vec3 pos, float scale) {
        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, (int) (90 * scale), 1.5D * scale, 1.5D * scale,
                1.5D * scale, 0.5D);
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 4, 1.2D * scale, 1.2D * scale, 1.2D * scale,
                0.0D);
        SingularityActions.playSound(level, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, 4.0F, 1.4F);
        SingularityActions.playSound(level, pos, SoundEvents.WARDEN_SONIC_BOOM, 2.5F, 1.2F);
    }

    /** Only ever called when the spear's destroyBlocks component is true. Never touches unbreakable blocks. */
    public static void destroyBlocks(ServerLevel level, Vec3 center, int radius, Entity source) {
        BlockPos c = BlockPos.containing(center);
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-radius, -radius, -radius), c.offset(radius, radius, radius))) {
            if (p.distSqr(c) > (double) radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(p);
            if (state.isAir() || state.getDestroySpeed(level, p) < 0.0F || state.is(BlockTags.WITHER_IMMUNE)) {
                continue;
            }
            level.destroyBlock(p.immutable(), true, source);
        }
    }
}
