# Validação v2.2

## Executado neste ambiente

- `tools/validate_project.py`: 3.288 checks de domínio aprovados;
- parser Java: 45 fontes aceitas sintaticamente;
- validação JSON: 14 arquivos carregados pelo parser padrão;
- `tools/validate_v22.py`: isolamento de imports opcionais, estrutura e dados verificados.

## Cenários estruturais

### A — somente RPG Stats

O core não importa classes externas. `CompatManager` ativa zero módulos e os JSON internos fornecem defaults. Esperado: inicialização normal.

### B — RPG Stats + uma integração

Somente o descritor cujo mod ID está presente é ativado. Os demais não são carregados. Conteúdo pode ser classificado por datapack sem chamada de API externa.

### C — RPG Stats + várias integrações

Descritores são independentes, consultam o mesmo snapshot e passam pelos mesmos serviços/caps. Não existe scan por tick nem reflexão em combate.

## Limite da validação

O build Fabric completo não terminou aqui porque o wrapper tentou baixar Gradle 8.8 e a rede do ambiente bloqueou `services.gradle.org`. Portanto ainda é obrigatório rodar `gradlew.bat build` com Java 17 no PC e testar em cliente e servidor dedicado. A validação sintática não confirma assinaturas Yarn/Fabric nem comportamento dentro do Minecraft.

## Checklist em jogo

- iniciar com somente Fabric API + RPG Stats;
- usar `/rpg`, comprar habilidades e relogar;
- matar Wither, Ender Dragon e entidade da tag tier 3;
- conferir XP de primeira vitória e repetição;
- atacar com arma classificada abaixo/acima do requisito;
- recarregar datapack e confirmar novos perfis;
- repetir em servidor dedicado com dois jogadores;
- instalar uma integração por vez e procurar `ClassNotFoundException`/`NoClassDefFoundError`.

