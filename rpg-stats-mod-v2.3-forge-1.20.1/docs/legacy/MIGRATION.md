# Migração de personagens 2.0
Faça cópia do mundo antes de testar esta alpha.
DATA_VERSION passa a 10. Personagens com classe já definida são considerados despertos.
Cada compra antiga com prefixo sec_ reconhecida na antiga classe/Casa secundária devolve custo do talento +1 PH. IDs desconhecidos são removidos sem inventar valores de reembolso.
Slots equipados e último talento sec_ são limpos. Campos da segunda classe deixam de ser gravados.
Uma vez salvo no formato 10, carregar novamente não reembolsa outra vez.
A progressão principal continua sujeita às migrações históricas já existentes.
A afinidade começa sem escolha. Não há conversão automática de outra classe em Casa.

