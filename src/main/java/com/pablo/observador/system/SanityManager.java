package com.pablo.observador.system;

import com.pablo.observador.Diagnostics;
import com.pablo.observador.HorrorConfig;
import com.pablo.observador.entity.ObservadorEntity;
import com.pablo.observador.entity.Placement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Sanidade de 0 a 100 por jogador (guardada em memória: volta a 100 quando o servidor reinicia).
 *
 * Cai com: escuridão, noite ao ar livre, isolamento, cavernas profundas, ver a criatura,
 * ficar perto dela e ser atacado. Sobe com: luz forte, dia, companhia e tempo em segurança.
 * Efeitos: abaixo de 70 os eventos psicológicos ficam mais frequentes, abaixo de 60 surgem
 * miragens, abaixo de 25 há um escurecimento leve e raro da tela (desligado em "reduzirFlashes").
 */
public final class SanityManager {
    private static final Map<UUID, Double> SANITY = new HashMap<>();
    private static final Map<UUID, Long> LAST_EFFECT = new HashMap<>();

    private SanityManager() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(SanityManager::onPlayerTick);
    }

    private static boolean enabled() {
        return HorrorConfig.SPEC.isLoaded() && HorrorConfig.SANITY_ENABLED.get();
    }

    public static double get(Player player) {
        if (!enabled()) {
            return 100.0;
        }
        return SANITY.getOrDefault(player.getUUID(), 100.0);
    }

    public static void add(Player player, double delta) {
        if (!enabled()) {
            return;
        }
        double value = SANITY.getOrDefault(player.getUUID(), 100.0) + delta;
        SANITY.put(player.getUUID(), Math.max(0.0, Math.min(100.0, value)));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0 || !enabled() || player.isSpectator()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = player.blockPosition();
        int light = level.getMaxLocalRawBrightness(pos);
        boolean sky = level.canSeeSky(pos);
        double delta = 0.0;

        // Fatores que derrubam a sanidade.
        if (light <= 4) {
            delta -= 0.25;
        }
        if (sky && light <= 7) {
            delta -= 0.10; // noite ao ar livre
        }
        if (!sky && pos.getY() < 0) {
            delta -= 0.20; // caverna profunda
        }
        boolean alone = true;
        for (Player other : level.players()) {
            if (other != player && other.distanceToSqr(player) < 16.0 * 16.0) {
                alone = false;
                break;
            }
        }
        if (alone && light <= 7) {
            delta -= 0.10; // isolamento no escuro
        }

        // A própria criatura.
        for (ObservadorEntity e : level.getEntitiesOfClass(ObservadorEntity.class,
                player.getBoundingBox().inflate(40.0))) {
            double dist = e.distanceTo(player);
            boolean seen = dist < 40.0
                    && Placement.viewDot(player, e.getEyePosition()) > 0.9
                    && player.hasLineOfSight(e);
            if (seen) {
                delta -= e.mirage ? 1.0 : 2.0;
            } else if (dist < 12.0 && !e.mirage) {
                delta -= 0.8;
            }
        }

        // Fatores que recuperam.
        if (light >= 12) {
            delta += 0.40;
        } else if (sky && light >= 10) {
            delta += 0.30;
        } else if (light >= 8) {
            delta += 0.10;
        }
        if (!alone) {
            delta += 0.15;
        }

        add(player, delta);
        double sanity = get(player);

        // Efeito visual leve e raro, nunca em modo "reduzir flashes".
        long now = level.getGameTime();
        long last = LAST_EFFECT.getOrDefault(player.getUUID(), -100000L);
        if (sanity < 25.0 && !HorrorConfig.REDUCE_FLASHES.get() && now - last >= 1800
                && level.getRandom().nextFloat() < 0.15F) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0, false, false));
            LAST_EFFECT.put(player.getUUID(), now);
            Diagnostics.log("Efeito de sanidade baixa aplicado a {}", player.getName().getString());
        }

        if (player.tickCount % 200 == 0) {
            Diagnostics.log("Sanidade de {}: {}", player.getName().getString(), (int) sanity);
        }
    }
}
