"""Verify the active custom Codex routes corruption through real buttons."""
from pathlib import Path
import re
import textwrap

root = Path(__file__).resolve().parents[1]
screen = (root / "src/main/java/com/rpgstats/gui/GuideScreen.java").read_text()
copy = (root / "src/main/java/com/rpgstats/gui/MageCorruptionText.java").read_text()


def method(name):
    match = re.search(r"private [^\n]+ " + name + r"\([^\n]*\) \{", screen)
    assert match, name
    start = match.end()
    depth = 1
    for end in range(start, len(screen)):
        depth += (screen[end] == "{") - (screen[end] == "}")
        if depth == 0:
            return screen[start:end]
    raise AssertionError("unclosed " + name)


buttons = method("addPageButtons")
house = buttons.split('if (pageKey.startsWith("house:"))')[1].split('if (pageKey.startsWith("spec:"))')[0]
spec = buttons.split('if (pageKey.startsWith("spec:"))')[1]
assert 'if (house == RPGPath.MAGE_OCCULT)' in house
assert 'addIndexButton("Entender Corrupção", corruptionKey(0)' in house
assert 'if (spec == RPGSpecialization.BLOODMANCER)' in spec
assert 'addIndexButton("Entender Corrupção", corruptionKey(0)' in spec
assert 'new RpgButton' in method("addIndexButton") and 'btn -> open(target)' in method("addIndexButton")
assert 'btn -> open(corruptionKey(section))' in buttons
assert 'MageCorruptionText.guidePages().get(section)' in method("renderRightPage")
assert 'page.body().split("\\n")' in method("renderRightPage")
assert 'out.add(corruptionKey(i))' in method("orderedKeys")
assert 'section >= 0 && section < MageCorruptionText.guidePages().size()' in method("parseCorruption")
assert 'history.add(pageKey)' in method("open")
assert 'ClickEvent' not in screen

pages = re.findall(r'new GuidePage\("([^"]+)",\s*"([^"]+)"\)', copy)
assert len(pages) == 5
# Minimum book is 600x340: right content width 252, footer starts at312.
# Bound every character at6px, including spaces: conservative text-height check.
for title, body in pages:
    paragraphs = body.replace('\\n', '\n').split('\n')
    rows = sum(len(textwrap.wrap(p, width=42)) for p in paragraphs)
    bottom = 24 + 28 + rows * 9 + len(paragraphs) * 6
    assert bottom < 312, (title, bottom)
    assert len(title) * 7 <= 252, title
assert 72 + 5 * 29 + 5 + 23 < 312, "section buttons overlap footer"
assert 72 + 3 * 29 + 5 + 23 < 312, "entry button overlaps footer"
print("Active corruption Codex: entry buttons, section routes, render source, history and minimum layout passed")
