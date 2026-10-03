package com.rpgstats.compat.irons;

import com.rpgstats.combat.MageBalance;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Balance layer for Iron's Spells when RPG Stats progression is applied on top.
 *
 * Iron's spells differ heavily in hit count, persistence, sustain, weapon scaling and native burst.
 * A single global multiplier makes low-risk spells feel weak while turning a handful of high-volume
 * spells into boss deletions. This table therefore balances five independent axes per spell shape:
 * native damage, retained RPG bonus, final total multiplier, mana-reduction benefit and CDR benefit.
 *
 * A second guard limits single-target burst/DPS. This is intentionally per target, so AoE spells keep
 * their identity against groups while a 5-80 projectile spell cannot multiply its full RPG damage into
 * one boss. The current playtest reference is the maxed Warrior active hit with an iron sword (~28).
 */
public final class IronsSpellBalance {
    public enum Profile {
        STANDARD(1.00f, 1.00f, 2.10f, 1.00f, 1.00f),
        BURST(1.00f, 0.85f, 1.90f, 0.90f, 0.85f),
        SPAM(0.95f, 0.72f, 1.70f, 0.80f, 0.70f),
        HEAVY_BURST(0.95f, 0.55f, 1.55f, 0.70f, 0.65f),
        EXTREME(0.85f, 0.35f, 1.35f, 0.55f, 0.55f),
        MULTIHIT(0.90f, 0.60f, 1.55f, 0.75f, 0.70f),
        CONTINUOUS(0.90f, 0.50f, 1.45f, 0.75f, 0.70f),
        PERSISTENT(0.90f, 0.45f, 1.40f, 0.70f, 0.65f),
        SUMMON(0.95f, 0.55f, 1.50f, 0.80f, 0.70f),
        SUSTAIN(0.90f, 0.30f, 1.30f, 0.75f, 0.70f),
        WEAPON_DERIVED(0.95f, 0.25f, 1.25f, 0.85f, 0.80f),
        MOBILITY_DAMAGE(0.95f, 0.50f, 1.35f, 0.90f, 0.85f),
        UTILITY(1.00f, 0.00f, 1.00f, 0.90f, 0.80f),
        UNCLASSIFIED(0.90f, 0.50f, 1.40f, 0.75f, 0.70f);

        private final float nativeDamageMultiplier;
        private final float rpgBonusRetention;
        private final float maxTotalMultiplier;
        private final float manaReductionRetention;
        private final float cooldownReductionRetention;

        Profile(float nativeDamageMultiplier, float rpgBonusRetention, float maxTotalMultiplier,
                float manaReductionRetention, float cooldownReductionRetention) {
            this.nativeDamageMultiplier = nativeDamageMultiplier;
            this.rpgBonusRetention = rpgBonusRetention;
            this.maxTotalMultiplier = maxTotalMultiplier;
            this.manaReductionRetention = manaReductionRetention;
            this.cooldownReductionRetention = cooldownReductionRetention;
        }

        public float nativeDamageMultiplier() { return nativeDamageMultiplier; }
        public float rpgBonusRetention() { return rpgBonusRetention; }
        public float maxTotalMultiplier() { return maxTotalMultiplier; }
        public float manaReductionRetention() { return manaReductionRetention; }
        public float cooldownReductionRetention() { return cooldownReductionRetention; }
    }

    private record Rule(Profile profile, float nativeDamageMultiplier, float rpgBonusRetention,
                        float maxTotalMultiplier, float manaReductionRetention,
                        float cooldownReductionRetention) {}

    /**
     * Per-target limiter. perHitCap prevents a single Iron spell hit from overtaking the Warrior
     * reference burst. windowCap/windowTicks form a leaky bucket for volleys, recasts and DoTs.
     * A zero window disables cumulative limiting for genuine single-hit spells.
     */
    private record DamageBudget(float perHitCap, float windowCap, int windowTicks) {
        boolean hasWindow() { return windowCap > 0f && windowTicks > 0; }
    }

    private record DamageBudgetKey(UUID casterId, UUID targetId, String spellPath) {}

    private static final class DamageBudgetState {
        float spent;
        long lastTick;

        DamageBudgetState(long tick) {
            this.lastTick = tick;
        }
    }

    /** Warrior lvl 50 + active ability + iron sword measured by the playtest. */
    public static final float WARRIOR_IRON_SWORD_BURST_REFERENCE = 28f;
    /** Iron Mage single hits stay just below that benchmark; AoE still wins through target count. */
    public static final float MAGE_SINGLE_HIT_CEILING = 27f;
    private static final int MAX_BUDGET_STATES = 4096;

