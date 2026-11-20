package com.redstonelab.app.exporter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.FileProvider
import com.redstonelab.app.model.BlockState
import com.redstonelab.app.model.BlockType
import com.redstonelab.app.model.CircuitGrid
import com.redstonelab.app.model.Facing
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minecraft Bedrock Edition .mcpack Generator.
 * Packages the circuit into a valid Bedrock Behavior Pack containing:
 * - manifest.json (format_version 2 with unique UUIDs)
 * - pack_icon.png (Procedural high-res circuit badge)
 * - functions/spawn.mcfunction (/setblock sequence with Bedrock block states)
 * - functions/clear.mcfunction (/fill cleanup command)
 */
object BedrockPackExporter {

    fun generateManifestJson(packName: String): String {
        val headerUuid = UUID.randomUUID().toString()
        val moduleUuid = UUID.randomUUID().toString()

        return """
        {
          "format_version": 2,
          "header": {
            "name": "RedstoneLab - $packName",
            "description": "Exported from RedstoneLab Android Simulator. Execute '/function spawn' to deploy in-game.",
            "uuid": "$headerUuid",
            "version": [1, 0, 0],
            "min_engine_version": [1, 20, 0]
          },
          "modules": [
            {
              "type": "data",
              "uuid": "$moduleUuid",
              "version": [1, 0, 0]
            }
          ]
        }
        """.trimIndent()
    }

    /**
     * Converts the circuit blocks into Minecraft Bedrock .mcfunction /setblock commands.
     * Normalized so min coordinates map to ~0 ~0 ~0.
     * Note: In Bedrock coordinates, Y is UP (Z in our grid), and Z is NORTH/SOUTH (Y in our grid).
     */
    fun generateSpawnFunction(grid: CircuitGrid): String {
        val bounds = grid.getBounds()
        val sb = StringBuilder()
        sb.append("# RedstoneLab Bedrock Circuit Spawn Script\n")
        sb.append("# Generated bounds: ${bounds.sizeX}x${bounds.sizeY}x${bounds.sizeZ}\n")
        sb.append("# Run inside Minecraft with: /function spawn\n\n")

        val blocks = grid.getAll().values.sortedWith(
            compareBy<BlockState> { it.z }
                .thenBy { it.y }
                .thenBy { it.x }
        )

        for (b in blocks) {
            val relX = b.x - bounds.minX
            val relY = b.z - bounds.minZ // Minecraft Y is vertical elevation
            val relZ = b.y - bounds.minY // Minecraft Z is horizontal north/south

            val command = formatBedrockSetBlock(b, relX, relY, relZ)
            if (command != null) {
                sb.append(command).append("\n")
            }
        }

        sb.append("\ntellraw @a {\"rawtext\":[{\"text\":\"§b[RedstoneLab] §aCircuit successfully spawned!\"}]}\n")
        return sb.toString()
    }

    fun generateClearFunction(grid: CircuitGrid): String {
        val bounds = grid.getBounds()
        val dx = (bounds.sizeX - 1).coerceAtLeast(0)
        val dy = (bounds.sizeZ - 1).coerceAtLeast(0)
        val dz = (bounds.sizeY - 1).coerceAtLeast(0)

        return """
        # RedstoneLab Circuit Cleanup Script
        fill ~0 ~0 ~0 ~$dx ~$dy ~$dz air replace
        tellraw @a {"rawtext":[{"text":"§b[RedstoneLab] §7Circuit space cleared."}]}
        """.trimIndent()
    }

