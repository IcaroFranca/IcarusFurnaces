# IcarusFurnaces — instruções permanentes do projeto

Convenções combinadas com o dono do projeto, herdadas do IcarusChests (mesmo
dono, mesmo estilo de plugin). Valem para toda sessão futura, não só para a
mudança que estava em andamento quando foram escritas.

## 1. Todo item craftável novo vai para o Livro de Receitas

Sempre que um item novo puder ser craftado (kit de upgrade, o que for), a
receita dele **tem que** ser adicionada em
`src/main/java/dev/icaro/icarusfurnaces/gui/FurnaceRecipeBookRegistry.java`
(`buildAll()`). O plugin não lista as receitas do Bukkit automaticamente — se
não entrar aqui, o jogador não descobre como craftar, mesmo com a receita de
verdade registrada e funcionando.

## 2. Versionamento — MAJOR.MINOR.PATCH em `build.gradle`

Toda mudança publicada muda a `version` em `build.gradle` (`plugin.yml` lê
`${version}` de lá, não precisa mexer nos dois). Antes de cada commit que
altera comportamento do plugin, decida:

- **Correção de erro/bug** → sobe o terceiro número (`PATCH`): `1.0.0` → `1.0.1`.
- **Atualização pequena** (ajuste, melhoria pontual, não quebra nada) → sobe o
  segundo número e zera o terceiro (`MINOR`): `1.0.1` → `1.1.0`.
- **Atualização grande** (feature nova, mudança estrutural) → sobe o primeiro
  número e zera os outros dois (`MAJOR`): `1.1.0` → `2.0.0`.

Mudança só em documentação/comentário/teste sem afetar o jar publicado não
precisa de bump.

## 3. README

O `README.md` da raiz deve continuar refletindo o estado real do plugin —
tiers, ticks, comandos, etc. Ao adicionar uma feature grande o suficiente
para entrar aqui neste CLAUDE.md, considere se o README também precisa de
uma seção nova.

## 4. Aplicar kit de upgrade não é sequencial — decisão deliberada

Qualquer kit de upgrade funciona em qualquer fornalha, não importa o tier
atual dela — dá pra pular direto pro Netherite numa fornalha comum, ou
"rebaixar" aplicando um kit de tier mais baixo. Isso é proposital, pedido
explicitamente pelo dono do projeto, não uma lacuna de validação: nunca
reintroduza uma checagem de "só serve pra próximo tier" em
`FurnaceInteractListener` sem confirmar com ele antes. A única restrição
que existe é não deixar aplicar um kit do tier que a fornalha já tem (evita
gastar o kit à toa).

## 5. Escopo deliberado do MVP

O sistema de tier só se aplica à Fornalha comum (`Material.FURNACE`) —
Fornalha a Lenha (Blast Furnace) e Defumador (Smoker) continuam 100%
vanilla, decisão explícita do dono do projeto. Se isso mudar no futuro, via
de regra cada bloco vai precisar da sua própria tag de tier (a mesma PDC
`FURNACE_TIER` não deve ser reaproveitada entre tipos de bloco diferentes
sem revisar `FurnaceCookSpeedListener`/`FurnaceInteractListener` a fundo).
