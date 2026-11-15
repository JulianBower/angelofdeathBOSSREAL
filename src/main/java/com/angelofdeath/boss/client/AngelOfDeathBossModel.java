package com.angelofdeath.boss.client;

import com.angelofdeath.boss.entity.AngelOfDeathBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Angel of Death body: hooded black cloak, full-coverage charcoal mask, belted robe and long empty-handed
 * sleeves (no staff). Wings are NOT part of this model; they are drawn by the wings layer from AngelWingsModel.
 *
 * <p>Pose targets use NaN for "unchanged"; indexes follow ATTACK_POSE (0-8).</p>
 */
public class AngelOfDeathBossModel extends EntityModel<AngelOfDeathBossEntity> {
    static final float N = Float.NaN;

    //                                       0    1      2      3      4      5      6      7      8
    private static final float[] L_ARM_X = {N, -1.45F, -0.30F, -1.50F, -0.60F, -1.20F, 0.40F, -0.90F, -0.50F};
    private static final float[] L_ARM_Z = {N, -0.20F, -0.25F, -0.05F, -0.80F, 0.10F, -0.10F, 0.35F, -0.50F};
    private static final float[] L_FORE_X = {N, -0.10F, -0.90F, -0.05F, -0.40F, -0.60F, -0.20F, -1.40F, -0.50F};
    private static final float[] R_ARM_X = {N, 0.75F, -1.55F, -2.90F, -3.00F, -1.20F, 0.40F, -0.90F, -0.50F};
    private static final float[] R_ARM_Z = {N, 0.15F, 0.05F, 0.35F, 0.25F, -0.10F, 0.10F, -0.35F, 0.50F};
    private static final float[] R_FORE_X = {N, -1.90F, -0.05F, -0.25F, -0.10F, -0.60F, -0.20F, -1.40F, -0.50F};
    private static final float[] BODY_X = {N, 0.0F, 0.25F, 0.0F, 0.85F, 0.0F, 0.55F, 0.0F, -0.10F};
    private static final float[] BODY_Y = {N, 0.50F, -0.50F, 0.20F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F};

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart leftForearm;
    private final ModelPart leftHand;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart rightHand;
    private final ModelPart[] drawable;