    private fun formatBedrockSetBlock(b: BlockState, rx: Int, ry: Int, rz: Int): String? {
        val pos = "~$rx ~$ry ~$rz"
        return when (b.type) {
            BlockType.STONE -> "setblock $pos stone replace"
            BlockType.GLASS -> "setblock $pos glass replace"
            BlockType.REDSTONE_BLOCK -> "setblock $pos redstone_block replace"
            BlockType.TARGET_BLOCK -> "setblock $pos target replace"

            BlockType.REDSTONE_WIRE -> {
                val sig = b.power.coerceIn(0, 15)
                "setblock $pos redstone_wire [\"redstone_signal\"=$sig] replace"
            }

            BlockType.REDSTONE_TORCH -> {
                val torchFacing = when (b.facing) {
                    Facing.UP, Facing.DOWN -> "top"
                    Facing.NORTH -> "south"
                    Facing.SOUTH -> "north"
                    Facing.EAST -> "west"
                    Facing.WEST -> "east"
                }
                "setblock $pos redstone_torch [\"torch_facing_direction\"=\"$torchFacing\"] replace"
            }

            BlockType.REDSTONE_REPEATER -> {
                val blockId = if (b.power > 0) "powered_repeater" else "unpowered_repeater"
                val dir = b.facing.bedrockName
                // repeater delay in Bedrock is 0..3 (representing 1..4 ticks)
                val bedrockDelay = (b.delay - 1).coerceIn(0, 3)
                "setblock $pos $blockId [\"minecraft:cardinal_direction\"=\"$dir\", \"repeater_delay\"=$bedrockDelay] replace"
            }

            BlockType.REDSTONE_COMPARATOR -> {
                val blockId = if (b.power > 0) "powered_comparator" else "unpowered_comparator"
                val dir = b.facing.bedrockName
                val mode = if (b.isSubtractMode) "\"subtraction\"" else "\"compare\""
                "setblock $pos $blockId [\"minecraft:cardinal_direction\"=\"$dir\", \"output_subtract_bit\"=${b.isSubtractMode}] replace"
            }

            BlockType.REDSTONE_LAMP -> {
                val blockId = if (b.isActive || b.power > 0) "lit_redstone_lamp" else "redstone_lamp"
                "setblock $pos $blockId replace"
            }

            BlockType.LEVER -> {
                val openBit = b.isActive
                "setblock $pos lever [\"lever_direction\"=\"up_north_south\", \"open_bit\"=$openBit] replace"
            }

            BlockType.STONE_BUTTON -> {
                val pressed = b.pulseTicksLeft > 0
                "setblock $pos stone_button [\"facing_direction\"=1, \"button_pressed_bit\"=$pressed] replace"
            }

            BlockType.PISTON -> {
                val facingId = when (b.facing) {
                    Facing.DOWN -> 0
                    Facing.UP -> 1
                    Facing.NORTH -> 2
                    Facing.SOUTH -> 3
                    Facing.WEST -> 4
                    Facing.EAST -> 5
                }
                "setblock $pos piston [\"facing_direction\"=$facingId] replace"
            }

            BlockType.STICKY_PISTON -> {
                val facingId = when (b.facing) {
                    Facing.DOWN -> 0
                    Facing.UP -> 1
                    Facing.NORTH -> 2
                    Facing.SOUTH -> 3
                    Facing.WEST -> 4
                    Facing.EAST -> 5
                }
                "setblock $pos sticky_piston [\"facing_direction\"=$facingId] replace"
            }

            BlockType.OBSERVER -> {
                val facingId = when (b.facing) {
                    Facing.DOWN -> 0
                    Facing.UP -> 1
                    Facing.NORTH -> 2
                    Facing.SOUTH -> 3
                    Facing.WEST -> 4
                    Facing.EAST -> 5
                }
                "setblock $pos observer [\"facing_direction\"=$facingId] replace"
            }

            BlockType.AIR -> null
        }
    }

    /**
     * Generates a 128x128 pack_icon.png bitmap for Bedrock pack.
     */
    fun createPackIcon(): ByteArray {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark obsidian background
        val bgPaint = Paint().apply { color = android.graphics.Color.rgb(6, 9, 19) }
        canvas.drawRect(Rect(0, 0, size, size), bgPaint)

        // Border cyan stroke
        val borderPaint = Paint().apply {
            color = android.graphics.Color.rgb(0, 240, 255)
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRect(Rect(3, 3, size - 3, size - 3), borderPaint)

        // Diagonal cyber redstone trace
        val tracePaint = Paint().apply {
            color = android.graphics.Color.rgb(255, 36, 52)
            strokeWidth = 8f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(24f, 64f, 64f, 64f, tracePaint)
        canvas.drawLine(64f, 64f, 104f, 24f, tracePaint)
        canvas.drawLine(64f, 64f, 104f, 104f, tracePaint)

        // Glowing center node
        val glowPaint = Paint().apply {
            color = android.graphics.Color.rgb(0, 240, 255)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(64f, 64f, 12f, glowPaint)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    /**
     * Compresses manifest, icon, and functions into a valid Bedrock .mcpack ZIP byte buffer.
     */
    fun buildMcPackZip(grid: CircuitGrid, packName: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            // 1. manifest.json
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(generateManifestJson(packName).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 2. pack_icon.png
            zip.putNextEntry(ZipEntry("pack_icon.png"))
            zip.write(createPackIcon())
            zip.closeEntry()

            // 3. functions/spawn.mcfunction
            zip.putNextEntry(ZipEntry("functions/spawn.mcfunction"))
            zip.write(generateSpawnFunction(grid).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 4. functions/clear.mcfunction
            zip.putNextEntry(ZipEntry("functions/clear.mcfunction"))
            zip.write(generateClearFunction(grid).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    /**
     * Writes the .mcpack file to the application's cache directory ready for sharing.
     */
    fun exportToCacheFile(context: Context, grid: CircuitGrid, packName: String): File {
        val sanitized = packName.trim().replace("[^a-zA-Z0-9_\\-]".toRegex(), "_").ifEmpty { "Circuit" }
        val fileName = "$sanitized.mcpack"
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val packFile = File(cacheDir, fileName)

        val zipBytes = buildMcPackZip(grid, sanitized)
        FileOutputStream(packFile).use { it.write(zipBytes) }
        return packFile
    }

    /**
     * Builds an Android Intent to share the .mcpack file or open directly in Minecraft.
     */
    fun createShareIntent(context: Context, packFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            packFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "RedstoneLab Circuit Export")
            putExtra(Intent.EXTRA_TEXT, "Import this circuit into Minecraft Bedrock Edition!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
