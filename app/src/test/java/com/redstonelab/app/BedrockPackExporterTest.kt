package com.redstonelab.app

import com.redstonelab.app.exporter.BedrockPackExporter
import com.redstonelab.app.model.BlockCoord
import com.redstonelab.app.model.BlockState
import com.redstonelab.app.model.BlockType
import com.redstonelab.app.model.CircuitGrid
import com.redstonelab.app.model.Facing
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class BedrockPackExporterTest {

    @Test
    fun testManifestJsonStructure() {
        val manifest = BedrockPackExporter.generateManifestJson("TestCircuit")
        assertTrue(manifest.contains("\"format_version\": 2"))
        assertTrue(manifest.contains("RedstoneLab - TestCircuit"))
        assertTrue(manifest.contains("\"type\": \"data\""))
        assertTrue(manifest.contains("\"min_engine_version\""))
    }

    @Test
    fun testSpawnFunctionGeneration() {
        val grid = CircuitGrid()
        grid.set(BlockState(BlockCoord(0, 0, 0), BlockType.STONE))
        grid.set(BlockState(BlockCoord(1, 0, 0), BlockType.REDSTONE_WIRE, power = 15))
        grid.set(BlockState(BlockCoord(2, 0, 0), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 2))
        grid.set(BlockState(BlockCoord(3, 0, 0), BlockType.REDSTONE_LAMP, isActive = true))

        val script = BedrockPackExporter.generateSpawnFunction(grid)
        assertTrue(script.contains("setblock ~0 ~0 ~0 stone replace"))
        assertTrue(script.contains("setblock ~1 ~0 ~0 redstone_wire [\"redstone_signal\"=15] replace"))
        assertTrue(script.contains("unpowered_repeater [\"minecraft:cardinal_direction\"=\"east\""))
        assertTrue(script.contains("lit_redstone_lamp replace"))
    }

    @Test
    fun testPackIconBytes() {
        val iconBytes = BedrockPackExporter.createPackIcon()
        assertNotNull(iconBytes)
        assertTrue(iconBytes.isNotEmpty())
        // PNG magic header: 0x89 'P' 'N' 'G'
        assertEquals(0x89.toByte(), iconBytes[0])
        assertEquals('P'.code.toByte(), iconBytes[1])
        assertEquals('N'.code.toByte(), iconBytes[2])
        assertEquals('G'.code.toByte(), iconBytes[3])
    }

    @Test
    fun testMcPackZipIntegrity() {
        val grid = CircuitGrid()
        grid.set(BlockState(BlockCoord(0, 0, 0), BlockType.REDSTONE_BLOCK))
        grid.set(BlockState(BlockCoord(1, 0, 0), BlockType.REDSTONE_LAMP))

        val zipBytes = BedrockPackExporter.buildMcPackZip(grid, "TestPulse")
        assertTrue(zipBytes.isNotEmpty())

        val entries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                entry = zis.nextEntry
            }
        }

        assertTrue(entries.contains("manifest.json"))
        assertTrue(entries.contains("pack_icon.png"))
        assertTrue(entries.contains("functions/spawn.mcfunction"))
        assertTrue(entries.contains("functions/clear.mcfunction"))
    }

    private fun assertEquals(expected: Byte, actual: Byte) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
