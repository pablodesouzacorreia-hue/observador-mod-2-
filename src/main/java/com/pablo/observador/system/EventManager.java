package com.pablo.observador.system;

import com.pablo.observador.Diagnostics;
import com.pablo.observador.HorrorConfig;
import com.pablo.observador.entity.ModEntities;
import com.pablo.observador.entity.ObservadorEntity;
import com.pablo.observador.entity.Placement;
import com.pablo.observador.sound.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Eventos psicológicos: sons e aparições que criam medo sem nenhuma criatura de verdade por perto.
 *
 * Cada evento só entra no sorteio se o contexto combinar (escuro, dentro de casa, caverna, porta por perto,
 * sanidade baixa...). O mesmo evento não se repete duas vezes seguidas. O intervalo entre eventos
 * diminui quando a sanidade cai e é ajustado por intensidadeTerror e frequenciaAparicoes.
 */
public final class EventManager {
    private enum Ev { FOOTSTEPS, KNOCK, CREAK, ROOF, DIG, WHISPER, MIRAGE }

    private record Task(long tick, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final Map<UUID, Ev> LAST = new HashMap<>();
    private static long serverTicks = 0;

    private EventManager() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(EventManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(EventManager::onPlayerTick);
    }

    // ------------------------------------------------------------------ agendamento de sons

    private static void later(int delayTicks, Runnable action) {
        TASKS.add(new Task(serverTicks + delayTicks, action));
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        serverTicks++;
        if (TASKS.isEmpty()) {
            return;
        }
        List<Task> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (task.tick() <= serverTicks) {
                due.add(task);
                it.remove();
            }
        }
        for (Task task : due) {
            task.action().run();
        }
    }

    // ------------------------------------------------------------------ sorteio por jogador

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        if (!HorrorConfig.SPEC.isLoaded() || !HorrorConfig.PSYCHOLOGICAL_EVENTS.get()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (level.dimension() != Level.OVERWORLD || player.isSpectator() || !player.isAlive()) {
            return;
        }

        boolean fast = HorrorConfig.ACCELERATE_EVENTS.get();
        if (!fast && player.tickCount < 1200) {
            return; // um minuto de paz ao entrar no mundo
        }
        double scale = HorrorConfig.HORROR_INTENSITY.get() * HorrorConfig.APPEARANCE_FREQUENCY.get();
        if (scale <= 0.001) {
            return;
        }

        UUID id = player.getUUID();
        Long next = NEXT.get(id);
        if (next != null && serverTicks < next) {
            return;
        }

        RandomSource rnd = level.getRandom();
        BlockPos pos = player.blockPosition();
        int light = level.getMaxLocalRawBrightness(pos);
        boolean sky = level.canSeeSky(pos);
        double sanity = SanityManager.get(player);
        boolean dark = fast || light <= 7;

        boolean alone = true;
        for (Player other : level.players()) {
            if (other != player && other.distanceToSqr(player) < 24.0 * 24.0) {
                alone = false;
                break;
            }
        }

        List<Ev> options = new ArrayList<>();
        BlockPos door = null;
        if (dark) {
            options.add(Ev.FOOTSTEPS);
            door = findDoor(level, pos);
            if (door != null) {
                options.add(Ev.KNOCK);
                options.add(Ev.CREAK);
            }
        }
        if (!sky && pos.getY() >= level.getSeaLevel() - 2 && (fast || light <= 9)) {
            options.add(Ev.ROOF);
        }
        if (!sky && pos.getY() < 45 && (fast || light <= 9)) {
            options.add(Ev.DIG);
        }
        if (dark && (fast || sanity < 75.0 || alone)) {
            options.add(Ev.WHISPER);
        }
        boolean noCreatureNear = level.getEntitiesOfClass(ObservadorEntity.class,
                player.getBoundingBox().inflate(100.0)).isEmpty();
        if (noCreatureNear && (fast || (dark && sanity < 60.0))) {
            options.add(Ev.MIRAGE);
        }

        // Próximo sorteio: mais cedo se a sanidade estiver baixa.
        double sanityFactor = 0.55 + 0.45 * sanity / 100.0;
        long interval = fast ? 160 + rnd.nextInt(120)
                : (long) ((1200 + rnd.nextInt(2400)) * sanityFactor / Math.min(3.0, scale));

        if (options.isEmpty()) {
            NEXT.put(id, serverTicks + 200); // nada combina com o contexto agora; tenta de novo logo
            return;
        }
        Ev last = LAST.get(id);
        if (options.size() > 1 && last != null) {
            options.remove(last);
        }
        Ev chosen = options.get(rnd.nextInt(options.size()));
        LAST.put(id, chosen);
        NEXT.put(id, serverTicks + interval);

        Diagnostics.log("Evento psicológico {} para {} (sanidade={}, luz={})",
                chosen, player.getName().getString(), (int) sanity, light);
        switch (chosen) {
            case FOOTSTEPS -> footsteps(level, player, rnd);
            case KNOCK -> knock(level, door, rnd);
            case CREAK -> ModSounds.play(level, door.getX() + 0.5, door.getY() + 1.0, door.getZ() + 0.5,
                    ModSounds.CREAK, SoundSource.AMBIENT, 1.2F, 0.9F + rnd.nextFloat() * 0.2F);
            case ROOF -> roof(level, player, rnd);
            case DIG -> dig(level, player, rnd);
            case WHISPER -> whisper(level, player, rnd);
            case MIRAGE -> mirage(level, player, rnd);
        }
    }

    // ------------------------------------------------------------------ eventos

