#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rpgstats"
RES = ROOT / "src/main/resources"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


state = (SRC / "boss/EncounterState.java").read_text(encoding="utf-8")
manager = (SRC / "boss/EncounterManager.java").read_text(encoding="utf-8")
mixin = (SRC / "mixin/ClassMechanicsSupportMixin.java").read_text(encoding="utf-8")
mixins = json.loads((RES / "rpgstats.mixins.json").read_text(encoding="utf-8"))

require("recordSupport(UUID playerId" in state,
        "EncounterState precisa registrar suporte separadamente")
require("normalizedHealing" in state and "normalizedProtection" in state,
        "Cura e protecao precisam de componentes normalizados")
require("healing / safeMaxHealth" in state and "protection / safeMaxHealth" in state,
        "Suporte nao pode ser comparado apenas por numeros brutos")
require("MAX_SUPPORT_FRACTION_PER_EVENT = 0.50" in state,
        "Pulsos de suporte precisam de cap anti-inflacao")
require("participant.normalizedHealing" in state and "participant.normalizedProtection" in state,
        "Contribution Score precisa incluir cura e protecao")

require("recordSupport(ServerPlayerEntity supporter, ServerPlayerEntity ally" in manager,
        "EncounterManager precisa aceitar suporte explicito")
require("supporter == ally" in manager,
        "Self-heal nao deve duplicar score de tanking")
require("state.hasParticipant(ally.getUuid())" in manager,
        "Suporte so pode entrar em encounter onde o aliado ja participa")
require("ACTIVE.values()" in manager,
        "Suporte deve consultar apenas encounters ativos, sem scan global de entidades")

require("@Mixin(targets = \"com.rpgstats.combat.ClassMechanics\", remap = false)" in mixin,
        "Mixin de suporte deve mirar apenas ClassMechanics")
require("method = \"healNearby\"" in mixin,
        "Suporte precisa observar o helper real de cura do Paladino")
require("ally.getHealth() - before.health()" in mixin,
        "So cura efetivamente aplicada pode gerar score")
require("ally.getAbsorptionAmount() - before.absorption()" in mixin,
        "So protecao/absorcao efetivamente concedida pode gerar score")
require("EncounterManager.recordSupport" in mixin,
        "Mixin precisa encaminhar suporte ao encounter server-authoritative")
require("p != player" in mixin,
        "Self-heal nao deve ser creditado como suporte de grupo")

require("ClassMechanicsSupportMixin" in mixins.get("mixins", []),
        "Mixin de suporte precisa estar registrado")

print("Progression phase 2f support validation OK")
