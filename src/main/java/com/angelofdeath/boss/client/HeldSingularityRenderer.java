package com.angelofdeath.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Held-singularity drawing from the original mod, re-targeted at the boss: instead of a channeling player's focal
 * point, the orb is drawn at an arbitrary model-space point (his palm, or the point between his wings).
 *
 * <p>Call {@link #renderAtModelPoint} while the pose stack is positioned at the owning model part. The point is
 * converted to a world-oriented translation so SingularityVisuals' camera-facing rings stay camera-facing.</p>
 */
public final class HeldSingularityRenderer {
    private HeldSingularityRenderer() {
    }

    /**
     * @param ps            pose stack currently positioned at the model part (carries yaw / model scaling)
     * @param px,py,pz      point in that part's local space, in blocks (pixels / 16)
     * @param worldRadius   desired orb radius in world blocks (compensates for the model scale in {@code ps})
     */
    public static void renderAtModelPoint(PoseStack ps, MultiBufferSource buffer, Quaternionf camera, int mode,
                                          float worldRadius, float time, float px, float py, float pz) {
        Vector3f world = ps.last().pose().transformPosition(px, py, pz, new Vector3f());
        PoseStack orb = new PoseStack();
        orb.translate(world.x(), world.y(), world.z());
        SingularityVisuals.renderOrb(orb, buffer, camera, mode, worldRadius, time);
    }

    /** Growth envelope used while an orb forms in the palm (0 -> 1 over {@code ticks}). */
    public static float formScale(float heldTicks, float ticks) {
        return Mth.clamp(heldTicks / ticks, 0.0F, 1.0F);
    }
}
