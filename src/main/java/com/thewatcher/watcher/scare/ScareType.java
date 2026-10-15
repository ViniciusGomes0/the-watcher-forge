package com.thewatcher.watcher.scare;

/**
 * Os tipos de susto que o Watcher pode causar.
 *
 * Cada tipo tem uma fase mínima: a fase 1 (Observação) nunca assusta, então
 * o menor valor usado é 2. O susto de login é exclusivo da fase 4.
 */
public enum ScareType {

    /** Mineração: após 3+ min sem olhar para trás → aparece atrás com screech. */
    MINING(2),
    /** Cama: ao acordar, 30% de chance → está na beira da cama olhando. */
    BED(2),
    /** Água: ao emergir e olhar para cima → está na superfície por 1 frame. */
    WATER(2),
    /** Corredor: em túnel 1-2 blocos → aparece no fim e recua para a escuridão. */
    CORRIDOR(2),
    /** Portal: ao atravessar Nether/End → aparece 1 frame na animação. */
    PORTAL(2),
    /** Login: primeiros 30s de sessão (fase 4) → aparece e desaparece. */
    LOGIN(4);

    private final int minPhase;

    ScareType(int minPhase) {
        this.minPhase = minPhase;
    }

    /** Fase mínima em que este susto pode ocorrer. */
    public int minPhase() {
        return minPhase;
    }

    /** Se este susto está disponível na fase informada. */
    public boolean availableInPhase(int phase) {
        return phase >= minPhase;
    }
}
