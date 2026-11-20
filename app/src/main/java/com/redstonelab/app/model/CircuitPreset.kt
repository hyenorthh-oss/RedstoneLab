package com.redstonelab.app.model

data class CircuitPreset(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val difficulty: String,
    val tags: List<String>,
    val builder: () -> CircuitGrid
)

object CircuitPresetsLibrary {

    val presets: List<CircuitPreset> = listOf(
        CircuitPreset(
            id = "repeater_clock",
            title = "Repeater Pulse Clock",
            category = "Clocks & Timers",
            description = "A standard 4-repeater oscillating clock generating rhythmic continuous pulses. Adjustable delay via repeater ticks.",
            difficulty = "Beginner",
            tags = listOf("Clock", "Oscillator", "Repeater"),
            builder = { buildRepeaterClock() }
        ),
        CircuitPreset(
            id = "t_flip_flop",
            title = "T-Flip-Flop (Toggle Latch)",
            category = "Memory & Logic",
            description = "Converts a momentary button pulse into a permanent toggle state (ON to OFF, OFF to ON) using sticky piston and redstone block.",
            difficulty = "Intermediate",
            tags = listOf("Memory", "Toggle", "Piston"),
            builder = { buildTFlipFlop() }
        ),
        CircuitPreset(
            id = "and_gate",
            title = "Dual-Input AND Gate",
            category = "Logic Gates",
            description = "Fundamental digital logic gate. Output activates only when both Lever A and Lever B are powered ON.",
            difficulty = "Beginner",
            tags = listOf("Logic", "AND", "Torch"),
            builder = { buildAndGate() }
        ),
        CircuitPreset(
            id = "xor_gate",
            title = "XOR Gate (Exclusive OR)",
            category = "Logic Gates",
            description = "Dual-switch logic circuit. Output activates when exactly one input is ON, and turns OFF when both are ON or both OFF.",
            difficulty = "Intermediate",
            tags = listOf("Logic", "XOR", "Two-Way"),
            builder = { buildXorGate() }
        ),
        CircuitPreset(
            id = "piston_door_2x2",
            title = "2x2 Flush Piston Door",
            category = "Mechanisms & Doors",
            description = "Classic Bedrock-compatible 2x2 secret piston door using sticky pistons, redstone torches, and synchronizing repeaters.",
            difficulty = "Advanced",
            tags = listOf("Door", "Piston", "Bedrock"),
            builder = { buildPistonDoor2x2() }
        ),
        CircuitPreset(
            id = "pulse_extender",
            title = "Comparator Pulse Extender",
            category = "Timers & Delays",
            description = "Fades redstone signal gradually using dual comparators in a feedback loop, extending brief button pulses for several seconds.",
            difficulty = "Intermediate",
            tags = listOf("Comparator", "Fader", "Pulse"),
            builder = { buildPulseExtender() }
        ),
        CircuitPreset(
            id = "rs_nor_latch",
            title = "RS-NOR Latch Memory",
            category = "Memory & Logic",
            description = "1-bit static memory cell with distinct SET and RESET input lines built from cross-coupled inverting torches.",
            difficulty = "Beginner",
            tags = listOf("Memory", "Latch", "1-Bit"),
            builder = { buildRsNorLatch() }
        )
    )

