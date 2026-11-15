package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws the boss' held orbs (palm holes, fusion spheres, back-mounted Grey Hole) after particles, exactly like the
 * original HeldSingularityRenderer drew the channeled singularity: translate to the focal point relative to the
 * camera, then call SingularityVisuals with the camera orientation. The boss renderer queues orbs every frame.
 */
@EventBusSubscriber(modid = AngelOfDeathBoss.MOD_ID, value = Dist.CLIENT)
public final class HeldSingularityRenderer {
    public static final int KIND_ORB = 0;
    public static final int KIND_FUSION = 1;

    private record Orb(int kind, Vec3 pos, int mode, float radius, float heldTicks, Vec3 look, float time) {
    }

    private static final List<Orb> QUEUE = new ArrayList<>();

    private HeldSingularityRenderer() {
    }

    public static void queueOrb(Vec3 worldPos, int mode, float radius, float time) {
        QUEUE.add(new Orb(KIND_ORB, worldPos, mode, radius, 0.0F, Vec3.ZERO, time));
    }

    public static void queueFusion(Vec3 worldPos, float radius, float heldTicks, Vec3 look, float time) {
        QUEUE.add(new Orb(KIND_FUSION, worldPos, SingularityVisuals.MODE_GREY_HOLE, radius, heldTicks, look, time));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (QUEUE.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();

        for (Orb orb : QUEUE) {
            poseStack.pushPose();
            poseStack.translate(orb.pos().x - cameraPos.x, orb.pos().y - cameraPos.y, orb.pos().z - cameraPos.z);
            if (orb.kind() == KIND_FUSION) {
                SingularityVisuals.renderFusion(poseStack, buffer, camera.rotation(), orb.radius(), orb.heldTicks(),
                        orb.look(), orb.time());
            } else {
                SingularityVisuals.renderOrb(poseStack, buffer, camera.rotation(), orb.mode(), orb.radius(), orb.time());
            }
            poseStack.popPose();
        }
        QUEUE.clear();
        buffer.endBatch();
    }
}
