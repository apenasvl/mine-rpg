# Balance Baseline v1.5 — Mago

Este arquivo registra a linha de base para testes em jogo. Os valores são deliberadamente conservadores: o objetivo é subir poder acima do vanilla no endgame sem transformar cada habilidade em one-shot.

## Faixas de dano
- básica: 4–6 HP;
- média: 6–9 HP;
- pesada: 9–14 HP;
- Ascensão ofensiva: ~12–20 HP totais em alvo único antes de ajustes por setup;
- crítico mágico: 1,5x.

## Caps
- magic power permanente: +60%;
- spell crit permanente: 20%;
- CDR permanente: 25%;
- CDR temporário: 50%;
- spell lifesteal: 10%;
- mana cost reduction: 45%.

## Recursos
- Mana base: 100;
- Mana por INT: +2;
- Mana cap do Mago: 280;
- regen base: 3/s;
- Concentração: 0–100;
- Corrupção: 0–100;
- Fragmentos Temporais: específicos da Casa Temporal.

## Testes recomendados antes de expandir as outras classes
1. Nível 10: Mago base contra zombie/skeleton/creeper sem equipamento absurdo.
2. Nível 25: cada Casa contra grupos vanilla e Nether.
3. Nível 35: cada uma das 15 especializações contra elite/miniboss.
4. Nível 50: todas as Ascensões contra Netherite player dummy e bosses.
5. Medir burst em 5 s, DPS em 30 s e sustain em 120 s.
6. Medir tempo para esvaziar Mana e tempo de recuperação.
7. Testar hard CC em boss para garantir que não gere stun-lock.
8. Testar AOE com 10+ mobs para garantir caps de Concentração/Mana/procs.
9. Testar summons simultâneos e expiração em lote.
10. Testar multiplayer para impedir stacking multiplicativo fora dos caps.

## Meta de design
Duas builds Mago nível 50 devem parecer classes diferentes em gameplay, mas nenhuma deve ganhar por simplesmente apertar uma tecla sem setup, gestão de recurso ou janela de risco.
