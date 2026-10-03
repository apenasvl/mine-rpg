package com.rpgstats.combat;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Estado temporário das mecânicas avançadas do Mago. Nunca substitui NBT permanente. */
public final class MageState {
    public enum School { NONE, ARCANE, FIRE, ICE, LIGHTNING, BLOOD, OCCULT, TEMPORAL, SPACE, SUMMON, HEXBLADE }
    public enum ZoneType { FLAME_WALL, STATIC_FIELD, STASIS, TIME_STOP, RUNE_CIRCLE, SPIRIT_TOTEM }
    public enum RuneType { ARCANE, EXPLOSIVE, PROTECTION, AMPLIFIER }
    public enum SummonType { FAMILIAR, GUARDIAN, GREAT, ASTRAL_BLADE, ASTRAL_SHIELD, ASTRAL_TURRET }

    public static final class TargetState {
        public float heat;
        public float frost;
        public int charge;
        public int fragilityTicks;
        public float fragilityAmp;
        public int weaknessTicks;
        public int ruinTicks;
        public int ruinPulse;
        public int bleedTicks;
        public int bleedStacks;
        public int bleedPulse;
        public int frozenTicks;
        public int reactionCooldown;
        public int markedByHuntTicks;
        public int timeLockCooldown;
        public boolean fateMarkReady;
        public int fateMarkTicks;
    }

    public static final class Zone {
        public final ZoneType type;
        public final Vec3d center;
        public final double radius;
        public int ticks;
        public final float power;
        public Zone(ZoneType type, Vec3d center, double radius, int ticks, float power) {
            this.type = type; this.center = center; this.radius = radius; this.ticks = ticks; this.power = power;
        }
    }

    public static final class Rune {
        public final Vec3d pos;
        public final RuneType type;
        public int ticks;
        public boolean triggered;
        public Rune(Vec3d pos, RuneType type, int ticks) {
            this.pos = pos; this.type = type; this.ticks = ticks;
        }
    }

    private static final Map<UUID, MageState> STATES = new HashMap<>();
    public static MageState get(UUID id) { return STATES.computeIfAbsent(id, u -> new MageState()); }
    public static void remove(UUID id) { STATES.remove(id); }

    public final Map<UUID, TargetState> targets = new HashMap<>();
    public final List<Zone> zones = new ArrayList<>();
    public final List<Rune> runes = new ArrayList<>();
    public final EnumMap<SummonType, Integer> summons = new EnumMap<>(SummonType.class);
    /** Native owned entities only; no synthetic lifetimes or virtual-summon entries. */
    public final Map<UUID, String> nativeSummons = new HashMap<>();

    public School currentSchool = School.NONE;
    public String currentNode = "";
    public boolean internalDamage;
    public boolean allowConcentration;
    public float concentrationThisCast;
    public boolean lastSpellCrit;

    public int idleTicks;
    public int spellsInWindow;
    public int spellWindowTicks;
    public int temporalRhythmCooldown;
    public String lastSpellNode = "";
    public final java.util.ArrayDeque<String> recentArcaneCategories = new java.util.ArrayDeque<>();
    public int arcaneSeals;

    public int flowUniqueCount;
    public final java.util.HashSet<String> flowRecent = new java.util.HashSet<>();
    public boolean flowReady;
    public boolean causalLoopReady;
    public int preparationTicks;
    public float manaSpentWindow;
    public int manaSpentWindowTicks;

    public int counterspellTicks;
    public float counterspellReduction;
    public int illusionCharges;
    public int illusionTicks;
    public int mirrorHallTicks;
    public int mirrorCastCounter;
    public int ghostAttackTicks;
    public int teleportBonusTicks;
    public int weavingTicks;
    public int parryTicks;
    public int blinkStrikeTicks;
    public int arcaneFormTicks;
    public int accelerationTicks;
    public int distortedTimeTicks;
    public int winterHeartTicks;
    public int iceGuardTicks;
    public int fireStepTicks;
    public int lightningStepTicks;
    public int astralGuardTicks;
    public int stormAvatarTicks;
    public int bloodEclipseTicks;
    public int instantCastTicks;
    public int temporalEchoTicks;
    public boolean temporalEchoReady;

    public Vec3d anchorPos;
    public int anchorTicks;
    public Vec3d portalA;
    public Vec3d portalB;
    public int portalTicks;

    public Vec3d temporalMarkPos;
    public String temporalMarkDimension = "";
    public float temporalMarkHealth;
    public float temporalMarkMana;
    public int temporalMarkTicks;

    public Vec3d rewritePos;
    public String rewriteDimension = "";
    public float rewriteHealth;
    public float rewriteMana;
    public int rewriteTicks;

    public int spiritLifeTicks;
    public int spiritEarthTicks;
    public int spiritHuntTicks;
    public int spiritTotemTicks;
    public int summonAssaultTicks;
    public UUID summonFocusTarget;
    public int summonFocusTicks;
    public float summonManaThisSecond;
    public boolean statsDirty;
    public int ephemeralArmyTicks;
    public int astralArsenalTicks;

    public int runeMode;
    public int spiritMode;
    public int astralFormation;
    public int hexElement;

    public int spellbladeStage;
    public int spellbladeStacks;
    public int spellbladeTicks;
    public int momentumStacks;
    public int momentumTicks;

    public UUID temporalEchoTarget;
    public float temporalEchoDamage;
    public School temporalEchoSchool = School.ARCANE;
    public int temporalEchoDelay;
    public float temporalDebt;
    public int temporalDebtTicks;
    public int temporalDebtPulse;
    public int temporalExhaustionTicks;

    public int tickCounter;

    public TargetState target(UUID id) { return targets.computeIfAbsent(id, u -> new TargetState()); }

    public void beginCast(String node, School school, boolean concentration) {
        currentNode = node == null ? "" : node;
        currentSchool = school == null ? School.ARCANE : school;
        allowConcentration = concentration;
        concentrationThisCast = 0f;
        lastSpellCrit = false;
        idleTicks = 0;
    }

    public void endCast() {
        currentNode = "";
        currentSchool = School.NONE;
        allowConcentration = false;
        concentrationThisCast = 0f;
        lastSpellCrit = false;
    }
}