    /** Passos que se aproximam por trás do jogador, irregulares, e param antes de chegar. */
    private static void footsteps(ServerLevel level, ServerPlayer player, RandomSource rnd) {
        Vec3 look = player.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0.0, -look.z);
        if (back.lengthSqr() < 1.0E-4) {
            back = new Vec3(1.0, 0.0, 0.0);
        }
        back = back.normalize();
        Vec3 side = new Vec3(-back.z, 0.0, back.x);
        double sideOffset = (rnd.nextDouble() - 0.5) * 3.0;
        int steps = 4 + rnd.nextInt(3);
        int delay = 0;
        for (int i = 0; i < steps; i++) {
            double dist = 10.0 - i * (5.0 / steps);
            Vec3 at = player.position().add(back.scale(dist)).add(side.scale(sideOffset));
            float pitch = 0.8F + rnd.nextFloat() * 0.25F;
            later(delay, () -> ModSounds.play(level, at.x, at.y, at.z, ModSounds.STEP,
                    SoundSource.AMBIENT, 1.1F, pitch));
            delay += 9 + rnd.nextInt(9);
        }
    }

    /** Três batidas na porta mais próxima, a segunda e a terceira em ritmo irregular. */
    private static void knock(ServerLevel level, BlockPos door, RandomSource rnd) {
        int delay = 0;
        for (int i = 0; i < 3; i++) {
            float pitch = 0.9F + rnd.nextFloat() * 0.15F;
            later(delay, () -> ModSounds.play(level, door.getX() + 0.5, door.getY() + 1.0, door.getZ() + 0.5,
                    ModSounds.KNOCK, SoundSource.AMBIENT, 1.4F, pitch));
            delay += (i == 0 ? 12 : 22) + rnd.nextInt(18);
        }
    }

    /** Passos no telhado, acima do jogador, atravessando de um lado para o outro. */
    private static void roof(ServerLevel level, ServerPlayer player, RandomSource rnd) {
        double angle = rnd.nextDouble() * Math.PI * 2.0;
        Vec3 dir = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        int steps = 5 + rnd.nextInt(3);
        int delay = 0;
        for (int i = 0; i < steps; i++) {
            Vec3 at = player.position().add(dir.scale(-3.0 + i * 1.0)).add(0.0, 3.6, 0.0);
            float pitch = 0.85F + rnd.nextFloat() * 0.2F;
            later(delay, () -> ModSounds.play(level, at.x, at.y, at.z, ModSounds.STEP,
                    SoundSource.AMBIENT, 0.9F, pitch));
            delay += 10 + rnd.nextInt(8);
        }
    }

    /** Alguém escavando em uma caverna próxima: algumas batidas de picareta distantes. */
    private static void dig(ServerLevel level, ServerPlayer player, RandomSource rnd) {
        double angle = rnd.nextDouble() * Math.PI * 2.0;
        double dist = 14.0 + rnd.nextDouble() * 8.0;
        Vec3 at = player.position().add(Math.cos(angle) * dist, -1.0 - rnd.nextInt(3), Math.sin(angle) * dist);
        int hits = 3 + rnd.nextInt(3);
        int delay = 0;
        for (int i = 0; i < hits; i++) {
            float pitch = 0.9F + rnd.nextFloat() * 0.2F;
            later(delay, () -> ModSounds.play(level, at.x, at.y, at.z, ModSounds.DIG,
                    SoundSource.AMBIENT, 1.2F, pitch));
            delay += 14 + rnd.nextInt(16);
        }
    }

    /** Sussurro colado na nuca: sempre do lado oposto ao que o jogador está olhando. */
    private static void whisper(ServerLevel level, ServerPlayer player, RandomSource rnd) {
        Vec3 look = player.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0.0, -look.z);
        back = back.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : back.normalize();
        Vec3 side = new Vec3(-back.z, 0.0, back.x).scale((rnd.nextDouble() - 0.5) * 1.2);
        Vec3 at = player.getEyePosition().add(back.scale(1.4)).add(side);
        ModSounds.play(level, at.x, at.y, at.z, ModSounds.WHISPER, SoundSource.AMBIENT, 1.0F,
                0.9F + rnd.nextFloat() * 0.2F);
    }

    /** Miragem: a silhueta inofensiva surge na lateral da visão e some no instante em que é encarada. */
    private static void mirage(ServerLevel level, ServerPlayer player, RandomSource rnd) {
        for (int i = 0; i < 14; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2.0;
            double dist = 14.0 + rnd.nextDouble() * 9.0;
            BlockPos pos = Placement.findStandPos(level,
                    player.getX() + Math.cos(angle) * dist, player.getY(), player.getZ() + Math.sin(angle) * dist);
            if (pos == null) {
                continue;
            }
            Vec3 where = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            double dot = Placement.viewDot(player, where);
            if (dot < 0.2 || dot > 0.75) {
                continue; // só na borda do campo de visão
            }
            ObservadorEntity entity = new ObservadorEntity(ModEntities.OBSERVADOR.get(), level);
            entity.mirage = true;
            entity.setPos(where.x, where.y, where.z);
            if (!level.noCollision(entity)) {
                continue;
            }
            level.addFreshEntity(entity);
            Diagnostics.log("Miragem criada em {}", pos);
            return;
        }
    }

    /** Porta mais próxima do jogador, a pelo menos 4 blocos (para a batida não ser "dentro" dele). */
    private static BlockPos findDoor(ServerLevel level, BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -10; dz <= 10; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (!state.is(BlockTags.DOORS)) {
                        continue;
                    }
                    double dist = dx * dx + dy * dy + dz * dz;
                    if (dist >= 16.0 && dist < bestDist) {
                        bestDist = dist;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }
}