    public AngelOfDeathBossModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        ModelPart robe = this.body.getChild("robe");
        this.head = this.body.getChild("head");
        ModelPart hood = this.head.getChild("hood");
        this.leftArm = this.body.getChild("left_arm");
        this.leftForearm = this.leftArm.getChild("left_forearm");
        this.leftHand = this.leftForearm.getChild("left_hand");
        this.rightArm = this.body.getChild("right_arm");
        this.rightForearm = this.rightArm.getChild("right_forearm");
        this.rightHand = this.rightForearm.getChild("right_hand");
        this.drawable = new ModelPart[] {this.body, robe, this.head, hood, this.leftArm, this.leftForearm,
                this.leftHand, this.rightArm, this.rightForearm, this.rightHand};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)          // torso
                .texOffs(24, 16).addBox(-4.5F, 9.0F, -2.5F, 9.0F, 2.0F, 5.0F)          // belt
                .texOffs(68, 32).addBox(-6.0F, -1.0F, -3.0F, 12.0F, 3.0F, 6.0F),       // mantle
                PartPose.ZERO);
        body.addOrReplaceChild("robe", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 12.0F, 6.0F)
                        .texOffs(32, 32).addBox(-5.5F, 10.0F, -3.5F, 11.0F, 2.0F, 7.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        head.addOrReplaceChild("hood", CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 2.0F, 10.0F)
                        .texOffs(72, 0).addBox(-5.0F, -8.0F, 4.0F, 10.0F, 9.0F, 2.0F)
                        .texOffs(96, 0).addBox(4.0F, -8.0F, -5.0F, 1.0F, 9.0F, 10.0F)
                        .texOffs(96, 0).mirror().addBox(-5.0F, -8.0F, -5.0F, 1.0F, 9.0F, 10.0F),
                PartPose.ZERO);

        PartDefinition leftArm = body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(52, 16).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 8.0F, 5.0F),
                PartPose.offset(7.0F, 2.0F, 0.0F));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm",
                CubeListBuilder.create().texOffs(72, 16).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 8.0F, 5.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        leftForearm.addOrReplaceChild("left_hand",
                CubeListBuilder.create().texOffs(92, 20).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        PartDefinition rightArm = body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(52, 16).mirror().addBox(-2.5F, -2.0F, -2.5F, 5.0F, 8.0F, 5.0F),
                PartPose.offset(-7.0F, 2.0F, 0.0F));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm",
                CubeListBuilder.create().texOffs(72, 16).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 8.0F, 5.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        rightForearm.addOrReplaceChild("right_hand",
                CubeListBuilder.create().texOffs(92, 20).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    static float blend(float idle, float[] weights, float[] targets) {
        float value = idle;
        for (int i = 1; i < targets.length; i++) {
            if (!Float.isNaN(targets[i])) {
                value += weights[i] * (targets[i] - idle);
            }
        }
        return value;
    }

    /** Interpolated per-pose weights for this frame. */
    public static float[] weights(AngelOfDeathBossEntity e, float partialTick) {
        float[] w = new float[9];
        for (int i = 0; i < w.length; i++) {
            w[i] = Mth.lerp(partialTick, e.poseWeightO[i], e.poseWeight[i]);
        }
        return w;
    }

    @Override
    public void setupAnim(AngelOfDeathBossEntity e, float limbSwing, float limbSwingAmount, float age,
                          float netHeadYaw, float headPitch) {
        float partial = age - e.tickCount;
        float[] w = weights(e, partial);
        float trans = Mth.lerp(partial, e.transitionWeightO, e.transitionWeight);

        float bob = Mth.sin(age * 0.08F);
        this.root.y = bob * 1.4F;
        this.root.x = Mth.sin(age * 2.7F) * 0.9F * trans; // fusion tremor

        float lx = blend(-0.10F + bob * 0.05F, w, L_ARM_X);
        float lz = blend(-0.12F, w, L_ARM_Z);
        float lf = blend(-0.30F, w, L_FORE_X);
        float rx = blend(-0.10F + bob * 0.05F, w, R_ARM_X);
        float rz = blend(0.12F, w, R_ARM_Z);
        float rf = blend(-0.30F, w, R_FORE_X);
        float bx = blend(0.0F, w, BODY_X);
        float by = blend(0.0F, w, BODY_Y);

        // Fusion: arms flung wide.
        lx = Mth.lerp(trans, lx, -0.35F);
        lz = Mth.lerp(trans, lz, -1.30F);
        lf = Mth.lerp(trans, lf, -0.20F);
        rx = Mth.lerp(trans, rx, -0.35F);
        rz = Mth.lerp(trans, rz, 1.30F);
        rf = Mth.lerp(trans, rf, -0.20F);

        this.leftArm.xRot = lx;
        this.leftArm.zRot = lz;
        this.leftForearm.xRot = lf;
        this.rightArm.xRot = rx;
        this.rightArm.zRot = rz;
        this.rightForearm.xRot = rf;
        this.body.xRot = bx;
        this.body.yRot = by;
        this.head.yRot = Mth.clamp(netHeadYaw, -50.0F, 50.0F) * Mth.DEG_TO_RAD * 0.7F - by * 0.5F;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD * 0.7F - bx * 0.6F;
        this.leftHand.xRot = 0.0F;
        this.rightHand.xRot = 0.0F;
    }

    /** Runs root -> body -> left arm -> forearm -> hand so the pose stack ends at the left wrist. */
    public void translateToLeftHand(PoseStack ps) {
        this.root.translateAndRotate(ps);
        this.body.translateAndRotate(ps);
        this.leftArm.translateAndRotate(ps);
        this.leftForearm.translateAndRotate(ps);
        this.leftHand.translateAndRotate(ps);
    }

    public void translateToBody(PoseStack ps) {
        this.root.translateAndRotate(ps);
        this.body.translateAndRotate(ps);
    }

    /** Emissive silhouette of just the right forearm + hand (glowing-hand slam). */
    public void renderRightHandGlow(PoseStack ps, VertexConsumer consumer, int overlay, int color, float inflate) {
        for (ModelPart part : this.drawable) {
            part.skipDraw = true;
        }
        this.rightForearm.skipDraw = false;
        this.rightHand.skipDraw = false;
        this.rightForearm.xScale = inflate;
        this.rightForearm.zScale = inflate;
        this.root.render(ps, consumer, 15728880, overlay, color);
        this.rightForearm.xScale = 1.0F;
        this.rightForearm.zScale = 1.0F;
        for (ModelPart part : this.drawable) {
            part.skipDraw = false;
        }
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer consumer, int light, int overlay, int color) {
        this.root.render(ps, consumer, light, overlay, color);
    }
}
