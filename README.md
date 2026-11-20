# RedstoneLab

> Simulador e Editor Isométrico 2.5D de Circuitos de Redstone para Android nativo (Kotlin + Jetpack Compose) com exportação facilitada para Minecraft Bedrock Edition (`.mcpack`).

---

## Destaques do Projeto

- **Arquitetura 100% Nativa e Moderna**: Desenvolvido em Kotlin com Jetpack Compose e Material 3, estruturado em arquitetura reativa com ViewModel e StateFlow.
- **Tema Futurista Dark Blue com Acabamento Glassmorphism**: Interface cyberpunk com tons de azul elétrico (`#0077FE`), neon ciano (`#00F0FF`), preto espacial (`#060913`) e superfícies translúcidas com bordas luminosas.
- **Sem Emojis em Botões ou Cabeçalhos**: Todos os botões, chips, cabeçalhos e painéis utilizam ícones vetoriais Material e renderização Canvas de alta precisão.
- **Motor Isométrico 2.5D de Alta Performance**:
  - Projeção isométrica com ordenação de profundidade espacial contínua (*depth sorting*).
  - Suporte completo a gestos multitouch de Pan (arrastar) e Zoom (pinça).
  - Controle de múltiplas camadas Z (*layer isolations* e navegação vertical).
  - Raycasting de toque em tempo real com cursor holográfico na grade ativa.
- **Simulador Realista de Redstone**:
  - Ciclo de ticks discretos de Redstone (1 a 20 Hz, com pausa e avanço passo a passo manual).
  - Degradação de sinal de Redstone Dust (níveis de 0 a 15, atenuação de -1 por bloco e propagação 3D).
  - Alimentação de blocos sólidos: *Hard powering* vs *Soft powering*.
  - Inversão de tochas de Redstone com atraso de 1 tick e detecção de base energizada.
  - Repetidores com atrasos configuráveis (1 a 4 ticks), reforço de sinal para 15 e mecanismo de travamento lateral (*repeater locking*).
  - Comparadores de Redstone nos modos Comparação e Subtração.
  - Fontes de energia: Alavancas comutáveis, Botões de pulso momentâneo (10 ticks) e Blocos de Redstone contínuos.
  - Atuadores: Lâmpadas de Redstone com iluminação visual dinâmica, Pistões normais e aderentes (com empurrão e puxão de blocos até 12 blocos de limite) e Observadores.
- **Exportação Facilitada em `.mcpack` para Minecraft Bedrock**:
  - Gera pacotes completos de comportamento (*Behavior Pack*) em formato ZIP `.mcpack`.
  - `manifest.json` com UUIDs únicos em `format_version: 2`.
  - `pack_icon.png` gerado programaticamente em alta resolução.
  - Script nativo `functions/spawn.mcfunction` mapeando estados exatos de blocos do Bedrock (`minecraft:redstone_wire ["redstone_signal"=15]`, repetidores, tochas, etc.).
  - Script de limpeza rápida `functions/clear.mcfunction`.
  - Integração nativa com o Android Share Sheet via `FileProvider` para abrir diretamente no Minecraft Bedrock instalado ou salvar no dispositivo.
  - Cópia com um clique dos comandos `/function spawn` para a área de transferência.
- **Biblioteca de Circuitos Pré-configurados**:
  - *Repeater Pulse Clock*
  - *T-Flip-Flop (Toggle Latch)*
  - *Dual-Input AND Gate*
  - *XOR Gate (Exclusive OR)*
  - *2x2 Flush Piston Door*
  - *Comparator Pulse Extender*
  - *RS-NOR Latch Memory*

---

## Estrutura do Código

```text
RedstoneLab/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/redstonelab/app/
│       │   │   ├── MainActivity.kt
│       │   │   ├── RedstoneLabApp.kt
│       │   │   ├── theme/
│       │   │   │   ├── Color.kt             # Paleta neon ciano, azul e preto obsidiana
│       │   │   │   ├── Type.kt              # Tipografia monospace e sans moderna
│       │   │   │   └── Theme.kt             # Material 3 Dark Glassmorphic Theme
│       │   │   ├── model/
│       │   │   │   ├── Facing.kt            # Direções cardinais e vetoriais
│       │   │   │   ├── BlockType.kt         # Enumeração dos blocos de Redstone
│       │   │   │   ├── BlockState.kt        # Estado imutável do bloco (potência, atraso, modo)
│       │   │   │   ├── CircuitGrid.kt       # Espaço 3D e cálculo de limites do circuito
│       │   │   │   └── CircuitPreset.kt     # Circuitos prontos para carga
│       │   │   ├── engine/
│       │   │   │   ├── SimulationState.kt   # Estado de execução da simulação
│       │   │   │   └── RedstoneSimulator.kt # Motor de física e propagação de sinal de Redstone
│       │   │   ├── renderer/
│       │   │   │   ├── IsometricTransform.kt# Transformações matemáticas 3D -> 2.5D
│       │   │   │   └── IsometricRenderer.kt # Canvas Compose com renderização isométrica
│       │   │   ├── exporter/
│       │   │   │   └── BedrockPackExporter.kt # Gerador do pacote .mcpack para Minecraft Bedrock
│       │   │   ├── components/
│       │   │   │   └── Glassmorphic.kt      # Componentes GlassBox, GlassButton, CyberBadge
│       │   │   ├── ui/
│       │   │   │   ├── EditorScreen.kt      # Tela principal do editor e HUD
│       │   │   │   ├── ExportDialog.kt      # Modal futurista de exportação .mcpack
│       │   │   │   ├── PresetsDialog.kt     # Galeria de circuitos pré-configurados
│       │   │   │   └── CircuitInspectorPanel.kt # Painel flutuante de inspeção de blocos
│       │   │   └── viewmodel/
│       │   │       └── EditorViewModel.kt   # Gestão de estado, ferramentas e coroutines
│       │   └── res/
│       │       ├── values/
│       │       │   ├── strings.xml
│       │       │   ├── colors.xml
│       │       │   └── themes.xml
│       │       └── xml/
│       │           └── file_paths.xml       # Configuração do FileProvider para compartilhamento
│       └── test/java/com/redstonelab/app/
│           ├── RedstoneSimulatorTest.kt     # Testes unitários do motor de simulação
│           └── BedrockPackExporterTest.kt   # Testes unitários da geração do pacote .mcpack
├── gradle/
│   ├── libs.versions.toml                   # Version catalog Gradle
│   └── wrapper/                             # Gradle wrapper
├── .github/workflows/
│   └── build.yml                            # Pipeline de CI/CD para compilação do APK
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## Como Compilar e Executar

### Pré-requisitos
- JDK 17 ou 21
- Android SDK (API 35, Min SDK 26) ou Android Studio Jellyfish / Koala / Ladybug

### Comandos de Compilação
```bash
# Executar testes unitários
./gradlew testDebugUnitTest

# Gerar APK de depuração (Debug APK)
./gradlew assembleDebug

# O APK gerado estará em:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## Licença
Distribuído sob licença MIT. Criado para a comunidade de engenharia de Redstone de Minecraft Bedrock Edition.