    private static final Map<DamageBudgetKey, DamageBudgetState> DAMAGE_BUDGET_STATES =
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<DamageBudgetKey, DamageBudgetState> eldest) {
                    return size() > MAX_BUDGET_STATES;
                }
            };

    // Full catalog used by the current 1.20.1 integration (legacy + expanded spell line).
    private static final Set<String> STANDARD = Set.of(
            "spectral_hammer", "guiding_bolt", "acid_orb"
    );

    private static final Set<String> BURST = Set.of(
            "lob_creeper", "fireball", "magma_bomb", "heat_surge", "scorch",
            "cone_of_cold", "frostwave", "lightning_lance", "shockwave", "poison_splash",
            "stomp", "gluttony", "ice_tomb", "frostbite", "volt_strike", "fire_arrow", "poison_arrow"
    );

    private static final Set<String> SPAM = Set.of(
            "magic_missile", "firecracker", "gust", "firebolt", "snowball",
            "wither_skull", "icicle", "wisp"
    );

    private static final Set<String> HEAVY_BURST = Set.of(
            "heartstop", "sacrifice", "lightning_bolt"
    );

    private static final Set<String> EXTREME = Set.of(
            "magic_arrow", "sonic_boom", "eldritch_blast", "arrow_volley", "starfall"
    );

    private static final Set<String> MULTIHIT = Set.of(
            "chain_creeper", "flaming_barrage", "chain_lightning", "ball_lightning",
            "fang_swirl", "fang_strike"
    );

    private static final Set<String> CONTINUOUS = Set.of(
            "dragon_breath", "fire_breath", "sunbeam", "ray_of_frost", "electrocute",
            "poison_breath"
    );

    private static final Set<String> PERSISTENT = Set.of(
            "black_hole", "fang_ward", "blaze_storm", "wall_of_fire",
            "thunderstorm", "blight", "firefly_swarm", "earthquake", "sculk_tentacles",
            "gravity_fissure", "ice_spikes", "blizzard"
    );

    private static final Set<String> SUMMON = Set.of(
            "raise_dead", "summon_vex", "summon_polar_bear", "summon_swords"
    );

    private static final Set<String> SUSTAIN = Set.of(
            "acupuncture", "blood_needles", "blood_slash", "devour", "ray_of_siphoning"
    );

    // These include weapon/projectile damage in their native formula; RPG magic scaling must not double-dip.
    private static final Set<String> WEAPON_DERIVED = Set.of(
            "echoing_strikes", "telekinesis", "throw", "shadow_slash",
            "flaming_strike", "divine_smite", "raise_hell"
    );

    private static final Set<String> MOBILITY_DAMAGE = Set.of(
            "blood_step", "burning_dash", "frost_step", "charge"
    );

    private static final Set<String> UTILITY = Set.of(
            "counterspell", "evasion", "teleport", "summon_ender_chest", "recall", "portal",
            "invisibility", "shield", "summon_horse", "slow", "wololo",
            "angel_wings", "blessing_of_life", "cloud_of_regeneration", "fortify",
            "greater_heal", "healing_circle", "heal", "haste", "cleanse",
            "ice_block", "ascension", "root", "spider_aspect", "oakskin",
            "abyssal_shroud", "planar_sight", "arcane_shackle", "scapegoat",
            "touch_dig", "pocket_dimension"
    );

    // Exact scaling overrides for the most dangerous interactions found during the source audit.
    private static final Map<String, Rule> OVERRIDES = Map.ofEntries(
            Map.entry("magic_arrow", new Rule(Profile.EXTREME, 0.90f, 0.40f, 1.35f, 0.60f, 0.60f)),
            Map.entry("sonic_boom", new Rule(Profile.EXTREME, 0.85f, 0.30f, 1.25f, 0.50f, 0.50f)),
            Map.entry("eldritch_blast", new Rule(Profile.EXTREME, 0.75f, 0.25f, 1.20f, 0.50f, 0.50f)),
            Map.entry("arrow_volley", new Rule(Profile.EXTREME, 0.80f, 0.25f, 1.20f, 0.55f, 0.55f)),
            Map.entry("starfall", new Rule(Profile.EXTREME, 0.85f, 0.30f, 1.25f, 0.55f, 0.55f)),
            Map.entry("ray_of_siphoning", new Rule(Profile.SUSTAIN, 0.90f, 0.25f, 1.25f, 0.70f, 0.65f)),
            Map.entry("devour", new Rule(Profile.SUSTAIN, 0.92f, 0.30f, 1.30f, 0.70f, 0.65f)),
            Map.entry("sacrifice", new Rule(Profile.HEAVY_BURST, 0.90f, 0.45f, 1.45f, 0.70f, 0.65f)),
            Map.entry("echoing_strikes", new Rule(Profile.WEAPON_DERIVED, 1.00f, 0.20f, 1.20f, 0.80f, 0.75f)),
            Map.entry("shadow_slash", new Rule(Profile.WEAPON_DERIVED, 1.00f, 0.20f, 1.20f, 0.80f, 0.75f)),
            Map.entry("flaming_strike", new Rule(Profile.WEAPON_DERIVED, 1.00f, 0.20f, 1.20f, 0.80f, 0.75f)),
            Map.entry("divine_smite", new Rule(Profile.WEAPON_DERIVED, 1.00f, 0.20f, 1.20f, 0.80f, 0.75f)),
            Map.entry("raise_hell", new Rule(Profile.WEAPON_DERIVED, 0.90f, 0.20f, 1.15f, 0.70f, 0.65f))
    );

    /**
     * Exact per-target budgets for mechanics where projectile/recast count matters more than broad profile.
     * Example: Flaming Barrage has 5 homing projectiles; Eldritch Blast has 3-7 blasts; Arrow Volley can
     * create dozens of arrows. Those are allowed to remain excellent AoE/multi-hit tools, but not to stack
     * five to eighty fully-scaled RPG hits into the same target.
     */
    private static final Map<String, DamageBudget> DAMAGE_BUDGET_OVERRIDES = Map.ofEntries(
            Map.entry("acupuncture", new DamageBudget(2.25f, 20f, 30)),
            Map.entry("blood_needles", new DamageBudget(4.5f, 22f, 30)),
            Map.entry("blood_slash", new DamageBudget(15f, 18f, 20)),
            Map.entry("devour", new DamageBudget(15f, 18f, 20)),
            Map.entry("ray_of_siphoning", new DamageBudget(6f, 14f, 20)),
            Map.entry("magic_arrow", new DamageBudget(26.5f, 0f, 0)),
            Map.entry("sonic_boom", new DamageBudget(MAGE_SINGLE_HIT_CEILING, 0f, 0)),
            Map.entry("eldritch_blast", new DamageBudget(4.5f, 27f, 40)),
            Map.entry("arrow_volley", new DamageBudget(1.5f, 27f, 40)),
            Map.entry("starfall", new DamageBudget(5f, 18f, 20)),
            Map.entry("chain_creeper", new DamageBudget(5f, 24f, 40)),
            Map.entry("flaming_barrage", new DamageBudget(5.4f, 27f, 40)),
            Map.entry("fang_strike", new DamageBudget(7f, 24f, 30)),
            Map.entry("fang_swirl", new DamageBudget(8f, 24f, 30)),
            Map.entry("ball_lightning", new DamageBudget(7f, 18f, 20)),
            Map.entry("chain_lightning", new DamageBudget(18f, 0f, 0)),
            Map.entry("ice_spikes", new DamageBudget(5f, 26f, 30)),
            Map.entry("raise_hell", new DamageBudget(7f, 27f, 40)),
            Map.entry("echoing_strikes", new DamageBudget(6f, 26f, 40)),
            Map.entry("flaming_strike", new DamageBudget(22f, 0f, 0)),
            Map.entry("divine_smite", new DamageBudget(24f, 0f, 0)),
            Map.entry("shadow_slash", new DamageBudget(20f, 0f, 0)),
            Map.entry("throw", new DamageBudget(20f, 0f, 0)),
            Map.entry("fire_arrow", new DamageBudget(20f, 25f, 20)),
            Map.entry("poison_arrow", new DamageBudget(14f, 20f, 20)),
            Map.entry("fireball", new DamageBudget(24f, 0f, 0)),
            Map.entry("lightning_lance", new DamageBudget(25f, 0f, 0)),
            Map.entry("lightning_bolt", new DamageBudget(25.5f, 0f, 0)),
            Map.entry("sacrifice", new DamageBudget(MAGE_SINGLE_HIT_CEILING, 0f, 0))
    );

    /**
     * Applies the spell rule after all RPG offensive layers have produced {@code rpgResult}.
     * Native damage is normalized first; only the positive RPG-added part is retained by profile.
     * Existing penalties below native damage are preserved and still receive the native normalization.
     */
    public static float applyRpgScaling(String spellId, float nativeAmount, float rpgResult) {
        if (nativeAmount <= 0f) return Math.max(0f, rpgResult);

        Rule rule = ruleFor(spellId);
        if (rpgResult <= nativeAmount) {
            return Math.max(0f, rpgResult * rule.nativeDamageMultiplier());
        }

        float nativeBalanced = nativeAmount * rule.nativeDamageMultiplier();
        float rpgAdded = (rpgResult - nativeAmount) * rule.rpgBonusRetention();
        float result = nativeBalanced + Math.max(0f, rpgAdded);
        float max = nativeAmount * rule.maxTotalMultiplier();
        return Math.max(0f, Math.min(max, result));
    }

    /** Normalize conditional/echo bonuses while retaining earned attribute/level/core growth once. */
    public static float applyRpgScaling(String spellId, float nativeAmount, float rpgResult,
                                       float persistentMultiplier) {
        if (ruleFor(spellId).profile() == Profile.UTILITY)
            return applyRpgScaling(spellId, nativeAmount, rpgResult);
        float progression = persistentMultiplier(persistentMultiplier);
        return applyRpgScaling(spellId, nativeAmount, rpgResult / progression) * progression;
    }

    /** Matches the bounded Mage persistent curve, including the Occult Arcane-stat supplement. */
    public static float persistentMultiplier(float value) {
        return Float.isFinite(value) ? Math.max(1f, Math.min(1f + MageBalance.MAGE_PERSISTENT_DAMAGE_CAP, value)) : 1f;
    }

    /** Earned Bloodmancer identity; the caller must also verify Iron's native Blood school. */
    public static float bloodmancerMultiplier(String spellId, int level, int intelligence,
                                              boolean hemomancy, boolean playerTarget) {
        Profile profile = profileFor(spellId);
        if (!hemomancy || playerTarget || level < 25 || profile == Profile.UTILITY
                || profile == Profile.SUMMON || profile == Profile.WEAPON_DERIVED
                || profile == Profile.MOBILITY_DAMAGE) return 1f;
        float progress = Math.max(0f, Math.min(1f, (level - 25f) / 25f));
        float intellect = Math.max(0f, Math.min(1f, intelligence / 50f));
        return 1f + (.15f + .35f * progress) * intellect;
    }

    /** RPG mana-efficiency still matters, but expensive/high-volume spells keep more of their native cost. */
    public static float balanceManaReduction(String spellId, float reduction) {
        return Math.max(0f, reduction) * ruleFor(spellId).manaReductionRetention();
    }

    /** RPG CDR still matters, but high-volume and utility loops cannot receive the full generic CDR budget. */
    public static float balanceCooldownReduction(String spellId, float reduction) {
        return Math.max(0f, reduction) * ruleFor(spellId).cooldownReductionRetention();
    }

    /**
     * Final single-target guard. A leaky bucket avoids ugly fixed-window spikes: damage budget slowly
     * replenishes instead of resetting all at once. Separate target keys preserve AoE identity.
     */
    public static synchronized float applyTargetBudget(String spellId, UUID casterId, UUID targetId,
                                                       long tick, float amount) {
        return applyTargetBudget(spellId, casterId, targetId, tick, amount, 1f);
    }

    /** Same bucket/key at every progression level; no budget reset when a ceiling changes. */
    public static synchronized float applyTargetBudget(String spellId, UUID casterId, UUID targetId,
                                                       long tick, float amount, float bossBudgetScale) {
        if (amount <= 0f) return 0f;
        DamageBudget budget = damageBudgetFor(spellId);
        float scale = Float.isFinite(bossBudgetScale) ? Math.max(1f, Math.min(3f, bossBudgetScale)) : 1f;
        float windowCap = budget.windowCap() * scale;
        float hit = Math.min(amount, budget.perHitCap() * scale);
        if (!budget.hasWindow() || hit <= 0f) return Math.max(0f, hit);

        DamageBudgetKey key = new DamageBudgetKey(casterId, targetId, spellPath(spellId));
        DamageBudgetState state = DAMAGE_BUDGET_STATES.get(key);
        if (state == null || tick < state.lastTick || tick - state.lastTick > budget.windowTicks() * 4L) {
            state = new DamageBudgetState(tick);
            DAMAGE_BUDGET_STATES.put(key, state);
        }

        long elapsed = Math.max(0L, tick - state.lastTick);
        if (elapsed > 0L) {
            float recovered = windowCap * (elapsed / (float) budget.windowTicks());
            state.spent = Math.max(0f, state.spent - recovered);
        }
        state.lastTick = tick;

        float available = Math.max(0f, windowCap - state.spent);
        float accepted = Math.min(hit, available);
        state.spent += accepted;
        return Math.max(0f, accepted);
    }

    public static synchronized void clearDamageBudget(UUID casterId) {
        DAMAGE_BUDGET_STATES.keySet().removeIf(key -> key.casterId().equals(casterId));
    }

    public static float perHitCap(String spellId) {
        return damageBudgetFor(spellId).perHitCap();
    }

    public static float targetWindowCap(String spellId) {
        return damageBudgetFor(spellId).windowCap();
    }

    public static int targetWindowTicks(String spellId) {
        return damageBudgetFor(spellId).windowTicks();
    }

    public static Profile profileFor(String spellId) {
        return ruleFor(spellId).profile();
    }

    public static boolean isAuditedIronSpell(String spellId) {
        String path = spellPath(spellId);
        return STANDARD.contains(path) || BURST.contains(path) || SPAM.contains(path)
                || HEAVY_BURST.contains(path) || EXTREME.contains(path)
                || MULTIHIT.contains(path) || CONTINUOUS.contains(path)
                || PERSISTENT.contains(path) || SUMMON.contains(path) || SUSTAIN.contains(path)
                || WEAPON_DERIVED.contains(path) || MOBILITY_DAMAGE.contains(path) || UTILITY.contains(path);
    }

    private static DamageBudget damageBudgetFor(String spellId) {
        String path = spellPath(spellId);
        DamageBudget override = DAMAGE_BUDGET_OVERRIDES.get(path);
        if (override != null) return override;

        return switch (ruleFor(path).profile()) {
            case STANDARD -> new DamageBudget(25.5f, 0f, 0);
            case BURST -> new DamageBudget(24f, 0f, 0);
            case SPAM -> new DamageBudget(12f, 18f, 20);
            case HEAVY_BURST -> new DamageBudget(MAGE_SINGLE_HIT_CEILING, 0f, 0);
            case EXTREME -> new DamageBudget(MAGE_SINGLE_HIT_CEILING, 0f, 0);
            case MULTIHIT -> new DamageBudget(8f, 26f, 30);
            case CONTINUOUS -> new DamageBudget(8f, 18f, 20);
            case PERSISTENT -> new DamageBudget(8f, 18f, 20);
            case SUMMON -> new DamageBudget(10f, 24f, 20);
            case SUSTAIN -> new DamageBudget(8f, 18f, 20);
            case WEAPON_DERIVED -> new DamageBudget(22f, 26f, 20);
            case MOBILITY_DAMAGE -> new DamageBudget(20f, 0f, 0);
            case UTILITY -> new DamageBudget(18f, 0f, 0);
            case UNCLASSIFIED -> new DamageBudget(20f, 24f, 20);
        };
    }

    private static Rule ruleFor(String spellId) {
        String path = spellPath(spellId);
        Rule override = OVERRIDES.get(path);
        if (override != null) return override;

        Profile profile;
        if (STANDARD.contains(path)) profile = Profile.STANDARD;
        else if (BURST.contains(path)) profile = Profile.BURST;
        else if (SPAM.contains(path)) profile = Profile.SPAM;
        else if (HEAVY_BURST.contains(path)) profile = Profile.HEAVY_BURST;
        else if (EXTREME.contains(path)) profile = Profile.EXTREME;
        else if (MULTIHIT.contains(path)) profile = Profile.MULTIHIT;
        else if (CONTINUOUS.contains(path)) profile = Profile.CONTINUOUS;
        else if (PERSISTENT.contains(path)) profile = Profile.PERSISTENT;
        else if (SUMMON.contains(path)) profile = Profile.SUMMON;
        else if (SUSTAIN.contains(path)) profile = Profile.SUSTAIN;
        else if (WEAPON_DERIVED.contains(path)) profile = Profile.WEAPON_DERIVED;
        else if (MOBILITY_DAMAGE.contains(path)) profile = Profile.MOBILITY_DAMAGE;
        else if (UTILITY.contains(path)) profile = Profile.UTILITY;
        else profile = Profile.UNCLASSIFIED;

        return new Rule(profile, profile.nativeDamageMultiplier(), profile.rpgBonusRetention(),
                profile.maxTotalMultiplier(), profile.manaReductionRetention(),
                profile.cooldownReductionRetention());
    }

    private static String spellPath(String spellId) {
        if (spellId == null) return "";
        int separator = spellId.indexOf(':');
        return separator >= 0 ? spellId.substring(separator + 1) : spellId;
    }

    private IronsSpellBalance() {}
}
