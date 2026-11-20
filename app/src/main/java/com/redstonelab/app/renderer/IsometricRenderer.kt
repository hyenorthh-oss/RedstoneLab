package com.redstonelab.app.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.redstonelab.app.model.BlockCoord
import com.redstonelab.app.model.BlockState
import com.redstonelab.app.model.BlockType
import com.redstonelab.app.model.CircuitGrid
import com.redstonelab.app.model.Facing
import com.redstonelab.app.theme.*

@Composable
fun IsometricCanvas(
    grid: CircuitGrid,
    activeLayerY: Int,
    isOnionSkinEnabled: Boolean,
    showSignalNumbers: Boolean,
    selectedBlockCoord: BlockCoord?,
    panOffset: Offset,
    zoom: Float,
    onPanChange: (Offset) -> Unit,
    onZoomChange: (Float) -> Unit,
    onTileClicked: (BlockCoord) -> Unit,
    onTileLongClicked: (BlockCoord) -> Unit,
    modifier: Modifier = Modifier
) {
    val transform = remember { IsometricTransform() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    onPanChange(panOffset + pan)
                    val newZoom = (zoom * gestureZoom).coerceIn(0.4f, 3.5f)
                    onZoomChange(newZoom)
                }
            }
            .pointerInput(activeLayerY, panOffset, zoom) {
                detectTapGestures(
                    onTap = { screenOffset ->
                        val (gx, gz) = transform.screenToGrid(
                            screenOffset,
                            activeLayerY,
                            panOffset,
                            zoom
                        )
                        onTileClicked(BlockCoord(gx, activeLayerY, gz))
                    },
                    onLongPress = { screenOffset ->
                        val (gx, gz) = transform.screenToGrid(
                            screenOffset,
                            activeLayerY,
                            panOffset,
                            zoom
                        )
                        onTileLongClicked(BlockCoord(gx, activeLayerY, gz))
                    }
                )
            }
    ) {
        val hw = transform.metrics.halfWidth * zoom
        val hh = transform.metrics.halfHeight * zoom
        val elev = transform.metrics.tileElevation * zoom

        // 1. Grade Isométrica de Referência para a Camada Ativa
        drawIsoGridPlane(
            layerY = activeLayerY,
            panOffset = panOffset,
            zoom = zoom,
            hw = hw,
            hh = hh,
            elev = elev
        )

        // 2. Filtro de blocos conforme modo Onion Skin ou 3D Completo
        val allBlocks = grid.getAll().values.toList()
        val blocksToDraw = if (isOnionSkinEnabled) {
            allBlocks.filter { it.y <= activeLayerY }
        } else {
            allBlocks
        }

        // Ordenação por profundidade: Menor Y primeiro, depois menor (X + Z), depois X
        val sortedBlocks = blocksToDraw.sortedWith(
            compareBy<BlockState> { it.y }
                .thenBy { it.x + it.z }
                .thenBy { it.x }
        )

        // 3. Renderização dos Blocos
        for (block in sortedBlocks) {
            val center = transform.gridToScreen(
                block.x.toFloat(),
                block.y.toFloat(),
                block.z.toFloat(),
                panOffset,
                zoom
            )

            // Cálculo da opacidade e estilo fantasma (Onion Skin)
            val isCurrentLayer = block.y == activeLayerY
            val alpha = when {
                isCurrentLayer -> 1.0f
                block.y < activeLayerY -> {
                    if (isOnionSkinEnabled) 0.35f else 0.70f
                }
                else -> 0.40f
            }

            drawIsometricBlock(
                block = block,
                center = center,
                hw = hw,
                hh = hh,
                elev = elev,
                alpha = alpha,
                isGhost = !isCurrentLayer && isOnionSkinEnabled,
                showSignalNumbers = showSignalNumbers,
                grid = grid
            )
        }

        // 4. Cursor Diamante Holográfico na Camada Ativa
        if (selectedBlockCoord != null && selectedBlockCoord.y == activeLayerY) {
            val cursorCenter = transform.gridToScreen(
                selectedBlockCoord.x.toFloat(),
                selectedBlockCoord.y.toFloat(),
                selectedBlockCoord.z.toFloat(),
                panOffset,
                zoom
            )
            drawCursor(cursorCenter, hw, hh)
        }
    }
}

