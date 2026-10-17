package com.pablo.observador;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Ponto de entrada do mod. Nesta etapa (Etapa 2) ele só registra a
 * configuração e confirma no log que o mod carregou.
 */
@Mod(ObservadorMod.MOD_ID)
public class ObservadorMod {
    public static final String MOD_ID = "observador";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ObservadorMod(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.STARTUP, HorrorConfig.SPEC);
        modEventBus.addListener(this::commonSetup);
        LOGGER.info("[Observador] Mod construído.");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        if (HorrorConfig.SPEC.isLoaded()) {
            Diagnostics.log("Setup comum concluído. Configuração carregada: {}", HorrorConfig.summary());
        }
    }
}
