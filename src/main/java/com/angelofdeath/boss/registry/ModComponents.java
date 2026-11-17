package com.angelofdeath.boss.registry;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Per-stack data of the White-Hole Spear. Both default to false when absent. */
public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AngelOfDeathBoss.MOD_ID);

    /** X-key toggle: may the spear's blasts destroy blocks? */
    public static final Supplier<DataComponentType<Boolean>> DESTROY_BLOCKS = COMPONENTS.register("destroy_blocks",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    /** Stored charge: set after holding right-click for 25 ticks, consumed by the next melee hit or throw. */
    public static final Supplier<DataComponentType<Boolean>> IS_CHARGED = COMPONENTS.register("is_charged",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    private ModComponents() {
    }
}
