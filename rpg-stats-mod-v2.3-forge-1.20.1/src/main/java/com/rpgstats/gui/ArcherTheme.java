package com.rpgstats.gui;

/** Stable resource-pack routes: one atlas row per House, one cell per specialization. */
public enum ArcherTheme {
    MARKSMAN("ARC_MARKSMAN", "marksman", 0xFFA5D3A2, "SNIPER", "DEADEYE", "BALLISTICIAN"),
    WARDEN("ARC_WARDEN", "warden", 0xFF82C69D, "BEASTMASTER", "TRAPPER", "SURVIVALIST"),
    SKIRMISHER("ARC_SKIRMISHER", "skirmisher", 0xFF98D0B7, "WINDRUNNER", "ACROBAT", "GUERRILLA"),
    ARCANE("ARC_ARCANE", "arcane", 0xFF8BBFC2, "FLAMEBOW", "FROSTBOW", "STORMBOW"),
    ARTIFICER("ARC_ARTIFICER", "artificer", 0xFFBEC184, "CROSSBOW_EXPERT", "BOMBARDIER", "ENGINEER");

    private final String house, asset;
    private final int color;
    private final String[] specializations;
    ArcherTheme(String house, String asset, int color, String... specializations) {
        this.house=house; this.asset=asset; this.color=color; this.specializations=specializations;
    }
    public int color() { return color; }
    public String asset() { return asset; }
    public String[] specializations() { return specializations.clone(); }
    public int cell(String specialization) {
        for(int i=0;i<specializations.length;i++) if(specializations[i].equals(specialization)) return i+1;
        return 0;
    }
    public static ArcherTheme forHouse(String house) {
        for(ArcherTheme theme:values()) if(theme.house.equals(house)) return theme;
        return MARKSMAN;
    }
}
