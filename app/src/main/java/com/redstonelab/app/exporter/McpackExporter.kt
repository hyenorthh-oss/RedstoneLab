package com.redstonelab.app.exporter

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
 * Gerador e Exportador de Pacotes de Comportamento .mcpack para Minecraft Bedrock 1.21+.
 */
object McpackExporter {

    fun sanitizeCircuitName(name: String): String {
        val sanitized = name.trim().lowercase()
            .replace("[^a-z0-9_]".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .trim('_')
        return sanitized.ifEmpty { "circuito" }
    }

    fun generateManifestJson(sanitizedName: String, displayName: String): String {
        val headerUuid = UUID.randomUUID().toString()
        val moduleUuid = UUID.randomUUID().toString()

        return """
        {
          "format_version": 2,
          "header": {
            "name": "RedstoneLab - $displayName",
            "description": "Exportado pelo RedstoneLab Android Simulator. Execute '/function $sanitizedName' no chat.",
            "uuid": "$headerUuid",
            "version": [1, 0, 0],
            "min_engine_version": [1, 21, 0]
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
     * Gera o script .mcfunction com ordem estrita de colocação:
     * 1. Limpeza da área com air e piso de pedra lisa opcional.
     * 2. Todos os blocos sólidos estruturais primeiro.
     * 3. Blocos dependentes (pó, tochas, repetidores, etc.) por último.
     */
    fun generateMcfunctionScript(
        grid: CircuitGrid,
        sanitizedName: String,
        includeSmoothStoneBase: Boolean
    ): String {
        val bounds = grid.getBounds()
        val sb = StringBuilder()

        sb.append("# RedstoneLab - Minecraft Bedrock 1.21+ Spawn Script\n")
        sb.append("# Dimensões do Circuito: ${bounds.sizeX}x${bounds.sizeY}x${bounds.sizeZ} (X: largura, Y: altura, Z: profundidade)\n")
        sb.append("# Comando no chat: /function $sanitizedName\n\n")

        val dx = (bounds.sizeX - 1).coerceAtLeast(0)
        val dy = (bounds.sizeY - 1).coerceAtLeast(0)
        val dz = (bounds.sizeZ - 1).coerceAtLeast(0)

        // 1. Limpeza da área
        sb.append("# [1] Limpeza do espaço de construção\n")
        sb.append("fill ~0 ~0 ~0 ~$dx ~$dy ~$dz air replace\n\n")

        if (includeSmoothStoneBase) {
            sb.append("# [1.1] Piso base de Pedra Lisa\n")
            sb.append("fill ~0 ~-1 ~0 ~$dx ~-1 ~$dz smooth_stone replace\n\n")
        }

        val allBlocks = grid.getAll().values.toList()

        // Separa blocos em sólidos/estruturais vs dependentes
        val solidBlocks = allBlocks.filter { !it.type.isDependent && it.type != BlockType.AIR }
            .sortedWith(compareBy<BlockState> { it.y }.thenBy { it.z }.thenBy { it.x })

        val dependentBlocks = allBlocks.filter { it.type.isDependent && it.type != BlockType.AIR }
            .sortedWith(compareBy<BlockState> { it.y }.thenBy { it.z }.thenBy { it.x })

        // 2. Blocos sólidos e estruturais primeiro
        sb.append("# [2] Blocos sólidos e estruturas condutoras\n")
        for (b in solidBlocks) {
            val relX = b.x - bounds.minX
            val relY = b.y - bounds.minY
            val relZ = b.z - bounds.minZ
            val cmd = formatBedrockSetblock(b, relX, relY, relZ)
            if (cmd != null) {
                sb.append(cmd).append("\n")
            }
        }
        sb.append("\n")

        // 3. Blocos dependentes por último (para não caírem como itens)
        sb.append("# [3] Componentes de circuito dependentes (fios, tochas, repetidores, etc.)\n")
        for (b in dependentBlocks) {
            val relX = b.x - bounds.minX
            val relY = b.y - bounds.minY
            val relZ = b.z - bounds.minZ
            val cmd = formatBedrockSetblock(b, relX, relY, relZ)
            if (cmd != null) {
                sb.append(cmd).append("\n")
            }
        }

        sb.append("\ntellraw @a {\"rawtext\":[{\"text\":\"§b[RedstoneLab] §aCircuito '$sanitizedName' gerado com sucesso!\"}]}\n")
        return sb.toString()
    }

    private fun formatBedrockSetblock(b: BlockState, rx: Int, ry: Int, rz: Int): String? {
        val pos = "~$rx ~$ry ~$rz"
        return when (b.type) {
            // Estruturais & Sólidos
            BlockType.SMOOTH_STONE -> "setblock $pos smooth_stone replace"
            BlockType.IRON_BLOCK -> "setblock $pos iron_block replace"
            BlockType.RED_WOOL -> "setblock $pos wool [\"color\"=\"red\"] replace"
            BlockType.BLUE_WOOL -> "setblock $pos wool [\"color\"=\"blue\"] replace"
            BlockType.GREEN_WOOL -> "setblock $pos wool [\"color\"=\"green\"] replace"
            BlockType.YELLOW_WOOL -> "setblock $pos wool [\"color\"=\"yellow\"] replace"
            BlockType.GLASS -> "setblock $pos glass replace"
            BlockType.STONE_SLAB -> "setblock $pos stone_block_slab [\"top_slot_bit\"=true] replace"
            BlockType.SLIME_BLOCK -> "setblock $pos slime replace"
            BlockType.HONEY_BLOCK -> "setblock $pos honey_block replace"
            BlockType.OBSIDIAN -> "setblock $pos obsidian replace"
            BlockType.TARGET_BLOCK -> "setblock $pos target replace"
            BlockType.REDSTONE_BLOCK -> "setblock $pos redstone_block replace"

            // Transmissão e Lógica
            BlockType.REDSTONE_WIRE -> {
                val sig = b.power.coerceIn(0, 15)
                "setblock $pos redstone_wire [\"redstone_signal\"=$sig] replace"
            }

            BlockType.REDSTONE_REPEATER -> {
                val blockId = if (b.power > 0) "powered_repeater" else "unpowered_repeater"
                val dir = b.facing.bedrockName
                val delayIndex = (b.delay - 1).coerceIn(0, 3)
                "setblock $pos $blockId [\"minecraft:cardinal_direction\"=\"$dir\",\"repeater_delay\"=$delayIndex] replace"
            }

            BlockType.REDSTONE_COMPARATOR -> {
                val blockId = if (b.power > 0) "powered_comparator" else "unpowered_comparator"
                val dir = b.facing.bedrockName
                val subtractBit = b.isSubtractMode
                "setblock $pos $blockId [\"minecraft:cardinal_direction\"=\"$dir\",\"output_subtract_bit\"=$subtractBit] replace"
            }

            // Fontes de Energia
            BlockType.LEVER -> {
                val openBit = b.isActive
                val leverDir = when (b.attachmentFacing) {
                    Facing.UP -> "down_north_south"
                    Facing.DOWN -> "up_north_south"
                    Facing.NORTH -> "north"
                    Facing.SOUTH -> "south"
                    Facing.EAST -> "east"
                    Facing.WEST -> "west"
                }
                "setblock $pos lever [\"lever_direction\"=\"$leverDir\",\"open_bit\"=$openBit] replace"
            }

            BlockType.STONE_BUTTON -> {
                val facingDir = when (b.attachmentFacing) {
                    Facing.DOWN -> 1
                    Facing.UP -> 0
                    Facing.NORTH -> 2
                    Facing.SOUTH -> 3
                    Facing.WEST -> 4
                    Facing.EAST -> 5
                }
                val pressed = b.pulseTicksLeft > 0
                "setblock $pos stone_button [\"facing_direction\"=$facingDir,\"button_pressed_bit\"=$pressed] replace"
            }

            BlockType.STONE_PRESSURE_PLATE -> {
                val sig = if (b.isActive) 15 else 0
                "setblock $pos stone_pressure_plate [\"redstone_signal\"=$sig] replace"
            }

            BlockType.REDSTONE_TORCH -> {
                val torchFacing = when (b.attachmentFacing) {
                    Facing.DOWN, Facing.UP -> "top"
                    Facing.NORTH -> "south"
                    Facing.SOUTH -> "north"
                    Facing.EAST -> "west"
                    Facing.WEST -> "east"
                }
                "setblock $pos redstone_torch [\"torch_facing_direction\"=\"$torchFacing\"] replace"
            }

            BlockType.DAYLIGHT_DETECTOR -> "setblock $pos daylight_detector replace"
            BlockType.SCULK_SENSOR -> "setblock $pos sculk_sensor replace"

            // Mecanismos e Saídas
            BlockType.REDSTONE_LAMP -> {
                val blockId = if (b.isActive || b.power > 0) "lit_redstone_lamp" else "redstone_lamp"
                "setblock $pos $blockId replace"
            }

            BlockType.COPPER_BULB -> {
                val isLit = b.isActive
                val isPowered = b.power > 0
                "setblock $pos copper_bulb [\"lit\"=$isLit,\"powered\"=$isPowered] replace"
            }

            BlockType.PISTON -> {
                val f = getFacingIndex(b.facing)
                "setblock $pos piston [\"facing_direction\"=$f] replace"
            }

            BlockType.STICKY_PISTON -> {
                val f = getFacingIndex(b.facing)
                "setblock $pos sticky_piston [\"facing_direction\"=$f] replace"
            }

            BlockType.OBSERVER -> {
                val f = getFacingIndex(b.facing)
                "setblock $pos observer [\"facing_direction\"=$f] replace"
            }

            BlockType.NOTE_BLOCK -> "setblock $pos noteblock replace"
            BlockType.DISPENSER -> {
                val f = getFacingIndex(b.facing)
                "setblock $pos dispenser [\"facing_direction\"=$f] replace"
            }

            BlockType.DROPPER -> {
                val f = getFacingIndex(b.facing)
                "setblock $pos dropper [\"facing_direction\"=$f] replace"
            }

            BlockType.HOPPER -> {
                val f = getFacingIndex(b.facing)
                "setblock $pos hopper [\"facing_direction\"=$f] replace"
            }

            BlockType.IRON_DOOR -> {
                val openBit = b.isActive
                "setblock $pos iron_door [\"direction\"=0,\"open_bit\"=$openBit,\"upper_block_bit\"=false] replace"
            }

            BlockType.IRON_TRAPDOOR -> {
                val openBit = b.isActive
                "setblock $pos iron_trapdoor [\"direction\"=0,\"open_bit\"=$openBit] replace"
            }

            BlockType.BELL -> "setblock $pos bell [\"direction\"=0] replace"
            BlockType.TNT -> "setblock $pos tnt replace"

            BlockType.AIR -> null
        }
    }

    private fun getFacingIndex(facing: Facing): Int {
        return when (facing) {
            Facing.DOWN -> 0
            Facing.UP -> 1
            Facing.NORTH -> 2
            Facing.SOUTH -> 3
            Facing.WEST -> 4
            Facing.EAST -> 5
        }
    }

    fun createPackIcon(): ByteArray {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply { color = android.graphics.Color.rgb(6, 9, 19) }
        canvas.drawRect(Rect(0, 0, size, size), bgPaint)

        val borderPaint = Paint().apply {
            color = android.graphics.Color.rgb(0, 240, 255)
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
        canvas.drawRect(Rect(5, 5, size - 5, size - 5), borderPaint)

        val tracePaint = Paint().apply {
            color = android.graphics.Color.rgb(255, 36, 52)
            strokeWidth = 14f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(40f, 128f, 128f, 128f, tracePaint)
        canvas.drawLine(128f, 128f, 200f, 56f, tracePaint)
        canvas.drawLine(128f, 128f, 200f, 200f, tracePaint)

        val glowPaint = Paint().apply {
            color = android.graphics.Color.rgb(0, 240, 255)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(128f, 128f, 22f, glowPaint)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    fun buildMcpackBytes(
        grid: CircuitGrid,
        circuitName: String,
        includeSmoothStoneBase: Boolean
    ): Pair<String, ByteArray> {
        val sanitized = sanitizeCircuitName(circuitName)
        val out = ByteArrayOutputStream()

        ZipOutputStream(out).use { zip ->
            // 1. manifest.json
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(generateManifestJson(sanitized, circuitName).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 2. pack_icon.png
            zip.putNextEntry(ZipEntry("pack_icon.png"))
            zip.write(createPackIcon())
            zip.closeEntry()

            // 3. functions/<nome_sanitizado>.mcfunction
            zip.putNextEntry(ZipEntry("functions/$sanitized.mcfunction"))
            zip.write(generateMcfunctionScript(grid, sanitized, includeSmoothStoneBase).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        return Pair(sanitized, out.toByteArray())
    }

    /**
     * Salva o .mcpack em /sdcard/Download/RedstoneLab/<nome>.mcpack e no cache do app para FileProvider.
     */
    fun saveMcpackFile(
        context: Context,
        grid: CircuitGrid,
        circuitName: String,
        includeSmoothStoneBase: Boolean
    ): File {
        val (sanitized, zipBytes) = buildMcpackBytes(grid, circuitName, includeSmoothStoneBase)
        val fileName = "$sanitized.mcpack"

        // 1. Salva na pasta do Download acessível
        try {
            val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RedstoneLab")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            val targetFile = File(downloadDir, fileName)
            FileOutputStream(targetFile).use { it.write(zipBytes) }
        } catch (_: Exception) {}

        // 2. Salva no cache interno para o FileProvider seguro
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cacheFile = File(cacheDir, fileName)
        FileOutputStream(cacheFile).use { it.write(zipBytes) }
        return cacheFile
    }

    /**
     * Cria o Intent para abrir diretamente no Minecraft Bedrock instalado ou no compartilhador nativo.
     */
    fun createOpenInMinecraftIntent(context: Context, packFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            packFile
        )

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/octet-stream")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
