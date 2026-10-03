# Integração seletiva — Forge 1.20.1

Base: PR #41 (`cc3400a339a908559394c923bd5dc9485bd7a947`). As quatro classes, IDs de nodes, Casas e saves continuam iguais.

| Mod / arquivo CurseForge | Decisão | Comportamento |
|---|---|---|
| Combat Roll 1.3.3+1.20.1 / 5625925 | Usar parcialmente | Animação e som nativos nas técnicas Survivalist e Guerrilla. Movimento calculado no servidor RPG. |
| Immersive Armors 1.7.2+1.20.1 / 8076909 | Usar parcialmente | Proteção, resistência, peso e modelos originais; poderes e bônus gratuitos substituídos pela progressão RPG. |
| Presence Footsteps Forge 1.20.1-1.9.1-beta.1 / 5015286 | Usar no cliente | Ambientação de passos. Não é habilidade, não interfere em furtividade e não é exigido no servidor. |
| Rogues & Warriors 3.1.1 / 8891443 | Excluir do perfil | Livros/ações do Spell Engine e ajustes de combate sobrepõem as classes. Shadow, Duelist e Guerreiro usam os executores RPG existentes. |
| Simply Swords 1.70.2-1.20.1 / 8746028 | Excluir do perfil | Poderes únicos/rúnicos e mixins globais não têm gate completo verificado. Não anunciar armas comuns como integração segura apenas por tags. |
| Assassin's Craft do link fornecido | Excluir | Minecraft 1.12.2; não instalar em Forge 1.20.1. Não foi substituído por Chronicles. |

Immersive Armors foi listado duas vezes no pedido; apenas um arquivo é instalado.

## Rolamento por técnica

`arc_ward_surv_technique` e `arc_skirm_guer_technique` continuam com custo base **20** e recarga **280 ticks** (14 s), modificados pelos redutores e regras de Casa já existentes. Survivalist conserva a janela de sobrevivência e Guerrilla conserva a origem para suas outras ações. Não há node secundário inventado nem multiclass.

Com Combat Roll instalado: exige jogador vivo, no chão, fora de água/lava, sem montaria, voo ou uso de item. Use o slot RPG (R/Z/X/C). A tecla livre e o HUD de cargas de Combat Roll ficam desativados; o servidor rejeita todos os pacotes nativos `publish` antes de decodificar ou produzir efeitos. Um único pacote RPG de animação, exclusivamente servidor → cliente, acompanha o impulso. Velocidade não vem do cliente. Não há fome adicional, i-frames ou segunda recarga de Combat Roll.

Sem Combat Roll: o recuo original continua. Windrunner conserva avanço/Momentum e Acrobat conserva salto. Eles não viram rolamento.

Se a execução não iniciar, o recurso é devolvido e a recarga não começa. Não há fallback adicional depois de uma tentativa externa. O adaptador não guarda autorizações entre ticks, mortes, dimensões ou reconexões.

## Armaduras

O hook atua em `ExtendedArmorMaterial` do binário 1.7.2: remove a lista de efeitos, encantamentos embutidos, bônus de ataque/velocidade de ataque, vida, sorte e anti-skeleton. Assim, Berserk, bloqueio divino, espinhos nativos e salto de slime não contornam nodes nem entram no pipeline de procs. Encantamentos vanilla aplicados normalmente ao stack seguem as regras vanilla/RPG existentes.

Mantém proteção, toughness, resistência a empurrão, durabilidade, reparo, **peso do próprio Immersive Armors** e visuais. Peso negativo (que concederia velocidade gratuita) é limitado a zero; penalidades positivas permanecem. Os IDs das categorias RPG vêm dos resultados de receitas do JAR inspecionado e usam `required:false`. As categorias não criam requisitos novos de equipar. Campos JSON RPG de peso/stamina/velocidade sem consumidor não são anunciados como funcionalidades.

## Instalação e verificação

Use Forge 47.4.0 / Minecraft 1.20.1 / Java 17. Instale o JAR RPG e os mods separadamente. Nenhum JAR de terceiros é embutido no RPG.

Dependências de Combat Roll: playerAnimator Forge `1.0.2-rc1+1.20` (4587214) e Cloth Config Forge `11.1.136` (5729105). Immersive Armors não exige biblioteca adicional. Presence Footsteps Forge requer Forge >=47.2.19, já atendido por 47.4.0. O link Fabric original de Presence Footsteps não é o arquivo usado.

`compat/combat-mods.lock.json` registra arquivos, SHA-256, IDs, versões, lados e dependências inspecionadas. Mods excluídos estão explicitamente desativados nesse perfil; não são carregados nos testes de combate. Instalá-los por fora deste perfil não ganha gates RPG.

Instalador reproduzível (Python 3.11+): `python tools/install_combat_profile.py --destination /caminho/do/perfil/mods` no servidor; adicione `--client` no cliente. Ele verifica todos os hashes e dependências antes de colocar os JARs, e recusa versões duplicadas e mods excluídos ainda instalados, sem apagar seus arquivos. O JAR RPG é instalado separadamente.

Perfis Gradle:

- Core: `./gradlew runGameTestServer`.
- Arqueiro anterior: `./gradlew -ParcherCompatTests runGameTestServer`.
- Combate selecionado: `./gradlew -PcombatCompatTests runGameTestServer`.
- Combinado: `./gradlew -PcombinedCompatTests runGameTestServer`.
- Cliente combinado com passos: `./gradlew -PcombinedCompatTests -PcombatClient runClient`.

As dependências opcionais de Combat Roll/Immersive Armors no `mods.toml` aceitam somente as versões inspecionadas. Outra versão exige nova validação; não se aplica hook desconhecido silenciosamente. O perfil de desenvolvimento combinado também carrega `com.eliotlash.mclib:mclib:20`, dependência documentada de GeckoLib, para que os modelos dos mods de arqueiro inicializem no cliente.

Epic Fight não faz parte do perfil: a compatibilidade visual dele não foi validada.

Validação automatizada e limitações finais são registradas em `COMBAT-VALIDATION.md`; build ou teste estático sozinho não comprova animação/som em um cliente real.