    private fun buildRepeaterClock(): CircuitGrid {
        val grid = CircuitGrid()
        // Stone foundation
        for (x in 0..3) {
            for (y in 0..3) {
                grid.set(BlockState(BlockCoord(x, y, 0), BlockType.STONE))
            }
        }
        // Repeaters in a loop on layer 1
        grid.set(BlockState(BlockCoord(1, 0, 1), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 2))
        grid.set(BlockState(BlockCoord(3, 1, 1), BlockType.REDSTONE_REPEATER, facing = Facing.SOUTH, delay = 2))
        grid.set(BlockState(BlockCoord(2, 3, 1), BlockType.REDSTONE_REPEATER, facing = Facing.WEST, delay = 2))
        grid.set(BlockState(BlockCoord(0, 2, 1), BlockType.REDSTONE_REPEATER, facing = Facing.NORTH, delay = 2))

        // Corner redstone wire connections
        grid.set(BlockState(BlockCoord(0, 0, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(2, 0, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(3, 0, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(3, 2, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(3, 3, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(1, 3, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(0, 3, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(0, 1, 1), BlockType.REDSTONE_WIRE))

        // Starter button and indicator lamp
        grid.set(BlockState(BlockCoord(4, 1, 1), BlockType.STONE_BUTTON, facing = Facing.WEST))
        grid.set(BlockState(BlockCoord(1, 1, 1), BlockType.REDSTONE_LAMP))
        grid.set(BlockState(BlockCoord(1, 2, 1), BlockType.REDSTONE_WIRE))
        return grid
    }

    private fun buildTFlipFlop(): CircuitGrid {
        val grid = CircuitGrid()
        for (x in 0..4) {
            for (y in 0..2) {
                grid.set(BlockState(BlockCoord(x, y, 0), BlockType.STONE))
            }
        }
        // Input button
        grid.set(BlockState(BlockCoord(0, 1, 1), BlockType.STONE_BUTTON, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(1, 1, 1), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 1))
        // Sticky piston facing east
        grid.set(BlockState(BlockCoord(2, 1, 1), BlockType.STICKY_PISTON, facing = Facing.EAST))
        // Redstone block pushed by piston
        grid.set(BlockState(BlockCoord(3, 1, 1), BlockType.REDSTONE_BLOCK))
        // Output wire and lamp
        grid.set(BlockState(BlockCoord(4, 1, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(4, 0, 1), BlockType.REDSTONE_LAMP))
        return grid
    }

    private fun buildAndGate(): CircuitGrid {
        val grid = CircuitGrid()
        for (x in 0..4) {
            for (y in 0..2) {
                grid.set(BlockState(BlockCoord(x, y, 0), BlockType.STONE))
            }
        }
        // Levers A and B
        grid.set(BlockState(BlockCoord(0, 0, 1), BlockType.LEVER, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(0, 2, 1), BlockType.LEVER, facing = Facing.EAST))

        // Supporting blocks with torches
        grid.set(BlockState(BlockCoord(1, 0, 1), BlockType.STONE))
        grid.set(BlockState(BlockCoord(1, 2, 1), BlockType.STONE))
        grid.set(BlockState(BlockCoord(1, 0, 2), BlockType.REDSTONE_TORCH, facing = Facing.UP))
        grid.set(BlockState(BlockCoord(1, 2, 2), BlockType.REDSTONE_TORCH, facing = Facing.UP))

        // Connecting wire on central line
        grid.set(BlockState(BlockCoord(1, 1, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(2, 1, 1), BlockType.STONE))
        grid.set(BlockState(BlockCoord(2, 1, 2), BlockType.REDSTONE_TORCH, facing = Facing.UP))

        // Output line
        grid.set(BlockState(BlockCoord(3, 1, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(4, 1, 1), BlockType.REDSTONE_LAMP))
        return grid
    }

    private fun buildXorGate(): CircuitGrid {
        val grid = CircuitGrid()
        for (x in 0..5) {
            for (y in 0..4) {
                grid.set(BlockState(BlockCoord(x, y, 0), BlockType.STONE))
            }
        }
        grid.set(BlockState(BlockCoord(0, 1, 1), BlockType.LEVER, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(0, 3, 1), BlockType.LEVER, facing = Facing.EAST))

        grid.set(BlockState(BlockCoord(1, 1, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(1, 3, 1), BlockType.REDSTONE_WIRE))

        grid.set(BlockState(BlockCoord(2, 1, 1), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 1))
        grid.set(BlockState(BlockCoord(2, 3, 1), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 1))

        grid.set(BlockState(BlockCoord(3, 2, 1), BlockType.REDSTONE_TORCH, facing = Facing.UP))
        grid.set(BlockState(BlockCoord(3, 1, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(3, 3, 1), BlockType.REDSTONE_WIRE))

        grid.set(BlockState(BlockCoord(4, 2, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(5, 2, 1), BlockType.REDSTONE_LAMP))
        return grid
    }

    private fun buildPistonDoor2x2(): CircuitGrid {
        val grid = CircuitGrid()
        // Foundation
        for (x in 0..5) {
            for (y in 0..3) {
                grid.set(BlockState(BlockCoord(x, y, 0), BlockType.STONE))
            }
        }
        // Sticky pistons facing inward
        grid.set(BlockState(BlockCoord(1, 1, 1), BlockType.STICKY_PISTON, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(1, 2, 1), BlockType.STICKY_PISTON, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(4, 1, 1), BlockType.STICKY_PISTON, facing = Facing.WEST))
        grid.set(BlockState(BlockCoord(4, 2, 1), BlockType.STICKY_PISTON, facing = Facing.WEST))

        // Door door blocks
        grid.set(BlockState(BlockCoord(2, 1, 1), BlockType.STONE))
        grid.set(BlockState(BlockCoord(2, 2, 1), BlockType.STONE))
        grid.set(BlockState(BlockCoord(3, 1, 1), BlockType.STONE))
        grid.set(BlockState(BlockCoord(3, 2, 1), BlockType.STONE))

        // Control lever and activation wiring
        grid.set(BlockState(BlockCoord(0, 0, 1), BlockType.LEVER, facing = Facing.EAST, isActive = true, power = 15))
        grid.set(BlockState(BlockCoord(1, 0, 1), BlockType.REDSTONE_WIRE, power = 15))
        grid.set(BlockState(BlockCoord(2, 0, 1), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 1))
        grid.set(BlockState(BlockCoord(3, 0, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(4, 0, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(0, 1, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(5, 1, 1), BlockType.REDSTONE_WIRE))
        return grid
    }

    private fun buildPulseExtender(): CircuitGrid {
        val grid = CircuitGrid()
        for (x in 0..4) {
            for (y in 0..2) {
                grid.set(BlockState(BlockCoord(x, y, 0), BlockType.STONE))
            }
        }
        // Input button
        grid.set(BlockState(BlockCoord(0, 1, 1), BlockType.STONE_BUTTON, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(1, 1, 1), BlockType.REDSTONE_WIRE))

        // Comparators in anti-parallel feedback loop
        grid.set(BlockState(BlockCoord(2, 0, 1), BlockType.REDSTONE_COMPARATOR, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(2, 2, 1), BlockType.REDSTONE_COMPARATOR, facing = Facing.WEST))

        // Wire loop
        grid.set(BlockState(BlockCoord(3, 0, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(3, 1, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(3, 2, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(1, 0, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(1, 2, 1), BlockType.REDSTONE_WIRE))

        // Output lamp
        grid.set(BlockState(BlockCoord(4, 1, 1), BlockType.REDSTONE_LAMP))
        return grid
    }

    private fun buildRsNorLatch(): CircuitGrid {
        val grid = CircuitGrid()
        for (x in 0..4) {
            for (y in 0..2) {
                grid.set(BlockState(BlockCoord(x, y, 0), BlockType.STONE))
            }
        }
        // Set & Reset Buttons
        grid.set(BlockState(BlockCoord(0, 0, 1), BlockType.STONE_BUTTON, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(0, 2, 1), BlockType.STONE_BUTTON, facing = Facing.EAST))

        // Blocks with cross-inverting torches
        grid.set(BlockState(BlockCoord(1, 0, 1), BlockType.STONE))
        grid.set(BlockState(BlockCoord(1, 2, 1), BlockType.STONE))
        grid.set(BlockState(BlockCoord(2, 0, 1), BlockType.REDSTONE_TORCH, facing = Facing.EAST, isActive = true))
        grid.set(BlockState(BlockCoord(2, 2, 1), BlockType.REDSTONE_TORCH, facing = Facing.EAST, isActive = false))

        // Cross wires
        grid.set(BlockState(BlockCoord(2, 1, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(3, 0, 1), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(3, 2, 1), BlockType.REDSTONE_WIRE))

        // Output lamp
        grid.set(BlockState(BlockCoord(4, 0, 1), BlockType.REDSTONE_LAMP))
        return grid
    }
}
