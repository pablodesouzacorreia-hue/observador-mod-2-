package com.pablo.observador.sound;

import com.pablo.observador.HorrorConfig;
import com.pablo.observador.ObservadorMod;
import com.pablo.observador.system.FearManager;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sons originais do mod (sintetizados; ver assets/observador/sounds e sounds.json). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, ObservadorMod.MOD_ID);

    public static final Supplier<SoundEvent> STEP = reg("step");
    public static final Supplier<SoundEvent> KNOCK = reg("knock");
    public static final Supplier<SoundEvent> CREAK = reg("creak");
    public static final Supplier<SoundEvent> BREATH = reg("breath");
    public static final Supplier<SoundEvent> WHISPER = reg("whisper");
    public static final Supplier<SoundEvent> DIG = reg("dig");
    public static final Supplier<SoundEvent> SCREAM = reg("scream");
    public static final Supplier<SoundEvent> STINGER = reg("stinger");
    public static final Supplier<SoundEvent> CHASE = reg("chase");
    public static final Supplier<SoundEvent> JUMP = reg("jump");
    public static final Supplier<SoundEvent> VOICE = reg("voice");
    public static final Supplier<SoundEvent> HEARTBEAT = reg("heartbeat");

    private ModSounds() {}

    private static Supplier<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(ObservadorMod.MOD_ID, name)));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }

    /** Toca um som posicional para todos os jogadores por perto, respeitando a configuração de volume. */
    public static void play(Level level, double x, double y, double z, Supplier<SoundEvent> sound,
                            SoundSource source, float volume, float pitch) {
        // O medo do jogador (pausa, ficar parado escutando) deixa tudo mais alto.
        float fear = FearManager.boost(level, x, y, z);
        float scaled = (float) (volume * HorrorConfig.SOUND_INTENSITY.get() * fear);
        if (scaled <= 0.0F) {
            return;
        }
        level.playSound(null, x, y, z, sound.get(), source, scaled, pitch);
    }
}
