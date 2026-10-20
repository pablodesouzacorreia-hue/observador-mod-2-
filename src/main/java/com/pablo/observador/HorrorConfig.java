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

    public static final ModConfigSpec.BooleanValue ACCELERATE_EVENTS = B
            .comment("Teste: faz eventos e aparições acontecerem a cada poucos segundos, sem tempo de espera inicial.")
            .define("diagnostico.acelerarEventos", false);

    public static final ModConfigSpec.DoubleValue VANISH_SECONDS = B
            .comment("Quantos segundos o jogador precisa olhar para a criatura antes de ela reagir (some, na maioria das vezes).")
            .defineInRange("criaturas.segundosAteSumir", 1.0, 0.2, 10.0);

    public static final ModConfigSpec.BooleanValue FEAR_ENABLED = B
            .comment("Sistema de medo: pausar o jogo ou ficar parado ouvindo aumenta os sons e deixa a criatura mais ousada.")
            .define("medo.ativado", true);

    public static final ModConfigSpec.BooleanValue ANIMALS_STARE = B
            .comment("Animais e aldeões param e encaram o jogador quando a criatura está por perto.")
            .define("animais.encaramJogador", true);

    public static final ModConfigSpec.BooleanValue MESSAGES = B
            .comment("A criatura 'fala' no chat (\"eu te vejo\", \"está sozinho?\").")
            .define("mensagens.ativadas", true);

    public static final ModConfigSpec.BooleanValue COMMANDS_ENABLED = B
            .comment("Comandos de teste /observador. Em servidores públicos, desligue.")
            .define("diagnostico.comandosAtivados", true);

    public static final ModConfigSpec.BooleanValue FEAR_ENABLED = B
            .comment("Sistema de medo: pausar o jogo ou ficar parado escutando aumenta os sons e deixa a criatura mais ousada.")
            .define("medo.ativado", true);

    public static final ModConfigSpec.IntValue IDLE_SECONDS = B
            .comment("Quantos segundos parado (sem andar nem mexer a câmera) contam como 'ficar escutando'.")
            .defineInRange("medo.segundosParadoParaEscutar", 8, 3, 60);

    public static final ModConfigSpec.BooleanValue VOICE_ENABLED = B
            .comment("A criatura tenta se comunicar com uma voz distorcida dizendo 'Hi'.")
            .define("audio.vozDaCriatura", true);

    public static final ModConfigSpec.BooleanValue MESSAGES_ENABLED = B
            .comment("A criatura escreve frases no chat ('eu te vejo', 'está sozinho?').")
            .define("mensagens.ativadas", true);

    public static final ModConfigSpec.BooleanValue STARE_ENABLED = B
            .comment("Animais e outros mobs param e encaram o jogador quando a criatura está por perto.")
            .define("criaturas.mobsEncaramJogador", true);

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
