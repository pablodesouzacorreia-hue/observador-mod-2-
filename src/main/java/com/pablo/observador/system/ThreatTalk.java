package com.pablo.observador.system;

import com.pablo.observador.HorrorConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Frases que aparecem no chat, vindas de "???": a criatura tentando se comunicar.
 * Há um intervalo mínimo de ~20 s por jogador para não virar spam (exceto no início de uma perseguição
 * e em comandos de teste). Desligue em mensagens.ativadas.
 */
public final class ThreatTalk {
    public enum Mood { WATCH, VANISH, CHASE, FEAR }

    private static final String[] WATCH = {
            "eu te vejo", "está sozinho?", "estou olhando para você", "você sabe que estou aqui",
            "não se mexa", "olá?", "finalmente você me notou", "não olhe para trás", "eu estava te esperando"};
    private static final String[] VANISH = {
            "você me viu", "até logo", "ainda estou aqui", "volto já", "não precisa ter medo"};
    private static final String[] CHASE = {
            "corra", "te achei", "não adianta", "venha", "por que você está correndo?"};
    private static final String[] FEAR = {
            "está ouvindo?", "atrás de você", "você não está sozinho", "shhh", "ele sabe que você parou",
            "continue ouvindo", "estou mais perto do que você pensa", "por que parou?", "voltou..."};

    private static final Map<UUID, Long> LAST = new HashMap<>();

    private ThreatTalk() {}

    public static void say(Player target, Mood mood, RandomSource rnd, boolean ignoreCooldown) {
        if (!(target instanceof ServerPlayer player) || !HorrorConfig.SPEC.isLoaded()
                || !HorrorConfig.MESSAGES_ENABLED.get()) {
            return;
        }
        long now = player.level().getGameTime();
        Long last = LAST.get(player.getUUID());
        if (!ignoreCooldown && last != null && now - last < 400) {
            return;
        }
        LAST.put(player.getUUID(), now);

        String[] pool = switch (mood) {
            case WATCH -> WATCH;
            case VANISH -> VANISH;
            case CHASE -> CHASE;
            case FEAR -> FEAR;
        };
        String text = pool[rnd.nextInt(pool.length)];
        Component message = Component.literal("<???> ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(text).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        player.connection.send(new ClientboundSystemChatPacket(message, false));
    }

    public static void say(Player target, Mood mood, RandomSource rnd) {
        say(target, mood, rnd, false);
    }
}