private fun DrawScope.drawIsoGridPlane(
    layerY: Int,
    panOffset: Offset,
    zoom: Float,
    hw: Float,
    hh: Float,
    elev: Float
) {
    val size = 16
    for (x in 0..size) {
        val start = Offset(
            (x - 0) * hw + panOffset.x,
            (x + 0) * hh - (layerY * elev) + panOffset.y
        )
        val end = Offset(
            (x - size) * hw + panOffset.x,
            (x + size) * hh - (layerY * elev) + panOffset.y
        )
        drawLine(
            color = if (x == 0 || x == size) GridLineActive else GridLineSubtle,
            start = start,
            end = end,
            strokeWidth = if (x == 0 || x == size) 1.6f else 1.0f
        )
    }

    for (z in 0..size) {
        val start = Offset(
            (0 - z) * hw + panOffset.x,
            (0 + z) * hh - (layerY * elev) + panOffset.y
        )
        val end = Offset(
            (size - z) * hw + panOffset.x,
            (size + z) * hh - (layerY * elev) + panOffset.y
        )
        drawLine(
            color = if (z == 0 || z == size) GridLineActive else GridLineSubtle,
            start = start,
            end = end,
            strokeWidth = if (z == 0 || z == size) 1.6f else 1.0f
        )
    }
}

private fun DrawScope.drawCursor(center: Offset, hw: Float, hh: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - hh)
        lineTo(center.x + hw, center.y)
        lineTo(center.x, center.y + hh)
        lineTo(center.x - hw, center.y)
        close()
    }
    drawPath(path = path, color = NeonCyan.copy(alpha = 0.25f), style = Fill)
    drawPath(path = path, color = NeonCyan, style = Stroke(width = 2.5f))
}

