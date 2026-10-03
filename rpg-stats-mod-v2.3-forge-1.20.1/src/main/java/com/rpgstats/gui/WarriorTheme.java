package com.rpgstats.gui;

/** Stable resource-pack routes: one atlas row per House, one cell per specialization. */
public enum WarriorTheme {
    VANGUARD("WAR_VANGUARD", "vanguard", 0xFF985C5A, "BULWARK", "WARLORD", "JUGGERNAUT"),
    BERSERKER("WAR_BERSERKER", "berserker", 0xFFAD414B, "BLOOD_REAVER", "RAGEBORN", "PAIN_COLOSSUS"),
    WEAPONMASTER("WAR_WEAPONMASTER", "weaponmaster", 0xFFA36A46, "BLADEMASTER", "DUEL_MASTER", "TITAN_MAULER"),
    RUNIC("WAR_RUNIC", "runic", 0xFF79588F, "RUNE_KNIGHT", "SPELLBREAKER", "STORMBLADE"),
    COMMANDER("WAR_COMMANDER", "commander", 0xFF8D515D, "BANNER_LORD", "TACTICIAN", "IRON_GUARD");

    private final String house, asset;
    private final int color;
    private final String[] specializations;
    WarriorTheme(String house, String asset, int color, String... specializations) {
        this.house=house; this.asset=asset; this.color=color; this.specializations=specializations;
    }
    public int color() { return color; }
    public String asset() { return asset; }
    public String[] specializations() { return specializations.clone(); }
    public int cell(String specialization) {
        for(int i=0;i<specializations.length;i++) if(specializations[i].equals(specialization)) return i+1;
        return 0;
    }
    public static WarriorTheme forHouse(String house) {
        for(WarriorTheme theme:values()) if(theme.house.equals(house)) return theme;
        return VANGUARD;
    }
}
