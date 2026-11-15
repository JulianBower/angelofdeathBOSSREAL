package com.angelofdeath.boss.client;

import com.angelofdeath.boss.entity.VoidCrowEntity;
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

/** Small flapping crow (texture 64x32); the renderer scales it to 3x Allay size. */
public class VoidCrowModel extends EntityModel<VoidCrowEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public VoidCrowModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.head = body.getChild("head");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, -3.0F, -4.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -1.0F, -4.0F));
        head.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(0, 26).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -3.0F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 24).addBox(-1.5F, -0.5F, 0.0F, 3.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 4.0F));
        body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(32, 0).addBox(0.0F, -0.5F, -2.0F, 7.0F, 1.0F, 4.0F),
                PartPose.offset(2.0F, -1.5F, 0.0F));
        body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(32, 12).addBox(-7.0F, -0.5F, -2.0F, 7.0F, 1.0F, 4.0F),
                PartPose.offset(-2.0F, -1.5F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(VoidCrowEntity entity, float limbSwing, float limbSwingAmount, float age, float netHeadYaw,
                          float headPitch) {
        float flap = Mth.sin(age * 0.9F) * 0.7F;
        this.leftWing.zRot = -flap;
        this.rightWing.zRot = flap;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                               int color) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, color);
    }
}
