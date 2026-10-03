#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"

mod = (SRC / "RPGStatsMod.java").read_text(encoding="utf-8")
network = (SRC / "network/RpgNetwork.java").read_text(encoding="utf-8")
client = (SRC / "RPGStatsClient.java").read_text(encoding="utf-8")
screen = (SRC / "gui/GuideScreen.java").read_text(encoding="utf-8")

# /rpg guia must now sync the build and open a dedicated client Codex screen.
for token in (
    'literal("guia")',
    'syncStats(player)',
    'RpgNetwork.openGuide(player)',
):
    assert token in mod, f"/rpg guia custom Codex wiring missing: {token}"

# Dedicated S2C packet, client entry point and deterministic Screen.
for token in (
    'public record OpenGuide()',
    'registerMessage(3, OpenGuide.class',
    'public static void openGuide(ServerPlayerEntity player)',
    'RPGStatsClient.openGuideScreen()',
):
    assert token in network, f"Codex network wiring missing: {token}"

for token in (
    'import com.rpgstats.gui.GuideScreen;',
    'client.setScreen(new GuideScreen())',
):
    assert token in client, f"Codex client opening missing: {token}"

for token in (
    'public final class GuideScreen extends Screen',
    'private String pageKey = "index"',
    'private final List<String> history',
    'addGlobalNavigation()',
    'addPageButtons()',
    'openDirect(',
    'back()',
    'orderedKeys()',
    'classSpecsKey',
    'houseKey',
    'specKey',
    '"Função"',
    '"Base"',
    '"Motor"',
    '"Técnica"',
    '"Domínio"',
    '"Ascensão"',
    'AbilityRegistry.activeSummary',
    'ClassAbilityRegistry.description',
):
    assert token in screen, f"Deterministic Codex screen missing: {token}"

# The bug fix: custom navigation must not depend on vanilla written-book CHANGE_PAGE hitboxes.
for forbidden in (
    'ClickEvent.Action.CHANGE_PAGE',
    'WrittenBookItem',
    'useBook(',
    'RpgGuideBook.giveOrOpen',
):
    assert forbidden not in screen and forbidden not in mod, f"Flaky vanilla book navigation still active: {forbidden}"

# Every transition must be tied to actual widgets; no text hit-testing navigation.
assert 'new RpgButton' in screen
assert 'clearAndInit()' in screen
assert 'mouseClicked(' not in screen
assert 'styleAt' not in screen

print("RPG deterministic Codex screen validation OK")
