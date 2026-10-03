package com.rpgstats.gui;

/** Stable resource-pack routes: one atlas per House, one cell per specialization. */
public enum MageTheme {
    ELEMENTAL("MAGE_ELEMENTAL", "elemental", 0xFFF3A58E, "PYROMANCER", "CRYOMANCER", "STORMCALLER"),
    ARCANA("MAGE_ARCANA", "arcana", 0xFFB9ACF2, "RUNIST", "ILLUSIONIST", "TELEMANCER"),
    CONJURATION("MAGE_CONJURATION", "conjuration", 0xFF91D6BE, "CONJURER", "ANIMIST", "ASTRAL_FORGER"),
    OCCULT("MAGE_OCCULT", "occult", 0xFFD89ABF, "BLOODMANCER", "CURSEWEAVER", "HEXBLADE"),
    TEMPORAL("MAGE_TEMPORAL", "temporal", 0xFF8FCEEE, "ACCELERATOR", "STAGNATOR", "REVERSER");

    private final String house, asset;
    private final int color;
    private final String[] specializations;
    MageTheme(String house, String asset, int color, String... specializations) {
        this.house=house; this.asset=asset; this.color=color; this.specializations=specializations;
    }
    public int color() { return color; }
    public String asset() { return asset; }
    public String[] specializations() { return specializations.clone(); }
    public int cell(String specialization) {
        for(int i=0;i<specializations.length;i++) if(specializations[i].equals(specialization)) return i+1;
        return 0;
    }
    public static MageTheme forHouse(String house) {
        for(MageTheme theme:values()) if(theme.house.equals(house)) return theme;
        return ARCANA;
    }
}
