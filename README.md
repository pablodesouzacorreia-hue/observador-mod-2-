# O Observador — mod de terror para Minecraft Java

## Versões alvo (confirmadas em 17/10/2026)

| Item | Versão |
|---|---|
| Minecraft Java Edition | 26.3 ("Wilderness Bound", lançada em 15/09/2026) |
| Carregador | NeoForge 26.3.0.86 (última estável) |
| Java | 25 (exigido desde o Minecraft 26.1) |

Por que NeoForge: API de eventos e configuração (`ModConfigSpec`) bem documentada, boa
sincronização de entidades e rede em servidor, e ferramenta de desenvolvimento (ModDevGradle) mantida junto com o carregador.

## Estado atual: Etapa 3 (O Observador)

Etapa 2 (base) — **compilada e testada no jogo**:
- Estrutura Gradle (ModDevGradle), `neoforge.mods.toml`, classe principal, configuração e diagnóstico.

Etapa 3 — escrita, **ainda não compilada**:
- Entidade `O Observador` (`observador:observador`): humanoide alto e magro, braços longos, cabeça torta.
- IA com quatro estados: observar, seguir (STALK), perseguir (CHASE) e procurar (SEARCH).
  - Só percebe o jogador por visão direta ou por som de corrida; não sabe a posição "magicamente".
  - Congela quando o jogador olha para ela; se for encarada, some, muda de lugar ou (raramente) persegue.
  - Movimento irregular: quase parada, de repente uma arrancada.
  - Fica mais ousada conforme o jogador a encontra (tensão por jogador).
- Contramedidas: luz forte por perto a faz sumir; um golpe do jogador a faz recuar; depois de 2 golpes dela, ela recua.
- Aparições (`AppearanceManager`): só no Overworld, no escuro, fora do campo de visão do jogador,
  uma criatura por região, com intervalo mínimo e probabilidade controlada pela configuração.
- Modelo 3D próprio, textura própria (gerada por script) e nomes em pt-BR e en-US.

Ainda não existe: sons próprios, eventos psicológicos, sanidade, névoa/ambientação,
Imitador, Perseguidor, Presença, história e multiplayer testado (etapas 4 a 6).
Os sons desta etapa são só os de passos do próprio jogo.

## Como compilar

Requer Java 25 e acesso à internet (Maven do NeoForge e da Mojang).

```bash
cd observador-mod
gradle wrapper          # uma vez, se não houver ./gradlew
./gradlew build         # gera build/libs/observador-0.1.0.jar
./gradlew runClient     # abre o Minecraft de desenvolvimento
./gradlew runServer     # servidor de teste
```

## Como instalar

1. Instale o NeoForge 26.3.0.86 para o Minecraft 26.3.
2. Copie `build/libs/observador-0.1.0.jar` para a pasta `mods/` (no cliente e no servidor).
3. Inicie o jogo. O log (`logs/latest.log`) deve conter `[Observador] Mod construído.`

## Teste da Etapa 3

1. Crie um mundo novo em modo Sobrevivência (ou Criativo para testar sem morrer).
2. Chame a criatura direto, sem esperar: `/summon observador:observador ~ ~ ~20`
   (precisa de cheats ativados no mundo).
3. Olhe para ela: ela deve congelar, e depois de alguns segundos sumir, mudar de lugar ou perseguir.
4. Afaste-se, corra, esconda-se atrás de blocos: ela só deve te seguir quando te vê ou te ouve correndo.
5. Coloque várias tochas perto dela: ela deve desaparecer.
6. Para ver o que a IA decide, ligue `diagnostico.logDetalhado = true` em `config/observador-common.toml`
   e acompanhe `logs/latest.log`.
7. Aparição natural: fique no escuro (caverna ou noite), sozinho, por alguns minutos.
