package com.redstonelab.app.renderer

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.redstonelab.app.model.BlockCoord
import com.redstonelab.app.model.BlockState
import com.redstonelab.app.model.BlockType
import com.redstonelab.app.model.CircuitGrid
import com.redstonelab.app.model.Facing
import com.redstonelab.app.theme.*

private val srcPoly = floatArrayOf(
    0f, 0f,
    16f, 0f,
    16f, 16f,
    0f, 16f
)
private val dstPoly = FloatArray(8)
private val tempMatrix = Matrix()

private val baseTopPaint = Paint().apply {
    isFilterBitmap = false
    isAntiAlias = false
}
private val baseLeftPaint = Paint().apply {
    isFilterBitmap = false
    isAntiAlias = false
    colorFilter = PorterDuffColorFilter(0x38000000, PorterDuff.Mode.SRC_ATOP)
}
private val baseRightPaint = Paint().apply {
    isFilterBitmap = false
    isAntiAlias = false
    colorFilter = PorterDuffColorFilter(0x6B000000, PorterDuff.Mode.SRC_ATOP)
}

@Composable
fun IsometricCanvas(
    grid: CircuitGrid,
    activeLayerY: Int,
    cameraAngle: Float,
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
    transform.cameraAngle = cameraAngle

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
            .pointerInput(activeLayerY, cameraAngle, panOffset, zoom) {
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

        // 1. Onion Skin (Camadas Inferiores Y - 1, etc.)
        if (isOnionSkinEnabled && activeLayerY > 0) {
            val lowerBlocks = grid.getAll().values.filter { it.y < activeLayerY }
            val sortedLower = lowerBlocks.sortedWith(
                compareBy<BlockState> { it.y }
                    .thenBy { transform.computeDepth(it.x.toFloat(), it.z.toFloat()) }
            )
            for (block in sortedLower) {
                val center = transform.gridToScreen(
                    block.x.toFloat(),
                    block.y.toFloat(),
                    block.z.toFloat(),
                    panOffset,
                    zoom
                )
                drawIsometricBlock(
                    transform = transform,
                    block = block,
                    center = center,
                    hw = hw,
                    hh = hh,
                    elev = elev,
                    alpha = 0.35f,
                    isGhost = true,
                    showSignalNumbers = false,
                    grid = grid,
                    panOffset = panOffset,
                    zoom = zoom
                )
            }
        }

        // 2. Tabuleiro / Plataforma Grossa da Camada Ativa
        drawFloorPlatform(
            transform = transform,
            layerY = activeLayerY,
            panOffset = panOffset,
            zoom = zoom,
            hw = hw,
            hh = hh,
            elev = elev
        )

        // 3. Blocos 3D da Camada Ativa (ou todas as camadas se Onion Skin desativado)
        val allBlocks = grid.getAll().values.toList()
        val blocksToDraw = if (isOnionSkinEnabled) {
            allBlocks.filter { it.y == activeLayerY }
        } else {
            allBlocks
        }

        val sortedBlocks = blocksToDraw.sortedWith(
            compareBy<BlockState> { it.y }
                .thenBy { transform.computeDepth(it.x.toFloat(), it.z.toFloat()) }
        )

        for (block in sortedBlocks) {
            val center = transform.gridToScreen(
                block.x.toFloat(),
                block.y.toFloat(),
                block.z.toFloat(),
                panOffset,
                zoom
            )

            drawIsometricBlock(
                transform = transform,
                block = block,
                center = center,
                hw = hw,
                hh = hh,
                elev = elev,
                alpha = 1.0f,
                isGhost = false,
                showSignalNumbers = showSignalNumbers,
                grid = grid,
                panOffset = panOffset,
                zoom = zoom
            )
        }

        // 4. Cursor de Seleção da Camada Ativa
        if (selectedBlockCoord != null && selectedBlockCoord.y == activeLayerY) {
            drawCursor(
                transform = transform,
                coord = selectedBlockCoord,
                panOffset = panOffset,
                zoom = zoom
            )
        }
    }
}

/**
 * Desenha a plataforma física da camada ativa com ladrilhos de pedra e borda 3D visível.
 */
