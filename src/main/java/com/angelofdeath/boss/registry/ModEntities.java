package com.angelofdeath.boss.registry;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.entity.AngelOfDeathBossEntity;
import com.angelofdeath.boss.entity.SingularityEntity;
import com.angelofdeath.boss.entity.VoidCrowEntity;
import com.angelofdeath.boss.entity.VoidFeatherEntity;
import com.angelofdeath.boss.entity.WhiteHoleSpearEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AngelOfDeathBoss.MOD_ID);

    // Allay is 0.35 x 0.6; crows are 3x that (1.05 x 1.8).
    private static final float CROW_W = 1.05F;
    private static final float CROW_H = 1.8F;

    public static final DeferredHolder<EntityType<?>, EntityType<SingularityEntity>> SINGULARITY = register(
            "singularity", () -> EntityType.Builder.<SingularityEntity>of(SingularityEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(16).updateInterval(1).fireImmune()
                    .build("singularity"));

    public static final DeferredHolder<EntityType<?>, EntityType<VoidCrowEntity>> BLACK_CROW = register(
            "black_crow", () -> EntityType.Builder.<VoidCrowEntity>of((t, l) -> new VoidCrowEntity(t, l), MobCategory.MONSTER)
                    .sized(CROW_W, CROW_H).clientTrackingRange(8).build("black_crow"));

    public static final DeferredHolder<EntityType<?>, EntityType<VoidCrowEntity>> WHITE_CROW = register(
            "white_crow", () -> EntityType.Builder.<VoidCrowEntity>of((t, l) -> {
                VoidCrowEntity crow = new VoidCrowEntity(t, l);
                crow.setWhite(true);
                return crow;
            }, MobCategory.MONSTER).sized(CROW_W, CROW_H).clientTrackingRange(8).build("white_crow"));

    public static final DeferredHolder<EntityType<?>, EntityType<VoidFeatherEntity>> VOID_FEATHER = register(
            "void_feather", () -> EntityType.Builder.<VoidFeatherEntity>of(VoidFeatherEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(2).fireImmune()
                    .build("void_feather"));

    public static final DeferredHolder<EntityType<?>, EntityType<AngelOfDeathBossEntity>> ANGEL_OF_DEATH = register(
            "angel_of_death", () -> EntityType.Builder.<AngelOfDeathBossEntity>of(AngelOfDeathBossEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.6F).clientTrackingRange(16).fireImmune().build("angel_of_death"));

    public static final DeferredHolder<EntityType<?>, EntityType<WhiteHoleSpearEntity>> WHITE_HOLE_SPEAR = register(
            "white_hole_spear", () -> EntityType.Builder.<WhiteHoleSpearEntity>of(WhiteHoleSpearEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1).fireImmune().noSummon()
                    .build("white_hole_spear"));

    private ModEntities() {
    }

    private static <T extends net.minecraft.world.entity.Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(
            String name, Supplier<EntityType<T>> supplier) {
        return ENTITIES.register(name, supplier);
    }
}
