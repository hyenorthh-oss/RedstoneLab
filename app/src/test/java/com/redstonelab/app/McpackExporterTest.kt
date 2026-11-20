package com.redstonelab.app

import com.redstonelab.app.exporter.McpackExporter
import com.redstonelab.app.model.BlockCoord
import com.redstonelab.app.model.BlockState
import com.redstonelab.app.model.BlockType
import com.redstonelab.app.model.CircuitGrid
import com.redstonelab.app.model.Facing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class McpackExporterTest {

    @Test
    fun testSanitizeName() {
        assertEquals("porta_secreta", McpackExporter.sanitizeCircuitName("Porta Secreta"))
        assertEquals("relogio_4_ticks", McpackExporter.sanitizeCircuitName("Relógio 4 Ticks!"))
        assertEquals("circuito", McpackExporter.sanitizeCircuitName("   "))
    }

    @Test
    fun testManifestJsonBedrock21() {
        val manifest = McpackExporter.generateManifestJson("porta_secreta", "Porta Secreta")
        assertTrue(manifest.contains("\"format_version\": 2"))
        assertTrue(manifest.contains("\"min_engine_version\": [\n      1,\n      21,\n      0\n    ]"))
        assertTrue(manifest.contains("\"type\": \"data\""))
    }

    @Test
    fun testStrictBlockOrderInMcfunction() {
        val grid = CircuitGrid()
        // Adiciona pó dependente e pedra sólida
        grid.set(BlockState(BlockCoord(1, 1, 1), BlockType.REDSTONE_WIRE, power = 15))
        grid.set(BlockState(BlockCoord(1, 0, 1), BlockType.SMOOTH_STONE))
        grid.set(BlockState(BlockCoord(2, 1, 1), BlockType.REDSTONE_REPEATER, facing = Facing.EAST))

        val script = McpackExporter.generateMcfunctionScript(grid, "teste_ordem", includeSmoothStoneBase = true)

        // 1. Limpeza e base devem vir antes dos blocos
        val fillIndex = script.indexOf("fill ~0 ~0 ~0")
        val solidIndex = script.indexOf("setblock ~0 ~0 ~0 smooth_stone replace")
        val wireIndex = script.indexOf("setblock ~0 ~1 ~0 redstone_wire")
        val repeaterIndex = script.indexOf("setblock ~1 ~1 ~0 unpowered_repeater")

        assertTrue("Fill deve vir primeiro", fillIndex < solidIndex)
        assertTrue("Bloco sólido deve vir antes do fio dependente", solidIndex < wireIndex)
        assertTrue("Blocos dependentes devem vir depois dos sólidos", solidIndex < repeaterIndex)
    }

    @Test
    fun testMcpackZipIntegrity() {
        val grid = CircuitGrid()
        grid.set(BlockState(BlockCoord(0, 0, 0), BlockType.SMOOTH_STONE))
        grid.set(BlockState(BlockCoord(0, 1, 0), BlockType.REDSTONE_LAMP))

        val (_, zipBytes) = McpackExporter.buildMcpackBytes(grid, "Meu Circuito", true)
        assertNotNull(zipBytes)
        assertTrue(zipBytes.isNotEmpty())

        val entryNames = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                entry = zis.nextEntry
            }
        }

        assertTrue(entryNames.contains("manifest.json"))
        assertTrue(entryNames.contains("pack_icon.png"))
        assertTrue(entryNames.contains("functions/meu_circuito.mcfunction"))
    }
}
