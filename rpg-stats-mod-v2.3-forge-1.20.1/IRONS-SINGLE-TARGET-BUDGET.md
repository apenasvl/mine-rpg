# Iron's — orçamento de dano por alvo

Esta fase complementa os perfis de balanceamento das 111 spells do Iron's com um limite final por **caster + alvo + spell**.

## Referência

O playtest atual informou aproximadamente **28 de dano** para o Guerreiro lvl 50, classe maximizada, habilidade ativa e espada de ferro. Esse valor passa a ser a referência de burst single-target do RPG Stats.

Para manter a identidade das classes:

- Guerreiro continua sendo a referência de maior pancada single-target nesse estágio de equipamento;
- Mago ganha valor por alcance, escolas, utilidade, controle e AoE;
- uma única instância de dano de spell do Iron's fica em no máximo **27** nesta camada;
- spells que lançam vários projéteis/recasts usam um orçamento cumulativo por alvo, então acertar 5–80 hits no mesmo boss não multiplica livremente todo o bônus do RPG Stats.

## AoE não é nerf global

O orçamento é separado por alvo. Portanto, uma Flaming Barrage/Arrow Volley/Starfall acertando vários inimigos ainda pode causar muito dano total no grupo. O que não pode acontecer é concentrar todos os projéteis com scaling completo em um único boss e superar facilmente uma build melee dedicada.

## Exemplos importantes

- **Flaming Barrage**: 5 projéteis; cada hit é limitado e o alvo pode receber no máximo 27 dentro da janela inicial de 40 ticks.
- **Eldritch Blast**: vários blasts; máximo de 27 por alvo na janela inicial.
- **Arrow Volley**: dezenas de flechas; máximo de 27 por alvo na janela inicial.
- **Chain Creeper**: múltiplos projéteis/chains; máximo de 24 por alvo na janela inicial.
- **Starfall** e spells contínuas/persistentes: usam orçamento por segundo/janela para evitar DPS explosivo em boss.
- **Sonic Boom**: nuke single-hit preservado, mas limitado a 27 nesta integração.
- **Flaming Strike / Divine Smite / Raise Hell / Echoing Strikes**: tratados como derivados de arma quando aplicável para evitar double dipping de dano físico + mágico.

## Leaky bucket

O orçamento não reinicia de forma brusca. Ele recupera gradualmente conforme os ticks passam. Isso evita picos artificiais na virada de uma janela fixa e mantém canais/DoTs funcionando de forma contínua.

## Segurança/performance

- somente hits reais criam estado;
- chave: caster UUID + target UUID + spell;
- máximo de 4096 estados em cache, descartando os mais antigos;
- estados muito antigos são reiniciados automaticamente;
- sem scan global de entidades e sem loop por tick.

## Checklist de runtime

1. Guerreiro lvl 50 + habilidade + espada de ferro: confirmar referência aproximada de 28.
2. Mago lvl 50: testar single-hit forte como Sonic Boom/Magic Arrow e confirmar que um hit não passa da referência.
3. Flaming Barrage: acertar os 5 projéteis no mesmo dummy/boss e medir o total; depois espalhar em vários mobs para confirmar que AoE continua valiosa.
4. Eldritch Blast e Arrow Volley: testar concentração em um alvo.
5. Starfall/Dragon Breath/Fire Breath/Ray of Frost: observar DPS sustentado.
6. Fireball/Lightning Lance/Lightning Bolt: validar burst single-hit.
7. Echoing Strikes/Flaming Strike/Divine Smite: confirmar que arma forte não recebe multiplicação dupla absurda.
8. Repetir pelo menos alguns testes em lvl 1, 25 e 50 para conferir progressão.

O hard cap global x2.25 continua existindo como última proteção, mas o orçamento por alvo é aplicado depois dele e é a proteção final contra spells cujo formato nativo multiplica muitos hits no mesmo inimigo.
