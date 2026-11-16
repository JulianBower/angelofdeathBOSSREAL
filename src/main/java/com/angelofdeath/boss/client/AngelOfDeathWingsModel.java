package com.angelofdeath.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;

/** The attached Wings.java model (geometry in {@link WingGeometry}) with a flap / sweep pose for flight and boosting. */
public class AngelOfDeathWingsModel extends EntityModel<LivingEntity> {
    private final ModelPart negWing;
    private final ModelPart posWing;

    public AngelOfDeathWingsModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.negWing = root.getChild("neg_wing");
        this.posWing = root.getChild("pos_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition neg = root.addOrReplaceChild("neg_wing", CubeListBuilder.create(),
                PartPose.offset(WingGeometry.PIVOT_X, WingGeometry.PIVOT_Y, WingGeometry.PIVOT_Z));
        WingGeometry.addNegWing(neg);
        PartDefinition pos = root.addOrReplaceChild("pos_wing", CubeListBuilder.create(),
                PartPose.offset(WingGeometry.PIVOT_X, WingGeometry.PIVOT_Y, WingGeometry.PIVOT_Z));
        WingGeometry.addPosWing(pos);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * @param time  animation clock in ticks
     * @param flying true while airborne
     * @param boost  0..1, how hard the wings are beating (boost glow)
     */
    public void pose(float time, boolean flying, float boost) {
        float up;
        float sweep;
        if (flying) {
            float freq = 0.42F + 0.5F * boost;
            float amp = 0.5F + 0.35F * boost;
            up = 0.3F + 0.2F * boost + (float) Math.sin(time * freq) * amp;
            sweep = 0.05F + 0.3F * boost;
        } else {
            up = 0.12F + (float) Math.sin(time * 0.06F) * 0.03F;
            sweep = 0.0F;
        }
        // -x wing: +zRot raises the tip, -yRot sweeps it forward. +x wing mirrors both.
        setWingAngles(up, sweep);
    }

    /** Pull both wing roots toward the spine so the inner tips start on the back (player layer). */
    public void setRootX(float leftX, float rightX) {
        this.negWing.x = leftX;
        this.posWing.x = rightX;
    }

    /** up = tip raise (rad), sweep = forward fold (rad). -x wing: +zRot raises, -yRot folds forward; +x mirrors. */
    public void setWingAngles(float up, float sweep) {
        this.negWing.zRot = up;
        this.negWing.yRot = -sweep;
        this.posWing.zRot = -up;
        this.posWing.yRot = sweep;
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                               int color) {
        this.negWing.render(poseStack, consumer, packedLight, packedOverlay, color);
        this.posWing.render(poseStack, consumer, packedLight, packedOverlay, color);
    }
}