private fun DrawScope.drawIsometricBlock(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float,
    isGhost: Boolean,
    showSignalNumbers: Boolean,
    grid: CircuitGrid
) {
    if (isGhost) {
        // Estilo Blueprint / Fantasma para camadas inferiores no modo Onion Skin
        drawGhostCube(center, hw, hh, elev, alpha)
        return
    }

    when (block.type) {
        // --- Estruturais e Sólidos ---
        BlockType.SMOOTH_STONE -> drawSolidCube(
            center, hw, hh, elev,
            topColor = StoneGrayLight.copy(alpha = alpha),
            leftColor = StoneGray.copy(alpha = alpha),
            rightColor = StoneGrayDark.copy(alpha = alpha)
        )

        BlockType.IRON_BLOCK -> drawSolidCube(
            center, hw, hh, elev,
            topColor = Color(0xFFE8EEF5).copy(alpha = alpha),
            leftColor = Color(0xFFCBD5E1).copy(alpha = alpha),
            rightColor = Color(0xFF94A3B8).copy(alpha = alpha)
        )

        BlockType.RED_WOOL -> drawSolidCube(
            center, hw, hh, elev,
            topColor = Color(0xFFC53030).copy(alpha = alpha),
            leftColor = Color(0xFF9B2C2C).copy(alpha = alpha),
            rightColor = Color(0xFF742A2A).copy(alpha = alpha)
        )

        BlockType.BLUE_WOOL -> drawSolidCube(
            center, hw, hh, elev,
            topColor = Color(0xFF3182CE).copy(alpha = alpha),
            leftColor = Color(0xFF2B6CB0).copy(alpha = alpha),
            rightColor = Color(0xFF2C5282).copy(alpha = alpha)
        )

        BlockType.GREEN_WOOL -> drawSolidCube(
            center, hw, hh, elev,
            topColor = Color(0xFF38A169).copy(alpha = alpha),
            leftColor = Color(0xFF2F855A).copy(alpha = alpha),
            rightColor = Color(0xFF276749).copy(alpha = alpha)
        )

        BlockType.YELLOW_WOOL -> drawSolidCube(
            center, hw, hh, elev,
            topColor = Color(0xFFD69E2E).copy(alpha = alpha),
            leftColor = Color(0xFFB7791F).copy(alpha = alpha),
            rightColor = Color(0xFF975A16).copy(alpha = alpha)
        )

        BlockType.GLASS -> drawGlassCube(center, hw, hh, elev, alpha)

        BlockType.STONE_SLAB -> {
            val halfElev = elev * 0.5f
            drawSolidCube(
                center, hw, hh, halfElev,
                topColor = StoneGrayLight.copy(alpha = alpha),
                leftColor = StoneGray.copy(alpha = alpha),
                rightColor = StoneGrayDark.copy(alpha = alpha)
            )
        }

        BlockType.SLIME_BLOCK -> drawSolidCube(
            center, hw, hh, elev,
            topColor = Color(0x9968D391).copy(alpha = alpha),
            leftColor = Color(0x9948BB78).copy(alpha = alpha),
            rightColor = Color(0x9938A169).copy(alpha = alpha)
        )

        BlockType.HONEY_BLOCK -> drawSolidCube(
            center, hw, hh, elev,
            topColor = Color(0x99F6AD55).copy(alpha = alpha),
            leftColor = Color(0x99ED8936).copy(alpha = alpha),
            rightColor = Color(0x99DD6B20).copy(alpha = alpha)
        )

        BlockType.OBSIDIAN -> drawSolidCube(
            center, hw, hh, elev,
            topColor = Color(0xFF231734).copy(alpha = alpha),
            leftColor = Color(0xFF191024).copy(alpha = alpha),
            rightColor = Color(0xFF100A17).copy(alpha = alpha),
            glowColor = Color(0x66B794F4)
        )

        BlockType.REDSTONE_BLOCK -> drawSolidCube(
            center, hw, hh, elev,
            topColor = RedstoneFull.copy(alpha = alpha),
            leftColor = RedstoneMid.copy(alpha = alpha),
            rightColor = RedstoneDim.copy(alpha = alpha),
            glowColor = NeonCyan.copy(alpha = 0.4f * alpha)
        )

        BlockType.TARGET_BLOCK -> drawSolidCube(
            center, hw, hh, elev,
            topColor = Color(0xFFE2E8F0).copy(alpha = alpha),
            leftColor = Color(0xFFC53030).copy(alpha = alpha),
            rightColor = Color(0xFF9B2C2C).copy(alpha = alpha)
        )

        // --- Transmissão e Lógica ---
        BlockType.REDSTONE_WIRE -> drawRedstoneWire(
            block, center, hw, hh, alpha, showSignalNumbers, grid
        )

        BlockType.REDSTONE_REPEATER -> drawRepeater(
            block, center, hw, hh, elev, alpha
        )

        BlockType.REDSTONE_COMPARATOR -> drawComparator(
            block, center, hw, hh, elev, alpha
        )

        // --- Fontes de Energia ---
        BlockType.REDSTONE_TORCH -> drawRedstoneTorch(
            block, center, hw, hh, elev, alpha
        )

        BlockType.LEVER -> drawLever(
            block, center, hw, hh, elev, alpha
        )

        BlockType.STONE_BUTTON -> drawButton(
            block, center, hw, hh, elev, alpha
        )

        BlockType.STONE_PRESSURE_PLATE -> drawPressurePlate(
            block, center, hw, hh, elev, alpha
        )

        BlockType.DAYLIGHT_DETECTOR -> drawDaylightDetector(
            block, center, hw, hh, elev, alpha
        )

        BlockType.SCULK_SENSOR -> drawSculkSensor(
            block, center, hw, hh, elev, alpha
        )

        // --- Mecanismos e Saídas ---
        BlockType.REDSTONE_LAMP -> drawLamp(
            block, center, hw, hh, elev, alpha
        )

        BlockType.COPPER_BULB -> drawCopperBulb(
            block, center, hw, hh, elev, alpha
        )

        BlockType.PISTON, BlockType.STICKY_PISTON -> drawPiston(
            block, center, hw, hh, elev, alpha
        )

        BlockType.OBSERVER -> drawObserver(
            block, center, hw, hh, elev, alpha
        )

        BlockType.NOTE_BLOCK -> drawNoteBlock(
            block, center, hw, hh, elev, alpha
        )

        BlockType.DISPENSER, BlockType.DROPPER -> drawDispenserDropper(
            block, center, hw, hh, elev, alpha
        )

        BlockType.HOPPER -> drawHopper(
            block, center, hw, hh, elev, alpha
        )

        BlockType.IRON_DOOR, BlockType.IRON_TRAPDOOR -> drawDoorTrapdoor(
            block, center, hw, hh, elev, alpha
        )

        BlockType.BELL -> drawBell(
            block, center, hw, hh, elev, alpha
        )

        BlockType.TNT -> drawTnt(
            block, center, hw, hh, elev, alpha
        )

        BlockType.AIR -> {}
    }
}

