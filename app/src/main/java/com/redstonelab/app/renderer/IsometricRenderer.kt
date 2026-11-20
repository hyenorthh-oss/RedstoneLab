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

        // 1. Grade Isométrica de Referência para a Camada Ativa (Rotacionada com a Câmera)
        drawIsoGridPlane(
            transform = transform,
            layerY = activeLayerY,
            panOffset = panOffset,
            zoom = zoom
        )

        // 2. Filtro de blocos conforme modo Onion Skin ou 3D Completo
        val allBlocks = grid.getAll().values.toList()
        val blocksToDraw = if (isOnionSkinEnabled) {
            allBlocks.filter { it.y <= activeLayerY }
        } else {
            allBlocks
        }

        // 3. Ordenação por profundidade correta para qualquer ângulo de rotação da câmera
        val sortedBlocks = blocksToDraw.sortedWith(
            compareBy<BlockState> { it.y }
                .thenBy { transform.computeDepth(it.x.toFloat(), it.z.toFloat()) }
        )

        // 4. Renderização Voxel 3D com estilo Pixel-Art dos Blocos
        for (block in sortedBlocks) {
            val center = transform.gridToScreen(
                block.x.toFloat(),
                block.y.toFloat(),
                block.z.toFloat(),
                panOffset,
                zoom
            )

            val isCurrentLayer = block.y == activeLayerY
            val alpha = when {
                isCurrentLayer -> 1.0f
                block.y < activeLayerY -> {
                    if (isOnionSkinEnabled) 0.35f else 0.70f
                }
                else -> 0.40f
            }

            drawIsometricBlock(
                transform = transform,
                block = block,
                center = center,
                hw = hw,
                hh = hh,
                elev = elev,
                alpha = alpha,
                isGhost = !isCurrentLayer && isOnionSkinEnabled,
                showSignalNumbers = showSignalNumbers,
                grid = grid,
                panOffset = panOffset,
                zoom = zoom
            )
        }

        // 5. Cursor Diamante Holográfico na Camada Ativa
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

private fun DrawScope.drawIsoGridPlane(
    transform: IsometricTransform,
    layerY: Int,
    panOffset: Offset,
    zoom: Float
) {
    val size = 16
    for (x in 0..size) {
        val start = transform.gridToScreen(x.toFloat(), layerY.toFloat(), 0f, panOffset, zoom)
        val end = transform.gridToScreen(x.toFloat(), layerY.toFloat(), size.toFloat(), panOffset, zoom)
        drawLine(
            color = if (x == 0 || x == size) GridLineActive else GridLineSubtle,
            start = start,
            end = end,
            strokeWidth = if (x == 0 || x == size) 1.6f else 1.0f
        )
    }

    for (z in 0..size) {
        val start = transform.gridToScreen(0f, layerY.toFloat(), z.toFloat(), panOffset, zoom)
        val end = transform.gridToScreen(size.toFloat(), layerY.toFloat(), z.toFloat(), panOffset, zoom)
        drawLine(
            color = if (z == 0 || z == size) GridLineActive else GridLineSubtle,
            start = start,
            end = end,
            strokeWidth = if (z == 0 || z == size) 1.6f else 1.0f
        )
    }
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
    drawPath(path = path, color = NeonCyan.copy(alpha = 0.22f), style = Fill)
    drawPath(path = path, color = NeonCyan, style = Stroke(width = 2.2f))
}

