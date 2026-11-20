package com.redstonelab.app.model

enum class BlockCategory(val label: String) {
    POWER("Power Sources"),
    TRANSMISSION("Transmission & Logic"),
    ACTUATOR("Actuators & Mechanics"),
    SOLID("Solid & Utility")
}

enum class BlockType(
    val displayName: String,
    val category: BlockCategory,
    val bedrockIdentifier: String,
    val isSolidConductor: Boolean = false,
    val isInteractive: Boolean = false,
    val isDirectional: Boolean = false,
    val maxSignalEmission: Int = 0
) {
    AIR(
        displayName = "Air",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:air"
    ),

    STONE(
        displayName = "Smooth Stone",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:stone",
        isSolidConductor = true
    ),

    GLASS(
        displayName = "Tinted Glass",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:glass",
        isSolidConductor = false
    ),

    REDSTONE_WIRE(
        displayName = "Redstone Dust",
        category = BlockCategory.TRANSMISSION,
        bedrockIdentifier = "minecraft:redstone_wire"
    ),

    REDSTONE_TORCH(
        displayName = "Redstone Torch",
        category = BlockCategory.POWER,
        bedrockIdentifier = "minecraft:redstone_torch",
        isDirectional = true,
        maxSignalEmission = 15
    ),

    REDSTONE_REPEATER(
        displayName = "Repeater",
        category = BlockCategory.TRANSMISSION,
        bedrockIdentifier = "minecraft:unpowered_repeater",
        isInteractive = true,
        isDirectional = true,
        maxSignalEmission = 15
    ),

    REDSTONE_COMPARATOR(
        displayName = "Comparator",
        category = BlockCategory.TRANSMISSION,
        bedrockIdentifier = "minecraft:unpowered_comparator",
        isInteractive = true,
        isDirectional = true
    ),

    LEVER(
        displayName = "Lever",
        category = BlockCategory.POWER,
        bedrockIdentifier = "minecraft:lever",
        isInteractive = true,
        isDirectional = true,
        maxSignalEmission = 15
    ),

    STONE_BUTTON(
        displayName = "Stone Button",
        category = BlockCategory.POWER,
        bedrockIdentifier = "minecraft:stone_button",
        isInteractive = true,
        isDirectional = true,
        maxSignalEmission = 15
    ),

    REDSTONE_BLOCK(
        displayName = "Redstone Block",
        category = BlockCategory.POWER,
        bedrockIdentifier = "minecraft:redstone_block",
        isSolidConductor = true,
        maxSignalEmission = 15
    ),

    REDSTONE_LAMP(
        displayName = "Redstone Lamp",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:redstone_lamp",
        isSolidConductor = true
    ),

    PISTON(
        displayName = "Piston",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:piston",
        isDirectional = true
    ),

    STICKY_PISTON(
        displayName = "Sticky Piston",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:sticky_piston",
        isDirectional = true
    ),

    OBSERVER(
        displayName = "Observer",
        category = BlockCategory.ACTUATOR,
        bedrockIdentifier = "minecraft:observer",
        isDirectional = true,
        maxSignalEmission = 15
    ),

    TARGET_BLOCK(
        displayName = "Target Block",
        category = BlockCategory.SOLID,
        bedrockIdentifier = "minecraft:target",
        isSolidConductor = true
    );

    val isDust: Boolean get() = this == REDSTONE_WIRE
    val isTorch: Boolean get() = this == REDSTONE_TORCH
    val isRepeater: Boolean get() = this == REDSTONE_REPEATER
    val isComparator: Boolean get() = this == REDSTONE_COMPARATOR
    val isPiston: Boolean get() = this == PISTON || this == STICKY_PISTON
}
