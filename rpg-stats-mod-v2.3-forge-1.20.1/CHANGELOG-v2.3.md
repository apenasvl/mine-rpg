# v2.3.0-forge-port-alpha

- Porte exclusivo do ZIP v2.2 para Forge 1.20.1.
- Forge @Mod e META-INF/mods.toml substituem os entrypoints Fabric.
- Eventos Forge para ticks, login, logout, clone, respawn, dimensão, entidades e reload.
- Rede Forge SimpleChannel com direções fixas e versão de protocolo.
- Limite de pedidos por jogador, payload limitado e rejeição de valores inválidos na esquiva.
- Registro de teclas e HUD apenas no cliente físico.
- Limpeza do cache visual ao desconectar.
- Acesso NBT renomeado para não conflitar com o método do Forge.
- Hooks separados para morte confirmada de mobs e ServerPlayer.
- Mixins configurados com refmap e verificação do JAR remapeado no build.
- COMPILAR-FORGE.bat mostra saída, mantém a janela aberta e grava build-forge.log.
- Regras do RPG e IDs preservados; Iron's ainda não integrado.
- Documentação antiga preservada em docs/legacy.

Status: fontes migradas; build Forge e execução no jogo não confirmados.
