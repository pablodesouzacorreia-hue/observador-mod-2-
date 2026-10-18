package com.pablo.observador;

import com.mojang.logging.LogUtils;
import com.pablo.observador.entity.ModEntities;
import com.pablo.observador.sound.ModSounds;
import com.pablo.observador.system.AppearanceManager;
import com.pablo.observador.system.EventManager;
import com.pablo.observador.system.SanityManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Ponto de entrada do mod.
 * Registra: configuração, entidade O Observador, sons, aparições, sanidade e eventos psicológicos.
 */
@Mod(ObservadorMod.MOD_ID)
public class ObservadorMod {
    public static final String MOD_ID = "observador";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ObservadorMod(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.STARTUP, HorrorConfig.SPEC);
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);
        AppearanceManager.init();
        SanityManager.init();
        EventManager.init();
        modEventBus.addListener(this::commonSetup);
        LOGGER.info("[Observador] Mod construído.");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        if (HorrorConfig.SPEC.isLoaded()) {
            Diagnostics.log("Setup comum concluído. Configuração carregada: {}", HorrorConfig.summary());
        }
    }
}
