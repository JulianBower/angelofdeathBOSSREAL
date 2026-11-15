package com.angelofdeath.boss.network;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: current BOOST_GLOW_ALPHA (0.0F-1.0F) of a player, sent to that player and everyone tracking them. */
public record BoostGlowPayload(int entityId, float alpha) implements CustomPacketPayload {
    public static final Type<BoostGlowPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "boost_glow"));
    public static final StreamCodec<ByteBuf, BoostGlowPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BoostGlowPayload::entityId,
            ByteBufCodecs.FLOAT, BoostGlowPayload::alpha,
            BoostGlowPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BoostGlowPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = context.player().level().getEntity(payload.entityId());
            if (entity != null) {
                entity.setData(ModAttachments.BOOST_GLOW_ALPHA, payload.alpha());
            }
        });
    }
}
