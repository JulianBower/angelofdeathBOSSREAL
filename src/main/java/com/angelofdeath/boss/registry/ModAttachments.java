package com.angelofdeath.boss.registry;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AngelOfDeathBoss.MOD_ID);

    /**
     * Smooth 0.0F-1.0F wing glow after a boost. Decays on the server; clients receive it through
     * BoostGlowPayload (the attachment itself is intentionally not auto-synced).
     */
    public static final Supplier<AttachmentType<Float>> BOOST_GLOW_ALPHA = ATTACHMENTS.register("boost_glow_alpha",
            () -> AttachmentType.builder(() -> 0.0F)
                    .serialize(Codec.FLOAT)
                    .build());

    private ModAttachments() {
    }
}
