package com.pablo.observador.entity;

import com.pablo.observador.ObservadorMod;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(ObservadorMod.MOD_ID);

    // MISC: não nasce sozinha pelo jogo; só o nosso AppearanceManager (ou /summon) a cria.
    public static final Supplier<EntityType<ObservadorEntity>> OBSERVADOR = ENTITY_TYPES.registerEntityType(
            "observador",
            ObservadorEntity::new,
            MobCategory.MISC,
            builder -> builder.sized(0.6f, 2.3f).eyeHeight(2.05f).clientTrackingRange(10).updateInterval(2));

    private ModEntities() {}

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(OBSERVADOR.get(), ObservadorEntity.createAttributes().build());
    }
}
