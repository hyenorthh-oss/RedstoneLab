package com.redstonelab.app.engine

import com.redstonelab.app.model.BlockCoord
import com.redstonelab.app.model.BlockState
import com.redstonelab.app.model.BlockType
import com.redstonelab.app.model.CircuitGrid
import com.redstonelab.app.model.Facing
import java.util.ArrayDeque
import kotlin.math.max

/**
 * Accurate Minecraft Redstone Simulation Engine.
 * Handles discrete redstone tick progression, signal degradation (0..15),
 * hard vs soft powering, repeater delays & locking, comparator arithmetic,
 * torch inversion queues, button pulse lifecycles, and piston push/pull mechanics.
 */
class RedstoneSimulator(val grid: CircuitGrid) {

    // Delay queues for scheduled tick events
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

    private data class ScheduledObserverEvent(
        val coord: BlockCoord,
        var ticksRemaining: Int
    )

    private val repeaterQueue = mutableListOf<ScheduledRepeaterEvent>()
    private val torchQueue = mutableListOf<ScheduledTorchEvent>()
    private val observerQueue = mutableListOf<ScheduledObserverEvent>()

    // Track block changes to trigger observers
    private val previousBlockTypes = HashMap<BlockCoord, BlockType>()

    var tickCount: Long = 0L
        private set

    fun reset() {
        tickCount = 0L
        repeaterQueue.clear()
        torchQueue.clear()
        observerQueue.clear()
        previousBlockTypes.clear()

        // Reset dynamic states
        val all = grid.getAll()
        for ((coord, state) in all) {
            val resetState = when (state.type) {
                BlockType.LEVER -> state // keep lever manual state
                BlockType.REDSTONE_BLOCK -> state.withPower(15)
                BlockType.REDSTONE_TORCH -> state.copy(isActive = true, power = 15)
                BlockType.REDSTONE_REPEATER -> state.copy(power = 0, isLocked = false)
                BlockType.REDSTONE_COMPARATOR -> state.copy(power = 0)
                BlockType.REDSTONE_LAMP -> state.copy(power = 0, isActive = false)
                BlockType.PISTON, BlockType.STICKY_PISTON -> state.copy(isExtended = false, power = 0)
                BlockType.STONE_BUTTON -> state.copy(isActive = false, power = 0, pulseTicksLeft = 0)
                BlockType.OBSERVER -> state.copy(power = 0, pulseTicksLeft = 0)
                BlockType.REDSTONE_WIRE -> state.withPower(0)
                else -> state.withPower(0)
            }
            grid.set(resetState)
        }
        recalculateWireSignals()
    }

    /**
     * Executes exactly one Redstone Tick (100ms in Minecraft time).
     */
    fun stepTick() {
        tickCount++

        // 1. Process Button and Observer pulse decrements
        processPulses()

        // 2. Process Scheduled Repeater Delays
        processRepeaterQueue()

        // 3. Process Scheduled Torch Events
        processTorchQueue()

        // 4. Update Repeaters inputs & locking
        updateRepeaters()

        // 5. Update Torches attached block checks
        updateTorchSchedules()

        // 6. Update Comparators
        updateComparators()

        // 7. Full Signal Propagation for Redstone Dust & Solid Conductor Blocks
        recalculateWireSignals()

        // 8. Update Actuators (Lamps, Pistons)
        updateActuators()

        // 9. Observer detection
        detectObserverTriggers()
    }

    private fun processPulses() {
        val all = grid.getAll()
        for ((coord, state) in all) {
            if (state.type == BlockType.STONE_BUTTON && state.pulseTicksLeft > 0) {
                val nextTicks = state.pulseTicksLeft - 1
                if (nextTicks == 0) {
                    grid.set(state.copy(isActive = false, power = 0, pulseTicksLeft = 0))
                } else {
                    grid.set(state.copy(pulseTicksLeft = nextTicks))
                }
            } else if (state.type == BlockType.OBSERVER && state.pulseTicksLeft > 0) {
                val nextTicks = state.pulseTicksLeft - 1
                if (nextTicks == 0) {
                    grid.set(state.copy(power = 0, pulseTicksLeft = 0))
                } else {
                    grid.set(state.copy(pulseTicksLeft = nextTicks))
                }
            }
        }
    }

    private fun processRepeaterQueue() {
        val iterator = repeaterQueue.iterator()
        while (iterator.hasNext()) {
            val event = iterator.next()
            event.ticksRemaining--
            if (event.ticksRemaining <= 0) {
                val current = grid.get(event.coord)
                if (current != null && current.type == BlockType.REDSTONE_REPEATER && !current.isLocked) {
                    grid.set(current.withPower(event.targetPower))
                }
                iterator.remove()
            }
        }
    }

    private fun processTorchQueue() {
        val iterator = torchQueue.iterator()
        while (iterator.hasNext()) {
            val event = iterator.next()
            event.ticksRemaining--
            if (event.ticksRemaining <= 0) {
                val current = grid.get(event.coord)
                if (current != null && current.type == BlockType.REDSTONE_TORCH) {
                    val newPower = if (event.targetActive) 15 else 0
                    grid.set(current.copy(isActive = event.targetActive, power = newPower))
                }
                iterator.remove()
            }
        }
    }

