package com.redstonelab.app.viewmodel

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redstonelab.app.engine.RedstoneEngine
import com.redstonelab.app.engine.SimulationState
import com.redstonelab.app.exporter.McpackExporter
import com.redstonelab.app.model.BlockCoord
import com.redstonelab.app.model.BlockState
import com.redstonelab.app.model.BlockType
import com.redstonelab.app.model.CircuitGrid
import com.redstonelab.app.model.CircuitPreset
import com.redstonelab.app.model.CircuitPresetsLibrary
import com.redstonelab.app.model.Facing
import com.redstonelab.app.storage.CircuitProjectMetadata
import com.redstonelab.app.storage.ProjectManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.ArrayDeque
import java.util.UUID

enum class EditorMode {
    BUILD,      // Construção: tocar coloca/substitui blocos
    SIMULATION  // Simulação: tocar interage com alavancas, botões, etc.
}

enum class EditorTool {
    PLACE,
    ERASE,
    INSPECT
}

class EditorViewModel : ViewModel() {

    // Estado do Circuito e Motor de Simulação
    var circuitGrid by mutableStateOf(CircuitGrid())
        private set

    private var engine = RedstoneEngine(circuitGrid)

    var currentProjectId by mutableStateOf(UUID.randomUUID().toString())
        private set

    var circuitName by mutableStateOf("Porta Secreta")
        private set

    // Modos de Operação
    var editorMode by mutableStateOf(EditorMode.BUILD)
        private set

    var selectedTool by mutableStateOf(EditorTool.PLACE)
        private set

    // Camada e Visualização
    var activeLayerY by mutableIntStateOf(1) // Altura Y (0 a 7+)
        private set

    var isOnionSkinEnabled by mutableStateOf(true)
        private set

    var showSignalNumbers by mutableStateOf(true)
        private set

    // Bloco Selecionado na Paleta
    var selectedBlockType by mutableStateOf(BlockType.REDSTONE_WIRE)
        private set

    var selectedFacing by mutableStateOf(Facing.NORTH)
        private set

    var selectedBlockCoord by mutableStateOf<BlockCoord?>(null)
        private set

    // Transformações da Câmera Isométrica
    var panOffset by mutableStateOf(Offset(450f, 400f))
    var zoom by mutableFloatStateOf(1.0f)

    // Simulação
    var simulationState by mutableStateOf(SimulationState(tickRateHz = 10.0f))
        private set

    private var simulationJob: Job? = null

    // Pilhas de Histórico Undo / Redo
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    // Modais e Diálogos
    var showProjectsDialog by mutableStateOf(false)
    var showPresetsDialog by mutableStateOf(false)
    var showExportDialog by mutableStateOf(false)
    var exportedPackFile by mutableStateOf<File?>(null)
    var exportedSanitizedName by mutableStateOf("")

    // Lista de Projetos Locais Offline
    var localProjects by mutableStateOf<List<CircuitProjectMetadata>>(emptyList())
        private set

    init {
        // Inicializa com base de pedra lisa na camada 0
        clearToDefaultPlatform()
        saveHistorySnapshot()
    }