private fun DrawScope.drawSolidCube(
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    topColor: Color,
    leftColor: Color,
    rightColor: Color,
    glowColor: Color? = null
) {
    val topPath = Path().apply {
        moveTo(center.x, center.y - hh - elev)
        lineTo(center.x + hw, center.y - elev)
        lineTo(center.x, center.y + hh - elev)
        lineTo(center.x - hw, center.y - elev)
        close()
    }
    drawPath(path = topPath, color = topColor, style = Fill)

    val leftPath = Path().apply {
        moveTo(center.x - hw, center.y - elev)
        lineTo(center.x, center.y + hh - elev)
        lineTo(center.x, center.y + hh)
        lineTo(center.x - hw, center.y)
        close()
    }
    drawPath(path = leftPath, color = leftColor, style = Fill)

    val rightPath = Path().apply {
        moveTo(center.x, center.y + hh - elev)
        lineTo(center.x + hw, center.y - elev)
        lineTo(center.x + hw, center.y)
        lineTo(center.x, center.y + hh)
        close()
    }
    drawPath(path = rightPath, color = rightColor, style = Fill)

    drawPath(path = topPath, color = GlassBorderSubtle, style = Stroke(width = 1.0f))
    drawPath(path = leftPath, color = GlassBorderSubtle, style = Stroke(width = 1.0f))
    drawPath(path = rightPath, color = GlassBorderSubtle, style = Stroke(width = 1.0f))

    if (glowColor != null) {
        drawCircle(
            color = glowColor,
            radius = hw * 0.75f,
            center = Offset(center.x, center.y - elev * 0.5f),
            style = Stroke(width = 1.5f)
        )
    }
}

private fun DrawScope.drawGhostCube(
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val ghostColor = Color(0x3300F0FF).copy(alpha = alpha * 0.25f)
    val ghostBorder = Color(0x6600F0FF).copy(alpha = alpha * 0.45f)

    val topPath = Path().apply {
        moveTo(center.x, center.y - hh - elev)
        lineTo(center.x + hw, center.y - elev)
        lineTo(center.x, center.y + hh - elev)
        lineTo(center.x - hw, center.y - elev)
        close()
    }
    drawPath(path = topPath, color = ghostColor, style = Fill)
    drawPath(path = topPath, color = ghostBorder, style = Stroke(width = 1.0f))
}

private fun DrawScope.drawGlassCube(
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    drawSolidCube(
        center, hw, hh, elev,
        topColor = GlassTransparent.copy(alpha = 0.35f * alpha),
        leftColor = Color(0x3300F0FF).copy(alpha = 0.20f * alpha),
        rightColor = Color(0x330077FE).copy(alpha = 0.20f * alpha)
    )
}

private fun DrawScope.drawRedstoneWire(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    alpha: Float,
    showSignalNumbers: Boolean,
    grid: CircuitGrid
) {
    val signal = block.power
    val wireColor = if (signal > 0) {
        val fraction = signal / 15f
        Color(
            red = 0.5f + 0.5f * fraction,
            green = 0.08f * fraction,
            blue = 0.12f * fraction,
            alpha = alpha
        )
    } else {
        RedstoneOff.copy(alpha = alpha)
    }

    val centerTop = Offset(center.x, center.y)
    val wireWidth = 4.5f

    val c = block.coord
    val northConnect = shouldConnectWire(grid.get(c.offset(Facing.NORTH)))
    val southConnect = shouldConnectWire(grid.get(c.offset(Facing.SOUTH)))
    val eastConnect = shouldConnectWire(grid.get(c.offset(Facing.EAST)))
    val westConnect = shouldConnectWire(grid.get(c.offset(Facing.WEST)))
    val noConnections = !northConnect && !southConnect && !eastConnect && !westConnect

    drawCircle(
        color = wireColor,
        radius = if (noConnections) 5.5f else 3.5f,
        center = centerTop
    )

    if (signal > 0) {
        drawCircle(
            color = RedstoneGlow.copy(alpha = 0.35f * (signal / 15f) * alpha),
            radius = if (noConnections) 11f else 8f,
            center = centerTop
        )
    }

    if (northConnect || noConnections) {
        val p = Offset(center.x + hw * 0.45f, center.y - hh * 0.45f)
        drawLine(color = wireColor, start = centerTop, end = p, strokeWidth = wireWidth)
    }
    if (southConnect || noConnections) {
        val p = Offset(center.x - hw * 0.45f, center.y + hh * 0.45f)
        drawLine(color = wireColor, start = centerTop, end = p, strokeWidth = wireWidth)
    }
    if (eastConnect || noConnections) {
        val p = Offset(center.x + hw * 0.45f, center.y + hh * 0.45f)
        drawLine(color = wireColor, start = centerTop, end = p, strokeWidth = wireWidth)
    }
    if (westConnect || noConnections) {
        val p = Offset(center.x - hw * 0.45f, center.y - hh * 0.45f)
        drawLine(color = wireColor, start = centerTop, end = p, strokeWidth = wireWidth)
    }

    // Exibição do número da força de sinal (0 a 15) sobre o fio
    if (showSignalNumbers) {
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = if (signal > 0) android.graphics.Color.WHITE else android.graphics.Color.LTGRAY
                textSize = 22f
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawText("$signal", centerTop.x, centerTop.y - 6f, paint)
        }
    }
}

