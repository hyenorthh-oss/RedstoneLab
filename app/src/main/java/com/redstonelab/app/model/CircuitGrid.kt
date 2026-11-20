package com.redstonelab.app.model

data class CircuitBounds(
    val minX: Int, val maxX: Int,
    val minY: Int, val maxY: Int,
    val minZ: Int, val maxZ: Int
) {
    val sizeX: Int get() = if (maxX >= minX) maxX - minX + 1 else 0
    val sizeY: Int get() = if (maxY >= minY) maxY - minY + 1 else 0
    val sizeZ: Int get() = if (maxZ >= minZ) maxZ - minZ + 1 else 0
    val totalVolume: Int get() = sizeX * sizeY * sizeZ
}

class CircuitGrid(
    initialBlocks: Map<BlockCoord, BlockState> = emptyMap()
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

    fun getLayer(z: Int): List<BlockState> {
        return blocks.values.filter { it.z == z }
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
        return CircuitGrid(blocks)
    }
}
