package com.pablo.observador;

/**
 * Diagnóstico simples: mensagens só aparecem no log quando
 * diagnostico.logDetalhado = true na configuração.
 * As próximas etapas usam isto para registrar decisões da IA e dos eventos.
 */
public final class Diagnostics {
    private Diagnostics() {}

    public static void log(String message, Object... args) {
        if (HorrorConfig.SPEC.isLoaded() && HorrorConfig.DEBUG_LOGGING.get()) {
            ObservadorMod.LOGGER.info("[Observador/diag] " + message, args);
        }
    }
}
