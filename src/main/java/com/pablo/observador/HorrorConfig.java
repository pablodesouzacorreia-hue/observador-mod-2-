package com.pablo.observador;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuração do mod (arquivo observador-common.toml na pasta config/).
 * Cada opção abaixo será lida pelos sistemas correspondentes nas próximas
 * etapas; nada aqui é decorativo, mas por enquanto só a opção de diagnóstico
 * já é usada pelo código.
 */
public final class HorrorConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue HORROR_INTENSITY = B
            .comment("Intensidade geral do terror (0.0 = suave, 1.0 = padrão, 2.0 = extremo).")
            .defineInRange("geral.intensidadeTerror", 1.0, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue APPEARANCE_FREQUENCY = B
            .comment("Multiplicador da frequência de aparições das criaturas.")
            .defineInRange("criaturas.frequenciaAparicoes", 1.0, 0.0, 3.0);

    public static final ModConfigSpec.DoubleValue JUMPSCARE_FREQUENCY = B
            .comment("Multiplicador da frequência de sustos fortes. 0 desativa jumpscares.")
            .defineInRange("sustos.frequenciaJumpscares", 0.5, 0.0, 3.0);

    public static final ModConfigSpec.DoubleValue AGGRESSION = B
            .comment("Agressividade das entidades (afeta duração e velocidade das perseguições).")
            .defineInRange("criaturas.agressividade", 1.0, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue SOUND_INTENSITY = B
            .comment("Volume relativo dos efeitos sonoros do mod.")
            .defineInRange("audio.intensidadeSons", 1.0, 0.0, 1.5);

    public static final ModConfigSpec.BooleanValue SANITY_ENABLED = B
            .comment("Ativa o sistema de sanidade.")
            .define("sanidade.ativada", true);

    public static final ModConfigSpec.BooleanValue PSYCHOLOGICAL_EVENTS = B
            .comment("Ativa eventos psicológicos (passos, batidas na porta, sussurros etc.).")
            .define("eventos.psicologicos", true);

    public static final ModConfigSpec.BooleanValue MULTIPLAYER_SHARED_EVENTS = B
            .comment("Em servidores, permite eventos que ameaçam o grupo inteiro.")
            .define("multiplayer.eventosCompartilhados", true);

    public static final ModConfigSpec.BooleanValue REDUCE_FLASHES = B
            .comment("Acessibilidade: reduz flashes, tremores e efeitos visuais intensos.")
            .define("acessibilidade.reduzirFlashes", false);

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = B
            .comment("Diagnóstico: escreve no log o que os sistemas do mod estão decidindo.")
            .define("diagnostico.logDetalhado", false);

    public static final ModConfigSpec SPEC = B.build();

    private HorrorConfig() {}

    public static String summary() {
        return "intensidade=" + HORROR_INTENSITY.get()
                + ", aparicoes=" + APPEARANCE_FREQUENCY.get()
                + ", jumpscares=" + JUMPSCARE_FREQUENCY.get()
                + ", sanidade=" + SANITY_ENABLED.get()
                + ", eventos=" + PSYCHOLOGICAL_EVENTS.get()
                + ", reduzirFlashes=" + REDUCE_FLASHES.get();
    }
}
