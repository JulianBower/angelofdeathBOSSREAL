package com.angelofdeath.boss.network;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.item.WhiteHoleSpearItem;
import com.angelofdeath.boss.registry.ModComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: the X key was pressed; flips destroyBlocks on the held White-Hole Spear. */
public record ToggleSpearGriefingPayload() implements CustomPacketPayload {
    public static final Type<ToggleSpearGriefingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "toggle_spear_griefing"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleSpearGriefingPayload> CODEC =
            StreamCodec.unit(new ToggleSpearGriefingPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleSpearGriefingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof WhiteHoleSpearItem) {
                    boolean now = !WhiteHoleSpearItem.destroysBlocks(stack);
                    stack.set(ModComponents.DESTROY_BLOCKS.get(), now);
                    player.displayClientMessage(Component.translatable(now
                            ? "message.angelofdeathboss.spear_griefing_on"
                            : "message.angelofdeathboss.spear_griefing_off"), true);
                    return;
                }
            }
        });
    }
}
