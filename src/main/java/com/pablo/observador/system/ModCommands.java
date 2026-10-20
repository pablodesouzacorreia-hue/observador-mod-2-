package com.pablo.observador.system;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pablo.observador.entity.ObservadorEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos de teste (precisam de permissão de operador / trapaças ligadas):
 *   /observador aparecer [distancia]   /observador perseguir   /observador remover
 *   /observador evento <tipo>          /observador sanidade <0-100>   /observador medo <0-100>
 *   /observador mensagem               /observador status
 */
public final class ModCommands {
    private ModCommands() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ModCommands::onRegister);
    }

    private static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("observador")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("aparecer")
                        .executes(c -> spawn(c.getSource(), 20.0))
                        .then(Commands.argument("distancia", DoubleArgumentType.doubleArg(8.0, 60.0))
                                .executes(c -> spawn(c.getSource(),
                                        DoubleArgumentType.getDouble(c, "distancia")))))
                .then(Commands.literal("perseguir").executes(c -> chase(c.getSource())))
                .then(Commands.literal("remover").executes(c -> remove(c.getSource())))
                .then(Commands.literal("evento")
                        .then(Commands.argument("tipo", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(EventManager.NAMES, b))
                                .executes(c -> event(c.getSource(), StringArgumentType.getString(c, "tipo")))))
                .then(Commands.literal("sanidade")
                        .then(Commands.argument("valor", IntegerArgumentType.integer(0, 100))
                                .executes(c -> sanity(c.getSource(), IntegerArgumentType.getInteger(c, "valor")))))
                .then(Commands.literal("medo")
                        .then(Commands.argument("valor", IntegerArgumentType.integer(0, 100))
                                .executes(c -> fear(c.getSource(), IntegerArgumentType.getInteger(c, "valor")))))
                .then(Commands.literal("mensagem").executes(c -> message(c.getSource())))
                .then(Commands.literal("status").executes(c -> status(c.getSource()))));
    }

    private static ServerPlayer player(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Este comando precisa ser usado por um jogador."));
            return null;
        }
    }

    private static ObservadorEntity nearest(ServerPlayer player) {
        ObservadorEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (ObservadorEntity e : ((ServerLevel) player.level()).getEntitiesOfClass(ObservadorEntity.class,
                player.getBoundingBox().inflate(160.0), x -> !x.mirage)) {
            double dist = e.distanceToSqr(player);
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }

    private static int spawn(CommandSourceStack source, double dist) {
        ServerPlayer player = player(source);
        if (player == null) {
            return 0;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (nearest(player) != null) {
            source.sendFailure(Component.literal("Já existe um Observador por perto. Use /observador remover."));
            return 0;
        }
        ObservadorEntity e = AppearanceManager.spawnNear(level, player, true, dist, dist + 6.0);
        if (e == null) {
            source.sendFailure(Component.literal("Não achei um lugar válido. Tente outra distância ou outro local."));
            return 0;
        }
        e.testMode = true;
        source.sendSuccess(() -> Component.literal("O Observador apareceu."), false);
        return 1;
    }

    private static int chase(CommandSourceStack source) {
        ServerPlayer player = player(source);
        if (player == null) {
            return 0;
        }
        ServerLevel level = (ServerLevel) player.level();
        ObservadorEntity e = nearest(player);
        if (e == null) {
            e = AppearanceManager.spawnNear(level, player, true, 14.0, 22.0);
        }
        if (e == null) {
            source.sendFailure(Component.literal("Não achei um lugar válido para ela aparecer."));
            return 0;
        }
        e.testMode = true;
        e.forceChase = true;
        source.sendSuccess(() -> Component.literal("Ela vai te perseguir."), false);
        return 1;
    }

    private static int remove(CommandSourceStack source) {
        ServerPlayer player = player(source);
        if (player == null) {
            return 0;
        }
        int count = 0;
        for (ObservadorEntity e : ((ServerLevel) player.level()).getEntitiesOfClass(ObservadorEntity.class,
                player.getBoundingBox().inflate(300.0))) {
            e.discard();
            count++;
        }
        int total = count;
        source.sendSuccess(() -> Component.literal("Removidos: " + total), false);
        return count;
    }

    private static int event(CommandSourceStack source, String type) {
        ServerPlayer player = player(source);
        if (player == null) {
            return 0;
        }
        String error = EventManager.trigger(player, type);
        if (error != null) {
            source.sendFailure(Component.literal(error));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Evento: " + type), false);
        return 1;
    }

    private static int sanity(CommandSourceStack source, int value) {
        ServerPlayer player = player(source);
        if (player == null) {
            return 0;
        }
        SanityManager.set(player, value);
        source.sendSuccess(() -> Component.literal("Sanidade: " + value), false);
        return 1;
    }

    private static int fear(CommandSourceStack source, int value) {
        ServerPlayer player = player(source);
        if (player == null) {
            return 0;
        }
        FearManager.set(player, value);
        source.sendSuccess(() -> Component.literal("Medo: " + value), false);
        return 1;
    }

    private static int message(CommandSourceStack source) {
        ServerPlayer player = player(source);
        if (player == null) {
            return 0;
        }
        ThreatTalk.say(player, ThreatTalk.Mood.WATCH, player.getRandom(), true);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        ServerPlayer player = player(source);
        if (player == null) {
            return 0;
        }
        ServerLevel level = (ServerLevel) player.level();
        ObservadorEntity e = nearest(player);
        String creature = e == null ? "nenhuma"
                : "a " + (int) e.distanceTo(player) + " blocos";
        int light = level.getMaxLocalRawBrightness(player.blockPosition());
        String text = "Sanidade " + (int) SanityManager.get(player)
                + " | Medo " + (int) FearManager.get(player)
                + " | Parado há " + FearManager.idleSeconds(player) + " s"
                + " | Criatura: " + creature + " | Luz: " + light;
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
