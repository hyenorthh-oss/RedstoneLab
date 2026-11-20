package com.redstonelab.app.model

import org.json.JSONArray
import org.json.JSONObject

data class CircuitBounds(
    val minX: Int, val maxX: Int,
    val minY: Int, val maxY: Int, // Y is height (layers)
    val minZ: Int, val maxZ: Int
) {
    val sizeX: Int get() = if (maxX >= minX) maxX - minX + 1 else 0
    val sizeY: Int get() = if (maxY >= minY) maxY - minY + 1 else 0
    val sizeZ: Int get() = if (maxZ >= minZ) maxZ - minZ + 1 else 0
    val totalVolume: Int get() = sizeX * sizeY * sizeZ
}

class CircuitGrid(
    initialBlocks: Map<BlockCoord, BlockState> = emptyMap(),
    val widthX: Int = 16,
    val depthZ: Int = 16,
    val maxHeightY: Int = 8
) {
    private val blocks = HashMap<BlockCoord, BlockState>(initialBlocks)

    fun get(coord: BlockCoord): BlockState? = blocks[coord]
    fun get(x: Int, y: Int, z: Int): BlockState? = blocks[BlockCoord(x, y, z)]

    fun set(state: BlockState) {
        if (state.type == BlockType.AIR) {
            blocks.remove(state.coord)
        } else {
            blocks[state.coord] = state
        }
    }

    fun remove(coord: BlockCoord): BlockState? = blocks.remove(coord)
    fun remove(x: Int, y: Int, z: Int): BlockState? = blocks.remove(BlockCoord(x, y, z))

    fun clear() {
        blocks.clear()
    }

    fun getAll(): Map<BlockCoord, BlockState> = HashMap(blocks)

    fun getLayer(y: Int): List<BlockState> {
        return blocks.values.filter { it.y == y }
    }

    val blockCount: Int get() = blocks.size

    val activeCount: Int get() = blocks.values.count { it.isPowered || it.isActive }

    fun getBounds(): CircuitBounds {
        if (blocks.isEmpty()) {
            return CircuitBounds(0, 0, 0, 0, 0, 0)
        }
        var minX = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var minY = Int.MAX_VALUE
        var maxY = Int.MIN_VALUE
        var minZ = Int.MAX_VALUE
        var maxZ = Int.MIN_VALUE

        for (c in blocks.keys) {
            if (c.x < minX) minX = c.x
            if (c.x > maxX) maxX = c.x
            if (c.y < minY) minY = c.y
            if (c.y > maxY) maxY = c.y
            if (c.z < minZ) minZ = c.z
            if (c.z > maxZ) maxZ = c.z
        }

        return CircuitBounds(minX, maxX, minY, maxY, minZ, maxZ)
    }

    fun clone(): CircuitGrid {
        return CircuitGrid(blocks, widthX, depthZ, maxHeightY)
    }

    /**
     * Serializes this circuit grid to a JSON string for offline local storage.
     */
    fun toJson(): String {
        val root = JSONObject()
        root.put("widthX", widthX)
        root.put("depthZ", depthZ)
        root.put("maxHeightY", maxHeightY)

        val arr = JSONArray()
        for ((coord, state) in blocks) {
            val obj = JSONObject()
            obj.put("x", coord.x)
            obj.put("y", coord.y)
            obj.put("z", coord.z)
            obj.put("type", state.type.name)
            obj.put("power", state.power)
            obj.put("facing", state.facing.name)
            obj.put("delay", state.delay)
            obj.put("isLocked", state.isLocked)
            obj.put("isSubtractMode", state.isSubtractMode)
            obj.put("isExtended", state.isExtended)
            obj.put("isActive", state.isActive)
            obj.put("attachmentFacing", state.attachmentFacing.name)
            if (state.woolColor != null) {
                obj.put("woolColor", state.woolColor)
            }
            arr.put(obj)
        }
        root.put("blocks", arr)
        return root.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): CircuitGrid {
            val root = JSONObject(jsonStr)
            val w = root.optInt("widthX", 16)
            val d = root.optInt("depthZ", 16)
            val h = root.optInt("maxHeightY", 8)
            val grid = CircuitGrid(widthX = w, depthZ = d, maxHeightY = h)

            val arr = root.optJSONArray("blocks") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val x = obj.getInt("x")
                val y = obj.getInt("y")
                val z = obj.getInt("z")
                val typeName = obj.getString("type")
                val type = try {
                    BlockType.valueOf(typeName)
                } catch (_: Exception) {
                    BlockType.SMOOTH_STONE
                }
                val power = obj.optInt("power", 0)
                val facingName = obj.optString("facing", "NORTH")
                val facing = try {
                    Facing.valueOf(facingName)
                } catch (_: Exception) {
                    Facing.NORTH
                }
                val delay = obj.optInt("delay", 1)
                val isLocked = obj.optBoolean("isLocked", false)
                val isSubtractMode = obj.optBoolean("isSubtractMode", false)
                val isExtended = obj.optBoolean("isExtended", false)
                val isActive = obj.optBoolean("isActive", false)
                val attachName = obj.optString("attachmentFacing", "DOWN")
                val attachmentFacing = try {
                    Facing.valueOf(attachName)
                } catch (_: Exception) {
                    Facing.DOWN
                }
                val woolColor = if (obj.has("woolColor")) obj.getString("woolColor") else null

                grid.set(
                    BlockState(
                        coord = BlockCoord(x, y, z),
                        type = type,
                        power = power,
                        facing = facing,
                        delay = delay,
                        isLocked = isLocked,
                        isSubtractMode = isSubtractMode,
                        isExtended = isExtended,
                        isActive = isActive,
                        attachmentFacing = attachmentFacing,
                        woolColor = woolColor
                    )
                )
            }
            return grid
        }
    }
}
