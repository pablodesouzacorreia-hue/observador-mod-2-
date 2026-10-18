package com.pablo.observador.system;

import com.pablo.observador.Diagnostics;
import com.pablo.observador.HorrorConfig;
import com.pablo.observador.entity.ModEntities;
import com.pablo.observador.entity.ObservadorEntity;
import com.pablo.observador.entity.Placement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Decide quando e onde o Observador aparece para cada jogador.
 * Regras: escuro, Overworld, longe de outra criatura dele, fora do campo de visão,
 * com tempo mínimo entre aparições e probabilidade controlada pela configuração.
 * Cada aparição aumenta a "tensão" daquele jogador, o que deixa a IA mais ousada.
 */
public final class AppearanceManager {
    private static final Map<UUID, Long> NEXT_ALLOWED = new HashMap<>();
    private static final Map<UUID, Integer> ENCOUNTERS = new HashMap<>();

    private AppearanceManager() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(AppearanceManager::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!HorrorConfig.SPEC.isLoaded()) {
            return;
        }
        boolean fast = HorrorConfig.ACCELERATE_EVENTS.get();
        if (player.tickCount % 40 != 0 || (!fast && player.tickCount < 2400)) {
            return; // verifica a cada 2 s e dá 2 min de paz ao entrar no mundo (sem paz no modo de teste)
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (level.dimension() != Level.OVERWORLD || player.isSpectator()) {
            return;
        }

        double freq = HorrorConfig.APPEARANCE_FREQUENCY.get();
        double intensity = HorrorConfig.HORROR_INTENSITY.get();
        if (freq <= 0.0 || intensity <= 0.0) {
            return;
        }

        UUID id = player.getUUID();
        long now = level.getGameTime();
        Long next = NEXT_ALLOWED.get(id);
        if (next != null && now < next) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        if (!fast && level.getMaxLocalRawBrightness(playerPos) > 7) {
            return; // só aparece quando o jogador está no escuro
        }

        double chance = fast ? 1.0 : 0.03 * freq * intensity;
        if (!level.canSeeSky(playerPos)) {
            chance *= 1.3; // cavernas e interiores
        }
        if (isAlone(level, player)) {
            chance *= 1.5; // isolamento
        }
        RandomSource rnd = level.getRandom();
        if (rnd.nextDouble() > chance) {
            return;
        }

        // Só uma criatura por região.
        if (!level.getEntitiesOfClass(ObservadorEntity.class, player.getBoundingBox().inflate(160.0)).isEmpty()) {
            return;
        }

        int encounters = ENCOUNTERS.getOrDefault(id, 0);
        if (trySpawn(level, player, rnd, Math.min(100, encounters), fast)) {
            ENCOUNTERS.put(id, encounters + 1);
            long cooldown = fast ? 600 : (long) ((3600 + rnd.nextInt(6000)) / freq);
            NEXT_ALLOWED.put(id, now + cooldown);
        }
    }

    private static boolean isAlone(ServerLevel level, ServerPlayer self) {
        for (Player other : level.players()) {
            if (other != self && other.distanceToSqr(self) < 40.0 * 40.0) {
                return false;
            }
        }
        return true;
    }

    private static boolean trySpawn(ServerLevel level, ServerPlayer player, RandomSource rnd, int tension,
                                    boolean fast) {
        for (int i = 0; i < 14; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2.0;
            double dist = 24.0 + rnd.nextDouble() * 16.0;
            double x = player.getX() + Math.cos(angle) * dist;
            double z = player.getZ() + Math.sin(angle) * dist;

            BlockPos pos = Placement.findStandPos(level, x, player.getY(), z);
            if (pos == null) {
                continue;
            }
            Vec3 where = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (Placement.viewDot(player, where) > 0.55) {
                continue; // nunca nasce no meio da visão do jogador
            }
            if (!fast && level.getMaxLocalRawBrightness(pos) > 9) {
                continue;
            }

            ObservadorEntity entity = new ObservadorEntity(ModEntities.OBSERVADOR.get(), level);
            entity.setPos(where.x, where.y, where.z);
            if (!level.noCollision(entity)) {
                continue;
            }
            entity.tension = tension;
            level.addFreshEntity(entity);
            Diagnostics.log("Observador apareceu em {} para {} (tensão={})", pos, player.getName().getString(), tension);
            return true;
        }
        return false;
    }
}
