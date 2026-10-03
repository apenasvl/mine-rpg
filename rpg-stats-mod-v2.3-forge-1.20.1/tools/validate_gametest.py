#!/usr/bin/env python3
"""Static contract for the in-game RPG Stats GameTest harness.

This validator intentionally checks the wiring that makes runtime tests useful in CI:
- Forge discovers the holder;
- Yarn-mapped GameTest methods use our shared structure;
- Architectury Loom exposes a GameTestServer run;
- GitHub Actions actually executes that server.

Behavior is then verified by the GameTests themselves inside Minecraft.
"""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
GAME_TEST = ROOT / "src/main/java/com/rpgstats/gametest/RPGStatsGameTests.java"
STRUCTURE = ROOT / "src/main/resources/data/rpgstats/structures/empty.nbt"
BUILD = ROOT / "build.gradle"
WORKFLOW = ROOT.parent / ".github/workflows/forge-build.yml"

errors: list[str] = []


def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)


require(GAME_TEST.is_file(), "missing RPGStatsGameTests.java")
require(STRUCTURE.is_file(), "missing data/rpgstats/structures/empty.nbt")

if GAME_TEST.is_file():
    source = GAME_TEST.read_text(encoding="utf-8") + (GAME_TEST.parent / "TestPlayers.java").read_text(encoding="utf-8")
    require("@GameTestHolder(RPGStatsMod.MOD_ID)" in source, "GameTest holder is not registered under rpgstats")
    require("@PrefixGameTestTemplate(false)" in source, "GameTest template prefix must be disabled")
    require('@GameTest(templateName = "empty"' in source, "GameTests must use the shared empty structure")
    require("new ServerPlayerEntity(" in source, "runtime tests must construct a real ServerPlayerEntity")
    require("new EmbeddedChannel(connection)" in source and "onPlayerConnect(connection, player)" in source,
            "GameTest players must use a real in-memory Netty channel before Forge login")
    require("target.damage(" in source, "runtime tests must pass through LivingEntity.damage")
    require("RPGSpecialization.values()" in source, "specialization registry coverage must be automatic")
    require("PAIN_COLOSSUS" in source and "war_pain" in source, "Colosso da Dor regression coverage is missing")
    require("deferredSameTargetProcLandsAfterHurtResistance" in source and "waitAndRun(14" in source,
            "deferred same-target proc runtime coverage is missing")
    require("playerStatsRoundTripThroughPersistentNbt" in source and "StatsManager.get(player)" in source,
            "PlayerStats NBT round-trip runtime coverage is missing")

build = BUILD.read_text(encoding="utf-8")
require('create("gameTestServer")' in build or "create('gameTestServer')" in build,
        "Architectury Loom gameTestServer run is missing")
require('environment("gameTestServer")' in build or "environment('gameTestServer')" in build,
        "gameTestServer must use Forge's dedicated gameTestServer runtime environment")
require('forgeTemplate("gameTestServer")' in build or "forgeTemplate('gameTestServer')" in build,
        "gameTestServer must use Forge's dedicated gameTestServer template")
require("server()" not in build.split('create("gameTestServer")', 1)[1].split("}", 1)[0],
        "gameTestServer must not combine generic server() args with the Forge GameTest template")
require("forge.gameTestServer" in build and "forge.enableGameTest" in build,
        "Forge GameTest runtime flags are missing")
require("forge.enabledGameTestNamespaces" in build and "rpgstats" in build,
        "GameTest namespace is not restricted to rpgstats")

workflow = WORKFLOW.read_text(encoding="utf-8")
require("validate_gametest.py" in workflow, "CI does not run the GameTest wiring validator")
require("runGameTestServer" in workflow, "CI does not execute the Minecraft GameTest server")

if errors:
    print("GameTest validation FAILED:")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("GameTest validation OK")
