package com.redstonelab.app.engine

import com.redstonelab.app.model.BlockCoord
import com.redstonelab.app.model.BlockState
import com.redstonelab.app.model.BlockType
import com.redstonelab.app.model.CircuitGrid
import com.redstonelab.app.model.Facing
import java.util.ArrayDeque
import kotlin.math.max

/**
 * Motor de Simulação de Redstone em Tempo Real (Minecraft Bedrock / Java Logic).
 *
 * Suporta:
 * - Ciclo de ticks discretos (10 ticks/s padrão).
 * - Força de sinal 0 a 15 com decaimento de 1 por bloco de pó.
 * - Energia forte (strong power) vs energia fraca (weak power).
 * - Lajes superiores (diodo vertical) e Vidro transparente.
 * - Inversão de tochas com fila de atraso de 1 tick.
 * - Repetidores com atraso de 1 a 4 ticks e travamento lateral.
 * - Comparadores nos modos Comparar e Subtrair.
 * - Observadores com pulso de 1 tick na saída traseira.
 * - Lâmpada de Cobre (Copper Bulb) com comportamento T-Flip-Flop nativo na borda de subida.
 * - Pistões e pistões grudentos com empurrão/puxão de blocos até 12 blocos, respeitando Obsidiana como imóvel.
 * - Lâmpadas, Portas de Ferro, Alçapões, Note Block, TNT e Sino com feedback de ativação.
 */
class RedstoneEngine(val grid: CircuitGrid) {

    private data class ScheduledRepeaterEvent(
        val coord: BlockCoord,
        val targetPower: Int,
        var ticksRemaining: Int
    )

    private data class ScheduledTorchEvent(
        val coord: BlockCoord,
        val targetActive: Boolean,
        var ticksRemaining: Int
    )

    private val repeaterQueue = mutableListOf<ScheduledRepeaterEvent>()
    private val torchQueue = mutableListOf<ScheduledTorchEvent>()

    // Track state changes in front of observers
    private val previousObserverWatchedTypes = HashMap<BlockCoord, BlockType>()

    // Track previous power states for rising edge detection (e.g. Copper Bulb T-Flip-Flop)
    private val previousBlockPowers = HashMap<BlockCoord, Int>()

    var tickCount: Long = 0L
        private set

    fun reset() {
        tickCount = 0L
        repeaterQueue.clear()
        torchQueue.clear()
        previousObserverWatchedTypes.clear()
        previousBlockPowers.clear()

        val all = grid.getAll()
        for ((_, state) in all) {
            val resetState = when (state.type) {
                BlockType.LEVER -> state // keep lever user state
                BlockType.DAYLIGHT_DETECTOR -> state
                BlockType.REDSTONE_BLOCK -> state.withPower(15)
                BlockType.REDSTONE_TORCH -> state.copy(isActive = true, power = 15)
                BlockType.REDSTONE_REPEATER -> state.copy(power = 0, isLocked = false)
                BlockType.REDSTONE_COMPARATOR -> state.copy(power = 0)
                BlockType.COPPER_BULB -> state.copy(power = 0) // keep isActive T-Flip-Flop
                BlockType.REDSTONE_LAMP -> state.copy(power = 0, isActive = false)
                BlockType.IRON_DOOR, BlockType.IRON_TRAPDOOR -> state.copy(isActive = false)
                BlockType.TNT -> state.copy(isActive = false)
                BlockType.PISTON, BlockType.STICKY_PISTON -> state.copy(isExtended = false, power = 0)
                BlockType.STONE_BUTTON -> state.copy(isActive = false, power = 0, pulseTicksLeft = 0)
                BlockType.STONE_PRESSURE_PLATE -> state.copy(isActive = false, power = 0)
                BlockType.SCULK_SENSOR -> state.copy(isActive = false, power = 0, pulseTicksLeft = 0)
                BlockType.OBSERVER -> state.copy(power = 0, pulseTicksLeft = 0)
                BlockType.REDSTONE_WIRE -> state.withPower(0)
                else -> state.withPower(0)
            }
            grid.set(resetState)
        }
        recalculateWireSignals()
    }