private fun DrawScope.drawFloorPlatform(
    transform: IsometricTransform,
    layerY: Int,
    panOffset: Offset,
    zoom: Float,
    hw: Float,
    hh: Float,
    elev: Float
) {
    val floorBmp = VoxelTextureAtlas.getTexture("FLOOR_TILE")

    // Desenha cada um dos 16x16 ladrilhos
    for (x in 0 until 16) {
        for (z in 0 until 16) {
            val center = transform.gridToScreen(
                x.toFloat(),
                layerY.toFloat(),
                z.toFloat(),
                panOffset,
                zoom
            )

            if (floorBmp != null) {
                drawIntoCanvas { canvas ->
                    dstPoly[0] = center.x
                    dstPoly[1] = center.y - hh
                    dstPoly[2] = center.x + hw
                    dstPoly[3] = center.y
                    dstPoly[4] = center.x
                    dstPoly[5] = center.y + hh
                    dstPoly[6] = center.x - hw
                    dstPoly[7] = center.y
                    tempMatrix.setPolyToPoly(srcPoly, 0, dstPoly, 0, 4)
                    canvas.nativeCanvas.drawBitmap(floorBmp, tempMatrix, baseTopPaint)
                }
            } else {
                val tilePath = Path().apply {
                    moveTo(center.x, center.y - hh)
                    lineTo(center.x + hw, center.y)
                    lineTo(center.x, center.y + hh)
                    lineTo(center.x - hw, center.y)
                    close()
                }
                drawPath(tilePath, color = Color(0xFF23262D), style = Fill)
                drawPath(tilePath, color = Color(0xFF1B1D22), style = Stroke(width = 1.0f))
            }
        }
    }

    // Borda 3D extrudada da plataforma nos limites visíveis
    val rimThickness = elev * 0.45f
    val stoneBmp = VoxelTextureAtlas.getTexture("SMOOTH_STONE")

    for (x in 0 until 16) {
        val center = transform.gridToScreen(x.toFloat(), layerY.toFloat(), 15f, panOffset, zoom)
        drawPlatformSide(center, hw, hh, rimThickness, isLeft = false, stoneBmp)
    }
    for (z in 0 until 16) {
        val center = transform.gridToScreen(15f, layerY.toFloat(), z.toFloat(), panOffset, zoom)
        drawPlatformSide(center, hw, hh, rimThickness, isLeft = true, stoneBmp)
    }
}

private fun DrawScope.drawPlatformSide(
    center: Offset,
    hw: Float,
    hh: Float,
    thickness: Float,
    isLeft: Boolean,
    stoneBmp: Bitmap?
) {
    val path = Path().apply {
        if (isLeft) {
            moveTo(center.x - hw, center.y)
            lineTo(center.x, center.y + hh)
            lineTo(center.x, center.y + hh + thickness)
            lineTo(center.x - hw, center.y + thickness)
        } else {
            moveTo(center.x, center.y + hh)
            lineTo(center.x + hw, center.y)
            lineTo(center.x + hw, center.y + thickness)
            lineTo(center.x, center.y + hh + thickness)
        }
        close()
    }
    val sideColor = if (isLeft) Color(0xFF1D1F25) else Color(0xFF16181C)
    drawPath(path, color = sideColor, style = Fill)
    drawPath(path, color = Color(0xFF0F1013), style = Stroke(width = 1.0f))
}

private fun DrawScope.drawCursor(
    transform: IsometricTransform,
    coord: BlockCoord,
    panOffset: Offset,
    zoom: Float
) {
    val x = coord.x.toFloat()
    val y = coord.y.toFloat()
    val z = coord.z.toFloat()

    val p1 = transform.gridToScreen(x - 0.48f, y, z - 0.48f, panOffset, zoom)
    val p2 = transform.gridToScreen(x + 0.48f, y, z - 0.48f, panOffset, zoom)
    val p3 = transform.gridToScreen(x + 0.48f, y, z + 0.48f, panOffset, zoom)
    val p4 = transform.gridToScreen(x - 0.48f, y, z + 0.48f, panOffset, zoom)

    val path = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        close()
    }
    drawPath(path = path, color = RedstoneAccent.copy(alpha = 0.25f), style = Fill)
    drawPath(path = path, color = BorderSelected, style = Stroke(width = 2.0f))
}

/**
 * Renderiza um bloco 3D com texturização afim via setPolyToPoly nas 3 faces isométricas.
 */
