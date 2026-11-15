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

/** Crow: Allay-scale body (rendered at 1.8x), beak, tail and two rapidly flapping wings. 64x32 texture. */
public class VoidCrowModel extends EntityModel<VoidCrowEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;

    public VoidCrowModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.tail = this.body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -5.0F, 5.0F, 5.0F, 10.0F),
                PartPose.offset(0.0F, 18.5F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 15).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -1.5F, -4.5F));
        head.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(16, 15).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 0.5F, -4.0F));
        body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(30, 0).addBox(0.0F, -0.5F, -3.0F, 10.0F, 1.0F, 6.0F),
                PartPose.offset(2.0F, -2.0F, -0.5F));
        body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(30, 0).mirror().addBox(-10.0F, -0.5F, -3.0F, 10.0F, 1.0F, 6.0F),
                PartPose.offset(-2.0F, -2.0F, -0.5F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 25).addBox(-1.5F, -0.5F, 0.0F, 3.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 0.5F, 5.0F));
        body.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(30, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(1.2F, 2.5F, 1.0F));
        body.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(30, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(-1.2F, 2.5F, 1.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(VoidCrowEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float phase = ageInTicks * 1.7F + entity.getId() * 0.7F;
        float flap = Mth.sin(phase);
        this.leftWing.zRot = -flap * 0.95F;
        this.rightWing.zRot = flap * 0.95F;
        this.leftWing.yRot = -0.15F + Mth.cos(phase) * 0.12F;
        this.rightWing.yRot = 0.15F - Mth.cos(phase) * 0.12F;
        this.body.y = 18.5F - flap * 0.9F;
        double vy = entity.getDeltaMovement().y;
        this.body.xRot = Mth.clamp((float) -vy * 1.1F, -0.6F, 0.9F);
        this.tail.xRot = -0.25F + flap * 0.15F;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD * 0.6F - this.body.xRot * 0.5F;
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer consumer, int light, int overlay, int color) {
        this.root.render(ps, consumer, light, overlay, color);
    }
}
