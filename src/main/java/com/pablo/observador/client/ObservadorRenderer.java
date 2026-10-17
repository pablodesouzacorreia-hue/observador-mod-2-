package com.pablo.observador.client;

import com.pablo.observador.ObservadorMod;
import com.pablo.observador.entity.ObservadorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class ObservadorRenderer
        extends LivingEntityRenderer<ObservadorEntity, LivingEntityRenderState, ObservadorModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ObservadorMod.MOD_ID, "textures/entity/observador.png");

    public ObservadorRenderer(EntityRendererProvider.Context context) {
        super(context, new ObservadorModel(context.bakeLayer(ObservadorModel.LAYER)), 0.3F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