private fun DrawScope.drawIsometricBlock(
    transform: IsometricTransform,
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float,
    isGhost: Boolean,
    showSignalNumbers: Boolean,
    grid: CircuitGrid,
    panOffset: Offset,
    zoom: Float
) {
    if (isGhost) {
        drawGhostBlock(center, hw, hh, elev, alpha)
        return
    }

    when (block.type) {
        BlockType.REDSTONE_WIRE -> {
            drawRedstoneWire(
                transform = transform,
                block = block,
                center = center,
                hw = hw,
                hh = hh,
                alpha = alpha,
                showSignalNumbers = showSignalNumbers,
                grid = grid,
                panOffset = panOffset,
                zoom = zoom
            )
        }

        BlockType.REDSTONE_TORCH -> {
            drawRedstoneTorch(block, center, elev, alpha)
        }

        BlockType.REDSTONE_REPEATER -> {
            drawRepeater(
                transform = transform,
                block = block,
                center = center,
                hw = hw,
                hh = hh,
                elev = elev,
                alpha = alpha,
                panOffset = panOffset,
                zoom = zoom
            )
        }

        BlockType.REDSTONE_COMPARATOR -> {
            drawComparator(
                transform = transform,
                block = block,
                center = center,
                hw = hw,
                hh = hh,
                elev = elev,
                alpha = alpha,
                panOffset = panOffset,
                zoom = zoom
            )
        }

        BlockType.LEVER -> {
            drawLever(block, center, hw, hh, elev, alpha)
        }

        BlockType.STONE_BUTTON -> {
            drawButton(block, center, hw, hh, alpha)
        }

        BlockType.STONE_PRESSURE_PLATE -> {
            drawPressurePlate(center, hw, hh, elev, alpha, block.isActive)
        }

        BlockType.STONE_SLAB -> {
            val slabElev = elev * 0.5f
            val topKey = VoxelTextureAtlas.getBlockTopKey(block.type, block.isActive, block.power)
            val sideKey = VoxelTextureAtlas.getBlockSideKey(block.type, block.isActive, block.power)
            drawTexturedVoxelCube(center, hw, hh, slabElev, alpha, topKey, sideKey)
        }

        BlockType.AIR -> {}

        else -> {
            // Cubos sólidos texturizados (Ferro, Pedra, Lã, Vidro, Obsidiana, Redstone, Slime, Mel, TNT, Alvo, Lâmpada, Pistão, Observador, etc.)
            val topKey = VoxelTextureAtlas.getBlockTopKey(block.type, block.isActive, block.power)
            val sideKey = VoxelTextureAtlas.getBlockSideKey(block.type, block.isActive, block.power)
            val cubeElev = if (block.type == BlockType.PISTON && block.isExtended) elev * 1.35f else elev

            drawTexturedVoxelCube(
                center = center,
                hw = hw,
                hh = hh,
                elev = cubeElev,
                alpha = alpha,
                topKey = topKey,
                sideKey = sideKey
            )

            // Extensão do pistão
            if ((block.type == BlockType.PISTON || block.type == BlockType.STICKY_PISTON) && block.isExtended) {
                drawLine(
                    color = WoodBrown.copy(alpha = alpha),
                    start = Offset(center.x, center.y),
                    end = Offset(center.x, center.y - cubeElev * 0.5f),
                    strokeWidth = 6.0f
                )
            }
        }
    }
}

private fun DrawScope.drawTexturedVoxelCube(
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float,
    topKey: String,
    sideKey: String
) {
    val topBmp = VoxelTextureAtlas.getTexture(topKey)
    val sideBmp = VoxelTextureAtlas.getTexture(sideKey)

    if (topBmp != null && sideBmp != null) {
        val paintTop = Paint(baseTopPaint).apply { this.alpha = (alpha * 255).toInt() }
        val paintLeft = Paint(baseLeftPaint).apply { this.alpha = (alpha * 255).toInt() }
        val paintRight = Paint(baseRightPaint).apply { this.alpha = (alpha * 255).toInt() }

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas

            // Top Face (100% Brightness)
            dstPoly[0] = center.x
            dstPoly[1] = center.y - hh - elev
            dstPoly[2] = center.x + hw
            dstPoly[3] = center.y - elev
            dstPoly[4] = center.x
            dstPoly[5] = center.y + hh - elev
            dstPoly[6] = center.x - hw
            dstPoly[7] = center.y - elev
            tempMatrix.setPolyToPoly(srcPoly, 0, dstPoly, 0, 4)
            native.drawBitmap(topBmp, tempMatrix, paintTop)

            // Left Face (78% Brightness)
            dstPoly[0] = center.x - hw
            dstPoly[1] = center.y - elev
            dstPoly[2] = center.x
            dstPoly[3] = center.y + hh - elev
            dstPoly[4] = center.x
            dstPoly[5] = center.y + hh
            dstPoly[6] = center.x - hw
            dstPoly[7] = center.y
            tempMatrix.setPolyToPoly(srcPoly, 0, dstPoly, 0, 4)
            native.drawBitmap(sideBmp, tempMatrix, paintLeft)

            // Right Face (58% Brightness)
            dstPoly[0] = center.x
            dstPoly[1] = center.y + hh - elev
            dstPoly[2] = center.x + hw
            dstPoly[3] = center.y - elev
            dstPoly[4] = center.x + hw
            dstPoly[5] = center.y
            dstPoly[6] = center.x
            dstPoly[7] = center.y + hh
            tempMatrix.setPolyToPoly(srcPoly, 0, dstPoly, 0, 4)
            native.drawBitmap(sideBmp, tempMatrix, paintRight)
        }
    } else {
        // Fallback geométrico com sombreamento
        drawFallbackCube(center, hw, hh, elev, alpha)
    }
}

