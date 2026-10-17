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
 * Silhueta humanoide errada: cabeça pequena e inclinada, tronco fino,
 * braços longos demais (chegam aos joelhos) e pernas finas.
 */
public class ObservadorModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(ObservadorMod.MOD_ID, "observador"), "main");

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public ObservadorModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Pés em y=24. Pernas 16 + tronco 12 + cabeça 8 = 36 px (2,25 blocos).
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -8.0F, -3.0F, 6.0F, 8.0F, 6.0F),
                PartPose.offset(0.0F, -4.0F, 0.0F));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, 0.0F, -1.5F, 6.0F, 12.0F, 3.0F),
                PartPose.offset(0.0F, -4.0F, 0.0F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(24, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F),
                PartPose.offset(-4.0F, -3.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F),
                PartPose.offset(4.0F, -3.0F, 0.0F));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 36).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 16.0F, 2.0F),
                PartPose.offset(-1.5F, 8.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(8, 36).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 16.0F, 2.0F),
                PartPose.offset(1.5F, 8.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);

        float t = state.ageInTicks;
        float pos = state.walkAnimationPos;
        float speed = Math.min(1.0F, state.walkAnimationSpeed * 2.0F);
        float deg = (float) Math.PI / 180.0F;

        // Cabeça sempre meio torta, com um espasmo curto de vez em quando.
        this.head.yRot = state.yRot * deg;
        this.head.xRot = state.xRot * deg;
        this.head.zRot = 0.22F + Mth.sin(t * 0.05F) * 0.06F;
        if (((int) t) % 97 < 3) {
            this.head.zRot += 0.35F;
        }

        // Pernas com passo rígido; braços pendurados balançando pouco e fora de compasso.
        float swing = pos * 0.6662F;
        this.rightLeg.xRot = Mth.cos(swing) * 1.2F * speed;
        this.leftLeg.xRot = Mth.cos(swing + (float) Math.PI) * 1.2F * speed;

        this.rightArm.xRot = Mth.cos(swing + (float) Math.PI) * 0.4F * speed + 0.05F;
        this.leftArm.xRot = Mth.cos(swing) * 0.4F * speed + 0.05F;
        this.rightArm.zRot = 0.06F + Mth.sin(t * 0.04F) * 0.03F;
        this.leftArm.zRot = -0.06F - Mth.sin(t * 0.045F) * 0.03F;
    }
}
