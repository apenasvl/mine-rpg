package com.rpgstats.stats;

public enum Stat {
    VITALIDADE("Vitalidade"),
    TENACIDADE("Tenacidade"),
    FORCA("Força"),
    DESTREZA("Destreza"),
    INTELIGENCIA("Inteligência"),
    FE("Fé"),
    ARCANO("Arcano");

    public final String display;

    Stat(String display) {
        this.display = display;
    }

    public static Stat byName(String name) {
        if ("AGILIDADE".equalsIgnoreCase(name)) return TENACIDADE;
        for (Stat s : values()) {
            if (s.name().equalsIgnoreCase(name)) return s;
        }
        return null;
    }
}
