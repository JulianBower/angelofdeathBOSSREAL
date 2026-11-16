package com.angelofdeath.boss.client;

import com.angelofdeath.boss.entity.AngelOfDeathBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Angel of Death: hooded #0F0F0F cloak, #2A2A2A mask with dark eye slits, bare arms (no staff) and multi-tiered wings
 * with the player wing model rendered on his back by BossWingsLayer. Texture 128x128.
 */
public class AngelOfDeathBossModel extends EntityModel<AngelOfDeathBossEntity> {
    /** Wing pose for the separately rendered AngelOfDeathWingsModel (see BossWingsLayer). */
    public float wingUp;
    public float wingSweep;
    private final ModelPart root;
    private final ModelPart head;
    public final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightHand;
    private final ModelPart handGlow;

    public AngelOfDeathBossModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.rightHand = this.rightArm.getChild("right_hand");
        this.handGlow = this.rightArm.getChild("hand_glow");
        this.handGlow.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 22.0F, 6.0F), PartPose.ZERO);
        body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                        new CubeDeformation(0.6F)), PartPose.ZERO);
        PartDefinition rightArm = body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 44).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-7.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild("right_hand",
                CubeListBuilder.create().texOffs(0, 64).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 10.0F, 0.0F));
        rightArm.addOrReplaceChild("hand_glow",
                CubeListBuilder.create().texOffs(0, 64).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F,
                        new CubeDeformation(0.35F)), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(16, 44).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(7.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild("left_hand",
                CubeListBuilder.create().texOffs(0, 64).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 10.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static float mix(float cur, float target, float w) {
        return cur + (target - cur) * w;
    }

    @Override
    public void setupAnim(AngelOfDeathBossEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw,
                          float headPitch) {
        float pt = age - e.tickCount;
        float t = age;
        float[] w = new float[9];
        for (int i = 0; i < 9; i++) {
            w[i] = Mth.lerp(pt, e.poseWeightO[i], e.poseWeight[i]);
        }
        float tw = Mth.lerp(pt, e.transitionWeightO, e.transitionWeight);

        // idle: arms hang with a slow sway, wings breathe
        float rax = 0.05F + Mth.sin(t * 0.07F) * 0.05F;
        float lax = 0.05F - Mth.sin(t * 0.07F) * 0.05F;
        float raz = 0.12F;
        float laz = -0.12F;
        float up = 0.25F + Mth.sin(t * 0.12F) * 0.12F;
        float sweep = 0.1F;
        float bodyX = 0.0F;
        float headX = 0.0F;

        // 1 black hole grapple: left hand reaches out, right fist cocked back
        lax = mix(lax, -1.45F, w[1]); laz = mix(laz, -0.05F, w[1]);
        rax = mix(rax, 0.55F, w[1]); up = mix(up, 0.7F, w[1]); sweep = mix(sweep, 0.0F, w[1]);
        // 2 punch: right fist straight forward
        rax = mix(rax, -1.6F, w[2]); raz = mix(raz, 0.05F, w[2]);
        lax = mix(lax, 0.5F, w[2]); bodyX = mix(bodyX, 0.2F, w[2]);
        // 3 arms out
        lax = mix(lax, -0.3F, w[3]); laz = mix(laz, -1.3F, w[3]);
        rax = mix(rax, -0.3F, w[3]); raz = mix(raz, 1.3F, w[3]); up = mix(up, 0.8F, w[3]);
        // 4 aerial slam: arms overhead, leaning into the dive
        rax = mix(rax, -3.0F, w[4]); lax = mix(lax, -3.0F, w[4]);
        bodyX = mix(bodyX, 0.45F, w[4]); up = mix(up, 1.0F, w[4]); sweep = mix(sweep, 0.0F, w[4]);
        // 5 wing clap / cocoon / tornado, driven by how long the pose has been held
        float pTicks = e.poseTicks + pt;
        float wu;
        float ws;
        if (pTicks < 8.0F) {            // wings pulled far back
            wu = 1.0F; ws = -0.9F;
        } else if (pTicks < 12.0F) {    // clap shut in front
            float k = (pTicks - 8.0F) / 4.0F;
            wu = mix(1.0F, 0.25F, k); ws = mix(-0.9F, 1.95F, k);
        } else if (pTicks < 18.0F) {    // wrapped tight in a cocoon
            wu = 0.25F; ws = 1.95F;
        } else if (pTicks < 22.0F) {    // flung wide open
            float k = (pTicks - 18.0F) / 4.0F;
            wu = mix(0.25F, 0.95F, k); ws = mix(1.95F, -0.2F, k);
        } else if (pTicks < 65.0F) {    // held wide while the tornado spins
            wu = 0.95F + Mth.sin(pTicks * 0.5F) * 0.08F; ws = -0.2F;
        } else {                        // fold back in
            wu = 0.4F; ws = 0.6F;
        }
        up = mix(up, wu, w[5]); sweep = mix(sweep, ws, w[5]);
        rax = mix(rax, -0.2F, w[5]); raz = mix(raz, 1.0F, w[5]);
        lax = mix(lax, -0.2F, w[5]); laz = mix(laz, -1.0F, w[5]);
        // 6 piercing wing thrust: tips jab straight forward
        up = mix(up, 0.0F, w[6]); sweep = mix(sweep, 1.5F, w[6]);
        rax = mix(rax, 0.3F, w[6]); lax = mix(lax, 0.3F, w[6]); bodyX = mix(bodyX, 0.55F, w[6]);
        // 7 wing guard: wings folded in front like a shield, arms crossed
        up = mix(up, 0.1F, w[7]); sweep = mix(sweep, 1.9F, w[7]);
        rax = mix(rax, -0.9F, w[7]); raz = mix(raz, -0.3F, w[7]);
        lax = mix(lax, -0.9F, w[7]); laz = mix(laz, 0.3F, w[7]);
        // 8 feather barrage / grey shots
        rax = mix(rax, -0.6F, w[8]); raz = mix(raz, 0.9F, w[8]);
        lax = mix(lax, -0.6F, w[8]); laz = mix(laz, -0.9F, w[8]);
        up = mix(up, 0.9F, w[8]); sweep = mix(sweep, 0.1F, w[8]);
        // phase transition: arms thrown wide, wings spread, head tilted back
        rax = mix(rax, -0.1F, tw); raz = mix(raz, 2.6F, tw);
        lax = mix(lax, -0.1F, tw); laz = mix(laz, -2.6F, tw);
        up = mix(up, 1.0F, tw); sweep = mix(sweep, 0.0F, tw); headX = mix(headX, -0.3F, tw);

        this.wingUp = up;
        this.wingSweep = sweep;
        this.body.xRot = bodyX;
        this.head.xRot = headX + Mth.clamp(headPitch, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
        this.head.yRot = Mth.clamp(netHeadYaw, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        this.rightArm.xRot = rax; this.rightArm.zRot = raz;
        this.leftArm.xRot = lax; this.leftArm.zRot = laz;
    }

    /** True: only the oversized right-hand shell is drawn (for the white emissive GLOWING_HAND overlay). */
    public void setHandOnly(boolean only) {
        this.head.visible = !only;
        this.leftArm.visible = !only;
        this.rightHand.visible = !only;
        this.handGlow.visible = only;
        this.body.skipDraw = only;
        this.rightArm.skipDraw = only;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                               int color) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, color);
    }
}