    /**
     * Executa exatamente 1 Tick de Redstone (100ms).
     */
    fun stepTick() {
        tickCount++

        // 1. Processa botões, sensores de sculk e pulsos de observadores
        processPulseCountdowns()

        // 2. Processa atrasos agendados de repetidores
        processRepeaterQueue()

        // 3. Processa eventos agendados de tochas (inversão com 1 tick de atraso)
        processTorchQueue()

        // 4. Atualiza entradas e travamento lateral de repetidores
        updateRepeaters()

        // 5. Atualiza agendamentos de tochas
        updateTorchSchedules()

        // 6. Atualiza comparadores (Comparar vs Subtrair)
        updateComparators()

        // 7. Propagação de sinal de pó de redstone na grade 3D
        recalculateWireSignals()

        // 8. Atualiza atuadores (Lâmpadas, Lâmpada de Cobre, Pistões, Portas, TNT)
        updateActuators()

        // 9. Observadores: detecta mudanças na face frontal
        detectObserverTriggers()
    }

    private fun processPulseCountdowns() {
        val all = grid.getAll()
        for ((_, state) in all) {
            if (state.pulseTicksLeft > 0) {
                val nextTicks = state.pulseTicksLeft - 1
                if (nextTicks == 0) {
                    when (state.type) {
                        BlockType.STONE_BUTTON -> {
                            grid.set(state.copy(isActive = false, power = 0, pulseTicksLeft = 0))
                        }
                        BlockType.SCULK_SENSOR -> {
                            grid.set(state.copy(isActive = false, power = 0, pulseTicksLeft = 0))
                        }
                        BlockType.OBSERVER -> {
                            grid.set(state.copy(power = 0, pulseTicksLeft = 0))
                        }
                        else -> {
                            grid.set(state.copy(pulseTicksLeft = 0))
                        }
                    }
                } else {
                    grid.set(state.copy(pulseTicksLeft = nextTicks))
                }
            }
        }
    }

    private fun processRepeaterQueue() {
        val iter = repeaterQueue.iterator()
        while (iter.hasNext()) {
            val event = iter.next()
            event.ticksRemaining--
            if (event.ticksRemaining <= 0) {
                val current = grid.get(event.coord)
                if (current != null && current.type == BlockType.REDSTONE_REPEATER && !current.isLocked) {
                    grid.set(current.withPower(event.targetPower))
                }
                iter.remove()
            }
        }
    }

    private fun processTorchQueue() {
        val iter = torchQueue.iterator()
        while (iter.hasNext()) {
            val event = iter.next()
            event.ticksRemaining--
            if (event.ticksRemaining <= 0) {
                val current = grid.get(event.coord)
                if (current != null && current.type == BlockType.REDSTONE_TORCH) {
                    val newPower = if (event.targetActive) 15 else 0
                    grid.set(current.copy(isActive = event.targetActive, power = newPower))
                }
                iter.remove()
            }
        }
    }

    private fun updateRepeaters() {
        val all = grid.getAll()
        for ((coord, state) in all) {
            if (state.type != BlockType.REDSTONE_REPEATER) continue

            // Verifica travamento lateral por outro repetidor ou comparador
            val leftSide = coord.offset(state.facing.counterClockwise())
            val rightSide = coord.offset(state.facing.clockwise())
            val isLocked = isRepeaterSidePowered(leftSide, state.facing.clockwise()) ||
                    isRepeaterSidePowered(rightSide, state.facing.counterClockwise())

            if (state.isLocked != isLocked) {
                grid.set(state.copy(isLocked = isLocked))
            }
            if (isLocked) continue

            // Sinal na entrada traseira
            val rearCoord = coord.offset(state.facing.opposite)
            val rearPower = getOutputPowerTowards(rearCoord, state.facing)
            val desiredOutput = if (rearPower > 0) 15 else 0

            if (state.power != desiredOutput) {
                val alreadyScheduled = repeaterQueue.any { it.coord == coord && it.targetPower == desiredOutput }
                if (!alreadyScheduled) {
                    repeaterQueue.add(ScheduledRepeaterEvent(coord, desiredOutput, state.delay))
                }
            }
        }
    }

    private fun isRepeaterSidePowered(sideCoord: BlockCoord, towardsRepeater: Facing): Boolean {
        val sideBlock = grid.get(sideCoord) ?: return false
        return when (sideBlock.type) {
            BlockType.REDSTONE_REPEATER -> sideBlock.facing == towardsRepeater && sideBlock.power > 0
            BlockType.REDSTONE_COMPARATOR -> sideBlock.facing == towardsRepeater && sideBlock.power > 0
            else -> false
        }
    }

