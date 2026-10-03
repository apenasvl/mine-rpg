# Validação v1.9.0-alpha.1

## Aprovado neste pacote

- arquivos JSON válidos;
- 5 classes, todas com origem de 33 pontos;
- 7 atributos e migração de Agilidade para Tenacidade;
- 25 caminhos e 75 especializações/Ascensões;
- 810 nodes sem IDs duplicados;
- 295 habilidades ativas esperadas;
- persistência de Stamina e Ecos recuperáveis;
- pacote de rede da esquiva validado no servidor;
- fases 2/3 de chefes ligadas ao pipeline de dano existente;
- HUD e tela K adaptadas aos sete atributos;
- nenhuma mecânica nova de frascos, equipamentos, postura ou quebra de postura.

Validador usado:

```text
python tools/validate_project.py
```

Resultado: `VALIDAÇÃO ESTÁTICA v1.9: OK`.

## Ainda precisa ser testado no Minecraft

O ambiente em que este pacote foi preparado não tinha a distribuição/dependências do Gradle disponíveis. Por isso, a compilação completa e os testes dentro do Minecraft devem ser feitos no computador do usuário.

No Windows, dentro da pasta do projeto:

```bat
gradlew.bat build
```

O arquivo esperado é `build\libs\rpgstats-1.9.0-alpha.1.jar`.

Teste primeiro em uma cópia do mundo. Verifique seleção de classe, migração de save antigo, esquiva em multiplayer, perda/recuperação de Ecos e cada mudança de fase de chefe.
