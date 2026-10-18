package com.pablo.observador.client;

import com.pablo.observador.ObservadorMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Figura alta coberta por um manto escuro: cabeça caída e torta, olhos vermelhos,
 * braços longos demais com mãos pálidas e uma coroa de fios negros finos saindo da cabeça
 * (eles balançam sozinhos, mesmo com a criatura parada).
 */
public class ObservadorModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(ObservadorMod.MOD_ID, "observador"), "main");

    private static final int HAIR_COUNT = 16;

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart[] hair = new ModelPart[HAIR_COUNT];

    public ObservadorModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        for (int i = 0; i < HAIR_COUNT; i++) {
            this.hair[i] = this.head.getChild("hair" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Pés em y=24. Pernas 14..24, manto 6..20, peito -6..6, cabeça acima do pescoço (y=-6).
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -6.0F, 0.0F));

        // Fios finos saindo da cabeça para trás, para os lados e para cima/baixo (nunca pela frente do rosto).
        for (int i = 0; i < HAIR_COUNT; i++) {
            float yaw = -2.4F + 4.8F * i / (HAIR_COUNT - 1);
            float pitch = (float) Math.sin(i * 2.1 + 0.7);
            float length = 9.0F + (i % 4) * 2.0F;
            head.addOrReplaceChild("hair" + i,
                    CubeListBuilder.create().texOffs(32, 40).addBox(-0.5F, -0.5F, 3.5F, 1.0F, 1.0F, length),
                    PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, pitch, yaw, 0.0F));
        }

        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, -6.0F, 0.0F));

        root.addOrReplaceChild("robe",
                CubeListBuilder.create().texOffs(0, 32).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 14.0F, 6.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 22.0F, 2.0F),
                PartPose.offset(-6.0F, -5.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(50, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 22.0F, 2.0F),
                PartPose.offset(6.0F, -5.0F, 0.0F));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(40, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(-2.0F, 14.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(50, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(2.0F, 14.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);

        float t = state.ageInTicks;
        float pos = state.walkAnimationPos;
        float speed = Math.min(1.0F, state.walkAnimationSpeed * 2.0F);
        float deg = (float) Math.PI / 180.0F;

        // Cabeça caída para a frente e torta; de vez em quando um espasmo curto.
        this.head.yRot = state.yRot * deg;
        this.head.xRot = 0.55F + state.xRot * deg * 0.4F;
        this.head.zRot = 0.32F + Mth.sin(t * 0.05F) * 0.05F;
        if (((int) t) % 83 < 4) {
            this.head.zRot -= 0.5F;
            this.head.yRot += 0.25F;
        }

        // Os fios balançam devagar e fora de compasso.
        for (int i = 0; i < HAIR_COUNT; i++) {
            this.hair[i].xRot += Mth.sin(t * 0.08F + i * 1.3F) * 0.12F;
            this.hair[i].zRot += Mth.cos(t * 0.06F + i) * 0.08F;
        }

        // Pernas com passo rígido; braços pendurados, balançando pouco.
        float swing = pos * 0.6662F;
        this.rightLeg.xRot = Mth.cos(swing) * 1.2F * speed;
        this.leftLeg.xRot = Mth.cos(swing + (float) Math.PI) * 1.2F * speed;

        this.rightArm.xRot = Mth.cos(swing + (float) Math.PI) * 0.35F * speed + 0.12F;
        this.leftArm.xRot = Mth.cos(swing) * 0.35F * speed + 0.12F;
        this.rightArm.zRot = 0.05F + Mth.sin(t * 0.04F) * 0.03F;
        this.leftArm.zRot = -0.05F - Mth.sin(t * 0.045F) * 0.03F;
    }
}
