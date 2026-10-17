package com.pablo.observador.client;

import com.pablo.observador.ObservadorMod;
import com.pablo.observador.entity.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Registro de modelo e renderizador. Só roda no cliente (nunca no servidor dedicado). */
@EventBusSubscriber(modid = ObservadorMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ObservadorModel.LAYER, ObservadorModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.OBSERVADOR.get(), ObservadorRenderer::new);
    }
}
