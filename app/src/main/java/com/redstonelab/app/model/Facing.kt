package com.redstonelab.app.model

enum class Facing(val dx: Int, val dy: Int, val dz: Int, val bedrockName: String) {
    NORTH(0, -1, 0, "north"),
    EAST(1, 0, 0, "east"),
    SOUTH(0, 1, 0, "south"),
    WEST(-1, 0, 0, "west"),
    UP(0, 0, 1, "up"),
    DOWN(0, 0, -1, "down");

    val opposite: Facing
        get() = when (this) {
            NORTH -> SOUTH
            SOUTH -> NORTH
            EAST -> WEST
            WEST -> EAST
            UP -> DOWN
            DOWN -> UP
        }

    fun clockwise(): Facing = when (this) {
        NORTH -> EAST
        EAST -> SOUTH
        SOUTH -> WEST
        WEST -> NORTH
        else -> this
    }

    fun counterClockwise(): Facing = when (this) {
        NORTH -> WEST
        WEST -> SOUTH
        SOUTH -> EAST
        EAST -> NORTH
        else -> this
    }

    companion object {
        val CARDINALS = listOf(NORTH, EAST, SOUTH, WEST)
        val ALL = entries.toTypedArray()

        fun fromBedrock(name: String): Facing {
            return entries.firstOrNull { it.bedrockName.equals(name, ignoreCase = true) } ?: NORTH
        }
    }
}
