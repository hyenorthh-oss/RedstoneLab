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
            title = "Oscilador com Repetidores",
            category = "Relógios e Timers",
            description = "Relógio contínuo clássico gerando pulsos oscilantes com 4 repetidores em loop fechado.",
            difficulty = "Iniciante",
            tags = listOf("Clock", "Oscilador", "Repetidor"),
            builder = { buildRepeaterClock() }
        ),
        CircuitPreset(
            id = "copper_bulb_t_flip_flop",
            title = "T-Flip-Flop com Lâmpada de Cobre",
            category = "Memória e Lógica",
            description = "O mais moderno T-Flip-Flop nativo do Minecraft 1.21+ usando Lâmpada de Cobre e Comparador.",
            difficulty = "Iniciante",
            tags = listOf("Copper Bulb", "1.21", "Toggle"),
            builder = { buildCopperBulbTFlipFlop() }
        ),
        CircuitPreset(
            id = "piston_t_flip_flop",
            title = "T-Flip-Flop com Pistão Grudento",
            category = "Memória e Lógica",
            description = "Converte pulso momentâneo de botão em alternância permanente ON/OFF com pistão e bloco de redstone.",
            difficulty = "Intermediário",
            tags = listOf("Piston", "Memória", "Toggle"),
            builder = { buildPistonTFlipFlop() }
        ),
        CircuitPreset(
            id = "and_gate",
            title = "Porta Lógica AND (E)",
            category = "Portas Lógicas",
            description = "Circuito de decisão binária: a saída só acende quando a Alavanca A e a Alavanca B estiverem ligadas.",
            difficulty = "Iniciante",
            tags = listOf("Lógica", "AND", "Tocha"),
            builder = { buildAndGate() }
        ),
        CircuitPreset(
            id = "xor_gate",
            title = "Porta Lógica XOR (OU Exclusivo)",
            category = "Portas Lógicas",
            description = "Interruptores Two-Way: acende se exatamente uma alavanca estiver ligada; desliga se ambas estiverem iguais.",
            difficulty = "Intermediário",
            tags = listOf("Lógica", "XOR", "Two-Way"),
            builder = { buildXorGate() }
        ),
        CircuitPreset(
            id = "piston_door_2x2",
            title = "Porta de Pistão 2x2 Oculta",
            category = "Mecanismos e Portas",
            description = "Porta secreta de passagem dupla 2x2 compatível com o comportamento nativo de Bedrock.",
            difficulty = "Avançado",
            tags = listOf("Porta", "Pistão", "Bedrock"),
            builder = { buildPistonDoor2x2() }
        ),
        CircuitPreset(
            id = "pulse_extender",
            title = "Extensor de Pulso com Comparadores",
            category = "Timers e Delays",
            description = "Prolonga a duração de um pulso breve de botão desacelerando a perda de energia em loop duplo.",
            difficulty = "Intermediário",
            tags = listOf("Comparador", "Fader", "Pulso"),
            builder = { buildPulseExtender() }
        )
    )

    private fun buildRepeaterClock(): CircuitGrid {
        val grid = CircuitGrid()
        // Base de pedra lisa na camada Y: 0
        for (x in 2..7) {
            for (z in 2..7) {
                grid.set(BlockState(BlockCoord(x, 0, z), BlockType.SMOOTH_STONE))
            }
        }

        // Repetidores e fios na camada Y: 1
        grid.set(BlockState(BlockCoord(3, 1, 3), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 2))
        grid.set(BlockState(BlockCoord(6, 1, 3), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(6, 1, 4), BlockType.REDSTONE_REPEATER, facing = Facing.SOUTH, delay = 2))
        grid.set(BlockState(BlockCoord(6, 1, 6), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(5, 1, 6), BlockType.REDSTONE_REPEATER, facing = Facing.WEST, delay = 2))
        grid.set(BlockState(BlockCoord(3, 1, 6), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(3, 1, 5), BlockType.REDSTONE_REPEATER, facing = Facing.NORTH, delay = 2))
        grid.set(BlockState(BlockCoord(3, 1, 4), BlockType.REDSTONE_WIRE))

        // Alavanca de controle e Lâmpada de saída
        grid.set(BlockState(BlockCoord(2, 1, 4), BlockType.LEVER, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(7, 1, 4), BlockType.REDSTONE_LAMP))
        grid.set(BlockState(BlockCoord(7, 1, 3), BlockType.REDSTONE_WIRE))
        return grid
    }

    private fun buildCopperBulbTFlipFlop(): CircuitGrid {
        val grid = CircuitGrid()
        for (x in 3..7) {
            for (z in 3..5) {
                grid.set(BlockState(BlockCoord(x, 0, z), BlockType.SMOOTH_STONE))
            }
        }
        // Botão de entrada na camada Y: 1
        grid.set(BlockState(BlockCoord(3, 1, 4), BlockType.STONE_BUTTON, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(4, 1, 4), BlockType.REDSTONE_WIRE))
        // Lâmpada de Cobre
        grid.set(BlockState(BlockCoord(5, 1, 4), BlockType.COPPER_BULB))
        // Comparador lendo a Lâmpada de Cobre
        grid.set(BlockState(BlockCoord(6, 1, 4), BlockType.REDSTONE_COMPARATOR, facing = Facing.EAST))
        // Lâmpada de saída
        grid.set(BlockState(BlockCoord(7, 1, 4), BlockType.REDSTONE_LAMP))
        return grid
    }

    private fun buildPistonTFlipFlop(): CircuitGrid {
        val grid = CircuitGrid()
        for (x in 2..7) {
            for (z in 3..5) {
                grid.set(BlockState(BlockCoord(x, 0, z), BlockType.SMOOTH_STONE))
            }
        }
        grid.set(BlockState(BlockCoord(2, 1, 4), BlockType.STONE_BUTTON, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(3, 1, 4), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 1))
        grid.set(BlockState(BlockCoord(4, 1, 4), BlockType.STICKY_PISTON, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(5, 1, 4), BlockType.REDSTONE_BLOCK))
        grid.set(BlockState(BlockCoord(6, 1, 4), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(7, 1, 4), BlockType.REDSTONE_LAMP))
        return grid
    }

    private fun buildAndGate(): CircuitGrid {
        val grid = CircuitGrid()
        for (x in 2..7) {
            for (z in 2..6) {
                grid.set(BlockState(BlockCoord(x, 0, z), BlockType.SMOOTH_STONE))
            }
        }
        // Entradas A e B
        grid.set(BlockState(BlockCoord(2, 1, 3), BlockType.LEVER, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(2, 1, 5), BlockType.LEVER, facing = Facing.EAST))

        grid.set(BlockState(BlockCoord(3, 1, 3), BlockType.SMOOTH_STONE))
        grid.set(BlockState(BlockCoord(3, 1, 5), BlockType.SMOOTH_STONE))
        grid.set(BlockState(BlockCoord(3, 2, 3), BlockType.REDSTONE_TORCH, facing = Facing.UP))
        grid.set(BlockState(BlockCoord(3, 2, 5), BlockType.REDSTONE_TORCH, facing = Facing.UP))

        grid.set(BlockState(BlockCoord(3, 1, 4), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(4, 1, 4), BlockType.SMOOTH_STONE))
        grid.set(BlockState(BlockCoord(4, 2, 4), BlockType.REDSTONE_TORCH, facing = Facing.UP))

        grid.set(BlockState(BlockCoord(5, 1, 4), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(6, 1, 4), BlockType.REDSTONE_LAMP))
        return grid
    }

    private fun buildXorGate(): CircuitGrid {
        val grid = CircuitGrid()
        for (x in 1..7) {
            for (z in 1..7) {
                grid.set(BlockState(BlockCoord(x, 0, z), BlockType.SMOOTH_STONE))
            }
        }
        grid.set(BlockState(BlockCoord(2, 1, 3), BlockType.LEVER, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(2, 1, 5), BlockType.LEVER, facing = Facing.EAST))

        grid.set(BlockState(BlockCoord(3, 1, 3), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(3, 1, 5), BlockType.REDSTONE_WIRE))

        grid.set(BlockState(BlockCoord(4, 1, 3), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 1))
        grid.set(BlockState(BlockCoord(4, 1, 5), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 1))

        grid.set(BlockState(BlockCoord(5, 1, 4), BlockType.REDSTONE_TORCH, facing = Facing.UP))
        grid.set(BlockState(BlockCoord(5, 1, 3), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(5, 1, 5), BlockType.REDSTONE_WIRE))

        grid.set(BlockState(BlockCoord(6, 1, 4), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(7, 1, 4), BlockType.REDSTONE_LAMP))
        return grid
    }

    private fun buildPistonDoor2x2(): CircuitGrid {
        val grid = CircuitGrid()
        for (x in 2..8) {
            for (z in 2..6) {
                grid.set(BlockState(BlockCoord(x, 0, z), BlockType.SMOOTH_STONE))
            }
        }
        // Pistões grudentos
        grid.set(BlockState(BlockCoord(3, 1, 3), BlockType.STICKY_PISTON, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(3, 1, 4), BlockType.STICKY_PISTON, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(6, 1, 3), BlockType.STICKY_PISTON, facing = Facing.WEST))
        grid.set(BlockState(BlockCoord(6, 1, 4), BlockType.STICKY_PISTON, facing = Facing.WEST))

        // Blocos da porta
        grid.set(BlockState(BlockCoord(4, 1, 3), BlockType.IRON_BLOCK))
        grid.set(BlockState(BlockCoord(4, 1, 4), BlockType.IRON_BLOCK))
        grid.set(BlockState(BlockCoord(5, 1, 3), BlockType.IRON_BLOCK))
        grid.set(BlockState(BlockCoord(5, 1, 4), BlockType.IRON_BLOCK))

        // Alavanca e fiação de acionamento
        grid.set(BlockState(BlockCoord(2, 1, 2), BlockType.LEVER, facing = Facing.EAST, isActive = true, power = 15))
        grid.set(BlockState(BlockCoord(3, 1, 2), BlockType.REDSTONE_WIRE, power = 15))
        grid.set(BlockState(BlockCoord(4, 1, 2), BlockType.REDSTONE_REPEATER, facing = Facing.EAST, delay = 1))
        grid.set(BlockState(BlockCoord(5, 1, 2), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(6, 1, 2), BlockType.REDSTONE_WIRE))
        return grid
    }

    private fun buildPulseExtender(): CircuitGrid {
        val grid = CircuitGrid()
        for (x in 2..7) {
            for (z in 3..5) {
                grid.set(BlockState(BlockCoord(x, 0, z), BlockType.SMOOTH_STONE))
            }
        }
        grid.set(BlockState(BlockCoord(2, 1, 4), BlockType.STONE_BUTTON, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(3, 1, 4), BlockType.REDSTONE_WIRE))

        // Comparadores em loop antiparalelo
        grid.set(BlockState(BlockCoord(4, 1, 3), BlockType.REDSTONE_COMPARATOR, facing = Facing.EAST))
        grid.set(BlockState(BlockCoord(5, 1, 5), BlockType.REDSTONE_COMPARATOR, facing = Facing.WEST))

        grid.set(BlockState(BlockCoord(5, 1, 3), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(5, 1, 4), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(4, 1, 5), BlockType.REDSTONE_WIRE))
        grid.set(BlockState(BlockCoord(4, 1, 4), BlockType.REDSTONE_WIRE))

        grid.set(BlockState(BlockCoord(6, 1, 4), BlockType.REDSTONE_LAMP))
        return grid
    }
}