private fun DrawScope.drawVoxelCube(
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    topColor: Color,
    leftColor: Color,
    rightColor: Color,
    bevelColor: Color? = null,
    outlineColor: Color = Color(0x33000000),
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

    // Pixel Bevel Highlights
    if (bevelColor != null) {
        drawLine(
            color = bevelColor,
            start = Offset(center.x - hw, center.y - elev),
            end = Offset(center.x, center.y - hh - elev),
            strokeWidth = 1.4f
        )
        drawLine(
            color = bevelColor,
            start = Offset(center.x, center.y - hh - elev),
            end = Offset(center.x + hw, center.y - elev),
            strokeWidth = 1.4f
        )
    }

    // Clean Contour Outline
    drawPath(path = topPath, color = outlineColor, style = Stroke(width = 1.0f))
    drawPath(path = leftPath, color = outlineColor, style = Stroke(width = 1.0f))
    drawPath(path = rightPath, color = outlineColor, style = Stroke(width = 1.0f))

    if (glowColor != null) {
        drawCircle(
            color = glowColor,
            radius = hw * 0.8f,
            center = Offset(center.x, center.y - elev * 0.5f),
            style = Stroke(width = 1.5f)
        )
    }
}

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
        drawGhostCube(center, hw, hh, elev, alpha)
        return
    }

    when (block.type) {
        BlockType.SMOOTH_STONE -> {
            drawVoxelCube(
                center, hw, hh, elev,
                topColor = Color(0xFF6B7280).copy(alpha = alpha),
                leftColor = Color(0xFF4B5563).copy(alpha = alpha),
                rightColor = Color(0xFF374151).copy(alpha = alpha),
                bevelColor = Color(0xFF9CA3AF).copy(alpha = alpha * 0.7f)
            )
            // Pixel flecks
            drawCircle(
                color = Color(0xFF374151).copy(alpha = alpha * 0.8f),
                radius = 1.8f,
                center = Offset(center.x - hw * 0.2f, center.y - elev)
            )
            drawCircle(
                color = Color(0xFF9CA3AF).copy(alpha = alpha * 0.8f),
                radius = 1.8f,
                center = Offset(center.x + hw * 0.25f, center.y - elev - hh * 0.2f)
            )
        }

        BlockType.IRON_BLOCK -> {
            drawVoxelCube(
                center, hw, hh, elev,
                topColor = Color(0xFFE2E8F0).copy(alpha = alpha),
                leftColor = Color(0xFFCBD5E1).copy(alpha = alpha),
                rightColor = Color(0xFF94A3B8).copy(alpha = alpha),
                bevelColor = Color(0xFFF8FAFC).copy(alpha = alpha)
            )
            // Iron plate rivets
            val rivetColor = Color(0xFF64748B).copy(alpha = alpha)
            drawCircle(color = rivetColor, radius = 1.8f, center = Offset(center.x - hw * 0.45f, center.y - elev))
            drawCircle(color = rivetColor, radius = 1.8f, center = Offset(center.x + hw * 0.45f, center.y - elev))
            drawCircle(color = rivetColor, radius = 1.8f, center = Offset(center.x, center.y - hh * 0.6f - elev))
            drawCircle(color = rivetColor, radius = 1.8f, center = Offset(center.x, center.y + hh * 0.6f - elev))
        }

        BlockType.RED_WOOL -> drawVoxelCube(
            center, hw, hh, elev,
            topColor = Color(0xFFDC2626).copy(alpha = alpha),
            leftColor = Color(0xFFB91C1C).copy(alpha = alpha),
            rightColor = Color(0xFF991B1B).copy(alpha = alpha),
            bevelColor = Color(0xFFEF4444).copy(alpha = alpha)
        )

        BlockType.BLUE_WOOL -> drawVoxelCube(
            center, hw, hh, elev,
            topColor = Color(0xFF2563EB).copy(alpha = alpha),
            leftColor = Color(0xFF1D4ED8).copy(alpha = alpha),
            rightColor = Color(0xFF1E40AF).copy(alpha = alpha),
            bevelColor = Color(0xFF60A5FA).copy(alpha = alpha)
        )

        BlockType.GREEN_WOOL -> drawVoxelCube(
            center, hw, hh, elev,
            topColor = Color(0xFF16A34A).copy(alpha = alpha),
            leftColor = Color(0xFF15803D).copy(alpha = alpha),
            rightColor = Color(0xFF166534).copy(alpha = alpha),
            bevelColor = Color(0xFF4ADE80).copy(alpha = alpha)
        )

        BlockType.YELLOW_WOOL -> drawVoxelCube(
            center, hw, hh, elev,
            topColor = Color(0xFFEAB308).copy(alpha = alpha),
            leftColor = Color(0xFFCA8A04).copy(alpha = alpha),
            rightColor = Color(0xFFA16207).copy(alpha = alpha),
            bevelColor = Color(0xFFFDE047).copy(alpha = alpha)
        )

        BlockType.GLASS -> {
            drawVoxelCube(
                center, hw, hh, elev,
                topColor = Color(0x5538BDF8).copy(alpha = alpha * 0.35f),
                leftColor = Color(0x550284C7).copy(alpha = alpha * 0.25f),
                rightColor = Color(0x550369A1).copy(alpha = alpha * 0.25f),
                bevelColor = Color(0x88FFFFFF).copy(alpha = alpha * 0.6f)
            )
            // Cyber specular reflection streak
            drawLine(
                color = Color(0x99FFFFFF).copy(alpha = alpha * 0.7f),
                start = Offset(center.x - hw * 0.3f, center.y - elev - hh * 0.2f),
                end = Offset(center.x + hw * 0.1f, center.y - elev + hh * 0.4f),
                strokeWidth = 1.5f
            )
        }

        BlockType.STONE_SLAB -> {
            val halfElev = elev * 0.5f
            drawVoxelCube(
                center, hw, hh, halfElev,
                topColor = Color(0xFF6B7280).copy(alpha = alpha),
                leftColor = Color(0xFF4B5563).copy(alpha = alpha),
                rightColor = Color(0xFF374151).copy(alpha = alpha),
                bevelColor = Color(0xFF9CA3AF).copy(alpha = alpha * 0.7f)
            )
        }

        BlockType.SLIME_BLOCK -> {
            drawVoxelCube(
                center, hw, hh, elev,
                topColor = Color(0xAA4ADE80).copy(alpha = alpha * 0.75f),
                leftColor = Color(0xAA22C55E).copy(alpha = alpha * 0.75f),
                rightColor = Color(0xAA16A34A).copy(alpha = alpha * 0.75f),
                bevelColor = Color(0xCC86EFAC).copy(alpha = alpha * 0.8f)
            )
            // Inner floating slime nucleus
            drawVoxelCube(
                center = Offset(center.x, center.y - elev * 0.2f),
                hw = hw * 0.45f,
                hh = hh * 0.45f,
                elev = elev * 0.45f,
                topColor = Color(0xFF15803D).copy(alpha = alpha * 0.85f),
                leftColor = Color(0xFF166534).copy(alpha = alpha * 0.85f),
                rightColor = Color(0xFF14532D).copy(alpha = alpha * 0.85f)
            )
        }

        BlockType.HONEY_BLOCK -> {
            drawVoxelCube(
                center, hw, hh, elev,
                topColor = Color(0xAAF59E0B).copy(alpha = alpha * 0.75f),
                leftColor = Color(0xAAD97706).copy(alpha = alpha * 0.75f),
                rightColor = Color(0xABB45309).copy(alpha = alpha * 0.75f),
                bevelColor = Color(0xCCFDE68A).copy(alpha = alpha * 0.8f)
            )
        }

        BlockType.OBSIDIAN -> {
            drawVoxelCube(
                center, hw, hh, elev,
                topColor = Color(0xFF1E1035).copy(alpha = alpha),
                leftColor = Color(0xFF150B25).copy(alpha = alpha),
                rightColor = Color(0xFF0C0617).copy(alpha = alpha),
                bevelColor = Color(0x66A855F7).copy(alpha = alpha),
                glowColor = Color(0x559333EA).copy(alpha = alpha)
            )
            // Glowing purple crystal flecks
            val fleckColor = Color(0xFFC084FC).copy(alpha = alpha)
            drawCircle(color = fleckColor, radius = 2.0f, center = Offset(center.x - hw * 0.2f, center.y - elev - hh * 0.2f))
            drawCircle(color = Color(0xFFF472B6).copy(alpha = alpha), radius = 1.8f, center = Offset(center.x + hw * 0.25f, center.y - elev + hh * 0.15f))
        }

        BlockType.REDSTONE_BLOCK -> {
            drawVoxelCube(
                center, hw, hh, elev,
                topColor = RedstoneFull.copy(alpha = alpha),
                leftColor = RedstoneMid.copy(alpha = alpha),
                rightColor = RedstoneDim.copy(alpha = alpha),
                bevelColor = Color(0xFFFF8595).copy(alpha = alpha),
                glowColor = NeonCyan.copy(alpha = 0.45f * alpha)
            )
            // Glowing pixel crystals
            val crystalColor = Color(0xFFFFF1F2).copy(alpha = alpha)
            drawCircle(color = crystalColor, radius = 2.0f, center = Offset(center.x, center.y - elev))
            drawCircle(color = Color(0xFFFFBAC2).copy(alpha = alpha), radius = 1.8f, center = Offset(center.x - hw * 0.3f, center.y - elev + hh * 0.2f))
            drawCircle(color = Color(0xFFFFBAC2).copy(alpha = alpha), radius = 1.8f, center = Offset(center.x + hw * 0.3f, center.y - elev - hh * 0.2f))
        }

        BlockType.TARGET_BLOCK -> {
            drawVoxelCube(
                center, hw, hh, elev,
                topColor = Color(0xFFF8FAFC).copy(alpha = alpha),
                leftColor = Color(0xFFDC2626).copy(alpha = alpha),
                rightColor = Color(0xFF991B1B).copy(alpha = alpha),
                bevelColor = Color(0xFFFFFFFF).copy(alpha = alpha)
            )
            // Red concentric bullseye rings on top face
            drawCircle(color = Color(0xFFDC2626).copy(alpha = alpha), radius = hw * 0.45f, center = Offset(center.x, center.y - elev), style = Stroke(width = 2.5f))
            drawCircle(color = Color(0xFFDC2626).copy(alpha = alpha), radius = hw * 0.18f, center = Offset(center.x, center.y - elev))
        }

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

        BlockType.REDSTONE_TORCH -> {
            drawRedstoneTorch(
                block = block,
                center = center,
                elev = elev,
                alpha = alpha
            )
        }

        BlockType.LEVER -> {
            drawLever(
                block = block,
                center = center,
                hw = hw,
                hh = hh,
                elev = elev,
                alpha = alpha
            )
        }

        BlockType.STONE_BUTTON -> {
            drawButton(
                block = block,
                center = center,
                hw = hw,
                hh = hh,
                alpha = alpha
            )
        }

        BlockType.STONE_PRESSURE_PLATE -> {
            drawPressurePlate(
                center = center,
                hw = hw,
                hh = hh,
                elev = elev,
                alpha = alpha,
                isActive = block.isActive
            )
        }

        BlockType.DAYLIGHT_DETECTOR -> {
            val slabElev = elev * 0.35f
            drawVoxelCube(
                center, hw, hh, slabElev,
                topColor = Color(0xFF92400E).copy(alpha = alpha),
                leftColor = Color(0xFF78350F).copy(alpha = alpha),
                rightColor = Color(0xFF451A03).copy(alpha = alpha),
                bevelColor = Color(0xFFB45309).copy(alpha = alpha),
                glowColor = if (block.isActive) Color(0xFFFFB300) else null
            )
            // Solar cell grid
            drawLine(
                color = if (block.isActive) NeonCyan else Color(0xFF1E293B),
                start = Offset(center.x - hw * 0.4f, center.y - slabElev),
                end = Offset(center.x + hw * 0.4f, center.y - slabElev),
                strokeWidth = 2.0f
            )
        }

        BlockType.SCULK_SENSOR -> {
            drawVoxelCube(
                center, hw, hh, elev,
                topColor = Color(0xFF06283D).copy(alpha = alpha),
                leftColor = Color(0xFF041C2B).copy(alpha = alpha),
                rightColor = Color(0xFF021019).copy(alpha = alpha),
                bevelColor = Color(0xFF1363DF).copy(alpha = alpha),
                glowColor = if (block.isActive) NeonCyan else null
            )
            // Sculk tendrils
            val tendrilColor = if (block.isActive) NeonCyan else Color(0xFF06B6D4)
            drawCircle(color = tendrilColor, radius = 2.5f, center = Offset(center.x - 5f, center.y - elev - 4f))
            drawCircle(color = tendrilColor, radius = 2.5f, center = Offset(center.x + 5f, center.y - elev - 4f))
        }

        BlockType.REDSTONE_LAMP -> {
            val isLit = block.isActive || block.power > 0
            if (isLit) {
                drawVoxelCube(
                    center, hw, hh, elev,
                    topColor = Color(0xFFFFCA28).copy(alpha = alpha),
                    leftColor = Color(0xFFFFA000).copy(alpha = alpha),
                    rightColor = Color(0xFFFF8F00).copy(alpha = alpha),
                    bevelColor = Color(0xFFFFF9C4).copy(alpha = alpha),
                    glowColor = Color(0xFFFFD54F)
                )
                // Internal radiant lattice
                drawCircle(color = Color(0xFFFFFFFF).copy(alpha = alpha), radius = hw * 0.3f, center = Offset(center.x, center.y - elev))
            } else {
                drawVoxelCube(
                    center, hw, hh, elev,
                    topColor = Color(0xFF3E2723).copy(alpha = alpha),
                    leftColor = Color(0xFF271510).copy(alpha = alpha),
                    rightColor = Color(0xFF1A0E0B).copy(alpha = alpha),
                    bevelColor = Color(0xFF5D4037).copy(alpha = alpha)
                )
            }
        }

        BlockType.COPPER_BULB -> {
            val isLit = block.isActive
            if (isLit) {
                drawVoxelCube(
                    center, hw, hh, elev,
                    topColor = Color(0xFF0F766E).copy(alpha = alpha),
                    leftColor = Color(0xFF115E59).copy(alpha = alpha),
                    rightColor = Color(0xFF134E4A).copy(alpha = alpha),
                    bevelColor = Color(0xFF2DD4BF).copy(alpha = alpha),
                    glowColor = Color(0xFFFF9800)
                )
                // Glowing orange-amber filament inside oxidized frame
                drawCircle(color = Color(0xFFFFB74D).copy(alpha = alpha), radius = hw * 0.32f, center = Offset(center.x, center.y - elev))
                drawCircle(color = Color(0xFFFFFFFF).copy(alpha = alpha), radius = hw * 0.15f, center = Offset(center.x, center.y - elev))
            } else {
                drawVoxelCube(
                    center, hw, hh, elev,
                    topColor = Color(0xFF14532D).copy(alpha = alpha),
                    leftColor = Color(0xFF0F3E22).copy(alpha = alpha),
                    rightColor = Color(0xFF0B2D19).copy(alpha = alpha),
                    bevelColor = Color(0xFF16A34A).copy(alpha = alpha)
                )
                drawCircle(color = Color(0xFF1F2937).copy(alpha = alpha), radius = hw * 0.22f, center = Offset(center.x, center.y - elev))
            }
        }

        BlockType.PISTON, BlockType.STICKY_PISTON -> {
            val isSticky = block.type == BlockType.STICKY_PISTON
            val headElev = if (block.isExtended) elev * 1.35f else elev

            drawVoxelCube(
                center, hw, hh, headElev,
                topColor = if (isSticky) StickySlime.copy(alpha = alpha) else PistonHead.copy(alpha = alpha),
                leftColor = Color(0xFF4B5563).copy(alpha = alpha),
                rightColor = Color(0xFF374151).copy(alpha = alpha),
                bevelColor = if (isSticky) Color(0xFF86EFAC).copy(alpha = alpha) else Color(0xFFA17448).copy(alpha = alpha)
            )

            if (block.isExtended) {
                drawLine(
                    color = WoodBrown.copy(alpha = alpha),
                    start = Offset(center.x, center.y),
                    end = Offset(center.x, center.y - headElev * 0.6f),
                    strokeWidth = 6.0f
                )
            }
        }

        BlockType.OBSERVER -> {
            drawVoxelCube(
                center, hw, hh, elev,
                topColor = Color(0xFF374151).copy(alpha = alpha),
                leftColor = Color(0xFF1F2937).copy(alpha = alpha),
                rightColor = Color(0xFF111827).copy(alpha = alpha),
                bevelColor = Color(0xFF4B5563).copy(alpha = alpha)
            )
            // Sensor pulse eye
            val isPulsing = block.pulseTicksLeft > 0
            val eyeColor = if (isPulsing) RedstoneFull.copy(alpha = alpha) else ObserverEye.copy(alpha = alpha)
            drawCircle(color = eyeColor, radius = 4.2f, center = Offset(center.x, center.y - elev * 0.6f))
            if (isPulsing) {
                drawCircle(color = RedstoneGlow.copy(alpha = 0.6f * alpha), radius = 9.0f, center = Offset(center.x, center.y - elev * 0.6f))
            }
        }

        BlockType.NOTE_BLOCK -> {
            drawVoxelCube(
                center, hw, hh, elev,
                topColor = Color(0xFF78350F).copy(alpha = alpha),
                leftColor = Color(0xFF5A270B).copy(alpha = alpha),
                rightColor = Color(0xFF3B1A07).copy(alpha = alpha),
                bevelColor = Color(0xFF92400E).copy(alpha = alpha),
                glowColor = if (block.isActive) NeonCyan else null
            )
            // Musical note pixel etch
            drawCircle(color = Color(0xFFFDE68A).copy(alpha = alpha), radius = 2.5f, center = Offset(center.x - 3f, center.y - elev + 2f))
            drawLine(
                color = Color(0xFFFDE68A).copy(alpha = alpha),
                start = Offset(center.x - 1f, center.y - elev + 2f),
                end = Offset(center.x - 1f, center.y - elev - 4f),
                strokeWidth = 1.8f
            )
        }

        BlockType.DISPENSER, BlockType.DROPPER -> {
            drawVoxelCube(
                center, hw, hh, elev,
                topColor = Color(0xFF6B7280).copy(alpha = alpha),
                leftColor = Color(0xFF4B5563).copy(alpha = alpha),
                rightColor = Color(0xFF374151).copy(alpha = alpha),
                bevelColor = Color(0xFF9CA3AF).copy(alpha = alpha)
            )
            // Mouth opening
            drawCircle(
                color = Color(0xFF111827).copy(alpha = alpha),
                radius = 3.5f,
                center = Offset(center.x, center.y - elev * 0.5f)
            )
        }

        BlockType.HOPPER -> {
            drawVoxelCube(
                center, hw * 0.85f, hh * 0.85f, elev * 0.7f,
                topColor = Color(0xFF374151).copy(alpha = alpha),
                leftColor = Color(0xFF1F2937).copy(alpha = alpha),
                rightColor = Color(0xFF111827).copy(alpha = alpha),
                bevelColor = Color(0xFF4B5563).copy(alpha = alpha)
            )
        }

        BlockType.IRON_DOOR, BlockType.IRON_TRAPDOOR -> {
            val plateElev = if (block.isActive) elev * 0.2f else elev * 0.8f
            drawVoxelCube(
                center, hw * 0.88f, hh * 0.88f, plateElev,
                topColor = Color(0xFFE2E8F0).copy(alpha = alpha),
                leftColor = Color(0xFFCBD5E1).copy(alpha = alpha),
                rightColor = Color(0xFF94A3B8).copy(alpha = alpha),
                bevelColor = Color(0xFFFFFFFF).copy(alpha = alpha)
            )
        }

        BlockType.BELL -> {
            drawVoxelCube(
                center, hw * 0.65f, hh * 0.65f, elev * 0.65f,
                topColor = Color(0xFFFACC15).copy(alpha = alpha),
                leftColor = Color(0xFFEAB308).copy(alpha = alpha),
                rightColor = Color(0xFFCA8A04).copy(alpha = alpha),
                bevelColor = Color(0xFFFEF08A).copy(alpha = alpha),
                glowColor = if (block.isActive) Color(0xFFFACC15) else null
            )
        }

        BlockType.TNT -> {
            drawVoxelCube(
                center, hw, hh, elev,
                topColor = Color(0xFFDC2626).copy(alpha = alpha),
                leftColor = Color(0xFFB91C1C).copy(alpha = alpha),
                rightColor = Color(0xFF991B1B).copy(alpha = alpha),
                bevelColor = Color(0xFFEF4444).copy(alpha = alpha),
                glowColor = if (block.isActive) Color.White else null
            )
            // TNT central white band
            drawLine(
                color = Color.White.copy(alpha = alpha),
                start = Offset(center.x - hw, center.y - elev * 0.5f),
                end = Offset(center.x + hw, center.y - elev * 0.5f),
                strokeWidth = 3.5f
            )
        }

        BlockType.AIR -> {}
    }
}