    private fun updateTorchSchedules() {
        val all = grid.getAll()
        for ((coord, state) in all) {
            if (state.type != BlockType.REDSTONE_TORCH) continue

            val baseCoord = when (state.attachmentFacing) {
                Facing.DOWN -> coord.offset(Facing.DOWN) // torch on floor
                else -> coord.offset(state.attachmentFacing) // torch on wall
            }

            val isBasePowered = isBlockSoftOrHardPowered(baseCoord)
            // A tocha inverte a base: base energizada -> tocha desliga (1 tick de atraso)
            val targetActive = !isBasePowered

            if (state.isActive != targetActive) {
                val alreadyScheduled = torchQueue.any { it.coord == coord && it.targetActive == targetActive }
                if (!alreadyScheduled) {
                    torchQueue.add(ScheduledTorchEvent(coord, targetActive, 1))
                }
            }
        }
    }

    private fun updateComparators() {
        val all = grid.getAll()
        for ((coord, state) in all) {
            if (state.type != BlockType.REDSTONE_COMPARATOR) continue

            // Entrada traseira
            val rearCoord = coord.offset(state.facing.opposite)
            val rearPower = getOutputPowerTowards(rearCoord, state.facing)

            // Entradas laterais: pega o valor máximo entre esquerda e direita
            val leftCoord = coord.offset(state.facing.counterClockwise())
            val rightCoord = coord.offset(state.facing.clockwise())
            val leftPower = getSideSignal(leftCoord, state.facing.clockwise())
            val rightPower = getSideSignal(rightCoord, state.facing.counterClockwise())
            val sideMax = max(leftPower, rightPower)

            val calculatedPower = if (state.isSubtractMode) {
                max(0, rearPower - sideMax)
            } else {
                if (rearPower >= sideMax) rearPower else 0
            }

            if (state.power != calculatedPower) {
                grid.set(state.withPower(calculatedPower))
            }
        }
    }

    private fun getSideSignal(coord: BlockCoord, towardsComparator: Facing): Int {
        val block = grid.get(coord) ?: return 0
        return when (block.type) {
            BlockType.REDSTONE_WIRE -> block.power
            BlockType.REDSTONE_REPEATER -> if (block.facing == towardsComparator) block.power else 0
            BlockType.REDSTONE_COMPARATOR -> if (block.facing == towardsComparator) block.power else 0
            BlockType.REDSTONE_BLOCK -> 15
            else -> 0
        }
    }

    fun isBlockSoftOrHardPowered(coord: BlockCoord): Boolean {
        for (facing in Facing.ALL) {
            val neighborCoord = coord.offset(facing)
            val neighbor = grid.get(neighborCoord) ?: continue

            when (neighbor.type) {
                BlockType.REDSTONE_BLOCK -> return true
                BlockType.LEVER -> if (neighbor.isActive) return true
                BlockType.STONE_BUTTON -> if (neighbor.pulseTicksLeft > 0) return true
                BlockType.STONE_PRESSURE_PLATE -> if (neighbor.isActive) return true
                BlockType.DAYLIGHT_DETECTOR -> if (neighbor.isActive) return true
                BlockType.SCULK_SENSOR -> if (neighbor.isActive) return true
                BlockType.REDSTONE_REPEATER -> if (neighbor.facing == facing.opposite && neighbor.power > 0) return true
                BlockType.REDSTONE_COMPARATOR -> if (neighbor.facing == facing.opposite && neighbor.power > 0) return true
                BlockType.REDSTONE_TORCH -> {
                    // Tocha embaixo energiza fortemente o bloco acima
                    if (facing == Facing.DOWN && neighbor.isActive) return true
                }
                BlockType.REDSTONE_WIRE -> {
                    if (neighbor.power > 0) return true
                }
                else -> {}
            }
        }
        return false
    }

    fun isBlockHardPowered(coord: BlockCoord): Boolean {
        for (facing in Facing.ALL) {
            val neighborCoord = coord.offset(facing)
            val neighbor = grid.get(neighborCoord) ?: continue
            when (neighbor.type) {
                BlockType.REDSTONE_BLOCK -> return true
                BlockType.REDSTONE_REPEATER -> if (neighbor.facing == facing.opposite && neighbor.power > 0) return true
                BlockType.REDSTONE_COMPARATOR -> if (neighbor.facing == facing.opposite && neighbor.power > 0) return true
                BlockType.REDSTONE_TORCH -> if (facing == Facing.DOWN && neighbor.isActive) return true
                else -> {}
            }
        }
        return false
    }

