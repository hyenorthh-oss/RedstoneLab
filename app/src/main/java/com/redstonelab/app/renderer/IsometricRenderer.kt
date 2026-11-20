package com.redstonelab.app.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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
    activeLayerZ: Int,
    isLayerIsolationEnabled: Boolean,
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
                    val newZoom = (zoom * gestureZoom).coerceIn(0.4f, 3.0f)
                    onZoomChange(newZoom)
                }
            }
            .pointerInput(activeLayerZ, panOffset, zoom) {
                detectTapGestures(
                    onTap = { screenOffset ->
                        val (gx, gy) = transform.screenToGrid(
                            screenOffset,
                            activeLayerZ,
                            panOffset,
                            zoom
                        )
                        onTileClicked(BlockCoord(gx, gy, activeLayerZ))
                    },
                    onLongPress = { screenOffset ->
                        val (gx, gy) = transform.screenToGrid(
                            screenOffset,
                            activeLayerZ,
                            panOffset,
                            zoom
                        )
                        onTileLongClicked(BlockCoord(gx, gy, activeLayerZ))
                    }
                )
            }
    ) {
        val hw = transform.metrics.halfWidth * zoom
        val hh = transform.metrics.halfHeight * zoom
        val elev = transform.metrics.tileElevation * zoom

        // 1. Draw Isometric Reference Grid for the active layer
        drawIsoGridPlane(
            centerZ = activeLayerZ,
            panOffset = panOffset,
            zoom = zoom,
            hw = hw,
            hh = hh,
            elev = elev
        )

        // 2. Collect and Depth-Sort Blocks for Rendering
        val allBlocks = grid.getAll()
        val blocksToDraw = if (isLayerIsolationEnabled) {
            allBlocks.values.filter { it.z == activeLayerZ }
        } else {
            allBlocks.values.toList()
        }

        // Sort by depth: lowest Z first, then smallest (X + Y)
        val sortedBlocks = blocksToDraw.sortedWith(
            compareBy<BlockState> { it.z }
                .thenBy { it.x + it.y }
                .thenBy { it.x }
        )

        // 3. Render Blocks
        for (block in sortedBlocks) {
            val center = transform.gridToScreen(
                block.x.toFloat(),
                block.y.toFloat(),
                block.z.toFloat(),
                panOffset,
                zoom
            )

            // Dim lower layers if not in isolation
            val layerAlpha = when {
                block.z == activeLayerZ -> 1.0f
                block.z < activeLayerZ -> 0.75f
                else -> 0.45f
            }

            drawIsometricBlock(
                block = block,
                center = center,
                hw = hw,
                hh = hh,
                elev = elev,
                alpha = layerAlpha,
                grid = grid
            )
        }

        // 4. Draw Selected Tile Cursor on the active layer
        if (selectedBlockCoord != null && selectedBlockCoord.z == activeLayerZ) {
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
    centerZ: Int,
    panOffset: Offset,
    zoom: Float,
    hw: Float,
    hh: Float,
    elev: Float
) {
    val range = -6..10
    val path = Path()

    for (x in range) {
        val start = Offset(
            (x - range.first) * hw + panOffset.x,
            (x + range.first) * hh - (centerZ * elev) + panOffset.y
        )
        val end = Offset(
            (x - range.last) * hw + panOffset.x,
            (x + range.last) * hh - (centerZ * elev) + panOffset.y
        )
        drawLine(
            color = if (x == 0) GridLineActive else GridLineSubtle,
            start = start,
            end = end,
            strokeWidth = if (x == 0) 1.8f else 1.0f
        )
    }

    for (y in range) {
        val start = Offset(
            (range.first - y) * hw + panOffset.x,
            (range.first + y) * hh - (centerZ * elev) + panOffset.y
        )
        val end = Offset(
            (range.last - y) * hw + panOffset.x,
            (range.last + y) * hh - (centerZ * elev) + panOffset.y
        )
        drawLine(
            color = if (y == 0) GridLineActive else GridLineSubtle,
            start = start,
            end = end,
            strokeWidth = if (y == 0) 1.8f else 1.0f
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
    drawPath(
        path = path,
        color = NeonCyan.copy(alpha = 0.25f),
        style = Fill
    )
    drawPath(
        path = path,
        color = NeonCyan,
        style = Stroke(width = 2.5f)
    )
}

private fun DrawScope.drawIsometricBlock(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float,
    grid: CircuitGrid
) {
    when (block.type) {
        BlockType.STONE -> drawSolidCube(
            center, hw, hh, elev,
            topColor = StoneGrayLight.copy(alpha = alpha),
            leftColor = StoneGray.copy(alpha = alpha),
            rightColor = StoneGrayDark.copy(alpha = alpha)
        )

        BlockType.REDSTONE_BLOCK -> drawSolidCube(
            center, hw, hh, elev,
            topColor = RedstoneFull.copy(alpha = alpha),
            leftColor = RedstoneMid.copy(alpha = alpha),
            rightColor = RedstoneDim.copy(alpha = alpha),
            glowColor = NeonCyan.copy(alpha = 0.4f * alpha)
        )

        BlockType.GLASS -> drawGlassCube(
            center, hw, hh, elev, alpha
        )

        BlockType.TARGET_BLOCK -> drawSolidCube(
            center, hw, hh, elev,
            topColor = Color(0xFFDCD2C5).copy(alpha = alpha),
            leftColor = Color(0xFFA8383B).copy(alpha = alpha),
            rightColor = Color(0xFF7A2426).copy(alpha = alpha)
        )

        BlockType.REDSTONE_WIRE -> drawRedstoneWire(
            block, center, hw, hh, alpha, grid
        )

        BlockType.REDSTONE_TORCH -> drawRedstoneTorch(
            block, center, hw, hh, elev, alpha
        )

        BlockType.REDSTONE_REPEATER -> drawRepeater(
            block, center, hw, hh, elev, alpha
        )

        BlockType.REDSTONE_COMPARATOR -> drawComparator(
            block, center, hw, hh, elev, alpha
        )

        BlockType.REDSTONE_LAMP -> drawLamp(
            block, center, hw, hh, elev, alpha
        )

        BlockType.LEVER -> drawLever(
            block, center, hw, hh, elev, alpha
        )

        BlockType.STONE_BUTTON -> drawButton(
            block, center, hw, hh, elev, alpha
        )

        BlockType.PISTON, BlockType.STICKY_PISTON -> drawPiston(
            block, center, hw, hh, elev, alpha
        )

        BlockType.OBSERVER -> drawObserver(
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
    // Top Diamond Face
    val topPath = Path().apply {
        moveTo(center.x, center.y - hh - elev)
        lineTo(center.x + hw, center.y - elev)
        lineTo(center.x, center.y + hh - elev)
        lineTo(center.x - hw, center.y - elev)
        close()
    }
    drawPath(topPath, topColor, Fill)

    // Left Parallelogram Face
    val leftPath = Path().apply {
        moveTo(center.x - hw, center.y - elev)
        lineTo(center.x, center.y + hh - elev)
        lineTo(center.x, center.y + hh)
        lineTo(center.x - hw, center.y)
        close()
    }
    drawPath(leftPath, leftColor, Fill)

    // Right Parallelogram Face
    val rightPath = Path().apply {
        moveTo(center.x, center.y + hh - elev)
        lineTo(center.x + hw, center.y - elev)
        lineTo(center.x + hw, center.y)
        lineTo(center.x, center.y + hh)
        close()
    }
    drawPath(rightPath, rightColor, Fill)

    // Cyber bevel highlights
    drawPath(topPath, GlassBorderSubtle, Stroke(width = 1.0f))
    drawPath(leftPath, GlassBorderSubtle, Stroke(width = 1.0f))
    drawPath(rightPath, GlassBorderSubtle, Stroke(width = 1.0f))

    if (glowColor != null) {
        drawCircle(
            color = glowColor,
            radius = hw * 0.8f,
            center = Offset(center.x, center.y - elev * 0.5f),
            style = Stroke(width = 1.5f)
        )
    }
}

private fun DrawScope.drawGlassCube(
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    val topColor = GlassTransparent.copy(alpha = 0.35f * alpha)
    val leftColor = Color(0x3300F0FF).copy(alpha = 0.25f * alpha)
    val rightColor = Color(0x330077FE).copy(alpha = 0.25f * alpha)

    drawSolidCube(
        center, hw, hh, elev,
        topColor = topColor,
        leftColor = leftColor,
        rightColor = rightColor
    )
}

private fun DrawScope.drawRedstoneWire(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    alpha: Float,
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

    // Base flat tile highlight
    val centerTop = Offset(center.x, center.y)
    val wireWidth = 4.5f

    // Check cardinal neighbors to draw directional wire arms
    val c = block.coord
    val northConnect = shouldConnectWire(grid.get(c.offset(Facing.NORTH)))
    val southConnect = shouldConnectWire(grid.get(c.offset(Facing.SOUTH)))
    val eastConnect = shouldConnectWire(grid.get(c.offset(Facing.EAST)))
    val westConnect = shouldConnectWire(grid.get(c.offset(Facing.WEST)))

    val noConnections = !northConnect && !southConnect && !eastConnect && !westConnect

    // Center node
    drawCircle(
        color = wireColor,
        radius = if (noConnections) 5.5f else 3.5f,
        center = centerTop
    )

    if (signal > 0) {
        // Glowing halo for active wire
        drawCircle(
            color = RedstoneGlow.copy(alpha = 0.35f * (signal / 15f) * alpha),
            radius = if (noConnections) 10f else 7f,
            center = centerTop
        )
    }

    // Connect north (towards -Y: top-right on isometric canvas)
    if (northConnect || noConnections) {
        val p = Offset(center.x + hw * 0.45f, center.y - hh * 0.45f)
        drawLine(color = wireColor, start = centerTop, end = p, strokeWidth = wireWidth)
    }
    // Connect south (towards +Y: bottom-left)
    if (southConnect || noConnections) {
        val p = Offset(center.x - hw * 0.45f, center.y + hh * 0.45f)
        drawLine(color = wireColor, start = centerTop, end = p, strokeWidth = wireWidth)
    }
    // Connect east (towards +X: bottom-right)
    if (eastConnect || noConnections) {
        val p = Offset(center.x + hw * 0.45f, center.y + hh * 0.45f)
        drawLine(color = wireColor, start = centerTop, end = p, strokeWidth = wireWidth)
    }
    // Connect west (towards -X: top-left)
    if (westConnect || noConnections) {
        val p = Offset(center.x - hw * 0.45f, center.y - hh * 0.45f)
        drawLine(color = wireColor, start = centerTop, end = p, strokeWidth = wireWidth)
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
        BlockType.REDSTONE_BLOCK,
        BlockType.TARGET_BLOCK -> true
        else -> false
    }
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

    // Wood stem
    drawLine(
        color = WoodBrown.copy(alpha = alpha),
        start = stalkBase,
        end = torchTop,
        strokeWidth = 3.5f
    )

    // Torch head
    val headColor = if (block.isActive) RedstoneFull.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)
    drawCircle(
        color = headColor,
        radius = 5.0f,
        center = torchTop
    )

    if (block.isActive) {
        // Glowing halo
        drawCircle(
            color = RedstoneGlow.copy(alpha = 0.5f * alpha),
            radius = 11.0f,
            center = torchTop
        )
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
    // Thin base slab
    val slabElev = elev * 0.35f
    drawSolidCube(
        center, hw, hh, slabElev,
        topColor = StoneGrayLight.copy(alpha = alpha),
        leftColor = StoneGray.copy(alpha = alpha),
        rightColor = StoneGrayDark.copy(alpha = alpha)
    )

    // Two miniature torches on top
    val topCenter = Offset(center.x, center.y - slabElev)
    val isLit = block.power > 0
    val torchColor = if (isLit) RedstoneFull.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)

    // Base stationary torch
    drawCircle(color = torchColor, radius = 3.5f, center = Offset(topCenter.x - 4f, topCenter.y - 2f))

    // Adjustable delay torch (offset by delay 1..4)
    val delayOffset = (block.delay - 1) * 3f
    drawCircle(color = torchColor, radius = 3.5f, center = Offset(topCenter.x + 4f + delayOffset, topCenter.y + 2f))

    if (block.isLocked) {
        // Bedrock locking bar across repeater
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

    // Rear dual torches
    drawCircle(color = sideColor, radius = 3.0f, center = Offset(topCenter.x - 6f, topCenter.y))
    drawCircle(color = sideColor, radius = 3.0f, center = Offset(topCenter.x + 6f, topCenter.y))

    // Front torch: illuminates when in Subtract Mode
    val frontTorchColor = if (block.isSubtractMode) RedstoneFull.copy(alpha = alpha) else RedstoneOff.copy(alpha = alpha)
    drawCircle(color = frontTorchColor, radius = 3.5f, center = Offset(topCenter.x, topCenter.y - 5f))
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
    val topColor = if (isLit) LampOn.copy(alpha = alpha) else LampOff.copy(alpha = alpha)
    val leftColor = if (isLit) Color(0xFFE6A100).copy(alpha = alpha) else Color(0xFF2A1C12).copy(alpha = alpha)
    val rightColor = if (isLit) Color(0xFFB37D00).copy(alpha = alpha) else Color(0xFF1D130C).copy(alpha = alpha)

    drawSolidCube(
        center, hw, hh, elev,
        topColor = topColor,
        leftColor = leftColor,
        rightColor = rightColor,
        glowColor = if (isLit) LampOn.copy(alpha = 0.45f * alpha) else null
    )
}

private fun DrawScope.drawLever(
    block: BlockState,
    center: Offset,
    hw: Float,
    hh: Float,
    elev: Float,
    alpha: Float
) {
    // Base cobblestone mount
    val mountPath = Path().apply {
        moveTo(center.x, center.y - hh * 0.4f)
        lineTo(center.x + hw * 0.4f, center.y)
        lineTo(center.x, center.y + hh * 0.4f)
        lineTo(center.x - hw * 0.4f, center.y)
        close()
    }
    drawPath(mountPath, StoneGray.copy(alpha = alpha), Fill)

    // Lever arm: tilted depending on active state
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
    drawPath(buttonPath, buttonColor, Fill)
    drawPath(buttonPath, GlassBorderSubtle, Stroke(width = 1.0f))
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
        // Wood rod visible underneath
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

    // Red pulse port or cyan sensor eye
    val isPulsing = block.pulseTicksLeft > 0
    val eyeColor = if (isPulsing) RedstoneFull.copy(alpha = alpha) else ObserverEye.copy(alpha = alpha)
    drawCircle(
        color = eyeColor,
        radius = 4.0f,
        center = Offset(center.x, center.y - elev * 0.6f)
    )
}
