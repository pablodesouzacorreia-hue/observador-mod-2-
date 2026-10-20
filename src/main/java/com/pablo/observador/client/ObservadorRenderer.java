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

    /** Luz máxima (bloco 15, céu 15): o corpo é quase preto, então só os olhos e as mãos "brilham". */
    private static final int FULL_BRIGHT = 0xF000F0;

    public ObservadorRenderer(EntityRendererProvider.Context context) {
        super(context, new ObservadorModel(context.bakeLayer(ObservadorModel.LAYER)), 0.35F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public void extractRenderState(ObservadorEntity entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // Sem isso, no escuro a criatura some por completo; assim a silhueta e os olhos continuam visíveis.
        state.lightCoords = FULL_BRIGHT;
        // Garante que nenhum nome flutue sobre a cabeça da criatura.
        state.nameTag = null;
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
