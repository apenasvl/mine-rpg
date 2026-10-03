# Auditoria do Mago com Iron's Spells 1.20.1-3.16.3

## Decisão estrutural

Iron's é o arsenal de feitiços lançáveis. RPG Stats mantém a classe Mago, as cinco Casas, as quinze especializações, progressão, atributos, recursos secundários e mecânicas de build. `REPLACE` remove a ativa própria, mas preserva o node como modificador da spell indicada. `ADAPT` converte a ativa em regra/passiva/técnica ligada ao ciclo real de casting. `KEEP` permanece como técnica própria porque não duplica uma spell comum.

Casa principal aplica o valor integral. Casa secundária da mesma classe recebe somente o efeito reduzido permitido por categoria, sem especialização profunda, capstone ou Ascensão. Multiclass continua proibido.

## Ativas atuais

| node | habilidade atual | decisão | spell/mecânica do Iron's relacionada | motivo | nova função |
|---|---|---|---|---|---|
| `mag_ele_burst` | Explosão Elemental | ADAPT | Fire/Ice/Lightning e `SpellDamageEvent` | A detonação é uma reação da Casa, não outra AoE equipável. | Spells elementais acumulam marcas; o node melhora a detonação/reação. |
| `mag_arc_pulse` | Pulso Arcano | REPLACE | Gust, Fang Swirl, Shockwave | É dano em área e empurrão genéricos. | Evocation ganha pulso/empurrão moderado sob condição da sequência Arcana. |
| `mag_arc_counterspell` | Contrafeitiço | REPLACE | Counterspell | Duplica proteção/contrafeitiço já existente. | Melhora a janela/eficiência do Counterspell real, respeitando caps. |
| `mag_conj_focus_order` | Ordem: Foco | KEEP | Summon Vex, Summon Polar Bear, Raise Dead, Summon Swords | Comando tático de invocações é identidade de build. | Ordena summons compatíveis a focarem o alvo mirado. |
| `mag_occ_purification` | Purificação | KEEP | Corrupção do RPG Stats | Técnica de gerenciamento de risco sem equivalente direto. | Remove Corrupção; não inicia nem altera cast data do Iron's. |
| `mag_time_shift` | Deslocamento Temporal | REPLACE | Teleport, Blood Step, Burning Dash, Frost Step, Thunder Step | Mobilidade genérica já existe no arsenal. | Spells de mobilidade ativam preparação/ritmo temporal. |
| `mag_pyr_ember_lance` | Lança de Brasas | REPLACE | Firebolt, Fire Arrow, Fireball | Projétil de fogo duplicado. | Fire spells aplicam Calor adicional e alimentam Combustível. |
| `mag_pyr_flame_wall` | Parede de Chamas | REPLACE | Wall of Fire | Equivalente direto. | Wall of Fire aplica/renova Calor e recebe sinergias do Piromante. |
| `mag_pyr_ash_step` | Passo de Cinzas | REPLACE | Burning Dash | Equivalente direto de dash com fogo. | Burning Dash deixa a janela de preparação e ganha interação com Calor. |
| `mag_cryo_ice_shard` | Estilhaço Glacial | REPLACE | Icicle, Ice Spikes, Snowball | Projétil de gelo duplicado. | Ice spells acumulam Frio adicional. |
| `mag_cryo_ice_barrier` | Barreira Glacial | REPLACE | Ice Block, Shield | Barreira/absorção genérica já existe. | Ice Block/Shield recebem eficiência defensiva condicionada ao Frio. |
| `mag_cryo_frost_nova` | Nova Congelante | REPLACE | Frostwave, Cone of Cold | AoE de gelo duplicada. | Frostwave/Cone of Cold propagam Frio e habilitam Estilhaçar. |
| `mag_storm_arc_bolt` | Raio Arcano | REPLACE | Chain Lightning, Lightning Bolt | Corrente elétrica equivalente. | Lightning spells aplicam Carga Estática e usam Sobrecarga. |
| `mag_storm_lightning_step` | Passo Relâmpago | REPLACE | Thunder Step, Charge | Mobilidade elétrica duplicada. | A spell real concede a janela curta de aceleração. |
| `mag_storm_static_field` | Campo Estático | ADAPT | Ball Lightning, Thunderstorm, Shockwave | A zona própria compete com várias zonas elétricas reais. | Spells elétricas de área mantêm Carga/Sobrecarga nos alvos. |
| `mag_storm_asc_avatar` | Avatar da Tempestade | KEEP | Transformação da especialização | Ascensão transforma o uso de todas as Lightning spells. | Buff temporário limitado de cast/movimento e encadeamento. |
| `mag_rune_inscribe` | Inscrever Runa | KEEP | Mecânica rúnica do RPG Stats | Runa programável é técnica, não projétil genérico. | Coloca runa que reage às próximas spells reais. |
| `mag_rune_asc_grand_circle` | Grande Círculo | KEEP | Rede rúnica | Ascensão exclusiva de posicionamento. | Amplifica runas dentro do círculo sem duplicar spells. |
| `mag_illu_mirror_image` | Imagem Espelhada | ADAPT | Scapegoat, Invisibility | O Iron's já fornece engano/invisibilidade. | Scapegoat/Invisibility concedem cargas de ilusão quando o node está ativo. |
| `mag_illu_illusory_step` | Passo Ilusório | REPLACE | Invisibility, Teleport | Blink + invisibilidade duplicados. | Spells de invisibilidade/mobilidade armam Ataque Fantasma. |
| `mag_illu_confusion` | Confusão | ADAPT | Scapegoat, Slow | Controle de alvo deve nascer das spells reais. | Spells de ilusão/controle enfraquecem brevemente mobs resistentes. |
| `mag_illu_asc_mirror_hall` | Salão dos Espelhos | KEEP | Transformação de ilusão | Alteração de comportamento de casts, não spell comum. | Cargas e ecos limitados de spells reais. |
| `mag_tele_blink` | Blink | REPLACE | Teleport | Equivalente direto. | Teleport ativa Compressão e outros bônus espaciais. |
| `mag_tele_repulsion` | Repulsão | REPLACE | Telekinesis, Throw, Gust | Cone de dano/knockback duplicado. | Spells de deslocamento recebem controle espacial moderado. |
| `mag_tele_anchor` | Âncora Espacial | KEEP | Recall como referência, implementação própria isolada | Marcar/retornar é técnica tática distinta. | Salva somente dados RPG próprios; nunca reutiliza cast data do Iron's. |
| `mag_tele_double_portal` | Portal Duplo | REPLACE | Portal | Equivalente direto. | Portal recebe janela de Compressão e progressão da especialização. |
| `mag_tele_singularity` | Singularidade | REPLACE | Black Hole, Gravity Fissure | Pull + dano já existe. | Black Hole/Gravity Fissure ganham bônus de controle limitado. |
| `mag_tele_asc_collapse` | Colapso Espacial | ADAPT | Black Hole, Gravity Fissure | Não deve existir uma segunda super-AoE espacial. | Ascensão transforma a próxima spell espacial elegível, com cap. |
| `mag_summ_familiar` | Familiar Arcano | REPLACE | Summon Vex, Summon Polar Bear, Raise Dead | Summon próprio compete com os summons reais. | Primeiro summon leve passa a consumir Vínculo. |
| `mag_summ_guardian` | Guardião Arcano | REPLACE | Summon Polar Bear, Summon Vex | Invocação defensiva duplicada. | Summons resistentes recebem custo/benefício de Vínculo. |
| `mag_summ_assault_order` | Ordem: Assalto | KEEP | Summons do Iron's | Comando de build sem equivalente direto. | Buff curto e limitado para summons já existentes. |
| `mag_summ_transfer` | Transferência Arcana | KEEP | Summon timer do Iron's | Manipulação de duração é mecânica da Casa. | Prolonga de forma limitada summons compatíveis. |
| `mag_summ_great_conjuration` | Grande Conjuração | REPLACE | Summon Polar Bear, Raise Dead, Summon Vex | Constructo pesado próprio é outro catálogo de summon. | Melhora summons de alto nível e aumenta seu custo de Vínculo. |
| `mag_summ_asc_ephemeral_army` | Exército Efêmero | KEEP | Summons do Iron's | Ascensão muda limite e ritmo, sem criar spell básica. | Vínculo temporário e bônus controlado aos summons reais. |
| `mag_anim_life_spirit` | Espírito da Vida | ADAPT | Heal, Healing Circle, Cloud of Regeneration | Aura de cura lançável compete com Holy. | Postura da Vida acionada por Nature/Holy, com cura limitada. |
| `mag_anim_earth_spirit` | Espírito da Terra | ADAPT | Oakskin, Fortify | Aura defensiva duplicada. | Postura da Terra reforça defesa após Nature/Fortify. |
| `mag_anim_hunt_spirit` | Espírito da Caça | ADAPT | Guiding Bolt, Root | Marca ofensiva deve reagir a spell real. | Postura da Caça marca alvos atingidos por Nature/controle. |
| `mag_anim_spirit_totem` | Totem Espiritual | KEEP | Ritual/postura | Totem de alternância das posturas é único. | Área de suporte baseada no espírito predominante. |
| `mag_anim_spirit_swap` | Troca Espiritual | KEEP | Posturas do RPG Stats | Comando de build, não magia duplicada. | Alterna o espírito predominante. |
| `mag_anim_asc_council` | Conselho Ancestral | KEEP | Transformação de posturas | Ascensão combina posturas existentes. | Ativa as três posturas com duração/caps próprios. |
| `mag_astral_blade` | Lâmina Astral | REPLACE | Summon Swords, Echoing Strikes | Arma espectral própria duplicada. | Summon Swords/Echoing Strikes alimentam formação ofensiva. |
| `mag_astral_shield` | Escudo Astral | REPLACE | Shield, Fang Ward | Defesa conjurada duplicada. | Shield/Fang Ward alimentam formação defensiva. |
| `mag_astral_turret` | Torre Arcana | ADAPT | Magic Missile, Magic Arrow, Summon Swords | Sentinela própria criaria outro summon. | Spells arcanas de projétil alimentam formação de artilharia. |
| `mag_astral_formation` | Formação | KEEP | Estado tático do RPG Stats | Alternância de comportamento é identidade da build. | Alterna ofensiva/defensiva/escolta para spells e summons reais. |
| `mag_astral_asc_arsenal` | Arsenal Astral | ADAPT | Summon Swords, Echoing Strikes, Magic Missile | Arsenal próprio duplica conteúdo Ender. | Ascensão amplifica temporariamente o arsenal espectral real. |
| `mag_blood_lance` | Lança de Sangue | REPLACE | Blood Needles, Blood Slash | Ataque de sangue equivalente. | Blood spells acumulam Hemorragia e usam conversão vital. |
| `mag_blood_transfusion` | Transfusão | REPLACE | Ray of Siphoning, Devour | Dano + cura equivalente. | Spells de dreno usam caps de lifesteal e eficiência reduzida em bosses. |
| `mag_blood_asc_eclipse` | Eclipse Carmesim | KEEP | Transformação de risco/recompensa | Modifica toda a escola Blood sem duplicar uma spell. | Janela de conversão vital/lifesteal com caps. |
| `mag_curse_weakness` | Maldição da Fraqueza | ADAPT | Blight, Heartstop, Wither Skull | Debuff próprio compete com arsenal Blood/Eldritch. | Primeira maldição elegível aplica Fraqueza reduzida. |
| `mag_curse_fragility` | Maldição da Fragilidade | ADAPT | Blight, Heartstop, Eldritch Blast | Outro cast separado não é necessário. | Maldições elegíveis acumulam Fragilidade limitada. |
| `mag_curse_ruin` | Ruína | REPLACE | Blight, Wither Skull, Heartstop | DoT genérico duplicado. | DoTs reais recebem duração/sinergias de Ruína. |
| `mag_curse_spread` | Propagação | KEEP | Blood/Eldritch debuffs | Propagar estado é mecânica única. | Espalha somente estados autorizados pelo RPG Stats. |
| `mag_curse_asc_great_curse` | Grande Maldição | KEEP | Ritual de maldição | Ascensão combina modificadores, não substitui spell básica. | Próxima spell Blood/Eldritch elegível propaga versões reduzidas. |
| `mag_hex_blink_strike` | Blink Strike | REPLACE | Blood Step, Shadow Slash, Echoing Strikes | Mobilidade + ataque já existe. | Essas spells armam o próximo golpe melee mágico. |
| `mag_hex_mystic_parry` | Parry Místico | KEEP | Técnica melee própria | Parry é técnica de classe, não spell genérica. | Janela curta defensiva que gera Concentração. |
| `mag_hex_elemental_imbue` | Imbuimento Elemental | KEEP | Fire/Ice/Lightning como escolas | Alternância de afinidade do melee é buildcraft. | Seleciona o elemento dos golpes imbuídos. |
| `mag_hex_dimensional_cut` | Corte Dimensional | REPLACE | Shadow Slash, Gravity Fissure | Corte mágico em linha duplicado. | Shadow Slash alimenta o ritmo Spellblade. |
| `mag_hex_asc_arcane_form` | Forma Arcana | KEEP | Transformação Spellblade | Ascensão híbrida exclusiva da especialização. | Janela melee/magia com caps globais. |
| `mag_acc_acceleration` | Aceleração | ADAPT | Haste, Charge | Buff lançável de velocidade é redundante. | Haste/Charge geram Momentum e melhoram cast dentro do cap. |
| `mag_acc_instant_cast` | Conjuração Instantânea | KEEP | Lifecycle do próximo cast | Técnica que altera o próximo cooldown real. | Marca a próxima spell não-ultimate para redução controlada. |
| `mag_acc_temporal_step` | Passo Temporal | REPLACE | Teleport, Thunder Step, Blood Step | Dash genérico duplicado. | Mobilidade real gera Fragmento/Momentum conforme cooldown interno. |
| `mag_acc_overclock` | Overclock | KEEP | Cooldowns do Iron's | Manipulação única de recarga. | Gasta Fragmento para reduzir cooldown real elegível. |
| `mag_acc_asc_distorted_time` | Tempo Distorcido | KEEP | Transformação temporal | Modifica ritmo global sem ser nova spell ofensiva. | Janela limitada de cast, movimento e CDR. |
| `mag_stag_temporal_slow` | Lentidão Temporal | REPLACE | Slow | Equivalente direto. | Slow real habilita Time Lock/Entropia. |
| `mag_stag_stasis_bubble` | Bolha de Stasis | KEEP | Zona temporal própria | Controle de projéteis/ritmo não tem equivalente direto completo. | Zona segura, sem tocar no cast data interno do Iron's. |
| `mag_stag_temporal_anchor` | Âncora Temporal | ADAPT | Arcane Shackle, Root, Slow | Controle separado é redundante. | Spells de imobilização aplicam resistência a deslocamento. |
| `mag_stag_asc_time_stop` | Parada Temporal | KEEP | Ascensão de zona | Técnica máxima única, com efeito reduzido em bosses. | Mantém zona temporal limitada e server-authoritative. |
| `mag_rev_temporal_mark` | Marca Temporal | KEEP | Estado próprio do RPG Stats | Conceito único; o problema é lifecycle, não redundância. | Salva snapshot RPG simples, fora de `ICastData` do Iron's. |
| `mag_rev_rewind` | Retrocesso | KEEP | Estado próprio do RPG Stats | Retorno à marca é identidade do Reversor. | Teleporte seguro após validação de dimensão/vida/posição. |
| `mag_rev_temporal_echo` | Eco Temporal | KEEP | `SpellDamageEvent`/cast concluído | Repete efeito de build, não registra spell falsa. | Eco de dano limitado da próxima spell real, sem copiar cast data. |
| `mag_rev_asc_rewrite` | Reescrever Destino | KEEP | Snapshot próprio do RPG Stats | Ascensão única de estado temporal. | Restaura snapshot validado sem reutilizar `TeleportData`/`TargetAreaCastData`. |

## Resumo da refatoração autorizada

- Remover do registro de ativas as entradas `REPLACE` e converter `ADAPT` em passivas/gatilhos de spell quando seguro.
- Manter somente técnicas `KEEP` nos slots ativos do RPG Stats.
- Iron's torna-se autoridade de Mana quando instalado; o core conserva fallback próprio quando ausente.
- Toda spell real passa pelos mesmos caps, atributos, Casa principal e Casa secundária reduzida.
- Nenhuma técnica RPG cria spell falsa nem grava objetos internos de cast do Iron's.