private fun DrawScope.drawGhostCube(
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val ghostColor = Color(0x2200E5FF).copy(alpha = alpha * 0.25f)
    val ghostBorder = Color(0x6600E5FF).copy(alpha = alpha * 0.45f)

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
            blue = 0.12f * fraction,
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
            radius = if (noConnections) 12f else 8.5f,
            center = centerTop
        )
    }

    // Connect wire branches using projected 3D coordinates so camera rotation rotates wire arms
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

    // Exibição do número da força de sinal (0 a 15)
    if (showSignalNumbers) {
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = if (signal > 0) android.graphics.Color.WHITE else android.graphics.Color.LTGRAY
                textSize = 22f
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
                setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
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
    drawVoxelCube(
        center, hw, hh, slabElev,
        topColor = Color(0xFF6B7280).copy(alpha = alpha),
        leftColor = Color(0xFF4B5563).copy(alpha = alpha),
        rightColor = Color(0xFF374151).copy(alpha = alpha),
        bevelColor = Color(0xFF9CA3AF).copy(alpha = alpha)
    )

    val isLit = block.power > 0
    val torchColor = if (isLit) RedstoneFull.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)

    // Torches positioned in 3D grid space relative to block facing
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

    // Draw repeater torches
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
    drawVoxelCube(
        center, hw, hh, slabElev,
        topColor = Color(0xFF6B7280).copy(alpha = alpha),
        leftColor = Color(0xFF4B5563).copy(alpha = alpha),
        rightColor = Color(0xFF374151).copy(alpha = alpha),
        bevelColor = Color(0xFF9CA3AF).copy(alpha = alpha)
    )

    val isLit = block.power > 0
    val sideColor = if (isLit) RedstoneFull.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)

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

    val frontTorchColor = if (block.isSubtractMode) RedstoneFull.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)
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
    val headColor = if (block.isActive) RedstoneFull.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)
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
    drawPath(path = mountPath, color = Color(0xFF6B7280).copy(alpha = alpha), style = Fill)

    val angleX = if (block.isActive) 8f else -8f
    drawLine(
        color = WoodBrown.copy(alpha = alpha),
        start = Offset(center.x, center.y),
        end = Offset(center.x + angleX, center.y - elev * 0.75f),
        strokeWidth = 3.8f
    )
    drawCircle(
        color = if (block.isActive) RedstoneFull.copy(alpha = alpha) else Color(0xFF374151).copy(alpha = alpha),
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
    val buttonColor = if (block.pulseTicksLeft > 0) NeonCyan.copy(alpha = alpha) else Color(0xFF9CA3AF).copy(alpha = alpha)
    drawPath(path = buttonPath, color = buttonColor, style = Fill)
    drawPath(path = buttonPath, color = MatteBorder, style = Stroke(width = 1.0f))
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
    drawVoxelCube(
        center, hw * plateSize, hh * plateSize, plateElev,
        topColor = if (isActive) NeonCyan.copy(alpha = alpha) else Color(0xFF9CA3AF).copy(alpha = alpha),
        leftColor = Color(0xFF6B7280).copy(alpha = alpha),
        rightColor = Color(0xFF4B5563).copy(alpha = alpha),
        bevelColor = Color(0xFFCBD5E1).copy(alpha = alpha)
    )
}
