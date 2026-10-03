#!/usr/bin/env python3
"""Regression contract for class mechanics that previously passed structural audits but failed at runtime."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/rpgstats"

combat = (JAVA / "combat/CombatHandler.java").read_text(encoding="utf-8")
mechanics = (JAVA / "combat/ClassMechanics.java").read_text(encoding="utf-8")
assassin = (JAVA / "combat/AssassinSpecializationHandler.java").read_text(encoding="utf-8")
warrior = (JAVA / "combat/WarriorSpecializationHandler.java").read_text(encoding="utf-8")
mage = (JAVA / "combat/MageCombatHandler.java").read_text(encoding="utf-8")
irons = (JAVA / "compat/irons/IronsSpellsCompat.java").read_text(encoding="utf-8")
proc_queue = (JAVA / "combat/ProcDamageQueue.java").read_text(encoding="utf-8")
mixin = (JAVA / "mixin/LivingEntityDamageMixin.java").read_text(encoding="utf-8")
forge_events = (JAVA / "forge/ForgeEvents.java").read_text(encoding="utf-8")

errors: list[str] = []

def require(ok: bool, message: str) -> None:
    if not ok:
        errors.append(message)

# Class active mutations must survive the final save (Berserker spends Fury inside ClassMechanics.activate).
require("class-specific resource amount" in combat,
        "CombatHandler can regress to reloading PlayerStats after a class active")
require("ClassMechanics.consumePreparedHit(player, stats, projectile, magic)" in combat,
        "confirmed-hit prepared-state finalization is not wired")
require("WarriorSpecializationHandler.onConfirmedMeleeHit" in combat,
        "Warrior specialization payoffs are not wired after confirmed damage")
require("AssassinSpecializationHandler.onConfirmedMeleeHit" in combat,
        "Assassin specialization cleanup is not wired after confirmed damage")

# Prepared attacks must only be consumed by an attack type that can use them.
require("stats.clazz == RPGClass.GUERREIRO && !projectile && !magic" in mechanics,
        "Warrior prepared-hit type gate missing")
require("stats.clazz == RPGClass.ARQUEIRO && projectile && !magic" in mechanics,
        "Archer prepared-shot type gate missing")
require("stats.clazz == RPGClass.ASSASSINO && !projectile && !magic" in mechanics,
        "Assassin prepared-hit type gate missing")

# LivingHurtEvent LOWEST still happens before our LivingEntity.damage RETURN mixin.
require("public static void onConfirmedMeleeHit" in assassin,
        "Assassin confirmed-hit lifecycle hook missing")
require("@SubscribeEvent(priority = EventPriority.LOWEST)\n    public static void afterClassPipeline" not in assassin,
        "Assassin still clears Opening from LivingHurtEvent LOWEST")
require("public static void onConfirmedMeleeHit" in warrior,
        "Warrior confirmed-hit lifecycle hook missing")
require("case PAIN_COLOSSUS -> painColossus" in warrior,
        "Pain Colossus payoff missing from confirmed Warrior lifecycle")

# Borrowed House effects are already scaled by HouseRules; Mage must sum them once, not twice.
require('sumPassive(stats.unlockedNodes, "spell_lifesteal");' in mage,
        "Mage spell lifesteal is not summing primary + scaled borrowed House nodes")
require("borrowedLifesteal" not in irons,
        "Iron spell hit still double-applies borrowed Mage lifesteal")

# Same-target proc damage must not be nested inside the original vanilla damage call.
require("SAME_TARGET_DELAY_TICKS = 11" in proc_queue and "queueSameTarget" in proc_queue,
        "deferred same-target proc queue is missing")
require("ProcDamageQueue.tick(server)" in forge_events and "ProcDamageQueue.clear()" in forge_events,
        "deferred proc queue is not wired to server lifecycle")
require("if (ProcDamageQueue.isApplying()) return amount;" in mixin,
        "deferred proc damage can re-enter outgoing RPG scaling")
require("if (ProcDamageQueue.isApplying()) return;" in mixin,
        "deferred proc damage can re-enter confirmed-hit callbacks")
require("procDamageAfterHit(player, target, release)" in warrior
        and "ProcDamageQueue.queueSameTarget(player, target, amount)" in warrior,
        "Pain Colossus same-target proc still risks vanilla iframes")
require("procDamageAfterHit(player, target, extra)" in assassin
        and "ProcDamageQueue.queueSameTarget(player, target, amount)" in assassin,
        "Soulknife same-target proc still risks vanilla iframes")
require("ProcDamageQueue.queueSameTarget(player, target" in mage,
        "Mage same-target echoes/reactions are not deferred past vanilla iframes")

if errors:
    print("Runtime regression validation FAILED:")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Runtime regression validation OK")
