package com.pablo.observador.system;

import com.pablo.observador.HorrorConfig;
import com.pablo.observador.entity.ObservadorEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Quando O Observador (de verdade, não miragem) está por perto, os outros mobs perto do jogador
 * param de andar e ficam olhando fixamente para ele, sem piscar. Quando a criatura some, eles voltam ao normal.
 * Mobs que já têm um alvo (por exemplo, um zumbi atacando) e mobs montados ou dormindo não são afetados.
 */
public final class StareManager {
    private static final Map<UUID, Boolean> ACTIVE = new HashMap<>();

    private StareManager() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(StareManager::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!HorrorConfig.SPEC.isLoaded() || !HorrorConfig.STARE_ENABLED.get()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        UUID id = player.getUUID();
        if (player.tickCount % 20 == 0) {
            boolean active = !level.getEntitiesOfClass(ObservadorEntity.class,
                    player.getBoundingBox().inflate(48.0), e -> !e.mirage).isEmpty();
            ACTIVE.put(id, active);
        }
        if (!ACTIVE.getOrDefault(id, false)) {
            return;
        }

        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24.0))) {
            if (mob instanceof ObservadorEntity || !mob.isAlive() || mob.getTarget() != null
                    || mob.isPassenger() || mob.isVehicle() || mob.isSleeping()) {
                continue;
            }
            if (mob.getId() % 5 == 0) {
                continue; // um em cada cinco ignora o jogador, para não parecer um efeito mecânico
            }
            stare(mob, player);
        }
    }

    private static void stare(Mob mob, ServerPlayer player) {
        double dx = player.getX() - mob.getX();
        double dz = player.getZ() - mob.getZ();
        double dy = player.getEyeY() - mob.getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, horizontal) * (180.0 / Math.PI)));

        mob.getNavigation().stop();
        mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setXRot(pitch);
    }
}
