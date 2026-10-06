package com.rpgstats.stats;

public enum Stat {
    VITALIDADE("Vitalidade"),
    TENACIDADE("Tenacidade"),
    FORCA("Força"),
    DESTREZA("Destreza"),
    INTELIGENCIA("Inteligência"),
    /** Reserved legacy identifier; never allocate or display it. Keeps old enum order stable. */
    @Deprecated FE("Fé"),
    ARCANO("Arcano");

    public final String display;

    Stat(String display) {
        this.display = display;
    }

    public static Stat byName(String name) {
        if ("AGILIDADE".equalsIgnoreCase(name)) return TENACIDADE;
        for (Stat s : values()) {
            if (s != FE && s.name().equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    public static Stat[] activeValues() {
        return new Stat[]{VITALIDADE,TENACIDADE,FORCA,DESTREZA,INTELIGENCIA,ARCANO};
    }
}