private fun DrawScope.drawFallbackCube(
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val topPath = Path().apply {
        moveTo(center.x, center.y - hh - elev)
        lineTo(center.x + hw, center.y - elev)
        lineTo(center.x, center.y + hh - elev)
        lineTo(center.x - hw, center.y - elev)
        close()
    }
    val leftPath = Path().apply {
        moveTo(center.x - hw, center.y - elev)
        lineTo(center.x, center.y + hh - elev)
        lineTo(center.x, center.y + hh)
        lineTo(center.x - hw, center.y)
        close()
    }
    val rightPath = Path().apply {
        moveTo(center.x, center.y + hh - elev)
        lineTo(center.x + hw, center.y - elev)
        lineTo(center.x + hw, center.y)
        lineTo(center.x, center.y + hh)
        close()
    }

    drawPath(topPath, color = Color(0xFF6B7280).copy(alpha = alpha), style = Fill)
    drawPath(leftPath, color = Color(0xFF4B5563).copy(alpha = alpha), style = Fill)
    drawPath(rightPath, color = Color(0xFF374151).copy(alpha = alpha), style = Fill)
    drawPath(topPath, color = Color(0xFF1E2127), style = Stroke(width = 1.0f))
    drawPath(leftPath, color = Color(0xFF1E2127), style = Stroke(width = 1.0f))
    drawPath(rightPath, color = Color(0xFF1E2127), style = Stroke(width = 1.0f))
}