private fun shouldConnectWire(neighbor: BlockState?): Boolean {
    if (neighbor == null) return false
    return when (neighbor.type) {
        BlockType.REDSTONE_WIRE,
        BlockType.REDSTONE_TORCH,
        BlockType.REDSTONE_REPEATER,
        BlockType.REDSTONE_COMPARATOR,
        BlockType.LEVER,
        BlockType.STONE_BUTTON,
        BlockType.STONE_PRESSURE_PLATE,
        BlockType.DAYLIGHT_DETECTOR,
        BlockType.SCULK_SENSOR,
        BlockType.REDSTONE_BLOCK,
        BlockType.TARGET_BLOCK -> true
        else -> false
    }
}

private fun DrawScope.drawRepeater(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val slabElev = elev * 0.35f
    drawSolidCube(
        center, hw, hh, slabElev,
        topColor = StoneGrayLight.copy(alpha = alpha),
        leftColor = StoneGray.copy(alpha = alpha),
        rightColor = StoneGrayDark.copy(alpha = alpha)
    )

    val topCenter = Offset(center.x, center.y - slabElev)
    val isLit = block.power > 0
    val torchColor = if (isLit) RedstoneFull.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)

    drawCircle(color = torchColor, radius = 3.5f, center = Offset(topCenter.x - 4f, topCenter.y - 2f))
    val delayOffset = (block.delay - 1) * 3f
    drawCircle(color = torchColor, radius = 3.5f, center = Offset(topCenter.x + 4f + delayOffset, topCenter.y + 2f))

    if (block.isLocked) {
        drawLine(
            color = Color(0xFF222831).copy(alpha = alpha),
            start = Offset(topCenter.x - hw * 0.3f, topCenter.y),
            end = Offset(topCenter.x + hw * 0.3f, topCenter.y),
            strokeWidth = 3.5f
        )
    }
}

private fun DrawScope.drawComparator(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val slabElev = elev * 0.35f
    drawSolidCube(
        center, hw, hh, slabElev,
        topColor = StoneGrayLight.copy(alpha = alpha),
        leftColor = StoneGray.copy(alpha = alpha),
        rightColor = StoneGrayDark.copy(alpha = alpha)
    )

    val topCenter = Offset(center.x, center.y - slabElev)
    val isLit = block.power > 0
    val sideColor = if (isLit) RedstoneFull.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)

    drawCircle(color = sideColor, radius = 3.0f, center = Offset(topCenter.x - 6f, topCenter.y))
    drawCircle(color = sideColor, radius = 3.0f, center = Offset(topCenter.x + 6f, topCenter.y))

    val frontTorchColor = if (block.isSubtractMode) RedstoneFull.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)
    drawCircle(color = frontTorchColor, radius = 3.5f, center = Offset(topCenter.x, topCenter.y - 5f))
}

