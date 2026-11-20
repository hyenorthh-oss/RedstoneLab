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
        // Bloco de redstone em (0, 1, 0)
        grid.set(BlockState(BlockCoord(0, 1, 0), BlockType.REDSTONE_BLOCK, power = 15))

        // Linha de 16 fios de redstone
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

        // Tick 0: saída desenergizada
        engine.stepTick()
        assertEquals(0, grid.get(1, 1, 0)?.power)
        assertEquals(0, grid.get(2, 1, 0)?.power)

        // Tick 2: repetidor dispara sinal 15
        engine.stepTick()
        assertEquals(15, grid.get(1, 1, 0)?.power)
        assertEquals(15, grid.get(2, 1, 0)?.power)
    }

    @Test
    fun testTorchInversion() {
        val grid = CircuitGrid()
        // Base de pedra lisa com tocha no topo
        grid.set(BlockState(BlockCoord(0, 1, 0), BlockType.SMOOTH_STONE))
        grid.set(BlockState(BlockCoord(0, 2, 0), BlockType.REDSTONE_TORCH, attachmentFacing = Facing.DOWN, isActive = true, power = 15))
        grid.set(BlockState(BlockCoord(-1, 1, 0), BlockType.LEVER, isActive = false, facing = Facing.EAST))

        val engine = RedstoneEngine(grid)
        engine.recalculateWireSignals()

        assertTrue(grid.get(0, 2, 0)!!.isActive)

        // Liga alavanca
        val lever = grid.get(-1, 1, 0)!!
        grid.set(lever.copy(isActive = true, power = 15))

        // Tocha inverte com 1 tick de atraso
        engine.stepTick()
        assertFalse(grid.get(0, 2, 0)!!.isActive)
        assertEquals(0, grid.get(0, 2, 0)!!.power)
    }

    @Test
    fun testCopperBulbTFlipFlop() {
        val grid = CircuitGrid()
        // Botão -> Lâmpada de Cobre
        grid.set(BlockState(BlockCoord(0, 1, 0), BlockType.STONE_BUTTON, isActive = false))
        grid.set(BlockState(BlockCoord(1, 1, 0), BlockType.COPPER_BULB, isActive = false))

        val engine = RedstoneEngine(grid)

        // Pressiona o botão (pulso de subida)
        val button = grid.get(0, 1, 0)!!
        grid.set(button.triggerButton())

        engine.stepTick()
        // A lâmpada de cobre deve alternar para acesa (isActive = true)
        assertTrue(grid.get(1, 1, 0)!!.isActive)

        // Pulso do botão acaba
        for (i in 1..10) {
            engine.stepTick()
        }
        assertEquals(0, grid.get(0, 1, 0)?.power)
        // A lâmpada de cobre continua acesa (T-Flip-Flop)
        assertTrue(grid.get(1, 1, 0)!!.isActive)

        // Segundo pulso de subida deve desligá-la
        grid.set(button.triggerButton())
        engine.stepTick()
        assertFalse(grid.get(1, 1, 0)!!.isActive)
    }

    @Test
    fun testPistonPushAndObsidianImmovable() {
        val grid = CircuitGrid()
        // Pistão virado para LESTE
        grid.set(BlockState(BlockCoord(0, 1, 0), BlockType.PISTON, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(1, 1, 0), BlockType.SMOOTH_STONE))
        grid.set(BlockState(BlockCoord(2, 1, 0), BlockType.OBSIDIAN)) // Imóvel
        grid.set(BlockState(BlockCoord(0, 1, 1), BlockType.LEVER, isActive = true, power = 15))

        val engine = RedstoneEngine(grid)
        engine.stepTick()

        // Devido à Obsidiana, o pistão não pode estender nem mover os blocos
        assertFalse(grid.get(0, 1, 0)!!.isExtended)
        assertEquals(BlockType.SMOOTH_STONE, grid.get(1, 1, 0)?.type)
    }
}