private fun DrawScope.drawGhostBlock(
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val ghostColor = Color(0x334B5563).copy(alpha = alpha * 0.4f)
    val ghostBorder = Color(0x666B7280).copy(alpha = alpha * 0.6f)

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

private fun DrawScope.drawRedstoneWire(
    transform: IsometricTransform,
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    alpha: Float,
    showSignalNumbers: Boolean,
    grid: CircuitGrid,
    panOffset: Offset,
    zoom: Float
) {
    val signal = block.power
    val wireColor = if (signal > 0) {
        val fraction = signal / 15f
        Color(
            red = 0.55f + 0.45f * fraction,
            green = 0.08f * fraction,
            blue = 0.10f * fraction,
            alpha = alpha
        )
    } else {
        RedstoneOff.copy(alpha = alpha)
    }

    val centerTop = Offset(center.x, center.y)
    val wireWidth = 5.0f

    val c = block.coord
    val northConnect = shouldConnectWire(grid.get(c.offset(Facing.NORTH)))
    val southConnect = shouldConnectWire(grid.get(c.offset(Facing.SOUTH)))
    val eastConnect = shouldConnectWire(grid.get(c.offset(Facing.EAST)))
    val westConnect = shouldConnectWire(grid.get(c.offset(Facing.WEST)))
    val noConnections = !northConnect && !southConnect && !eastConnect && !westConnect

    drawCircle(
        color = wireColor,
        radius = if (noConnections) 5.8f else 3.8f,
        center = centerTop
    )

    if (signal > 0) {
        drawCircle(
            color = RedstoneGlow.copy(alpha = 0.4f * (signal / 15f) * alpha),
            radius = if (noConnections) 11f else 8f,
            center = centerTop
        )
    }

    val bx = block.x.toFloat()
    val by = block.y.toFloat()
    val bz = block.z.toFloat()

    if (northConnect || noConnections) {
        val p = transform.gridToScreen(bx, by, bz - 0.46f, panOffset, zoom)
        drawLine(color = wireColor, start = centerTop, end = p, strokeWidth = wireWidth)
    }
    if (southConnect || noConnections) {
        val p = transform.gridToScreen(bx, by, bz + 0.46f, panOffset, zoom)
        drawLine(color = wireColor, start = centerTop, end = p, strokeWidth = wireWidth)
    }
    if (eastConnect || noConnections) {
        val p = transform.gridToScreen(bx + 0.46f, by, bz, panOffset, zoom)
        drawLine(color = wireColor, start = centerTop, end = p, strokeWidth = wireWidth)
    }
    if (westConnect || noConnections) {
        val p = transform.gridToScreen(bx - 0.46f, by, bz, panOffset, zoom)
        drawLine(color = wireColor, start = centerTop, end = p, strokeWidth = wireWidth)
    }

    if (showSignalNumbers) {
        drawContext.canvas.nativeCanvas.apply {
            val paint = Paint().apply {
                color = if (signal > 0) android.graphics.Color.WHITE else android.graphics.Color.LTGRAY
                textSize = 22f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
                setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
            }
            drawText("$signal", centerTop.x, centerTop.y - 7f, paint)
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
    transform: IsometricTransform,
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float,
    panOffset: Offset,
    zoom: Float
) {
    val slabElev = elev * 0.35f
    val topKey = VoxelTextureAtlas.getBlockTopKey(block.type, block.isActive, block.power)
    val sideKey = VoxelTextureAtlas.getBlockSideKey(block.type, block.isActive, block.power)
    drawTexturedVoxelCube(center, hw, hh, slabElev, alpha, topKey, sideKey)

    val isLit = block.power > 0
    val torchColor = if (isLit) RedstoneBright.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)

    val bx = block.x.toFloat()
    val by = block.y.toFloat()
    val bz = block.z.toFloat()

    val frontOffset = 0.24f
    val rearOffset = 0.08f + (block.delay - 1) * 0.08f

    val frontPos = transform.gridToScreen(
        bx + block.facing.dx * frontOffset,
        by + 0.35f,
        bz + block.facing.dz * frontOffset,
        panOffset,
        zoom
    )

    val rearPos = transform.gridToScreen(
        bx - block.facing.dx * rearOffset,
        by + 0.35f,
        bz - block.facing.dz * rearOffset,
        panOffset,
        zoom
    )

    drawCircle(color = torchColor, radius = 3.6f, center = frontPos)
    drawCircle(color = torchColor, radius = 3.6f, center = rearPos)

    if (isLit) {
        drawCircle(color = RedstoneGlow.copy(alpha = 0.45f * alpha), radius = 8.0f, center = frontPos)
        drawCircle(color = RedstoneGlow.copy(alpha = 0.45f * alpha), radius = 8.0f, center = rearPos)
    }

    if (block.isLocked) {
        drawLine(
            color = Color(0xFF1E293B).copy(alpha = alpha),
            start = Offset(center.x - hw * 0.3f, center.y - slabElev),
            end = Offset(center.x + hw * 0.3f, center.y - slabElev),
            strokeWidth = 3.5f
        )
    }
}

private fun DrawScope.drawComparator(
    transform: IsometricTransform,
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float,
    panOffset: Offset,
    zoom: Float
) {
    val slabElev = elev * 0.35f
    val topKey = VoxelTextureAtlas.getBlockTopKey(block.type, block.isActive, block.power)
    val sideKey = VoxelTextureAtlas.getBlockSideKey(block.type, block.isActive, block.power)
    drawTexturedVoxelCube(center, hw, hh, slabElev, alpha, topKey, sideKey)

    val isLit = block.power > 0
    val sideColor = if (isLit) RedstoneBright.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)

    val bx = block.x.toFloat()
    val by = block.y.toFloat()
    val bz = block.z.toFloat()

    val frontPos = transform.gridToScreen(
        bx + block.facing.dx * 0.22f,
        by + 0.35f,
        bz + block.facing.dz * 0.22f,
        panOffset,
        zoom
    )

    val leftFacing = block.facing.counterClockwise()
    val rightFacing = block.facing.clockwise()

    val rearLeftPos = transform.gridToScreen(
        bx - block.facing.dx * 0.20f + leftFacing.dx * 0.22f,
        by + 0.35f,
        bz - block.facing.dz * 0.20f + leftFacing.dz * 0.22f,
        panOffset,
        zoom
    )

    val rearRightPos = transform.gridToScreen(
        bx - block.facing.dx * 0.20f + rightFacing.dx * 0.22f,
        by + 0.35f,
        bz - block.facing.dz * 0.20f + rightFacing.dz * 0.22f,
        panOffset,
        zoom
    )

    drawCircle(color = sideColor, radius = 3.2f, center = rearLeftPos)
    drawCircle(color = sideColor, radius = 3.2f, center = rearRightPos)

    val frontTorchColor = if (block.isSubtractMode) RedstoneBright.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)
    drawCircle(color = frontTorchColor, radius = 3.6f, center = frontPos)
    if (block.isSubtractMode) {
        drawCircle(color = RedstoneGlow.copy(alpha = 0.5f * alpha), radius = 8.0f, center = frontPos)
    }
}