private fun DrawScope.drawRedstoneTorch(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val stalkBase = Offset(center.x, center.y)
    val torchTop = Offset(center.x, center.y - elev * 0.85f)

    drawLine(color = WoodBrown.copy(alpha = alpha), start = stalkBase, end = torchTop, strokeWidth = 3.5f)
    val headColor = if (block.isActive) RedstoneFull.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)
    drawCircle(color = headColor, radius = 5.0f, center = torchTop)
    if (block.isActive) {
        drawCircle(color = RedstoneGlow.copy(alpha = 0.5f * alpha), radius = 11.0f, center = torchTop)
    }
}

private fun DrawScope.drawLever(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val mountPath = Path().apply {
        moveTo(center.x, center.y - hh * 0.4f)
        lineTo(center.x + hw * 0.4f, center.y)
        lineTo(center.x, center.y + hh * 0.4f)
        lineTo(center.x - hw * 0.4f, center.y)
        close()
    }
    drawPath(path = mountPath, color = StoneGray.copy(alpha = alpha), style = Fill)

    val angleX = if (block.isActive) 8f else -8f
    drawLine(
        color = WoodBrown.copy(alpha = alpha),
        start = Offset(center.x, center.y),
        end = Offset(center.x + angleX, center.y - elev * 0.75f),
        strokeWidth = 3.5f
    )
    drawCircle(
        color = if (block.isActive) RedstoneFull.copy(alpha = alpha) else Color(0xFF424242).copy(alpha = alpha),
        radius = 3.5f,
        center = Offset(center.x + angleX, center.y - elev * 0.75f)
    )
}

private fun DrawScope.drawButton(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val buttonSize = if (block.pulseTicksLeft > 0) 0.25f else 0.35f
    val buttonPath = Path().apply {
        moveTo(center.x, center.y - hh * buttonSize)
        lineTo(center.x + hw * buttonSize, center.y)
        lineTo(center.x, center.y + hh * buttonSize)
        lineTo(center.x - hw * buttonSize, center.y)
        close()
    }
    val buttonColor = if (block.pulseTicksLeft > 0) NeonCyan.copy(alpha = alpha) else StoneGrayLight.copy(alpha = alpha)
    drawPath(path = buttonPath, color = buttonColor, style = Fill)
    drawPath(path = buttonPath, color = GlassBorderSubtle, style = Stroke(width = 1.0f))
}

private fun DrawScope.drawPressurePlate(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val plateSize = 0.75f
    val plateElev = elev * 0.15f
    drawSolidCube(
        center, hw * plateSize, hh * plateSize, plateElev,
        topColor = if (block.isActive) NeonCyan.copy(alpha = alpha) else StoneGrayLight.copy(alpha = alpha),
        leftColor = StoneGray.copy(alpha = alpha),
        rightColor = StoneGrayDark.copy(alpha = alpha)
    )
}

private fun DrawScope.drawDaylightDetector(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val slabElev = elev * 0.35f
    drawSolidCube(
        center, hw, hh, slabElev,
        topColor = Color(0xFFC4A482).copy(alpha = alpha),
        leftColor = Color(0xFF8B6B4F).copy(alpha = alpha),
        rightColor = Color(0xFF654321).copy(alpha = alpha),
        glowColor = if (block.isActive) Color(0xFFFFB300) else null
    )
}

private fun DrawScope.drawSculkSensor(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    drawSolidCube(
        center, hw, hh, elev,
        topColor = Color(0xFF032630).copy(alpha = alpha),
        leftColor = Color(0xFF021B24).copy(alpha = alpha),
        rightColor = Color(0xFF011218).copy(alpha = alpha),
        glowColor = if (block.isActive) NeonCyan else null
    )
}

private fun DrawScope.drawLamp(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val isLit = block.isActive || block.power > 0
    drawSolidCube(
        center, hw, hh, elev,
        topColor = if (isLit) LampOn.copy(alpha = alpha) else LampOff.copy(alpha = alpha),
        leftColor = if (isLit) Color(0xFFE6A100).copy(alpha = alpha) else Color(0xFF2A1C12).copy(alpha = alpha),
        rightColor = if (isLit) Color(0xFFB37D00).copy(alpha = alpha) else Color(0xFF1D130C).copy(alpha = alpha),
        glowColor = if (isLit) LampOn.copy(alpha = 0.45f * alpha) else null
    )
}

