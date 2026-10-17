# O Observador — mod de terror para Minecraft Java

## Versões alvo (confirmadas em 17/10/2026)

| Item | Versão |
|---|---|
| Minecraft Java Edition | 26.3 ("Wilderness Bound", lançada em 15/09/2026) |
| Carregador | NeoForge 26.3.0.86 (última estável) |
| Java | 25 (exigido desde o Minecraft 26.1) |

Por que NeoForge: API de eventos e configuração (`ModConfigSpec`) bem documentada, boa
sincronização de entidades e rede em servidor, e ferramenta de desenvolvimento (ModDevGradle) mantida junto com o carregador.

## Estado atual: Etapa 2 (esqueleto)

Implementado, ainda **não compilado**:
- Estrutura Gradle (ModDevGradle), `neoforge.mods.toml`.
- Classe principal do mod, carregando a configuração.
- Arquivo de configuração `observador-common.toml` com todas as opções planejadas.
- Diagnóstico por log (`diagnostico.logDetalhado`).

Ainda não existe: criaturas, IA, eventos, sanidade, sons, texturas (etapas 3 a 6).

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

## Teste da Etapa 2

- O jogo abre sem erro e o mod aparece na lista de mods.
- O arquivo `config/observador-common.toml` é criado.
- Com `logDetalhado = true`, o log mostra o resumo da configuração.
