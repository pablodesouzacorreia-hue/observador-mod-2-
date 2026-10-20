package com.pablo.observador.system;

import com.pablo.observador.Diagnostics;
import com.pablo.observador.HorrorConfig;
import com.pablo.observador.entity.ObservadorEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.pablo.observador.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Medo do jogador (0 a 100), separado da sanidade. Sobe quando:
 *  - o jogador PAUSA o jogo e volta (só em mundo individual): medo vai a 100, a criatura manda um "Hi"
 *    atrás dele, escreve no chat e pode aparecer por perto;
 *  - o jogador fica PARADO "escutando" (sem andar nem mexer a câmera por medo.segundosParadoParaEscutar):
 *    o medo sobe até ~85 e a cada 12 s de silêncio algo acontece.
 * Desce 1,5 por segundo quando o jogador volta a se mexer.
 *
 * Efeitos do medo: sons do mod ficam até 80% mais altos (ModSounds), eventos acontecem mais rápido
 * (EventManager), a criatura aparece com mais facilidade (AppearanceManager) e fica mais agressiva e
 * mais próxima (ObservadorBehaviorGoal).
 */
public final class FearManager {
    private static final Map<UUID, Double> FEAR = new HashMap<>();
    private static final Map<UUID, Integer> IDLE = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
    private static final Map<UUID, float[]> LAST_ROT = new HashMap<>();
    private static final Map<UUID, Long> LAST_SCARE = new HashMap<>();

    private static long ticks = 0;
    private static long lastNano = 0;

    private FearManager() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(FearManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(FearManager::onPlayerTick);
    }

    private static boolean enabled() {
        return HorrorConfig.SPEC.isLoaded() && HorrorConfig.FEAR_ENABLED.get();
    }

    /** Medo atual do jogador, de 0 a 100 (sempre 0 se o sistema estiver desligado). */
    public static double get(Player player) {
        return enabled() ? FEAR.getOrDefault(player.getUUID(), 0.0) : 0.0;
    }

    public static void set(Player player, double value) {
        FEAR.put(player.getUUID(), Math.max(0.0, Math.min(100.0, value)));
    }

    public static int idleSeconds(Player player) {
        return IDLE.getOrDefault(player.getUUID(), 0) / 20;
    }

    /** Multiplicador de volume (1,0 a 1,8) pelo jogador mais assustado num raio de 64 blocos. */
    public static float boost(Level level, double x, double y, double z) {
        if (!enabled()) {
            return 1.0F;
        }
        double max = 0.0;
        for (Player player : level.players()) {
            if (player.distanceToSqr(x, y, z) < 64.0 * 64.0) {
                max = Math.max(max, FEAR.getOrDefault(player.getUUID(), 0.0));
            }
        }
        return (float) (1.0 + max / 100.0 * 0.8);
    }

    // ------------------------------------------------------------------ pausa

    private static void onServerTick(ServerTickEvent.Post event) {
        ticks++;
        long now = System.nanoTime();
        long previous = lastNano;
        lastNano = now;
        if (previous == 0L || ticks < 400 || !enabled()) {
            return;
        }
        // Em mundo individual o servidor para de rodar quando o jogo está pausado. Uma lacuna grande
        // entre dois ticks seguidos significa "o jogador pausou e voltou".
        double gapSeconds = (now - previous) / 1.0E9;
        MinecraftServer server = event.getServer();
        if (gapSeconds >= 2.0 && server.isSingleplayer()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                onResume(player, gapSeconds);
            }
        }
    }

    private static void onResume(ServerPlayer player, double gapSeconds) {
        if (!player.isAlive() || player.isSpectator() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        UUID id = player.getUUID();
        Long last = LAST_SCARE.get(id);
        if (last != null && ticks - last < 400) {
            return;
        }
        LAST_SCARE.put(id, ticks);
        FEAR.put(id, 100.0);
        Diagnostics.log("{} voltou de uma pausa de {} s: medo ao máximo", player.getName().getString(), (int) gapSeconds);

        RandomSource rnd = level.getRandom();
        EventManager.voiceBehind(level, player, rnd, 4.0);
        EventManager.later(50, () -> ThreatTalk.say(player, ThreatTalk.Mood.FEAR, rnd, true));

        BlockPos pos = player.blockPosition();
        boolean darkEnough = level.getMaxLocalRawBrightness(pos) <= 9 || !level.canSeeSky(pos);
        boolean creatureNear = !level.getEntitiesOfClass(ObservadorEntity.class,
                player.getBoundingBox().inflate(100.0), e -> !e.mirage).isEmpty();
        if (darkEnough && !creatureNear) {
            AppearanceManager.spawnNear(level, player, false, 10.0, 18.0);
        }
    }

    // ------------------------------------------------------------------ parado escutando

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0 || !enabled()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        UUID id = player.getUUID();
        if (player.isSpectator() || player.isSleeping() || !player.isAlive()
                || level.dimension() != Level.OVERWORLD) {
            IDLE.put(id, 0);
            return;
        }

        Vec3 pos = player.position();
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        Vec3 lastPos = LAST_POS.get(id);
        float[] lastRot = LAST_ROT.get(id);
        boolean still = lastPos != null && lastRot != null
                && lastPos.distanceToSqr(pos) < 0.0025
                && Math.abs(Mth.wrapDegrees(yRot - lastRot[0])) < 1.5F
                && Math.abs(xRot - lastRot[1]) < 1.5F;
        LAST_POS.put(id, pos);
        LAST_ROT.put(id, new float[] {yRot, xRot});

        int idle = still ? IDLE.getOrDefault(id, 0) + 10 : 0;
        IDLE.put(id, idle);
        int needed = HorrorConfig.IDLE_SECONDS.get() * 20;
        boolean listening = idle >= needed;

        if (player.tickCount % 20 == 0) {
            double fear = FEAR.getOrDefault(id, 0.0);
            fear = listening ? Math.min(85.0, fear + 6.0) : Math.max(0.0, fear - 1.5);
            FEAR.put(id, fear);
            if (fear >= 50.0 && player.tickCount % 60 == 0) {
                ModSounds.play(level, player.getX(), player.getEyeY(), player.getZ(), ModSounds.HEARTBEAT,
                        SoundSource.AMBIENT, (float) (0.4 + fear / 100.0 * 0.8), 1.0F);
            }
        }

        // Ao começar a "escutar" e a cada 12 s de silêncio, algo responde.
        if (listening && (idle == needed || (idle - needed) % 240 == 0)) {
            FEAR.put(id, Math.max(FEAR.getOrDefault(id, 0.0), 40.0));
            Diagnostics.log("{} está parado escutando há {} s", player.getName().getString(), idle / 20);
            EventManager.triggerRandom(player);
        }
    }
}
