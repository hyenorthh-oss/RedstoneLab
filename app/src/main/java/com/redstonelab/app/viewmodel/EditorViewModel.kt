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
import com.redstonelab.app.engine.RedstoneSimulator
import com.redstonelab.app.engine.SimulationState
import com.redstonelab.app.exporter.BedrockPackExporter
import com.redstonelab.app.model.BlockCoord
import com.redstonelab.app.model.BlockState
import com.redstonelab.app.model.BlockType
import com.redstonelab.app.model.CircuitGrid
import com.redstonelab.app.model.CircuitPreset
import com.redstonelab.app.model.CircuitPresetsLibrary
import com.redstonelab.app.model.Facing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class EditorTool(val label: String) {
    PLACE("Place"),
    BREAK("Break"),
    INTERACT("Interact"),
    INSPECT("Inspect")
}

class EditorViewModel : ViewModel() {

    // Circuit Grid and Simulation Engine
    var circuitGrid by mutableStateOf(CircuitGrid())
        private set

    private var simulator = RedstoneSimulator(circuitGrid)

    var circuitName by mutableStateOf("Repeater Clock")
        private set

    // Editor Viewport States
    var activeLayerZ by mutableIntStateOf(1)
        private set

    var isLayerIsolationEnabled by mutableStateOf(false)
        private set

    var selectedTool by mutableStateOf(EditorTool.INTERACT)
        private set

    var selectedBlockType by mutableStateOf(BlockType.REDSTONE_WIRE)
        private set

    var selectedFacing by mutableStateOf(Facing.NORTH)
        private set

    var selectedBlockCoord by mutableStateOf<BlockCoord?>(null)
        private set

    var panOffset by mutableStateOf(Offset(500f, 600f))
    var zoom by mutableFloatStateOf(1.0f)

    // Simulation Execution States
    var simulationState by mutableStateOf(SimulationState())
        private set

    private var simulationJob: Job? = null

    // Dialog Visibilities
    var showPresetsDialog by mutableStateOf(false)
    var showExportDialog by mutableStateOf(false)
    var showHelpDialog by mutableStateOf(false)

    init {
        // Load default preset on startup
        loadPreset(CircuitPresetsLibrary.presets.first())
    }

    fun setTool(tool: EditorTool) {
        selectedTool = tool
    }

    fun setBlockType(type: BlockType) {
        selectedBlockType = type
        selectedTool = EditorTool.PLACE
    }

    fun setLayer(z: Int) {
        activeLayerZ = z.coerceIn(-5, 10)
    }

    fun toggleLayerIsolation() {
        isLayerIsolationEnabled = !isLayerIsolationEnabled
    }

    fun rotateFacingClockwise() {
        selectedFacing = selectedFacing.clockwise()
    }

    fun setProjectName(name: String) {
        circuitName = name
    }

    // --- Simulation Controls ---

    fun toggleSimulation() {
        if (simulationState.isRunning) {
            pauseSimulation()
        } else {
            startSimulation()
        }
    }

    fun startSimulation() {
        simulationState = simulationState.copy(isRunning = true)
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (isActive && simulationState.isRunning) {
                stepSingleTick()
                val intervalMs = (1000f / simulationState.tickRateHz).toLong().coerceAtLeast(40L)
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
        simulator.stepTick()
        val duration = System.currentTimeMillis() - start

        // Refresh Compose state
        circuitGrid = circuitGrid.clone()
        simulationState = simulationState.copy(
            totalTicks = simulator.tickCount,
            activeSignalCount = circuitGrid.activeCount,
            lastStepDurationMs = duration
        )
    }

    fun resetSimulation() {
        pauseSimulation()
        simulator.reset()
        circuitGrid = circuitGrid.clone()
        simulationState = simulationState.copy(
            totalTicks = 0L,
            activeSignalCount = circuitGrid.activeCount
        )
    }

    fun setTickRate(rateHz: Float) {
        val clamped = rateHz.coerceIn(1.0f, 20.0f)
        simulationState = simulationState.copy(tickRateHz = clamped)
        if (simulationState.isRunning) {
            startSimulation() // Restart loop with new interval
        }
    }

    // --- Interactive Editing ---

    fun onTileClicked(coord: BlockCoord, context: Context? = null) {
        selectedBlockCoord = coord

        when (selectedTool) {
            EditorTool.PLACE -> {
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

            EditorTool.BREAK -> {
                circuitGrid.remove(coord)
                vibrate(context, 25)
                refreshGridAndSimulate()
            }

            EditorTool.INTERACT -> {
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
                    vibrate(context, 20)
                    refreshGridAndSimulate()
                }
            }

            EditorTool.INSPECT -> {
                // Keep selectedBlockCoord highlighted
            }
        }
    }

    fun onTileLongClicked(coord: BlockCoord, context: Context? = null) {
        // Quick break on long press
        val block = circuitGrid.get(coord)
        if (block != null && block.type != BlockType.AIR) {
            circuitGrid.remove(coord)
            vibrate(context, 35)
            refreshGridAndSimulate()
        }
    }

    private fun refreshGridAndSimulate() {
        simulator.recalculateWireSignals()
        circuitGrid = circuitGrid.clone()
        simulationState = simulationState.copy(activeSignalCount = circuitGrid.activeCount)
    }

    fun loadPreset(preset: CircuitPreset) {
        pauseSimulation()
        circuitName = preset.title
        circuitGrid = preset.builder()
        simulator = RedstoneSimulator(circuitGrid)
        refreshGridAndSimulate()
        panOffset = Offset(450f, 550f)
        zoom = 1.0f
    }

    fun clearCircuit() {
        pauseSimulation()
        circuitGrid.clear()
        // Provide standard 5x5 stone platform at layer 0
        for (x in 0..4) {
            for (y in 0..4) {
                circuitGrid.set(BlockState(BlockCoord(x, y, 0), BlockType.STONE))
            }
        }
        simulator = RedstoneSimulator(circuitGrid)
        refreshGridAndSimulate()
        activeLayerZ = 1
    }

    fun exportMcPack(context: Context): File {
        return BedrockPackExporter.exportToCacheFile(context, circuitGrid, circuitName)
    }

    private fun vibrate(context: Context?, durationMs: Long) {
        if (context == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
