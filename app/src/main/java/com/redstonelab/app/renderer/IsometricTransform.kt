package com.redstonelab.app.renderer

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
 * and 2D Screen Space, with 360-degree interactive camera rotation around the 16x16 grid center.
 *
 * X: Horizontal width (0..15)
 * Y: Vertical layer / height (0..7)
 * Z: Horizontal depth (0..15)
 */
class IsometricTransform(
    var metrics: IsoMetrics = IsoMetrics(),
    var cameraAngle: Float = 0f
) {
    private val gridCenterX = 7.5f
    private val gridCenterZ = 7.5f

    /**
     * Rotates grid coordinates (X, Z) by [cameraAngle] around the grid center.
     */
    fun rotateCoord(x: Float, z: Float): Pair<Float, Float> {
        val angleNorm = ((cameraAngle % 360f) + 360f) % 360f
        if (angleNorm == 0f) return Pair(x, z)
        val rad = Math.toRadians(angleNorm.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val dx = x - gridCenterX
        val dz = z - gridCenterZ
        val rx = dx * cosA - dz * sinA + gridCenterX
        val rz = dx * sinA + dz * cosA + gridCenterZ
        return Pair(rx, rz)
    }

    /**
     * Inverse rotation: restores rotated coordinates (RX, RZ) back to world coordinates (X, Z).
     */
    fun unrotateCoord(rx: Float, rz: Float): Pair<Float, Float> {
        val angleNorm = ((cameraAngle % 360f) + 360f) % 360f
        if (angleNorm == 0f) return Pair(rx, rz)
        val rad = Math.toRadians(-angleNorm.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val rdx = rx - gridCenterX
        val rdz = rz - gridCenterZ
        val x = rdx * cosA - rdz * sinA + gridCenterX
        val z = rdx * sinA + rdz * cosA + gridCenterZ
        return Pair(x, z)
    }

    fun gridToScreen(
        x: Float,
        y: Float,
        z: Float,
        viewportOffset: Offset,
        zoom: Float
    ): Offset {
        val (rx, rz) = rotateCoord(x, z)
        val tw = metrics.halfWidth * zoom
        val th = metrics.halfHeight * zoom
        val elev = metrics.tileElevation * zoom

        val sx = (rx - rz) * tw + viewportOffset.x
        val sy = (rx + rz) * th - (y * elev) + viewportOffset.y
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

        val rx = (v + u) / 2f
        val rz = (v - u) / 2f

        val (gxFloat, gzFloat) = unrotateCoord(rx, rz)
        val gx = gxFloat.roundToInt().coerceIn(0, 15)
        val gz = gzFloat.roundToInt().coerceIn(0, 15)

        return Pair(gx, gz)
    }

    /**
     * Computes isometric view depth for sorting blocks at the current camera angle.
     * Blocks with lower depth are drawn first (background), higher depth last (foreground).
     */
    fun computeDepth(x: Float, z: Float): Float {
        val (rx, rz) = rotateCoord(x, z)
        return rx + rz
    }
}