    /**
     * Propagação precisa do Pó de Redstone com atenuação de 1 por bloco.
     */
    fun recalculateWireSignals() {
        val wires = HashMap<BlockCoord, BlockState>()
        val sourcesQueue = ArrayDeque<Pair<BlockCoord, Int>>()
        val all = grid.getAll()

        for ((coord, state) in all) {
            if (state.type == BlockType.REDSTONE_WIRE) {
                wires[coord] = state.withPower(0)
            }
        }

        // 1. Identifica injeções diretas de energia na rede de fios
        for ((coord, wire) in wires) {
            var directPower = 0

            for (facing in Facing.ALL) {
                val neighborCoord = coord.offset(facing)
                val neighbor = all[neighborCoord] ?: continue

                val power = when (neighbor.type) {
                    BlockType.REDSTONE_BLOCK -> 15
                    BlockType.LEVER -> if (neighbor.isActive) 15 else 0
                    BlockType.STONE_BUTTON -> if (neighbor.pulseTicksLeft > 0) 15 else 0
                    BlockType.STONE_PRESSURE_PLATE -> if (neighbor.isActive) 15 else 0
                    BlockType.DAYLIGHT_DETECTOR -> if (neighbor.isActive) 15 else 0
                    BlockType.SCULK_SENSOR -> if (neighbor.isActive) 15 else 0
                    BlockType.REDSTONE_TORCH -> if (neighbor.isActive) 15 else 0
                    BlockType.OBSERVER -> if (neighbor.facing.opposite == facing && neighbor.pulseTicksLeft > 0) 15 else 0
                    BlockType.REDSTONE_REPEATER -> {
                        if (neighbor.facing == facing.opposite) neighbor.power else 0
                    }
                    BlockType.REDSTONE_COMPARATOR -> {
                        if (neighbor.facing == facing.opposite) neighbor.power else 0
                    }
                    else -> {
                        if (neighbor.type.isSolidConductor && isBlockHardPowered(neighborCoord)) 15 else 0
                    }
                }
                if (power > directPower) directPower = power
            }

            if (directPower > 0) {
                wires[coord] = wire.withPower(directPower)
                sourcesQueue.add(Pair(coord, directPower))
            }
        }

        // 2. Propaga sinais pela malha de fios com decaimento (-1 por bloco)
        while (sourcesQueue.isNotEmpty()) {
            val (currentCoord, currentPower) = sourcesQueue.poll()
            if (currentPower <= 1) continue

            val nextPower = currentPower - 1

            for (cardinal in Facing.CARDINALS) {
                val neighborCoord = currentCoord.offset(cardinal)

                // Nível 0: Mesmo plano Y
                checkAndPropagateWire(neighborCoord, nextPower, wires, sourcesQueue)

                // Nível +1: Subir 1 bloco (se o bloco acima do atual não for condutor sólido opaco)
                val aboveCurrent = all[currentCoord.offset(Facing.UP)]
                if (aboveCurrent == null || !aboveCurrent.type.isSolidConductor) {
                    val stepUpCoord = neighborCoord.offset(Facing.UP)
                    checkAndPropagateWire(stepUpCoord, nextPower, wires, sourcesQueue)
                }

                // Nível -1: Descer 1 bloco (se o bloco acima do destino não for condutor sólido opaco)
                val aboveDest = all[neighborCoord]
                if (aboveDest == null || !aboveDest.type.isSolidConductor) {
                    val stepDownCoord = neighborCoord.offset(Facing.DOWN)
                    checkAndPropagateWire(stepDownCoord, nextPower, wires, sourcesQueue)
                }
            }
        }

        // 3. Grava de volta na grade
        for ((coord, wire) in wires) {
            grid.set(wire)
        }
    }

    private fun checkAndPropagateWire(
        targetCoord: BlockCoord,
        powerCandidate: Int,
        wires: HashMap<BlockCoord, BlockState>,
        queue: ArrayDeque<Pair<BlockCoord, Int>>
    ) {
        val target = wires[targetCoord] ?: return
        if (powerCandidate > target.power) {
            val updated = target.withPower(powerCandidate)
            wires[targetCoord] = updated
            queue.add(Pair(targetCoord, powerCandidate))
        }
    }

    private fun getOutputPowerTowards(coord: BlockCoord, towards: Facing): Int {
        val block = grid.get(coord) ?: return 0
        return when (block.type) {
            BlockType.REDSTONE_BLOCK -> 15
            BlockType.LEVER -> if (block.isActive) 15 else 0
            BlockType.STONE_BUTTON -> if (block.pulseTicksLeft > 0) 15 else 0
            BlockType.STONE_PRESSURE_PLATE -> if (block.isActive) 15 else 0
            BlockType.DAYLIGHT_DETECTOR -> if (block.isActive) 15 else 0
            BlockType.SCULK_SENSOR -> if (block.isActive) 15 else 0
            BlockType.REDSTONE_TORCH -> if (block.isActive) 15 else 0
            BlockType.REDSTONE_REPEATER -> if (block.facing == towards) block.power else 0
            BlockType.REDSTONE_COMPARATOR -> if (block.facing == towards) block.power else 0
            BlockType.REDSTONE_WIRE -> block.power
            BlockType.OBSERVER -> if (block.facing.opposite == towards && block.pulseTicksLeft > 0) 15 else 0
            else -> if (block.type.isSolidConductor && isBlockHardPowered(coord)) 15 else 0
        }
    }