    private fun updateRepeaters() {
        val all = grid.getAll()
        for ((coord, state) in all) {
            if (state.type != BlockType.REDSTONE_REPEATER) continue

            // Check repeater side locking
            val leftSide = coord.offset(state.facing.counterClockwise())
            val rightSide = coord.offset(state.facing.clockwise())
            val isLocked = isRepeaterSidePowered(leftSide, state.facing.clockwise()) ||
                    isRepeaterSidePowered(rightSide, state.facing.counterClockwise())

            if (state.isLocked != isLocked) {
                grid.set(state.copy(isLocked = isLocked))
            }
            if (isLocked) continue

            // Rear input signal
            val rearCoord = coord.offset(state.facing.opposite)
            val rearPower = getOutputPowerTowards(rearCoord, state.facing)
            val desiredOutput = if (rearPower > 0) 15 else 0

            if (state.power != desiredOutput) {
                // Check if already scheduled
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

            // The block the torch is mounted on
            val baseCoord = when (state.attachmentFacing) {
                Facing.DOWN -> coord.offset(0, 0, -1)
                else -> coord.offset(state.attachmentFacing)
            }

            val baseBlock = grid.get(baseCoord)
            val isBasePowered = if (baseBlock != null) {
                isBlockSoftOrHardPowered(baseCoord)
            } else false

            // Torch inverts base block state: base powered -> torch turns OFF (isActive = false)
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

            // Rear input
            val rearCoord = coord.offset(state.facing.opposite)
            val rearPower = getOutputPowerTowards(rearCoord, state.facing)

            // Side inputs: max of left and right
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

    /**
     * Checks if a solid block is either hard-powered or soft-powered.
     */
    fun isBlockSoftOrHardPowered(coord: BlockCoord): Boolean {
        // Hard-powered by repeater, comparator, torch, or direct redstone block
        for (facing in Facing.ALL) {
            val neighborCoord = coord.offset(facing)
            val neighbor = grid.get(neighborCoord) ?: continue

            when (neighbor.type) {
                BlockType.REDSTONE_BLOCK -> return true
                BlockType.LEVER -> if (neighbor.isActive) return true
                BlockType.STONE_BUTTON -> if (neighbor.pulseTicksLeft > 0) return true
                BlockType.REDSTONE_REPEATER -> if (neighbor.facing == facing.opposite && neighbor.power > 0) return true
                BlockType.REDSTONE_COMPARATOR -> if (neighbor.facing == facing.opposite && neighbor.power > 0) return true
                BlockType.REDSTONE_TORCH -> {
                    // Torch underneath powers the block above it
                    if (facing == Facing.DOWN && neighbor.isActive) return true
                }
                BlockType.REDSTONE_WIRE -> {
                    // Dust pointing into or on top of this block powers it softly
                    if (neighbor.power > 0) return true
                }
                else -> {}
            }
        }
        return false
    }

    /**
     * Propagates Redstone Wire signals using priority breadth-first propagation.
     */
    fun recalculateWireSignals() {
        // Collect all wire coordinates and reset their power to 0
        val wires = HashMap<BlockCoord, BlockState>()
        val sourcesQueue = ArrayDeque<Pair<BlockCoord, Int>>()

        val all = grid.getAll()
        for ((coord, state) in all) {
            if (state.type == BlockType.REDSTONE_WIRE) {
                wires[coord] = state.withPower(0)
            }
        }

        // 1. Identify primary power injections into the wire network
        for ((coord, wire) in wires) {
            var directPower = 0

            // Check 6 adjacent neighbors for direct power emission
            for (facing in Facing.ALL) {
                val neighborCoord = coord.offset(facing)
                val neighbor = all[neighborCoord] ?: continue

                val power = when (neighbor.type) {
                    BlockType.REDSTONE_BLOCK -> 15
                    BlockType.LEVER -> if (neighbor.isActive) 15 else 0
                    BlockType.STONE_BUTTON -> if (neighbor.pulseTicksLeft > 0) 15 else 0
                    BlockType.REDSTONE_TORCH -> if (neighbor.isActive) 15 else 0
                    BlockType.OBSERVER -> if (neighbor.facing.opposite == facing && neighbor.pulseTicksLeft > 0) 15 else 0
                    BlockType.REDSTONE_REPEATER -> {
                        if (neighbor.facing == facing.opposite) neighbor.power else 0
                    }
                    BlockType.REDSTONE_COMPARATOR -> {
                        if (neighbor.facing == facing.opposite) neighbor.power else 0
                    }
                    BlockType.STONE, BlockType.TARGET_BLOCK -> {
                        // If this solid conductor is powered, it can power adjacent wire
                        if (isBlockHardPowered(neighborCoord)) 15 else 0
                    }
                    else -> 0
                }
                if (power > directPower) directPower = power
            }

            if (directPower > 0) {
                wires[coord] = wire.withPower(directPower)
                sourcesQueue.add(Pair(coord, directPower))
            }
        }

        // 2. Propagate signals through wire mesh with distance decay (-1 per step)
        while (sourcesQueue.isNotEmpty()) {
            val (currentCoord, currentPower) = sourcesQueue.poll()
            if (currentPower <= 1) continue // Cannot propagate further if power <= 1

            val nextPower = currentPower - 1

            // Horizontal Cardinal Neighbors
            for (cardinal in Facing.CARDINALS) {
                val neighborCoord = currentCoord.offset(cardinal)

                // Level 0: Same height
                checkAndPropagateWire(neighborCoord, nextPower, wires, sourcesQueue)

                // Level +1: Step UP 1 block (if block above current is non-solid)
                val aboveCurrent = all[currentCoord.offset(0, 0, 1)]
                if (aboveCurrent == null || !aboveCurrent.type.isSolidConductor) {
                    val stepUpCoord = neighborCoord.offset(0, 0, 1)
                    checkAndPropagateWire(stepUpCoord, nextPower, wires, sourcesQueue)
                }

                // Level -1: Step DOWN 1 block (if block above destination is non-solid)
                val aboveDest = all[neighborCoord]
                if (aboveDest == null || !aboveDest.type.isSolidConductor) {
                    val stepDownCoord = neighborCoord.offset(0, 0, -1)
                    checkAndPropagateWire(stepDownCoord, nextPower, wires, sourcesQueue)
                }
            }
        }

        // 3. Write back calculated wire states to grid
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

    private fun isBlockHardPowered(coord: BlockCoord): Boolean {
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

    private fun getOutputPowerTowards(coord: BlockCoord, towards: Facing): Int {
        val block = grid.get(coord) ?: return 0
        return when (block.type) {
            BlockType.REDSTONE_BLOCK -> 15
            BlockType.LEVER -> if (block.isActive) 15 else 0
            BlockType.STONE_BUTTON -> if (block.pulseTicksLeft > 0) 15 else 0
            BlockType.REDSTONE_TORCH -> if (block.isActive) 15 else 0
            BlockType.REDSTONE_REPEATER -> if (block.facing == towards) block.power else 0
            BlockType.REDSTONE_COMPARATOR -> if (block.facing == towards) block.power else 0
            BlockType.REDSTONE_WIRE -> block.power
            BlockType.OBSERVER -> if (block.facing.opposite == towards && block.pulseTicksLeft > 0) 15 else 0
            BlockType.STONE -> if (isBlockHardPowered(coord)) 15 else 0
            else -> 0
        }
    }

    private fun updateActuators() {
        val all = grid.getAll()
        for ((coord, state) in all) {
            when (state.type) {
                BlockType.REDSTONE_LAMP -> {
                    val isPowered = isBlockSoftOrHardPowered(coord)
                    if (state.isActive != isPowered) {
                        grid.set(state.copy(isActive = isPowered, power = if (isPowered) 15 else 0))
                    }
                }
                BlockType.PISTON, BlockType.STICKY_PISTON -> {
                    val isPowered = isBlockSoftOrHardPowered(coord)
                    if (isPowered && !state.isExtended) {
                        tryExtendPiston(coord, state)
                    } else if (!isPowered && state.isExtended) {
                        tryRetractPiston(coord, state)
                    }
                }
                else -> {}
            }
        }
    }

    private fun tryExtendPiston(pistonCoord: BlockCoord, pistonState: BlockState) {
        val facing = pistonState.facing
        val pushList = mutableListOf<BlockCoord>()
        var probe = pistonCoord.offset(facing)

        // Find blocks in line up to push limit (12 blocks in Minecraft)
        while (pushList.size < 12) {
            val block = grid.get(probe) ?: break
            if (block.type == BlockType.AIR) break
            // Obsidian / Bedrock cannot be pushed
            pushList.add(probe)
            probe = probe.offset(facing)
        }

        if (pushList.size >= 12 && grid.get(probe) != null && grid.get(probe)!!.type != BlockType.AIR) {
            // Push limit exceeded; piston cannot extend
            return
        }

        // Shift blocks forward from farthest to closest
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
            // Pull the block located 2 units away back to 1 unit away
            val blockAheadCoord = pistonCoord.offset(pistonState.facing).offset(pistonState.facing)
            val blockToPull = grid.get(blockAheadCoord)
            if (blockToPull != null && blockToPull.type != BlockType.AIR) {
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

            // Observer face observes the block in front
            val faceCoord = coord.offset(state.facing)
            val currentFacingType = grid.get(faceCoord)?.type ?: BlockType.AIR
            val previousFacingType = previousBlockTypes[faceCoord] ?: BlockType.AIR

            if (currentFacingType != previousFacingType && state.pulseTicksLeft == 0) {
                // Trigger 1-tick observer pulse
                grid.set(state.copy(power = 15, pulseTicksLeft = 2))
            }
            previousBlockTypes[faceCoord] = currentFacingType
        }
    }
}
