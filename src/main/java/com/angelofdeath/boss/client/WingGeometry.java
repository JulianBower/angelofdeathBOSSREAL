package com.angelofdeath.boss.client;

import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Wing cube data taken 1:1 from the attached Wings.java (Blockbench export). The two group pivots are moved from the
 * export's mirrored offsets to the real spine point so the whole wing can be flapped / folded with one rotation.
 */
public final class WingGeometry {
    public static final float PIVOT_X = 0.1F;
    public static final float PIVOT_Y = 13.7F;
    public static final float PIVOT_Z = 1.3F;

    /** Neg-x wing feather cubes from the Blockbench export, pivot already moved to the spine (0.1, 13.7, 1.3). */
    public static void addNegWing(PartDefinition g) {
        g.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(22, 23).addBox(-20.6475F, -1.9F, 7.939F, 3.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.4843F, 0.0F));
        g.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 13).addBox(-18.6346F, -4.9F, 5.2331F, 4.0F, 11.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.336F, 0.0F));
        g.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-15.128F, -6.9F, 3.6928F, 5.0F, 13.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2313F, 0.0F));
        g.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(16, 13).addBox(-10.7222F, -6.9F, 1.1122F, 3.0F, 13.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0131F, 0.0F));
        g.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(12, 24).addBox(-7.6564F, -2.9F, -1.6569F, 2.0F, 9.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(6, 26).addBox(-5.6909F, -0.9F, -1.6738F, 1.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(10, 26).addBox(-2.908F, 0.1F, -1.6331F, 1.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(22, 31).addBox(-3.7624F, 0.1F, -1.6285F, 1.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(20, 30).addBox(-4.702F, 0.1F, -1.682F, 1.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(30, 31).addBox(-1.9512F, 1.1F, -1.6265F, 1.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(32, 6).addBox(-1.0968F, 2.2F, -1.631F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(26, 31).addBox(-1.0562F, 1.2F, -2.1631F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.3709F, 0.0F));
        g.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(22, 17).addBox(-12.4895F, -18.1619F, 4.0841F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2414F, -0.2178F, -1.1449F));
        g.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(22, 13).addBox(-13.3823F, -13.9551F, 4.1109F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1315F, -0.2967F, -0.7192F));
        g.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(22, 21).addBox(-12.2717F, -10.8619F, 1.2394F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0078F, -0.1105F, -0.3639F));
        g.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(26, 11).addBox(-13.1707F, -0.9052F, 0.2946F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0111F, 0.0188F, 0.5353F));
        g.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(28, 25).addBox(-7.4699F, 5.4861F, 0.2946F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0208F, 0.0065F, 1.268F));
        g.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(28, 23).addBox(-6.553F, 2.7846F, -2.1047F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2792F, 0.2474F, 0.864F));
        g.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(30, 2).addBox(-4.7839F, 0.9108F, -2.1296F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1477F, 0.3414F, 0.4182F));
        g.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(16, 28).addBox(-1.4672F, 1.3753F, -2.1206F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2325F, 0.2917F, 0.6888F));
    }


    /** Pos-x wing feather cubes from the Blockbench export, pivot already moved to the spine (0.1, 13.7, 1.3). */
    public static void addPosWing(PartDefinition g) {
        g.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(0, 26).addBox(17.2968F, -1.9F, 7.7466F, 3.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.1F, 0.0F, 0.0F, 0.5192F, 0.0F));
        g.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(8, 13).addBox(14.2593F, -4.9F, 5.0946F, 4.0F, 11.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.1F, 0.0F, 0.0F, 0.3709F, 0.0F));
        g.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(10, 0).addBox(9.7402F, -6.9F, 3.5943F, 5.0F, 13.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.1F, 0.0F, 0.0F, 0.2662F, 0.0F));
        g.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(20, 0).addBox(7.3222F, -6.9F, 1.1105F, 3.0F, 13.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.1F, 0.0F, 0.0F, 0.0218F, 0.0F));
        g.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(26, 0).addBox(5.2812F, -2.9F, -1.5185F, 2.0F, 9.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 26).addBox(4.3157F, -0.9F, -1.5353F, 1.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(16, 30).addBox(1.5327F, 0.1F, -1.4946F, 1.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(30, 4).addBox(2.3871F, 0.1F, -1.49F, 1.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(18, 30).addBox(3.3267F, 0.1F, -1.5435F, 1.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(24, 31).addBox(0.5759F, 1.1F, -1.488F, 1.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(10, 32).addBox(-0.2785F, 2.1F, -1.4926F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(32, 4).addBox(-0.3191F, 1.1F, -2.0246F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.1F, 0.0F, 0.0F, -0.336F, 0.0F));
        g.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(22, 19).addBox(6.3297F, -17.8184F, 3.9557F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2741F, 0.2322F, 1.1522F));
        g.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(22, 15).addBox(7.0967F, -13.7064F, 3.9825F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1557F, 0.3229F, 0.7266F));
        g.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(0, 24).addBox(6.901F, -10.7192F, 1.1924F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0047F, 0.1431F, 0.3655F));
        g.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(26, 9).addBox(8.8266F, -1.1092F, 0.2963F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0067F, 0.0113F, -0.5352F));
        g.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(30, 0).addBox(4.3506F, 5.1043F, 0.2963F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0125F, 0.0039F, -1.268F));
        g.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(28, 29).addBox(3.2995F, 2.5079F, -1.9662F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.252F, -0.2246F, -0.8576F));
        g.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(28, 27).addBox(1.4372F, 0.7672F, -1.9911F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1329F, -0.3095F, -0.4135F));
        g.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(16, 26).addBox(-0.8305F, 1.1468F, -1.9821F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2095F, -0.2646F, -0.6825F));
    }


    private WingGeometry() {
    }
}
