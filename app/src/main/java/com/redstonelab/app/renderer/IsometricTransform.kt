package com.redstonelab.app.renderer

import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt

data class IsoMetrics(
    val tileWidth: Float = 64f,
    val tileHeight: Float = 32f,
    val tileElevation: Float = 24f
) {
    val halfWidth: Float get() = tileWidth / 2f
    val halfHeight: Float get() = tileHeight / 2f
}

/**
 * 2.5D Isometric Coordinate Transformations between 3D Minecraft Grid Space (X, Y, Z)
 * and 2D Screen Space.
 *
 * X: Horizontal width (0..15)
 * Y: Vertical layer / height (0..7)
 * Z: Horizontal depth (0..15)
 */
class IsometricTransform(
    var metrics: IsoMetrics = IsoMetrics()
) {
    fun gridToScreen(
        x: Float,
        y: Float,
        z: Float,
        viewportOffset: Offset,
        zoom: Float
    ): Offset {
        val tw = metrics.halfWidth * zoom
        val th = metrics.halfHeight * zoom
        val elev = metrics.tileElevation * zoom

        val sx = (x - z) * tw + viewportOffset.x
        val sy = (x + z) * th - (y * elev) + viewportOffset.y
        return Offset(sx, sy)
    }

    fun screenToGrid(
        screenPos: Offset,
        activeLayerY: Int,
        viewportOffset: Offset,
        zoom: Float
    ): Pair<Int, Int> {
        val tw = metrics.halfWidth * zoom
        val th = metrics.halfHeight * zoom
        val elev = metrics.tileElevation * zoom

        val adjustedX = screenPos.x - viewportOffset.x
        val adjustedY = screenPos.y - viewportOffset.y + (activeLayerY * elev)

        val u = adjustedX / tw
        val v = adjustedY / th

        val gx = ((v + u) / 2f).roundToInt()
        val gz = ((v - u) / 2f).roundToInt()

        return Pair(gx, gz)
    }
}
