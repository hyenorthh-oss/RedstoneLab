package com.redstonelab.app

import com.redstonelab.app.engine.RedstoneEngine
import com.redstonelab.app.model.BlockCoord
import com.redstonelab.app.model.BlockState
import com.redstonelab.app.model.BlockType
import com.redstonelab.app.model.CircuitGrid
import com.redstonelab.app.model.Facing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedstoneEngineTest {

    @Test
    fun testWireSignalDegradation() {
        val grid = CircuitGrid()
        grid.set(BlockState(BlockCoord(0, 1, 0), BlockType.REDSTONE_BLOCK, power = 15))

        for (x in 1..16) {
            grid.set(BlockState(BlockCoord(x, 1, 0), BlockType.REDSTONE_WIRE))
        }

        val engine = RedstoneEngine(grid)
        engine.recalculateWireSignals()

        assertEquals(15, grid.get(1, 1, 0)?.power)
        assertEquals(14, grid.get(2, 1, 0)?.power)
        assertEquals(10, grid.get(6, 1, 0)?.power)
        assertEquals(1, grid.get(15, 1, 0)?.power)
        assertEquals(0, grid.get(16, 1, 0)?.power)
    }

    @Test
    fun testRepeaterDelayAndSignalBoost() {
        val grid = CircuitGrid()
        grid.set(BlockState(BlockCoord(0, 1, 0), BlockType.LEVER, isActive = true, power = 15))
        grid.set(BlockState(BlockCoord(1, 1, 0), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 2))
        grid.set(BlockState(BlockCoord(2, 1, 0), BlockType.REDSTONE_WIRE))

        val engine = RedstoneEngine(grid)

        // Tick 1: schedules repeater event with delay = 2
        engine.stepTick()
        assertEquals(0, grid.get(1, 1, 0)?.power)
        assertEquals(0, grid.get(2, 1, 0)?.power)

        // Tick 2: delay decrements
        engine.stepTick()

        // Tick 3: repeater outputs full 15 power
        engine.stepTick()
        assertEquals(15, grid.get(1, 1, 0)?.power)
        assertEquals(15, grid.get(2, 1, 0)?.power)
    }

    @Test
    fun testTorchInversion() {
        val grid = CircuitGrid()
        grid.set(BlockState(BlockCoord(0, 1, 0), BlockType.SMOOTH_STONE))
        grid.set(BlockState(BlockCoord(0, 2, 0), BlockType.REDSTONE_TORCH, attachmentFacing = Facing.DOWN, isActive = true, power = 15))
        grid.set(BlockState(BlockCoord(-1, 1, 0), BlockType.LEVER, isActive = false, facing = Facing.EAST))

        val engine = RedstoneEngine(grid)
        engine.recalculateWireSignals()

        assertTrue(grid.get(0, 2, 0)!!.isActive)

        // Liga alavanca
        val lever = grid.get(-1, 1, 0)!!
        grid.set(lever.copy(isActive = true, power = 15))

        // Tick 1: detects base powered, schedules torch turn OFF with 1-tick delay
        engine.stepTick()
        // Tick 2: processes scheduled torch event
        engine.stepTick()
        assertFalse(grid.get(0, 2, 0)!!.isActive)
        assertEquals(0, grid.get(0, 2, 0)!!.power)
    }

    @Test
    fun testCopperBulbTFlipFlop() {
        val grid = CircuitGrid()
        grid.set(BlockState(BlockCoord(0, 1, 0), BlockType.STONE_BUTTON, isActive = false))
        grid.set(BlockState(BlockCoord(1, 1, 0), BlockType.COPPER_BULB, isActive = false))

        val engine = RedstoneEngine(grid)

        val button = grid.get(0, 1, 0)!!
        grid.set(button.triggerButton())

        engine.stepTick()
        assertTrue(grid.get(1, 1, 0)!!.isActive)

        for (i in 1..10) {
            engine.stepTick()
        }
        assertEquals(0, grid.get(0, 1, 0)?.power)
        assertTrue(grid.get(1, 1, 0)!!.isActive)

        grid.set(button.triggerButton())
        engine.stepTick()
        assertFalse(grid.get(1, 1, 0)!!.isActive)
    }

    @Test
    fun testPistonPushAndObsidianImmovable() {
        val grid = CircuitGrid()
        grid.set(BlockState(BlockCoord(0, 1, 0), BlockType.PISTON, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(1, 1, 0), BlockType.SMOOTH_STONE))
        grid.set(BlockState(BlockCoord(2, 1, 0), BlockType.OBSIDIAN))
        grid.set(BlockState(BlockCoord(0, 1, 1), BlockType.LEVER, isActive = true, power = 15))

        val engine = RedstoneEngine(grid)
        engine.stepTick()

        assertFalse(grid.get(0, 1, 0)!!.isExtended)
        assertEquals(BlockType.SMOOTH_STONE, grid.get(1, 1, 0)?.type)
    }

    @Test
    fun testRepeaterExtractsSignalFromSoftPoweredSolidBlock() {
        val grid = CircuitGrid()
        // Lever -> Wire -> Stone Block -> Repeater (facing East) -> Wire
        grid.set(BlockState(BlockCoord(0, 1, 0), BlockType.LEVER, isActive = true, power = 15))
        grid.set(BlockState(BlockCoord(1, 1, 0), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(2, 1, 0), BlockType.SMOOTH_STONE))
        grid.set(BlockState(BlockCoord(3, 1, 0), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 1))
        grid.set(BlockState(BlockCoord(4, 1, 0), BlockType.REDSTONE_WIRE))

        val engine = RedstoneEngine(grid)
        engine.recalculateWireSignals()

        // Wire at (1,1,0) should have 14 power
        assertEquals(14, grid.get(1, 1, 0)?.power)

        // Tick 1: Repeater at (3,1,0) extracts signal from soft-powered stone at (2,1,0) and schedules output
        engine.stepTick()
        // Tick 2: Repeater fires output 15
        engine.stepTick()

        assertEquals(15, grid.get(3, 1, 0)?.power)
        assertEquals(15, grid.get(4, 1, 0)?.power)
    }
}