    private fun saveHistorySnapshot() {
        if (undoStack.size > 30) {
            undoStack.removeLast()
        }
        undoStack.push(circuitGrid.toJson())
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.push(circuitGrid.toJson())
            val prevJson = undoStack.pop()
            circuitGrid = CircuitGrid.fromJson(prevJson)
            engine = RedstoneEngine(circuitGrid)
            engine.recalculateWireSignals()
            circuitGrid = circuitGrid.clone()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(circuitGrid.toJson())
            val nextJson = redoStack.pop()
            circuitGrid = CircuitGrid.fromJson(nextJson)
            engine = RedstoneEngine(circuitGrid)
            engine.recalculateWireSignals()
            circuitGrid = circuitGrid.clone()
        }
    }

    // --- Alternância de Modos e Ferramentas ---

    fun toggleEditorMode() {
        editorMode = if (editorMode == EditorMode.BUILD) {
            EditorMode.SIMULATION
        } else {
            pauseSimulation()
            EditorMode.BUILD
        }
    }

    fun setTool(tool: EditorTool) {
        selectedTool = tool
    }

    fun setBlockType(type: BlockType) {
        selectedBlockType = type
        selectedTool = EditorTool.PLACE
    }

    fun setLayer(y: Int) {
        activeLayerY = y.coerceIn(0, 15)
    }

    fun toggleOnionSkin() {
        isOnionSkinEnabled = !isOnionSkinEnabled
    }

    fun toggleSignalNumbers() {
        showSignalNumbers = !showSignalNumbers
    }

    fun rotateFacingClockwise() {
        selectedFacing = selectedFacing.clockwise()
    }

    // --- Controles de Simulação ---

    fun toggleSimulation() {
        if (simulationState.isRunning) {
            pauseSimulation()
        } else {
            editorMode = EditorMode.SIMULATION
            startSimulation()
        }
    }

    fun startSimulation() {
        simulationState = simulationState.copy(isRunning = true)
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (isActive && simulationState.isRunning) {
                stepSingleTick()
                val intervalMs = (1000f / simulationState.tickRateHz).toLong().coerceAtLeast(50L)
                delay(intervalMs)
            }
        }
    }

    fun pauseSimulation() {
        simulationState = simulationState.copy(isRunning = false)
        simulationJob?.cancel()
        simulationJob = null
    }

    fun stepSingleTick() {
        val start = System.currentTimeMillis()
        engine.stepTick()
        val duration = System.currentTimeMillis() - start

        circuitGrid = circuitGrid.clone()
        simulationState = simulationState.copy(
            totalTicks = engine.tickCount,
            activeSignalCount = circuitGrid.activeCount,
            lastStepDurationMs = duration
        )
    }

    fun resetSimulation() {
        pauseSimulation()
        engine.reset()
        circuitGrid = circuitGrid.clone()
        simulationState = simulationState.copy(
            totalTicks = 0L,
            activeSignalCount = circuitGrid.activeCount
        )
    }

    // --- Interações do Usuário no Canvas ---

    fun onTileClicked(coord: BlockCoord, context: Context? = null) {
        selectedBlockCoord = coord

        if (editorMode == EditorMode.SIMULATION) {
            // No modo simulação: tocar interage com alavancas, botões, placas e sensores
            val block = circuitGrid.get(coord) ?: return
            var handled = false

            when (block.type) {
                BlockType.LEVER -> {
                    circuitGrid.set(block.toggleLever())
                    handled = true
                }
                BlockType.STONE_BUTTON -> {
                    circuitGrid.set(block.triggerButton())
                    handled = true
                }
                BlockType.STONE_PRESSURE_PLATE -> {
                    circuitGrid.set(block.togglePressurePlate())
                    handled = true
                }
                BlockType.DAYLIGHT_DETECTOR -> {
                    circuitGrid.set(block.toggleDaylightDetector())
                    handled = true
                }
                BlockType.SCULK_SENSOR -> {
                    circuitGrid.set(block.triggerSculkSensor())
                    handled = true
                }
                BlockType.REDSTONE_REPEATER -> {
                    circuitGrid.set(block.cycleDelay())
                    handled = true
                }
                BlockType.REDSTONE_COMPARATOR -> {
                    circuitGrid.set(block.toggleSubtractMode())
                    handled = true
                }
                else -> {}
            }

            if (handled) {
                vibrate(context, 15)
                engine.stepTick()
                circuitGrid = circuitGrid.clone()
            }
            return
        }

        // Modo Construção
        when (selectedTool) {
            EditorTool.PLACE -> {
                saveHistorySnapshot()
                val newState = BlockState(
                    coord = coord,
                    type = selectedBlockType,
                    facing = selectedFacing,
                    power = if (selectedBlockType == BlockType.REDSTONE_BLOCK) 15 else 0
                )
                circuitGrid.set(newState)
                vibrate(context, 15)
                refreshGridAndSimulate()
            }

            EditorTool.ERASE -> {
                if (circuitGrid.get(coord) != null) {
                    saveHistorySnapshot()
                    circuitGrid.remove(coord)
                    vibrate(context, 20)
                    refreshGridAndSimulate()
                }
            }

            EditorTool.INSPECT -> {
                // Mantém coord selecionada para o painel de inspeção
            }
        }
    }

    fun onTileLongClicked(coord: BlockCoord, context: Context? = null) {
        val block = circuitGrid.get(coord) ?: return

        saveHistorySnapshot()
        if (block.type.isInteractive) {
            // Toque longo ajusta rapidamente repetidores/comparadores ou gira o bloco
            when (block.type) {
                BlockType.REDSTONE_REPEATER -> circuitGrid.set(block.cycleDelay())
                BlockType.REDSTONE_COMPARATOR -> circuitGrid.set(block.toggleSubtractMode())
                else -> {
                    if (block.type.isDirectional) {
                        circuitGrid.set(block.copy(facing = block.facing.clockwise()))
                    }
                }
            }
        } else if (block.type.isDirectional) {
            circuitGrid.set(block.copy(facing = block.facing.clockwise()))
        }
        vibrate(context, 30)
        refreshGridAndSimulate()
    }

    private fun refreshGridAndSimulate() {
        engine.recalculateWireSignals()
        circuitGrid = circuitGrid.clone()
        simulationState = simulationState.copy(activeSignalCount = circuitGrid.activeCount)
    }

    fun clearToDefaultPlatform() {
        pauseSimulation()
        circuitGrid.clear()
        // Plataforma padrão de pedra lisa 8x8 na camada 0
        for (x in 4..11) {
            for (z in 4..11) {
                circuitGrid.set(BlockState(BlockCoord(x, 0, z), BlockType.SMOOTH_STONE))
            }
        }
        engine = RedstoneEngine(circuitGrid)
        refreshGridAndSimulate()
        activeLayerY = 1
    }

    // --- Projetos Locais Offline ---

    fun refreshProjectsList(context: Context) {
        localProjects = ProjectManager.listProjects(context)
    }

    fun saveCurrentProject(context: Context, name: String) {
        circuitName = name
        ProjectManager.saveProject(context, currentProjectId, name, circuitGrid)
        refreshProjectsList(context)
    }

    fun loadLocalProject(context: Context, id: String) {
        val loaded = ProjectManager.loadProject(context, id) ?: return
        pauseSimulation()
        currentProjectId = id
        circuitName = loaded.first
        circuitGrid = loaded.second
        engine = RedstoneEngine(circuitGrid)
        refreshGridAndSimulate()
        activeLayerY = 1
    }

    fun deleteLocalProject(context: Context, id: String) {
        ProjectManager.deleteProject(context, id)
        refreshProjectsList(context)
    }

    fun newProject() {
        currentProjectId = UUID.randomUUID().toString()
        circuitName = "Novo Circuito"
        clearToDefaultPlatform()
        saveHistorySnapshot()
    }

    fun loadPreset(preset: CircuitPreset) {
        pauseSimulation()
        circuitName = preset.title
        currentProjectId = UUID.randomUUID().toString()
        circuitGrid = preset.builder()
        engine = RedstoneEngine(circuitGrid)
        refreshGridAndSimulate()
        panOffset = Offset(450f, 400f)
        zoom = 1.0f
        saveHistorySnapshot()
    }

    // --- Exportação .mcpack ---

    fun exportToMcpack(
        context: Context,
        name: String,
        includeSmoothStoneBase: Boolean
    ): File {
        circuitName = name
        val packFile = McpackExporter.saveMcpackFile(context, circuitGrid, name, includeSmoothStoneBase)
        exportedPackFile = packFile
        exportedSanitizedName = McpackExporter.sanitizeCircuitName(name)
        return packFile
    }

    private fun vibrate(context: Context?, durationMs: Long) {
        if (context == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
