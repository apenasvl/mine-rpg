# Revisão corretiva da auditoria Mago + Iron's

Este documento complementa `MAGE-IRONS-AUDIT.md` e prevalece nos pontos abaixo até a próxima consolidação do arquivo principal.

## 1. Conjuração Instantânea

`mag_acc_instant_cast` não deve ser descrita como "Conjuração Instantânea" se o efeito real for somente reduzir cooldown. A identidade correta do node deve ser uma destas duas opções durante o rework:

- preferida: afetar o **cast time real** da próxima spell elegível do Iron's, respeitando limites e excluindo ultimates/efeitos sem cast time;
- fallback: se a API 1.20.1-3.16.3 não oferecer uma alteração segura de cast time, renomear a técnica para algo ligado a recuperação/ritmo temporal e descrever exatamente o efeito executável.

Não manter nome/tooltip que prometa cast instantâneo enquanto o código só altera recarga.

## 2. Nature não é sinônimo de summon

A escola Nature do Iron's não pode ser tratada globalmente como `SUMMON`. Apenas spells que realmente criam/controlam invocações devem alimentar mecânicas de Vínculo/Conjurador.

Spells Nature de cura, controle, defesa ou dano continuam podendo interagir com Animista quando explicitamente elegíveis, mas não devem acionar automaticamente todo o pipeline de summon.

Durante a implementação, separar a classificação por **função/spell ID/tag/categoria explícita**, e não somente pela escola.

## 3. Holy e Evocation não viram Arcane indiscriminadamente

Mapear toda Holy/Evocation para Arcane é amplo demais para buildcraft. A escola externa continua sendo preservada como informação de origem; as Houses do RPG Stats recebem apenas as interações explicitamente autorizadas.

- Evocation pode alimentar partes do loop Arcano quando a spell estiver na lista/categoria adequada.
- Holy deve permanecer neutra por padrão para o Mago e só alimentar Animista/Arcana quando a mecânica específica declarar isso.
- Nenhum bônus Arcano deve aplicar automaticamente a toda Holy spell apenas por fallback de mapeamento.

## 4. Documento não pode prometer integração inexistente

Cada linha `ADAPT`/`REPLACE` que cita uma spell do Iron's precisa ter correspondência executável no adapter ou ser marcada como **PLANEJADA**.

O adapter atual reconhece explicitamente apenas subconjuntos, por exemplo mobilidade e ilusão. Portanto referências como Telekinesis, Throw, Slow, Root, Guiding Bolt e outras não devem ser tratadas como já integradas sem regra real correspondente.

Critério de aceite futuro:

`node RPG -> condição/spell elegível -> hook/evento usado -> efeito aplicado -> cap -> teste`

Se qualquer elo estiver ausente, o documento deve dizer "planejado", não "implementado".

## 5. Nomes e tooltips dos nodes REPLACE/ADAPT

Quando uma antiga ativa deixa de conceder uma spell própria e vira modifier, seu nome e tooltip precisam refletir a nova função.

Exemplos conceituais:

- `Estilhaço Glacial` não deve continuar parecendo uma spell equipável se agora significa "Ice spells acumulam Frio adicional";
- `Lança de Brasas` não deve prometer um projétil próprio se virou modificador de Fire spells;
- `Blink` não deve aparecer como técnica independente se o node agora melhora Teleport do Iron's.

IDs internos podem ser mantidos por compatibilidade de save, mas o texto de UI deve ser honesto com o comportamento executável.

## 6. Mobilidade e esquiva

A esquiva universal continua removida. Especializações podem possuir mobilidade própria, parry, fase, salto ou reposicionamento, desde que sejam mecânicas específicas da build, tenham custo/recarga e não restaurem um dodge universal gratuito.

## 7. Regras para consolidar Mago + Iron's

Antes de considerar a reestruturação do Mago finalizada:

1. Iron's continua sendo o arsenal principal de spells;
2. RPG Stats não cria segundo catálogo concorrente;
3. Mana principal vem do Iron's quando instalado;
4. Nature/summon são classificados por função real;
5. Holy/Evocation só entram em loops de House por regra explícita;
6. cada modifier citado possui implementação/teste correspondente;
7. nomes/tooltips descrevem exatamente o que o node faz;
8. Marca Temporal/Rewind continuam isolados do `ICastData` do Iron's;
9. Casa secundária permanece abaixo da primária e sem especialização/Ascension secundária;
10. damage/CDR/mana/lifesteal continuam respeitando caps e autoridade do servidor.

## Estado da auditoria

Com estas correções, os documentos de design estão prontos para revisão antes da implementação do rework por classe. Nenhuma mudança de gameplay é autorizada apenas por este documento.