# Changelog v1.5.0 — Mage Expansion

## Progressão
- Mago convertido para progressão profunda de longo prazo.
- 12 nodes de núcleo.
- 5 Casas com 9 nodes cada.
- 15 especializações com 7 nodes cada, incluindo Ascensão.
- 162 nodes exclusivos de Mago; 242 nodes registrados no projeto inteiro.
- Casa no nível 10, especialização no 25, cross-house no 30 e Ascensão no 50.
- Cross-house limitado a 6 nodes totais e 3 por Casa alternativa.
- 1 PH por nível, incluindo 1 PH inicial ao escolher a classe no nível 1.

## Recursos
- Mana do Mago refeita: 100 base + 2 por INT, cap 280.
- Concentração Arcana 0–100.
- Corrupção para Casa Oculta.
- Fragmentos Temporais para Casa Temporal.
- Quatro slots de habilidade ativa: R/Z/X/C.

## Mecânicas de Casas
- Elemental: marcas de Calor/Frio/Carga e reações.
- Arcana: categorias, Tríade e Selos Arcanos.
- Conjuração: Pontos de Vínculo, summons virtuais e formações.
- Oculta: Corrupção, maldições e risco/recompensa.
- Temporal: Fragmentos, stasis e manipulação controlada de cooldown.

## Especializações
- Piromante, Criomante, Tempestário.
- Runista, Ilusionista, Telemante.
- Conjurador, Animista, Forjador Astral.
- Sanguimante, Maledicente, Hexblade.
- Acelerador, Estagnador, Reversor.

## Balanceamento / guardrails
- dano mágico trabalha em escala próxima do Minecraft vanilla;
- crítico mágico em 1,5x;
- caps rígidos para magic power, crit, CDR, lifesteal e redução de Mana;
- bosses recebem versões reduzidas de hard CC e alguns efeitos;
- geração de Concentração e Mana por AOE/summons possui caps;
- cooldown mínimo de Ascensões.

## Correções adicionais durante implementação
- Transferência de summons agora estende 20% da duração restante de verdade.
- Refund de summon ganhou cooldown interno de 1s contra expirações simultâneas.
- Harmonia Ancestral só ativa com dois ou mais espíritos.
- Espírito da Vida cura em intervalo controlado.
- Formação Astral possui modos ofensivo/defensivo/escolta com funções distintas.
- Maldições recebem duração consistente e Marca do Destino expira/é consumida.
- Weakness de Maledicente usa redução customizada em vez de empilhar efeito vanilla de forma inconsistente.
- Temporal Echo passou a ocorrer com atraso real.
- Dívida Temporal agora devolve cura de Rewind gradualmente e de forma não letal.
- Reescrever Destino aplica exaustão de Mana em vez de Weakness genérico.
- Time Lock e desaceleração de projéteis respeitam proteção de bosses/aliados.
- Shared Flow de invocação possui cap de Mana por segundo.
- Momentum do Acelerador é construído por sequência real de casts.
- traduções R/Z/X/C adicionadas aos arquivos de idioma.
- tooltips ganharam descrições legíveis para os novos efeitos passivos.
