# 🔥 IcarusFurnaces

**Fornalhas com tiers e kits de upgrade encaixáveis para servidores Paper — companheiro do IcarusChests, sem exigir nenhum mod do lado do jogador.**

[![Build](https://github.com/IcaroFranca/IcarusFurnaces/actions/workflows/build.yml/badge.svg)](https://github.com/IcaroFranca/IcarusFurnaces/actions/workflows/build.yml)
![Versão](https://img.shields.io/badge/vers%C3%A3o-1.4.1-blueviolet)
![Paper](https://img.shields.io/badge/Paper-1.21.x-2ecc71)
![Java](https://img.shields.io/badge/Java-21-orange)

Reaproveita o bloco de fornalha vanilla e acelera o smelt através de um kit
de upgrade consumível, craftado em cadeia — cada tier usa o kit do tier
anterior como base. Nada de resource pack, nada de instalar nada no cliente.

---

## ✨ Funcionalidades

- **7 tiers de fornalha**, do Cobre ao Netherite, cada um mais rápido que o anterior.
- **Kits de upgrade** — craftados com uma fornalha comum no meio da grade, cercada pelo material daquele tier (com duas exceções de receita, veja a tabela). Aplicar o kit continua sequencial: só pega numa fornalha que já esteja exatamente no tier anterior. Quebrar (ou explodir) uma fornalha evoluída devolve todos os kits usados nela até o tier atual — upgrade nunca é um gasto sem volta.
- **Fornalha vanilla de verdade**: a GUI de 3 slots (entrada/combustível/saída) nunca muda — só o tempo de smelt. O nome da fornalha na GUI muda para refletir o tier atual (ex: "Fornalha de Diamante").
- **Partículas coloridas** — cada tier solta uma fagulha na cor do seu minério (cobre, ferro, ouro, diamante, esmeralda, obsidiana, netherite) toda vez que começa a smeltar um item ou acende um novo combustível, além de uma explosão maior de partículas no instante do upgrade. Quanto mais rápido o tier, mais frequente a fagulha — é a única forma de ver o tier de fora sem abrir a fornalha.
- **Livro de Receitas** in-game — um menu mostra o ícone de todo kit craftável de uma vez; clique em qualquer um pra ver exatamente como craftar.
- **Escopo deliberado**: só a Fornalha comum é afetada. Fornalha a Lenha (Blast Furnace) e Defumador (Smoker) continuam 100% vanilla.

## 🔥 Progressão das fornalhas

| Tier | Ticks por item | Receita do kit |
|---|---|---|
| Cobre | 180 | Fornalha (centro) + 8× Lingote de Cobre |
| Ferro | 160 | Fornalha (centro) + 8× Lingote de Ferro |
| Ouro | 120 | Fornalha (centro) + 8× Lingote de Ouro |
| Diamante | 80 | Fornalha (centro) + 4× Diamante + 4× Vidro |
| Esmeralda | 40 | Fornalha (centro) + 8× Esmeralda |
| Obsidiana | 20 | Fornalha (centro) + 8× Obsidiana |
| Netherite | 5 | Fornalha (centro) + 2× Lingote de Netherite |

Para referência: uma fornalha vanilla, sem nenhum kit, leva 200 ticks (10s)
por item — todo tier do IcarusFurnaces já é mais rápido que isso desde o
primeiro degrau.

Cada tier evolui craftando o kit correspondente e usando shift + botão
direito na fornalha do tier anterior (ou numa fornalha comum, no caso do
Cobre). Aplicar o kit não reseta o que já está smeltando.

## ⌨️ Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/icarusfurnaces` (ou `/icarusfurnaces ping`) | Checagem rápida — confirma que o plugin está online. | — |
| `/icarusfurnaces info` | Mostra o tier e a velocidade da fornalha mirada. | `icarusfurnaces.info` (padrão: todos) |
| `/icarusfurnaces recipebook` | Abre o menu do Livro de Receitas. | `icarusfurnaces.recipebook` (padrão: todos) |
| `/icarusfurnaces give <tier> [jogador]` | Entrega um kit de upgrade de um tier. | `icarusfurnaces.admin` (padrão: op) |
| `/icarusfurnaces reload` | Recarrega `config.yml` sem reiniciar o servidor. | `icarusfurnaces.admin` (padrão: op) |

Aliases: `/icarusf`, `/ifu`.

## ⚙️ Configuração

Tudo em `config.yml` — nenhuma dessas seções é obrigatória: sem uma textura
configurada, o kit cai num ícone vanilla de fallback (ex: o kit de Cobre vira
um Lingote de Cobre renomeado) — nunca quebra por falta de configuração.

```yaml
# Textura Base64 de cada kit de upgrade (pegue em minecraft-heads.com ou similar).
upgrade-kit-heads:
  copper: "..."
  iron: "..."
  # ...
```

## 🛠️ Compilando

Requer JDK 21.

```bash
git clone https://github.com/IcaroFranca/IcarusFurnaces.git
cd IcarusFurnaces
./gradlew build        # gera o jar em build/libs/
./gradlew runServer    # sobe um servidor Paper de teste com o plugin já instalado
```

O build é validado no CI (GitHub Actions) a cada push.

## 🗺️ Arquitetura, em uma frase

O tier de uma fornalha vive inteiramente na PDC do próprio bloco (o servidor
já persiste isso como parte do chunk) — sem banco de dados, ao contrário do
IcarusChests: a capacidade de uma fornalha nunca muda entre tiers, só o
tempo de smelt (`FurnaceStartSmeltEvent#setTotalCookTime`), então não existe
conteúdo pra redimensionar num upgrade.

## 📋 Limitações conhecidas

- Só a Fornalha comum é afetada — Fornalha a Lenha e Defumador continuam vanilla.
- O tempo de queima do combustível não é ajustado por tier: uma fornalha mais
  rápida consome os itens de combustível bem mais rápido (mais itens
  smeltados por unidade de combustível) — comportamento esperado, não um bug.
- Nenhuma receita do plugin (kits de upgrade) aparece no livro de receitas
  *vanilla* do Minecraft — todas ficam só no Livro de Receitas próprio do
  IcarusFurnaces (`/icarusfurnaces recipebook`).

## 📜 Créditos

Companheiro do [IcarusChests](https://github.com/IcaroFranca/IcarusChests).
Desenvolvido para rodar inteiramente do lado do servidor, sem exigir nada do
jogador além de um cliente vanilla.
