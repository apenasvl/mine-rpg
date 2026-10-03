package com.rpgstats.gui;

/** Stable resource-pack routes: one atlas row per House, one cell per specialization. */
public enum AssassinTheme {
    SHADOW("ASS_SHADOW", "shadow", 0xFF53679C, "NIGHTBLADE", "PHANTOM", "EXECUTIONER"),
    VENOM("ASS_VENOM", "venom", 0xFF708A5A, "ALCHEMIST", "PLAGUEBRINGER", "TOXICOLOGIST"),
    DUELIST("ASS_DUELIST", "duelist", 0xFF79659B, "FENCER", "BLADE_DANCER", "COUNTERBLADE"),
    SABOTEUR("ASS_SABOTEUR", "saboteur", 0xFF686876, "DEMOLITIONIST", "INFILTRATOR", "WIREMASTER"),
    MYSTIC("ASS_MYSTIC", "mystic", 0xFF66558F, "HEXKILLER", "SOULKNIFE", "VOIDWALKER");

    private final String house, asset;
    private final int color;
    private final String[] specializations;
    AssassinTheme(String house, String asset, int color, String... specializations) {
        this.house=house; this.asset=asset; this.color=color; this.specializations=specializations;
    }
    public int color() { return color; }
    public String asset() { return asset; }
    public String[] specializations() { return specializations.clone(); }
    public int cell(String specialization) {
        for(int i=0;i<specializations.length;i++) if(specializations[i].equals(specialization)) return i+1;
        return 0;
    }
    public static AssassinTheme forHouse(String house) {
        for(AssassinTheme theme:values()) if(theme.house.equals(house)) return theme;
        return SHADOW;
    }
}
