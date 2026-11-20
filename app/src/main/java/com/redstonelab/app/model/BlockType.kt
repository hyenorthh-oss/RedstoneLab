package com.redstonelab.app.model

enum class BlockCategory(val label: String) {
    POWER("Fontes de Energia"),
    TRANSMISSION("Transmissão e Lógica"),
    ACTUATOR("Mecanismos e Saídas"),
    SOLID("Estruturais e Especiais")
}

enum class BlockType(
    val displayName: String,
    val category: BlockCategory,
    val bedrockIdentifier: String,
    val isSolidConductor: Boolean = false,
    val isInteractive: Boolean = false,
    val isDirectional: Boolean = false,
    val isDependent: Boolean = false, // Must be placed AFTER supporting solid blocks in .mcfunction
    val isImmovable: Boolean = false,  // Obsidian cannot be pushed/pulled by pistons
    val maxSignalEmission: Int = 0
) {
    // --- 1. FONTES DE ENERGIA ---
    LEVER(
        displayName = "Alavanca",
        category = BlockCategory.POWER,
        bedrockIdentifier = "minecraft:lever",
        isInteractive = true,
        isDirectional = true,
        isDependent = true,
        maxSignalEmission = 15
    ),

    STONE_BUTTON(
        displayName = "Botão de Pedra",
        category = BlockCategory.POWER,
        bedrockIdentifier = "minecraft:stone_button",
        isInteractive = true,
        isDirectional = true,
        isDependent = true,
        maxSignalEmission = 15
    ),

    REDSTONE_TORCH(
        displayName = "Tocha de Redstone",
        category = BlockCategory.POWER,
        bedrockIdentifier = "minecraft:redstone_torch",
        isDirectional = true,
        isDependent = true,
        maxSignalEmission = 15
    ),

    REDSTONE_BLOCK(
        displayName = "Bloco de Redstone",
        category = BlockCategory.POWER,
        bedrockIdentifier = "minecraft:redstone_block",
        isSolidConductor = true,
        maxSignalEmission = 15
    ),

    OBSERVER(
        displayName = "Observador",
        category = BlockCategory.POWER,
        bedrockIdentifier = "minecraft:observer",
        isDirectional = true,
        maxSignalEmission = 15
    ),

    STONE_PRESSURE_PLATE(
        displayName = "Placa de Pressão",
        category = BlockCategory.POWER,
        bedrockIdentifier = "minecraft:stone_pressure_plate",
        isInteractive = true,
        isDependent = true,
        maxSignalEmission = 15
    ),

    DAYLIGHT_DETECTOR(
        displayName = "Sensor de Luz Solar",
        category = BlockCategory.POWER,
        bedrockIdentifier = "minecraft:daylight_detector",
        isInteractive = true,
        maxSignalEmission = 15
    ),

    SCULK_SENSOR(
        displayName = "Sensor de Sculk",
        category = BlockCategory.POWER,
        bedrockIdentifier = "minecraft:sculk_sensor",
        isInteractive = true,
        maxSignalEmission = 15
    ),

    // --- 2. TRANSMISSÃO E LÓGICA ---
    REDSTONE_WIRE(
        displayName = "Pó de Redstone",
        category = BlockCategory.TRANSMISSION,
        bedrockIdentifier = "minecraft:redstone_wire",
        isDependent = true
    ),

    REDSTONE_REPEATER(
        displayName = "Repetidor",
        category = BlockCategory.TRANSMISSION,
        bedrockIdentifier = "minecraft:unpowered_repeater",
        isInteractive = true,
        isDirectional = true,
        isDependent = true,
        maxSignalEmission = 15
    ),

    REDSTONE_COMPARATOR(
        displayName = "Comparador",
        category = BlockCategory.TRANSMISSION,
        bedrockIdentifier = "minecraft:unpowered_comparator",
        isInteractive = true,
        isDirectional = true,
        isDependent = true
    ),

    TARGET_BLOCK(
        displayName = "Bloco Alvo",
        category = BlockCategory.TRANSMISSION,
        bedrockIdentifier = "minecraft:target",
        isSolidConductor = true
    ),

    // --- 3. MECANISMOS E SAÍDAS ---
    REDSTONE_LAMP(
        displayName = "Lâmpada de Redstone",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:redstone_lamp",
        isSolidConductor = true
    ),

    COPPER_BULB(
        displayName = "Lâmpada de Cobre (T-Flip-Flop)",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:copper_bulb",
        isSolidConductor = true
    ),

    PISTON(
        displayName = "Pistão",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:piston",
        isDirectional = true
    ),

    STICKY_PISTON(
        displayName = "Pistão Grudento",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:sticky_piston",
        isDirectional = true
    ),

    NOTE_BLOCK(
        displayName = "Bloco Musical",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:noteblock",
        isSolidConductor = true
    ),

    DISPENSER(
        displayName = "Ejetor",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:dispenser",
        isDirectional = true,
        isSolidConductor = true
    ),

    DROPPER(
        displayName = "Liberador",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:dropper",
        isDirectional = true,
        isSolidConductor = true
    ),

    HOPPER(
        displayName = "Funil",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:hopper",
        isDirectional = true
    ),

    IRON_DOOR(
        displayName = "Porta de Ferro",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:iron_door",
        isDirectional = true,
        isDependent = true
    ),

    IRON_TRAPDOOR(
        displayName = "Alçapão de Ferro",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:iron_trapdoor",
        isDirectional = true,
        isDependent = true
    ),

    BELL(
        displayName = "Sino",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:bell",
        isDirectional = true,
        isDependent = true
    ),

    TNT(
        displayName = "TNT",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:tnt"
    ),

    // --- 4. ESTRUTURAIS E ESPECIAIS ---
    SMOOTH_STONE(
        displayName = "Pedra Lisa",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:smooth_stone",
        isSolidConductor = true
    ),

    IRON_BLOCK(
        displayName = "Bloco de Ferro",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:iron_block",
        isSolidConductor = true
    ),

    RED_WOOL(
        displayName = "Lã Vermelha",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:wool",
        isSolidConductor = true
    ),

    BLUE_WOOL(
        displayName = "Lã Azul",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:wool",
        isSolidConductor = true
    ),

    GREEN_WOOL(
        displayName = "Lã Verde",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:wool",
        isSolidConductor = true
    ),

    YELLOW_WOOL(
        displayName = "Lã Amarela",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:wool",
        isSolidConductor = true
    ),

    GLASS(
        displayName = "Vidro (Transparente)",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:glass",
        isSolidConductor = false
    ),

    STONE_SLAB(
        displayName = "Laje Superior (Diodo Vertical)",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:stone_block_slab",
        isSolidConductor = false
    ),

    SLIME_BLOCK(
        displayName = "Bloco de Slime",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:slime"
    ),

    HONEY_BLOCK(
        displayName = "Bloco de Mel",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:honey_block"
    ),

    OBSIDIAN(
        displayName = "Obsidiana (Imóvel)",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:obsidian",
        isSolidConductor = true,
        isImmovable = true
    ),

    AIR(
        displayName = "Ar",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:air"
    );

    val isDust: Boolean get() = this == REDSTONE_WIRE
    val isTorch: Boolean get() = this == REDSTONE_TORCH
    val isRepeater: Boolean get() = this == REDSTONE_REPEATER
    val isComparator: Boolean get() = this == REDSTONE_COMPARATOR
    val isPiston: Boolean get() = this == PISTON || this == STICKY_PISTON
}
