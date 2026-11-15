package com.angelofdeath.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The wing model from Wings.bbmodel (see WingCubes), shared by the boss and the wearable wings.
 * Origin is the top-centre of the torso; both wings hang from a shoulder-blade pivot on the back.
 *
 * <p>Pose conventions (radians): {@code up} raises the tip, {@code forward} swings the tip toward the chest
 * (negative = swept back), {@code tilt} pitches the wing plane.</p>
 */
public class AngelWingsModel {
    public static final float PIVOT_X = 2.0F;
    public static final float PIVOT_Y = 4.0F;
    public static final float PIVOT_Z = 4.0F;

    private final ModelPart root;
    private final ModelPart left;
    private final ModelPart right;

    public AngelWingsModel(ModelPart root) {
        this.root = root;
        this.left = root.getChild("left_wing");
        this.right = root.getChild("right_wing");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootDef = mesh.getRoot();
        PartDefinition leftDef = rootDef.addOrReplaceChild("left_wing", CubeListBuilder.create(),
                PartPose.offset(PIVOT_X, PIVOT_Y, PIVOT_Z));
        PartDefinition rightDef = rootDef.addOrReplaceChild("right_wing", CubeListBuilder.create(),
                PartPose.offset(-PIVOT_X, PIVOT_Y, PIVOT_Z));
        addCubes(leftDef, WingCubes.LEFT, "l");
        addCubes(rightDef, WingCubes.RIGHT, "r");
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addCubes(PartDefinition parent, float[][] cubes, String prefix) {
        for (int i = 0; i < cubes.length; i++) {
            float[] c = cubes[i];
            parent.addOrReplaceChild(prefix + i,
                    CubeListBuilder.create().texOffs((int) c[0], (int) c[1]).addBox(c[2], c[3], c[4], c[5], c[6], c[7]),
                    PartPose.rotation(c[8], c[9], c[10]));
        }
    }

    /** Symmetric pose. */
    public void pose(float up, float forward, float tilt, float scale) {
        pose(up, forward, up, forward, tilt, scale);
    }

    public void pose(float upLeft, float forwardLeft, float upRight, float forwardRight, float tilt, float scale) {
        this.left.zRot = -upLeft;
        this.left.yRot = forwardLeft;
        this.right.zRot = upRight;
        this.right.yRot = -forwardRight;
        this.left.xRot = tilt;
        this.right.xRot = tilt;
        setScale(scale);
    }

    public void setScale(float scale) {
        this.left.xScale = scale;
        this.left.yScale = scale;
        this.left.zScale = scale;
        this.right.xScale = scale;
        this.right.yScale = scale;
        this.right.zScale = scale;
    }

    public void render(PoseStack ps, VertexConsumer consumer, int light, int overlay, int color) {
        this.root.render(ps, consumer, light, overlay, color);
    }

    public void renderLeft(PoseStack ps, VertexConsumer consumer, int light, int overlay, int color) {
        this.left.render(ps, consumer, light, overlay, color);
    }

    public void renderRight(PoseStack ps, VertexConsumer consumer, int light, int overlay, int color) {
        this.right.render(ps, consumer, light, overlay, color);
    }
}