private fun DrawScope.drawRedstoneTorch(
    block: BlockState,
    center: Offset,
    elev: Float,
    alpha: Float
) {
    val stalkBase = Offset(center.x, center.y)
    val torchTop = Offset(center.x, center.y - elev * 0.85f)

    drawLine(color = WoodBrown.copy(alpha = alpha), start = stalkBase, end = torchTop, strokeWidth = 3.8f)
    val headColor = if (block.isActive) RedstoneBright.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)
    drawCircle(color = headColor, radius = 5.2f, center = torchTop)
    if (block.isActive) {
        drawCircle(color = Color(0xFFFF8595).copy(alpha = alpha), radius = 2.5f, center = torchTop)
        drawCircle(color = RedstoneGlow.copy(alpha = 0.55f * alpha), radius = 12.0f, center = torchTop)
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
    drawPath(path = mountPath, color = Color(0xFF4B5563).copy(alpha = alpha), style = Fill)

    val angleX = if (block.isActive) 8f else -8f
    drawLine(
        color = WoodBrown.copy(alpha = alpha),
        start = Offset(center.x, center.y),
        end = Offset(center.x + angleX, center.y - elev * 0.75f),
        strokeWidth = 3.8f
    )
    drawCircle(
        color = if (block.isActive) RedstoneBright.copy(alpha = alpha) else Color(0xFF374151).copy(alpha = alpha),
        radius = 3.8f,
        center = Offset(center.x + angleX, center.y - elev * 0.75f)
    )
}

private fun DrawScope.drawButton(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
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
    val buttonColor = if (block.pulseTicksLeft > 0) RedstoneBright.copy(alpha = alpha) else Color(0xFF6B7280).copy(alpha = alpha)
    drawPath(path = buttonPath, color = buttonColor, style = Fill)
    drawPath(path = buttonPath, color = Color(0xFF374151), style = Stroke(width = 1.0f))
}

private fun DrawScope.drawPressurePlate(
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float,
    isActive: Boolean
) {
    val plateSize = 0.75f
    val plateElev = elev * 0.15f
    val topKey = "SMOOTH_STONE"
    drawTexturedVoxelCube(center, hw * plateSize, hh * plateSize, plateElev, alpha, topKey, topKey)
}

/**
 * Ícone Voxel 3D para exibir na paleta de blocos inferior.
 */
@Composable
fun BlockVoxelPreview(
    type: BlockType,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val hw = this.size.width * 0.38f
        val hh = this.size.height * 0.20f
        val elev = this.size.height * 0.28f
        val center = Offset(this.size.width / 2f, this.size.height * 0.65f)

        when (type) {
            BlockType.REDSTONE_WIRE -> {
                drawCircle(color = RedstoneBright, radius = hw * 0.45f, center = center)
                drawCircle(color = RedstoneGlow.copy(alpha = 0.5f), radius = hw * 0.7f, center = center)
            }
            BlockType.REDSTONE_TORCH -> {
                drawLine(
                    color = WoodBrown,
                    start = Offset(center.x, center.y),
                    end = Offset(center.x, center.y - elev * 0.9f),
                    strokeWidth = 3f
                )
                drawCircle(color = RedstoneBright, radius = 5f, center = Offset(center.x, center.y - elev * 0.9f))
            }
            else -> {
                val topKey = VoxelTextureAtlas.getBlockTopKey(type, isActive = true, power = 15)
                val sideKey = VoxelTextureAtlas.getBlockSideKey(type, isActive = true, power = 15)
                drawTexturedVoxelCube(center, hw, hh, elev, 1.0f, topKey, sideKey)
            }
        }
    }
}
