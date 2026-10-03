# Validação v2.0

Execute no diretório do projeto:

```powershell
.\gradlew.bat build
python tools\validate_project.py
```

O validador confirma a cobertura das ativas do Mago, das ativas genéricas, regras de Casa fixa, persistência de multiclasse e HUD de detalhes pós-compra.

O build completo continua precisando ser executado em um ambiente com as dependências Gradle do Minecraft disponíveis.
