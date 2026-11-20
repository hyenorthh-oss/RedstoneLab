package com.redstonelab.app.model

data class BlockCoord(val x: Int, val y: Int, val z: Int) {
    fun offset(facing: Facing): BlockCoord {
        return BlockCoord(x + facing.dx, y + facing.dy, z + facing.dz)
    }

    fun offset(dx: Int, dy: Int, dz: Int): BlockCoord {
        return BlockCoord(x + dx, y + dy, z + dz)
    }

    override fun toString(): String = "($x, $y, $z)"
}

data class BlockState(
    val coord: BlockCoord,
    val type: BlockType,
    val power: Int = 0,
    val facing: Facing = Facing.NORTH,
    val delay: Int = 1,              // Repeaters: 1..4 ticks
    val isLocked: Boolean = false,    // Repeaters: locked state
    val isSubtractMode: Boolean = false, // Comparators: compare vs subtract
    val isExtended: Boolean = false,  // Pistons: extended head
    val isActive: Boolean = false,    // Levers: ON/OFF, Lamps: LIT/UNLIT, Torches: LIT/UNLIT
    val pulseTicksLeft: Int = 0,     // Buttons: pulse countdown (10 ticks), Observer (1-tick pulse)
    val attachmentFacing: Facing = Facing.DOWN // For torches/buttons/levers attached to floor (DOWN) or wall
) {
    val x: Int get() = coord.x
    val y: Int get() = coord.y
    val z: Int get() = coord.z

    val isPowered: Boolean get() = power > 0 || isActive

    fun withPower(newPower: Int): BlockState {
        val clamped = newPower.coerceIn(0, 15)
        return if (power == clamped) this else copy(power = clamped)
    }

    fun withActive(active: Boolean): BlockState {
        return if (isActive == active) this else copy(isActive = active)
    }

    fun cycleDelay(): BlockState {
        if (type != BlockType.REDSTONE_REPEATER) return this
        val nextDelay = if (delay >= 4) 1 else delay + 1
        return copy(delay = nextDelay)
    }

    fun toggleSubtractMode(): BlockState {
        if (type != BlockType.REDSTONE_COMPARATOR) return this
        return copy(isSubtractMode = !isSubtractMode)
    }

    fun toggleLever(): BlockState {
        if (type != BlockType.LEVER) return this
        val newActive = !isActive
        return copy(
            isActive = newActive,
            power = if (newActive) 15 else 0
        )
    }

    fun triggerButton(): BlockState {
        if (type != BlockType.STONE_BUTTON) return this
        return copy(
            isActive = true,
            power = 15,
            pulseTicksLeft = 10
        )
    }
}
