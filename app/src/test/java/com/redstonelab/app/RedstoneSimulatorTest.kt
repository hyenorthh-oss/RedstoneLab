package com.redstonelab.app

import com.redstonelab.app.engine.RedstoneSimulator
import com.redstonelab.app.model.BlockCoord
import com.redstonelab.app.model.BlockState
import com.redstonelab.app.model.BlockType
import com.redstonelab.app.model.CircuitGrid
import com.redstonelab.app.model.Facing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedstoneSimulatorTest {

    @Test
    fun testWireSignalDegradation() {
        val grid = CircuitGrid()
        // Redstone block at (0, 0, 0)
        grid.set(BlockState(BlockCoord(0, 0, 0), BlockType.REDSTONE_BLOCK, power = 15))

        // Line of 16 redstone wire pieces along X axis
        for (x in 1..16) {
            grid.set(BlockState(BlockCoord(x, 0, 0), BlockType.REDSTONE_WIRE))
        }

        val simulator = RedstoneSimulator(grid)
        simulator.recalculateWireSignals()

        // Power should drop from 15 down to 1, then 0 at index 16
        assertEquals(15, grid.get(1, 0, 0)?.power)
        assertEquals(14, grid.get(2, 0, 0)?.power)
        assertEquals(10, grid.get(6, 0, 0)?.power)
        assertEquals(1, grid.get(15, 0, 0)?.power)
        assertEquals(0, grid.get(16, 0, 0)?.power)
    }

    @Test
    fun testRepeaterDelayAndSignalBoost() {
        val grid = CircuitGrid()
        // Weak wire of signal 2 feeding into repeater
        grid.set(BlockState(BlockCoord(0, 0, 0), BlockType.LEVER, isActive = true, power = 15))
        grid.set(BlockState(BlockCoord(1, 0, 0), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 2))
        grid.set(BlockState(BlockCoord(2, 0, 0), BlockType.REDSTONE_WIRE))

        val simulator = RedstoneSimulator(grid)

        // At tick 0, repeater output is unpowered
        simulator.stepTick()
        assertEquals(0, grid.get(1, 0, 0)?.power)
        assertEquals(0, grid.get(2, 0, 0)?.power)

        // At tick 2 (2-tick delay), repeater fires full 15 signal
        simulator.stepTick()
        assertEquals(15, grid.get(1, 0, 0)?.power)
        assertEquals(15, grid.get(2, 0, 0)?.power)
    }

    @Test
    fun testTorchInversion() {
        val grid = CircuitGrid()
        // Stone base with lever and torch on top
        grid.set(BlockState(BlockCoord(0, 0, 0), BlockType.STONE))
        grid.set(BlockState(BlockCoord(0, 0, 1), BlockType.REDSTONE_TORCH, facing = Facing.UP, isActive = true, power = 15))
        grid.set(BlockState(BlockCoord(-1, 0, 0), BlockType.LEVER, isActive = false, facing = Facing.EAST))

        val simulator = RedstoneSimulator(grid)
        simulator.recalculateWireSignals()

        // Initially torch is lit
        assertTrue(grid.get(0, 0, 1)!!.isActive)

        // Turn lever ON to power stone base
        val lever = grid.get(-1, 0, 0)!!
        grid.set(lever.copy(isActive = true, power = 15))

        // In Minecraft, torch takes 1 tick to invert and turn OFF
        simulator.stepTick()
        assertFalse(grid.get(0, 0, 1)!!.isActive)
        assertEquals(0, grid.get(0, 0, 1)!!.power)
    }

    @Test
    fun testComparatorSubtractionMode() {
        val grid = CircuitGrid()
        // Rear input = 10, Side input = 4 -> Output in subtract mode = 6
        grid.set(BlockState(BlockCoord(0, 1, 0), BlockType.REDSTONE_WIRE, power = 10))
        grid.set(BlockState(BlockCoord(1, 0, 0), BlockType.REDSTONE_WIRE, power = 4))
        grid.set(
            BlockState(
                BlockCoord(1, 1, 0),
                BlockType.REDSTONE_COMPARATOR,
                facing = Facing.SOUTH,
                isSubtractMode = true
            )
        )

        val simulator = RedstoneSimulator(grid)
        simulator.stepTick()

        val comparator = grid.get(1, 1, 0)
        assertEquals(6, comparator?.power)
    }

    @Test
    fun testPistonPushMechanics() {
        val grid = CircuitGrid()
        // Piston facing EAST at (0, 0, 0)
        grid.set(BlockState(BlockCoord(0, 0, 0), BlockType.PISTON, facing = Facing.EAST))
        // Stone block in front at (1, 0, 0)
        grid.set(BlockState(BlockCoord(1, 0, 0), BlockType.STONE))
        // Power source lever beside piston
        grid.set(BlockState(BlockCoord(0, 1, 0), BlockType.LEVER, isActive = true, power = 15))

        val simulator = RedstoneSimulator(grid)
        simulator.stepTick()

        // Piston should be extended
        assertTrue(grid.get(0, 0, 0)!!.isExtended)
        // Stone block should have been pushed forward to (2, 0, 0)
        assertEquals(BlockType.STONE, grid.get(2, 0, 0)?.type)
    }
}
