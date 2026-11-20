package com.redstonelab.app.renderer

import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt

data class IsoMetrics(
    val tileWidth: Float = 64f,
    val tileHeight: Float = 32f,
    val tileElevation: Float = 26f
) {
    val halfWidth: Float get() = tileWidth / 2f
    val halfHeight: Float get() = tileHeight / 2f
}

/**
 * 2.5D Isometric Coordinate Transformations between 3D Grid Space and 2D Screen Space.
 */
class IsometricTransform(
    var metrics: IsoMetrics = IsoMetrics()
) {
    /**
     * Converts 3D grid coordinate (x, y, z) to 2D screen coordinate.
     */
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

        val sx = (x - y) * tw + viewportOffset.x
        val sy = (x + y) * th - (z * elev) + viewportOffset.y
        return Offset(sx, sy)
    }

    /**
     * Inverse projection: converts 2D screen touch coordinate to 3D grid coordinate
     * on a given Z plane layer.
     */
    fun screenToGrid(
        screenPos: Offset,
        planeZ: Int,
        viewportOffset: Offset,
        zoom: Float
    ): Pair<Int, Int> {
        val tw = metrics.halfWidth * zoom
        val th = metrics.halfHeight * zoom
        val elev = metrics.tileElevation * zoom

        // Shift by viewport offset and current plane elevation
        val adjustedX = screenPos.x - viewportOffset.x
        val adjustedY = screenPos.y - viewportOffset.y + (planeZ * elev)

        // Solve linear system:
        // adjustedX = (gx - gy) * tw
        // adjustedY = (gx + gy) * th
        // -> gx - gy = adjustedX / tw
        // -> gx + gy = adjustedY / th
        val u = adjustedX / tw
        val v = adjustedY / th

        val gx = ((v + u) / 2f).roundToInt()
        val gy = ((v - u) / 2f).roundToInt()

        return Pair(gx, gy)
    }
}