    private fun updateActuators() {
        val all = grid.getAll()
        for ((coord, state) in all) {
            val isPowered = isBlockSoftOrHardPowered(coord)
            val prevPower = previousBlockPowers[coord] ?: 0

            when (state.type) {
                BlockType.REDSTONE_LAMP -> {
                    if (state.isActive != isPowered) {
                        grid.set(state.copy(isActive = isPowered, power = if (isPowered) 15 else 0))
                    }
                }

                BlockType.COPPER_BULB -> {
                    // Comportamento T-Flip-Flop: alterna estado na borda de subida (rising edge: 0 -> >0)
                    if (isPowered && prevPower == 0) {
                        val newActive = !state.isActive
                        grid.set(state.copy(isActive = newActive, power = if (newActive) 15 else 0))
                    }
                }

                BlockType.IRON_DOOR, BlockType.IRON_TRAPDOOR -> {
                    if (state.isActive != isPowered) {
                        grid.set(state.copy(isActive = isPowered))
                    }
                }

                BlockType.TNT, BlockType.NOTE_BLOCK, BlockType.BELL -> {
                    if (state.isActive != isPowered) {
                        grid.set(state.copy(isActive = isPowered))
                    }
                }

                BlockType.PISTON, BlockType.STICKY_PISTON -> {
                    if (isPowered && !state.isExtended) {
                        tryExtendPiston(coord, state)
                    } else if (!isPowered && state.isExtended) {
                        tryRetractPiston(coord, state)
                    }
                }

                else -> {}
            }

            previousBlockPowers[coord] = if (isPowered) 15 else 0
        }
    }

    private fun tryExtendPiston(pistonCoord: BlockCoord, pistonState: BlockState) {
        val facing = pistonState.facing
        val pushList = mutableListOf<BlockCoord>()
        var probe = pistonCoord.offset(facing)

        // Limite de empurrão no Minecraft: até 12 blocos
        while (pushList.size < 12) {
            val block = grid.get(probe) ?: break
            if (block.type == BlockType.AIR) break
            if (block.type.isImmovable) {
                // Obsidiana encontrada: pistão não consegue empurrar!
                return
            }
            pushList.add(probe)
            probe = probe.offset(facing)
        }

        if (pushList.size >= 12 && grid.get(probe) != null && grid.get(probe)!!.type != BlockType.AIR) {
            return // Limite de 12 blocos excedido
        }

        // Move blocos do mais distante para o mais próximo
        for (i in pushList.indices.reversed()) {
            val fromCoord = pushList[i]
            val toCoord = fromCoord.offset(facing)
            val blockToMove = grid.get(fromCoord)
            if (blockToMove != null) {
                grid.remove(fromCoord)
                grid.set(blockToMove.copy(coord = toCoord))
            }
        }

        grid.set(pistonState.copy(isExtended = true, power = 15))
    }

    private fun tryRetractPiston(pistonCoord: BlockCoord, pistonState: BlockState) {
        grid.set(pistonState.copy(isExtended = false, power = 0))

        if (pistonState.type == BlockType.STICKY_PISTON) {
            val blockAheadCoord = pistonCoord.offset(pistonState.facing).offset(pistonState.facing)
            val blockToPull = grid.get(blockAheadCoord)
            if (blockToPull != null && blockToPull.type != BlockType.AIR && !blockToPull.type.isImmovable) {
                val pulledCoord = pistonCoord.offset(pistonState.facing)
                grid.remove(blockAheadCoord)
                grid.set(blockToPull.copy(coord = pulledCoord))
            }
        }
    }

    private fun detectObserverTriggers() {
        val all = grid.getAll()
        for ((coord, state) in all) {
            if (state.type != BlockType.OBSERVER) continue

            val faceCoord = coord.offset(state.facing)
            val currentType = grid.get(faceCoord)?.type ?: BlockType.AIR
            val previousType = previousObserverWatchedTypes[faceCoord] ?: BlockType.AIR

            if (currentType != previousType && state.pulseTicksLeft == 0) {
                grid.set(state.copy(power = 15, pulseTicksLeft = 2))
            }
            previousObserverWatchedTypes[faceCoord] = currentType
        }
    }
}
