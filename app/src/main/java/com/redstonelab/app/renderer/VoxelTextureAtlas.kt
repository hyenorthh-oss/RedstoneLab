package com.redstonelab.app.renderer

import android.graphics.Bitmap
import com.redstonelab.app.model.BlockType

/**
 * Atlas de Texturas Voxel Pixel-Art 16x16 Procedurais para RedstoneLab.
 * Gera padrões detalhados com ruído de pixel, bordas chanfradas e sombreamento
 * autêntico de Minecraft para todos os blocos do catálogo.
 */
object VoxelTextureAtlas {

    private val cache = HashMap<String, Bitmap>()

    fun getTexture(key: String): Bitmap? {
        val cached = cache[key]
        if (cached != null) return cached

        val pixels = generatePixels(key)
        return try {
            val bmp = Bitmap.createBitmap(pixels, 16, 16, Bitmap.Config.ARGB_8888)
            cache[key] = bmp
            bmp
        } catch (_: Throwable) {
            null
        }
    }

    fun getBlockTopKey(type: BlockType, isActive: Boolean, power: Int): String {
        return when (type) {
            BlockType.SMOOTH_STONE, BlockType.STONE_SLAB -> "SMOOTH_STONE"
            BlockType.IRON_BLOCK -> "IRON_BLOCK"
            BlockType.RED_WOOL -> "RED_WOOL"
            BlockType.BLUE_WOOL -> "BLUE_WOOL"
            BlockType.GREEN_WOOL -> "GREEN_WOOL"
            BlockType.YELLOW_WOOL -> "YELLOW_WOOL"
            BlockType.GLASS -> "GLASS"
            BlockType.SLIME_BLOCK -> "SLIME_BLOCK"
            BlockType.HONEY_BLOCK -> "HONEY_BLOCK"
            BlockType.OBSIDIAN -> "OBSIDIAN"
            BlockType.REDSTONE_BLOCK -> "REDSTONE_BLOCK"
            BlockType.TARGET_BLOCK -> "TARGET_BLOCK"
            BlockType.REDSTONE_LAMP -> if (isActive || power > 0) "LAMP_ON" else "LAMP_OFF"
            BlockType.COPPER_BULB -> if (isActive) "COPPER_BULB_ON" else "COPPER_BULB_OFF"
            BlockType.PISTON -> "PISTON_TOP"
            BlockType.STICKY_PISTON -> "STICKY_PISTON_TOP"
            BlockType.OBSERVER -> "OBSERVER_TOP"
            BlockType.NOTE_BLOCK -> "NOTE_BLOCK"
            BlockType.DISPENSER -> "DISPENSER_TOP"
            BlockType.DROPPER -> "DROPPER_TOP"
            BlockType.HOPPER -> "HOPPER_TOP"
            BlockType.IRON_DOOR, BlockType.IRON_TRAPDOOR -> "IRON_BLOCK"
            BlockType.BELL -> "BELL_TOP"
            BlockType.TNT -> "TNT_TOP"
            BlockType.REDSTONE_REPEATER -> "REPEATER_BASE"
            BlockType.REDSTONE_COMPARATOR -> "COMPARATOR_BASE"
            BlockType.DAYLIGHT_DETECTOR -> "DAYLIGHT_DETECTOR"
            BlockType.SCULK_SENSOR -> "SCULK_SENSOR"
            else -> "SMOOTH_STONE"
        }
    }

    fun getBlockSideKey(type: BlockType, isActive: Boolean, power: Int): String {
        return when (type) {
            BlockType.TNT -> "TNT_SIDE"
            BlockType.PISTON, BlockType.STICKY_PISTON -> "PISTON_SIDE"
            BlockType.OBSERVER -> "OBSERVER_SIDE"
            BlockType.DISPENSER -> "DISPENSER_SIDE"
            BlockType.DROPPER -> "DROPPER_SIDE"
            BlockType.NOTE_BLOCK -> "NOTE_BLOCK_SIDE"
            BlockType.REDSTONE_LAMP -> if (isActive || power > 0) "LAMP_ON" else "LAMP_OFF"
            BlockType.COPPER_BULB -> if (isActive) "COPPER_BULB_ON" else "COPPER_BULB_OFF"
            else -> getBlockTopKey(type, isActive, power)
        }
    }

