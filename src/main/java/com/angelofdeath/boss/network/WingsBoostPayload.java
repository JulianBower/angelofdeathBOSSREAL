package com.angelofdeath.boss.network;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.event.WingsEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: the B key was pressed. */
public record WingsBoostPayload() implements CustomPacketPayload {
    public static final Type<WingsBoostPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "wings_boost"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WingsBoostPayload> CODEC = StreamCodec.unit(new WingsBoostPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WingsBoostPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                WingsEvents.tryBoost(player);
            }
        });
    }
}
