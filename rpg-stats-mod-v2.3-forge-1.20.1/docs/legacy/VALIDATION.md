# Validação — 10/09/2026
Executado: python3 tools/validate_project.py
- 3288 verificações de regras Java: pares de Casas, mesma classe, nível, escolha única, ordem, compras repetidas, orçamento, efeitos finitos, roundtrip NBT e reembolso legado exato/sem repetição.
- Compilação real das classes de domínio com Java 17. NBT e StatsManager substituídos por stubs apenas nos testes.
- Parser Java aceitou os 35 arquivos de produção.
- Gradle build --offline bloqueado antes da compilação: distribuição Gradle 8.8 ausente e Network is unreachable. Nenhum JAR gerado.
Isso NÃO comprova compatibilidade dos tipos Fabric/Minecraft, renderização, rede ou execução das habilidades.

## Pendências de jogo
1. Compilar com dependências completas e iniciar cliente e servidor dedicado.
2. Migrar uma cópia de personagem 2.0 e conferir PH, slots e progresso principal.
3. Testar todas as ativas contra alvos reais; medir perda de HP, custo, recarga, duração, alvos aliados e invocações.
4. Testar afinidades Temporal/Oculta e Vanguarda/Berserker no nível máximo.
5. Testar HUD nas escalas de GUI 2, 3 e 4; tooltips após compra/equipar e telas pequenas.
6. Testar morte, reconexão, Ecos, stamina e esquiva em multiplayer.

## Limitações conhecidas
A auditoria integral de todas as habilidades não está concluída. Árvores não mágicas continuam compartilhando implementações por famílias; nomes distintos não significam mecânicas exclusivas. As descrições foram alinhadas a essas implementações.
Mecânicas mágicas discretas e efeitos indiretos exigem verificação manual; escalas de magnitude não se aplicam automaticamente a constantes internas.
A ativação mágica ainda usa snapshots de dados: efeitos que alteram recursos durante callbacks aninhados precisam de teste específico para evitar sobrescrita de ganhos.
Não há redefinição de afinidade pela interface. Os scripts validate_v16/v17/v18/v19/v20 são históricos e não certificam esta versão.