    private fun generatePixels(key: String): IntArray {
        val pixels = IntArray(256)
        when (key) {
            "FLOOR_TILE" -> {
                // Ladrilho industrial de pedra escura com divisória e relevo
                val base = 0xFF23262D.toInt()
                val light = 0xFF2D313A.toInt()
                val dark = 0xFF191B20.toInt()
                val border = 0xFF15161A.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isBorder = x == 0 || y == 0 || x == 15 || y == 15
                        val isBevel = x == 1 || y == 1
                        pixels[y * 16 + x] = when {
                            isBorder -> border
                            isBevel -> light
                            (x + y * 7) % 5 == 0 -> dark
                            (x * 3 + y) % 7 == 0 -> light
                            else -> base
                        }
                    }
                }
            }

            "SMOOTH_STONE" -> {
                val base = 0xFF6C737F.toInt()
                val light = 0xFF7D8593.toInt()
                val dark = 0xFF585E6A.toInt()
                val border = 0xFF494E58.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isBorder = x == 0 || y == 0 || x == 15 || y == 15
                        val isBevel = x == 1 || y == 1
                        pixels[y * 16 + x] = when {
                            isBorder -> border
                            isBevel -> light
                            (x * 5 + y * 3) % 7 == 0 -> dark
                            (x + y * 11) % 13 == 0 -> light
                            else -> base
                        }
                    }
                }
            }

            "IRON_BLOCK" -> {
                val base = 0xFFDFE4EA.toInt()
                val light = 0xFFF5F7FA.toInt()
                val dark = 0xFFB4BAC4.toInt()
                val rivet = 0xFF68707C.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isRivet = (x in 2..3 && y in 2..3) ||
                                (x in 12..13 && y in 2..3) ||
                                (x in 2..3 && y in 12..13) ||
                                (x in 12..13 && y in 12..13)
                        val isInnerBorder = x == 1 || y == 1 || x == 14 || y == 14
                        pixels[y * 16 + x] = when {
                            isRivet -> rivet
                            isInnerBorder -> dark
                            x == 0 || y == 0 -> light
                            x == 15 || y == 15 -> dark
                            else -> base
                        }
                    }
                }
            }

            "REDSTONE_BLOCK" -> {
                val base = 0xFFD31024.toInt()
                val deep = 0xFF8C0A17.toInt()
                val bright = 0xFFFF3344.toInt()
                val crystal = 0xFFFFB3B8.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isCrystal = (x == 4 && y == 4) || (x == 11 && y == 5) || (x == 6 && y == 11) || (x == 12 && y == 12)
                        val isBright = (x in 3..5 && y in 3..5) || (x in 10..12 && y in 4..6)
                        pixels[y * 16 + x] = when {
                            isCrystal -> crystal
                            isBright -> bright
                            (x + y * 3) % 4 == 0 -> deep
                            else -> base
                        }
                    }
                }
            }

            "OBSIDIAN" -> {
                val base = 0xFF140D1F.toInt()
                val deep = 0xFF09060E.toInt()
                val purple = 0xFF6B3BA7.toInt()
                val magenta = 0xFFB84AD6.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isFleck = (x == 3 && y == 7) || (x == 12 && y == 4) || (x == 8 && y == 12)
                        val isPurple = (x * 7 + y * 5) % 11 == 0
                        pixels[y * 16 + x] = when {
                            isFleck -> magenta
                            isPurple -> purple
                            (x + y) % 3 == 0 -> deep
                            else -> base
                        }
                    }
                }
            }

            "GLASS" -> {
                val frame = 0xFF6FA8DC.toInt()
                val glass = 0x334A90E2.toInt()
                val streak = 0xAAFFFFFF.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isBorder = x == 0 || y == 0 || x == 15 || y == 15
                        val isStreak = (x - y == 2 || x - y == 3) && (x in 4..12)
                        pixels[y * 16 + x] = when {
                            isBorder -> frame
                            isStreak -> streak
                            else -> glass
                        }
                    }
                }
            }

            "SLIME_BLOCK" -> {
                val base = 0xCC58CC40.toInt()
                val core = 0xFF2A851B.toInt()
                val border = 0xEE7DE26A.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isBorder = x == 0 || y == 0 || x == 15 || y == 15
                        val isCore = x in 5..10 && y in 5..10
                        pixels[y * 16 + x] = when {
                            isCore -> core
                            isBorder -> border
                            else -> base
                        }
                    }
                }
            }

            "HONEY_BLOCK" -> {
                val base = 0xCCFFB300.toInt()
                val dark = 0xCCB26A00.toInt()
                val highlight = 0xEEFFE082.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isHighlight = x in 2..5 && y in 2..4
                        pixels[y * 16 + x] = when {
                            isHighlight -> highlight
                            (x + y) % 4 == 0 -> dark
                            else -> base
                        }
                    }
                }
            }

            "LAMP_ON" -> {
                val wood = 0xFF3D2314.toInt()
                val glow = 0xFFFFC107.toInt()
                val core = 0xFFFFF9C4.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isWood = x == 0 || y == 0 || x == 15 || y == 15 || x == 8 || y == 8
                        val isCore = (x in 3..5 && y in 3..5) || (x in 10..12 && y in 3..5) ||
                                (x in 3..5 && y in 10..12) || (x in 10..12 && y in 10..12)
                        pixels[y * 16 + x] = when {
                            isWood -> wood
                            isCore -> core
                            else -> glow
                        }
                    }
                }
            }

            "LAMP_OFF" -> {
                val wood = 0xFF3D2314.toInt()
                val offGlow = 0xFF5D391A.toInt()
                val dark = 0xFF2A150A.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isWood = x == 0 || y == 0 || x == 15 || y == 15 || x == 8 || y == 8
                        pixels[y * 16 + x] = when {
                            isWood -> wood
                            (x + y) % 2 == 0 -> offGlow
                            else -> dark
                        }
                    }
                }
            }

            "COPPER_BULB_ON" -> {
                val copper = 0xFF3B7E6D.toInt()
                val darkCopper = 0xFF245348.toInt()
                val flame = 0xFFFF9800.toInt()
                val hotCore = 0xFFFFE082.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isFrame = x in 0..2 || x in 13..15 || y in 0..2 || y in 13..15
                        val isCore = x in 6..9 && y in 6..9
                        pixels[y * 16 + x] = when {
                            isCore -> hotCore
                            !isFrame -> flame
                            (x + y) % 3 == 0 -> darkCopper
                            else -> copper
                        }
                    }
                }
            }

            "COPPER_BULB_OFF" -> {
                val copper = 0xFF3B7E6D.toInt()
                val darkCopper = 0xFF245348.toInt()
                val filamentOff = 0xFF1B3D34.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isFrame = x in 0..2 || x in 13..15 || y in 0..2 || y in 13..15
                        pixels[y * 16 + x] = when {
                            !isFrame -> filamentOff
                            (x + y) % 3 == 0 -> darkCopper
                            else -> copper
                        }
                    }
                }
            }

            "PISTON_TOP" -> {
                val wood = 0xFF9E6E38.toInt()
                val grain = 0xFF7A5123.toInt()
                val border = 0xFF5C3C16.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isBorder = x == 0 || y == 0 || x == 15 || y == 15
                        val isGrain = x == 4 || x == 8 || x == 12 || (x + y * 2) % 7 == 0
                        pixels[y * 16 + x] = when {
                            isBorder -> border
                            isGrain -> grain
                            else -> wood
                        }
                    }
                }
            }

            "STICKY_PISTON_TOP" -> {
                val woodPixels = generatePixels("PISTON_TOP")
                val slime = 0xFF4ADE80.toInt()
                val slimeDark = 0xFF15803D.toInt()

                for (i in 0 until 256) {
                    pixels[i] = woodPixels[i]
                }
                // Central slime splash
                for (y in 4..11) {
                    for (x in 4..11) {
                        val isSlime = (x - 7.5) * (x - 7.5) + (y - 7.5) * (y - 7.5) <= 14
                        if (isSlime) {
                            pixels[y * 16 + x] = if ((x + y) % 3 == 0) slimeDark else slime
                        }
                    }
                }
            }

            "PISTON_SIDE" -> {
                val stone = 0xFF6C737F.toInt()
                val wood = 0xFF9E6E38.toInt()
                val iron = 0xFFB4BAC4.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        pixels[y * 16 + x] = when {
                            y in 0..3 -> wood
                            y == 4 -> iron
                            else -> stone
                        }
                    }
                }
            }

            "OBSERVER_TOP" -> {
                val stone = 0xFF494E58.toInt()
                val dark = 0xFF353941.toInt()
                val chevron = 0xFFB4BAC4.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        // Chevron pointing up
                        val isChevron = (y in 5..7 && (x == y || x == 15 - y)) || (x in 7..8 && y in 7..12)
                        pixels[y * 16 + x] = when {
                            isChevron -> chevron
                            (x + y) % 3 == 0 -> dark
                            else -> stone
                        }
                    }
                }
            }

            "OBSERVER_SIDE" -> {
                val stone = 0xFF494E58.toInt()
                val eye = 0xFFEF4444.toInt()
                val redstone = 0xFFB91C1C.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isEye = x in 1..3 && y in 6..9
                        val isNode = x in 12..14 && y in 7..8
                        pixels[y * 16 + x] = when {
                            isEye -> eye
                            isNode -> redstone
                            else -> stone
                        }
                    }
                }
            }

            "NOTE_BLOCK" -> {
                val wood = 0xFF854D0E.toInt()
                val darkWood = 0xFF58310B.toInt()
                val note = 0xFFFDE68A.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isBorder = x == 0 || y == 0 || x == 15 || y == 15
                        // Musical note shape in center
                        val isNote = (x in 6..7 && y in 10..12) || (x == 7 && y in 5..9) || (x in 8..10 && y == 5)
                        pixels[y * 16 + x] = when {
                            isNote -> note
                            isBorder -> darkWood
                            else -> wood
                        }
                    }
                }
            }

            "NOTE_BLOCK_SIDE" -> {
                val wood = 0xFF854D0E.toInt()
                val darkWood = 0xFF58310B.toInt()
                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isBorder = x == 0 || y == 0 || x == 15 || y == 15
                        pixels[y * 16 + x] = if (isBorder) darkWood else wood
                    }
                }
            }

            "TNT_TOP" -> {
                val red = 0xFFDC2626.toInt()
                val darkRed = 0xFF991B1B.toInt()
                val fuse = 0xFF475569.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isFuse = x in 7..8 && y in 7..8
                        pixels[y * 16 + x] = when {
                            isFuse -> fuse
                            (x + y) % 3 == 0 -> darkRed
                            else -> red
                        }
                    }
                }
            }

            "TNT_SIDE" -> {
                val red = 0xFFDC2626.toInt()
                val white = 0xFFF8FAFC.toInt()
                val font = 0xFF0F172A.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val isBand = y in 5..10
                        // TNT lettering
                        val isLetterT1 = isBand && ((y == 6 && x in 2..4) || (x == 3 && y in 7..9))
                        val isLetterN = isBand && (x == 6 || x == 9 || (x == 7 && y == 7) || (x == 8 && y == 8))
                        val isLetterT2 = isBand && ((y == 6 && x in 11..13) || (x == 12 && y in 7..9))
                        pixels[y * 16 + x] = when {
                            isLetterT1 || isLetterN || isLetterT2 -> font
                            isBand -> white
                            else -> red
                        }
                    }
                }
            }

            "TARGET_BLOCK" -> {
                val white = 0xFFF8FAFC.toInt()
                val red = 0xFFDC2626.toInt()

                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val dist = maxOf(kotlin.math.abs(x - 7.5), kotlin.math.abs(y - 7.5))
                        pixels[y * 16 + x] = when {
                            dist <= 1.5 -> red
                            dist <= 3.5 -> white
                            dist <= 5.5 -> red
                            else -> white
                        }
                    }
                }
            }

            "RED_WOOL" -> generateWool(pixels, 0xFFDC2626.toInt(), 0xFFB91C1C.toInt())
            "BLUE_WOOL" -> generateWool(pixels, 0xFF2563EB.toInt(), 0xFF1D4ED8.toInt())
            "GREEN_WOOL" -> generateWool(pixels, 0xFF16A34A.toInt(), 0xFF15803D.toInt())
            "YELLOW_WOOL" -> generateWool(pixels, 0xFFEAB308.toInt(), 0xFFCA8A04.toInt())

            else -> {
                // Default fallback texture (neutral stone)
                val base = 0xFF5A6577.toInt()
                val dark = 0xFF3E4654.toInt()
                for (i in 0 until 256) {
                    pixels[i] = if (i % 5 == 0) dark else base
                }
            }
        }
        return pixels
    }

    private fun generateWool(pixels: IntArray, mainColor: Int, shadeColor: Int) {
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                pixels[y * 16 + x] = if ((x + y) % 2 == 0) mainColor else shadeColor
            }
        }
    }
}
