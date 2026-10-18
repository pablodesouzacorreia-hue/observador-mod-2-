# O Observador — mod de terror para Minecraft Java

## Versões alvo (confirmadas em 17/10/2026)

| Item | Versão |
|---|---|
| Minecraft Java Edition | 26.3 ("Wilderness Bound", lançada em 15/09/2026) |
| Carregador | NeoForge 26.3.0.86 (última estável) |
| Java | 25 (exigido desde o Minecraft 26.1) |

Por que NeoForge: API de eventos e configuração (`ModConfigSpec`) bem documentada, boa
sincronização de entidades e rede em servidor, e ferramenta de desenvolvimento (ModDevGradle) mantida junto com o carregador.

## Estado atual

**Compilado e testado no jogo (Etapas 2 e 3):** carregamento do mod, configuração, a entidade O Observador
com modelo, textura e IA básica (observar, seguir, perseguir, procurar).

**Escrito nesta rodada, ainda NÃO compilado nem testado:**
- Novo visual do Observador: manto escuro, cabeça caída, olhos vermelhos, mãos pálidas e 16 fios negros
  balançando em volta da cabeça. Fica sempre "iluminado" no escuro (só os olhos e as mãos se destacam).
- Sem nome flutuante sobre a criatura.
- Perseguição mais frequente (chance base subiu de 4% para 18%, +1,2% por encontro) e **provocação**:
  correr na direção dela a faz perseguir em ~60% das vezes.
- **Som alto e perturbador durante toda a perseguição**: lamento distorcido ao começar (tocando "dentro da cabeça"
  do jogador) e repetido a cada ~3 s vindo da criatura. Um impacto (`jump`) toca quando ela acerta o jogador.
  Grito/estímulo extra no início controlado por `sustos.frequenciaJumpscares`.
- Sons originais sintetizados (passos, batidas, rangido, respiração, sussurro, escavação, grito, estímulo, lamento, impacto).
- Sanidade (cai no escuro, caverna, isolamento e perto da criatura; recupera com luz e companhia).
- Eventos psicológicos: passos atrás do jogador, batidas e rangido de porta, passos no telhado,
  escavação em caverna, sussurro na nuca, e miragens (silhueta inofensiva na borda da visão que some ao ser encarada).
- Modo de teste `diagnostico.acelerarEventos`: aparições a cada ~30 s e eventos a cada ~10 s, de dia ou de noite.

**Ainda não existe:** Imitador, Perseguidor e Presença; itens e história ambiental; momentos de silêncio absoluto;
névoa e eventos de tempestade; efeitos visuais de sanidade além de um leve escurecimento; sincronização
multiplayer testada; testes automatizados.

## Configuração (`config/observador-common.toml`)

| Opção | O que faz |
|---|---|
| `geral.intensidadeTerror` | Escala geral (aparições e eventos). 0 desliga tudo. |
| `criaturas.frequenciaAparicoes` | Multiplica a frequência das aparições e dos eventos. |
| `criaturas.agressividade` | Chance, duração e dano das perseguições. |
| `sustos.frequenciaJumpscares` | Chance dos sustos fortes (grito inicial e impacto no golpe). 0 desliga. |
| `audio.intensidadeSons` | Volume de todos os sons do mod (0 a 1,5). **Os sons de perseguição são muito altos de propósito; reduza aqui se usar fones.** |
| `sanidade.ativada` | Liga/desliga a sanidade. |
| `eventos.psicologicos` | Liga/desliga os eventos psicológicos. |
| `acessibilidade.reduzirFlashes` | Desliga o escurecimento de tela da sanidade baixa. |
| `diagnostico.logDetalhado` | Escreve as decisões da IA no log e mostra a sanidade na tela. |
| `diagnostico.acelerarEventos` | Modo de teste (ver acima). |

## Como compilar

Requer Java 25 e acesso à internet (Maven do NeoForge e da Mojang). O jeito mais simples é o GitHub Actions
(arquivo `.github/workflows/build.yml`): ao enviar os arquivos para o repositório, a aba **Actions** gera o `.jar`.

Localmente:

```bash
gradle wrapper          # uma vez, se não houver ./gradlew
./gradlew build         # gera build/libs/observador-0.1.0.jar
./gradlew runClient     # abre o Minecraft de desenvolvimento
./gradlew runServer     # servidor de teste
```

## Como instalar

1. Instale o NeoForge 26.3.0.86 para o Minecraft 26.3.
2. Copie `observador-0.1.0.jar` para a pasta `mods/` (no cliente e no servidor).
3. Inicie o jogo. O log (`logs/latest.log`) deve conter `[Observador] Mod construído.`

## Como testar

1. Crie um mundo novo com cheats e feche o jogo.
2. Abra `config/observador-common.toml`, troque `acelerarEventos = true` e `logDetalhado = true`. Abra o jogo de novo.
3. Fique em uma caverna ou à noite. A cada ~30 s uma criatura pode aparecer; a cada ~10 s acontece um evento sonoro.
4. Para chamar a criatura na hora: `/summon observador:observador ~ ~ ~20`.
5. Teste: encará-la (ela some ou muda de lugar), correr até ela (ela costuma perseguir, com o lamento alto),
   tochas por perto (ela some), golpeá-la (ela recua).
6. Para voltar ao normal, desligue `acelerarEventos`.