private fun DrawScope.drawCopperBulb(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val isLit = block.isActive
    drawSolidCube(
        center, hw, hh, elev,
        topColor = if (isLit) Color(0xFFFFE082).copy(alpha = alpha) else Color(0xFFB87333).copy(alpha = alpha),
        leftColor = if (isLit) Color(0xFFFFD54F).copy(alpha = alpha) else Color(0xFF9E5B23).copy(alpha = alpha),
        rightColor = if (isLit) Color(0xFFFFCA28).copy(alpha = alpha) else Color(0xFF7A4315).copy(alpha = alpha),
        glowColor = if (isLit) Color(0xFFFFE082) else null
    )
}

private fun DrawScope.drawPiston(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val isSticky = block.type == BlockType.STICKY_PISTON
    val headElev = if (block.isExtended) elev * 1.35f else elev

    drawSolidCube(
        center, hw, hh, headElev,
        topColor = if (isSticky) StickySlime.copy(alpha = alpha) else PistonHead.copy(alpha = alpha),
        leftColor = StoneGray.copy(alpha = alpha),
        rightColor = StoneGrayDark.copy(alpha = alpha)
    )

    if (block.isExtended) {
        drawLine(
            color = WoodBrown.copy(alpha = alpha),
            start = Offset(center.x, center.y),
            end = Offset(center.x, center.y - headElev * 0.6f),
            strokeWidth = 5.0f
        )
    }
}

private fun DrawScope.drawObserver(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    drawSolidCube(
        center, hw, hh, elev,
        topColor = StoneGrayDark.copy(alpha = alpha),
        leftColor = Color(0xFF323B47).copy(alpha = alpha),
        rightColor = Color(0xFF222933).copy(alpha = alpha)
    )

    val isPulsing = block.pulseTicksLeft > 0
    val eyeColor = if (isPulsing) RedstoneFull.copy(alpha = alpha) else ObserverEye.copy(alpha = alpha)
    drawCircle(
        color = eyeColor,
        radius = 4.0f,
        center = Offset(center.x, center.y - elev * 0.6f)
    )
}

private fun DrawScope.drawNoteBlock(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    drawSolidCube(
        center, hw, hh, elev,
        topColor = Color(0xFF8B5A2B).copy(alpha = alpha),
        leftColor = Color(0xFF6E441F).copy(alpha = alpha),
        rightColor = Color(0xFF533315).copy(alpha = alpha),
        glowColor = if (block.isActive) NeonCyan else null
    )
}

private fun DrawScope.drawDispenserDropper(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    drawSolidCube(
        center, hw, hh, elev,
        topColor = StoneGrayLight.copy(alpha = alpha),
        leftColor = StoneGray.copy(alpha = alpha),
        rightColor = StoneGrayDark.copy(alpha = alpha)
    )
}

private fun DrawScope.drawHopper(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    drawSolidCube(
        center, hw * 0.8f, hh * 0.8f, elev * 0.7f,
        topColor = Color(0xFF4A4A4A).copy(alpha = alpha),
        leftColor = Color(0xFF3B3B3B).copy(alpha = alpha),
        rightColor = Color(0xFF2C2C2C).copy(alpha = alpha)
    )
}

private fun DrawScope.drawDoorTrapdoor(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val plateElev = if (block.isActive) elev * 0.2f else elev * 0.8f
    drawSolidCube(
        center, hw * 0.85f, hh * 0.85f, plateElev,
        topColor = Color(0xFFD9D9D9).copy(alpha = alpha),
        leftColor = Color(0xFFBFBFBF).copy(alpha = alpha),
        rightColor = Color(0xFFA6A6A6).copy(alpha = alpha)
    )
}

private fun DrawScope.drawBell(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    drawSolidCube(
        center, hw * 0.6f, hh * 0.6f, elev * 0.6f,
        topColor = Color(0xFFFFD700).copy(alpha = alpha),
        leftColor = Color(0xFFDAA520).copy(alpha = alpha),
        rightColor = Color(0xFFB8860B).copy(alpha = alpha),
        glowColor = if (block.isActive) Color(0xFFFFD700) else null
    )
}

private fun DrawScope.drawTnt(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    drawSolidCube(
        center, hw, hh, elev,
        topColor = Color(0xFFDB2828).copy(alpha = alpha),
        leftColor = Color(0xFFB91C1C).copy(alpha = alpha),
        rightColor = Color(0xFF991B1B).copy(alpha = alpha),
        glowColor = if (block.isActive) Color.White else null
    )
}
