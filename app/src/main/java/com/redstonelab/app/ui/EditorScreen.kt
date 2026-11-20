package com.redstonelab.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redstonelab.app.audio.SoundManager
import com.redstonelab.app.components.CyberBadge
import com.redstonelab.app.components.GlassButton
import com.redstonelab.app.components.GlassIconButton
import com.redstonelab.app.model.BlockCategory
import com.redstonelab.app.model.BlockType
import com.redstonelab.app.model.Facing
import com.redstonelab.app.renderer.BlockVoxelPreview
import com.redstonelab.app.renderer.IsometricCanvas
import com.redstonelab.app.theme.*
import com.redstonelab.app.viewmodel.EditorTool
import com.redstonelab.app.viewmodel.EditorViewModel

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(BlockCategory.TRANSMISSION) }

    // Inicialização do SoundPool e liberação de recursos no ciclo de vida
    DisposableEffect(Unit) {
        SoundManager.initialize(context)
        onDispose {
            SoundManager.release()
        }
    }

    // Estrutura principal: Column rígida com 3 faixas que NUNCA se sobrepõem
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121316))
            .safeDrawingPadding()
    ) {
        // 1. Barra Superior (Topo): Altura fixa de 56dp, fundo reto #1B1D21, scroll horizontal em linha única
        AnimatedVisibility(
            visible = !viewModel.isTopBarCollapsed,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TopBar(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }

        // 2. Área do Tabuleiro Isométrico (Meio): Box flexível com weight(1f) ocupando todo o espaço livre
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Inicializa a câmera dentro das dimensões reais do Canvas
            LaunchedEffect(maxWidth, maxHeight) {
                viewModel.initializeCamera(
                    constraints.maxWidth.toFloat(),
                    constraints.maxHeight.toFloat()
                )
            }

            // Canvas Isométrico 2.5D ocupando fillMaxSize(), completamente livre e interativo
            IsometricCanvas(
                grid = viewModel.circuitGrid,
                activeLayerY = viewModel.activeLayerY,
                cameraAngle = viewModel.cameraAngle,
                isOnionSkinEnabled = viewModel.isOnionSkinEnabled,
                showSignalNumbers = viewModel.showSignalNumbers,
                selectedBlockCoord = viewModel.selectedBlockCoord,
                panOffset = viewModel.panOffset,
                zoom = viewModel.zoom,
                onPanChange = { viewModel.panOffset = it },
                onZoomChange = { viewModel.zoom = it },
                onTileClicked = { coord -> viewModel.onTileClicked(coord, context) },
                onTileLongClicked = { coord -> viewModel.onTileLongClicked(coord, context) },
                modifier = Modifier.fillMaxSize()
            )

            // Se a barra superior estiver recolhida: botão flutuante compacto no canto superior esquerdo
            if (viewModel.isTopBarCollapsed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B1D21))
                        .border(1.dp, Color(0xFF3A3E47), CircleShape)
                        .clickable { viewModel.toggleTopBar() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Restaurar Barra Superior",
                        tint = TextWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Pílula discreta de 1 linha de inspeção do bloco selecionado no canto superior esquerdo
            if (viewModel.selectedBlockCoord != null) {
                val coord = viewModel.selectedBlockCoord!!
                val block = viewModel.circuitGrid.get(coord)
                val power = block?.power ?: 0
                val facingChar = when (block?.facing) {
                    Facing.NORTH -> "N"
                    Facing.SOUTH -> "S"
                    Facing.EAST -> "L"
                    Facing.WEST -> "O"
                    Facing.UP -> "C"
                    Facing.DOWN -> "B"
                    null -> "-"
                }
                val startPadding = if (viewModel.isTopBarCollapsed) 54.dp else 8.dp
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = startPadding, top = 8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B1D21))
                        .border(1.dp, Color(0xFF3A3E47), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "X:${coord.x} Y:${coord.y} Z:${coord.z} | Sinal: $power/15 | $facingChar",
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar Inspeção",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.clearSelectedBlockCoord() }
                        )
                    }
                }
            }

            // Seletor compacto de camada (-  CAMADA Y: X  +) flutuando no canto superior direito
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B1D21))
                    .border(1.dp, Color(0xFF3A3E47), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GlassIconButton(
                        onClick = {
                            viewModel.setLayer(viewModel.activeLayerY - 1)
                            SoundManager.playUi()
                        },
                        icon = Icons.Default.Remove,
                        contentDescription = "Camada Abaixo",
                        size = 34.dp
                    )

                    Text(
                        text = "CAMADA Y: ${viewModel.activeLayerY}",
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    GlassIconButton(
                        onClick = {
                            viewModel.setLayer(viewModel.activeLayerY + 1)
                            SoundManager.playUi()
                        },
                        icon = Icons.Default.Add,
                        contentDescription = "Camada Acima",
                        size = 34.dp
                    )
                }
            }

            // Se a barra inferior estiver recolhida: pílula flutuante discreta na parte inferior central
            if (viewModel.isBottomBarCollapsed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B1D21))
                        .border(1.dp, Color(0xFF3A3E47), CircleShape)
                        .clickable { viewModel.toggleBottomBar() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BlockVoxelPreview(
                            type = viewModel.selectedBlockType,
                            size = 24.dp
                        )
                        Text(
                            text = viewModel.selectedBlockType.displayName.uppercase(),
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Expandir Paleta de Blocos",
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 3. Barra Inferior (Rodapé): Paleta de Categorias e Blocos
        AnimatedVisibility(
            visible = !viewModel.isBottomBarCollapsed,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BottomPalette(
                selectedCategory = selectedCategory,
                onCategorySelected = {
                    selectedCategory = it
                    SoundManager.playUi()
                },
                selectedBlockType = viewModel.selectedBlockType,
                onBlockTypeSelected = {
                    viewModel.setBlockType(it)
                    SoundManager.playUi()
                },
                onCollapse = { viewModel.toggleBottomBar() },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }
    }

    // Diálogos e Modais
    if (viewModel.showProjectsDialog) {
        ProjectsDialog(
            projects = viewModel.localProjects,
            onProjectSelected = { id -> viewModel.loadLocalProject(context, id) },
            onProjectDeleted = { id -> viewModel.deleteLocalProject(context, id) },
            onNewProject = { viewModel.newProject() },
            onDismiss = { viewModel.showProjectsDialog = false }
        )
    }

    if (viewModel.showPresetsDialog) {
        PresetsDialog(
            onPresetSelected = { preset -> viewModel.loadPreset(preset) },
            onDismiss = { viewModel.showPresetsDialog = false }
        )
    }

    if (viewModel.showExportDialog) {
        ExportDialog(
            grid = viewModel.circuitGrid,
            initialCircuitName = viewModel.circuitName,
            onExportComplete = { file, sanitized ->
                viewModel.showExportDialog = false
                viewModel.exportedPackFile = file
                viewModel.exportedSanitizedName = sanitized
            },
            onDismiss = { viewModel.showExportDialog = false }
        )
    }

    val exportedFile = viewModel.exportedPackFile
    if (exportedFile != null) {
        ExportSuccessDialog(
            packFile = exportedFile,
            sanitizedName = viewModel.exportedSanitizedName,
            onDismiss = { viewModel.exportedPackFile = null }
        )
    }
}

@Composable
private fun TopBar(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .background(Color(0xFF1B1D21))
            .border(
                width = 1.dp,
                color = Color(0xFF2E323A),
                shape = RoundedCornerShape(0.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Logo REDSTONELAB v2
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(RedstoneAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Logo",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "REDSTONELAB v2",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // 2. PROJETOS
            GlassButton(
                onClick = {
                    SoundManager.playUi()
                    viewModel.refreshProjectsList(context)
                    viewModel.showProjectsDialog = true
                },
                icon = Icons.Default.FolderOpen,
                text = "PROJETOS",
                accentColor = ButtonDefault,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            )

            // 3. EXEMPLOS
            GlassButton(
                onClick = {
                    SoundManager.playUi()
                    viewModel.showPresetsDialog = true
                },
                icon = Icons.Default.MenuBook,
                text = "EXEMPLOS",
                accentColor = ButtonDefault,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            )

            // 4. SIMULAR / PAUSAR
            val isSimRunning = viewModel.simulationState.isRunning
            GlassButton(
                onClick = {
                    SoundManager.playUi()
                    viewModel.toggleSimulation()
                },
                icon = if (isSimRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                text = if (isSimRunning) "PAUSAR" else "SIMULAR",
                isActive = isSimRunning,
                accentColor = RedstoneAccent,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            )

            // 5. +1 TICK
            GlassButton(
                onClick = {
                    SoundManager.playUi()
                    viewModel.stepSingleTick()
                },
                icon = Icons.Default.SkipNext,
                text = "+1 TICK",
                accentColor = ButtonDefault,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            )

            // TICKS Badge
            CyberBadge(
                label = "TICKS",
                value = "${viewModel.simulationState.totalTicks}",
                accentColor = TextWhite
            )

            // 6. Desfazer
            GlassIconButton(
                onClick = { viewModel.undo() },
                icon = Icons.Default.Undo,
                contentDescription = "Desfazer",
                size = 40.dp
            )

            // 7. Refazer
            GlassIconButton(
                onClick = { viewModel.redo() },
                icon = Icons.Default.Redo,
                contentDescription = "Refazer",
                size = 40.dp
            )

            // 8. Borracha
            val isErasing = viewModel.selectedTool == EditorTool.ERASE
            GlassIconButton(
                onClick = {
                    SoundManager.playUi()
                    viewModel.setTool(if (isErasing) EditorTool.PLACE else EditorTool.ERASE)
                },
                icon = Icons.Default.Clear,
                contentDescription = "Borracha",
                isActive = isErasing,
                accentColor = StatusError,
                size = 40.dp
            )

            // 9. Girar Bloco
            GlassIconButton(
                onClick = {
                    SoundManager.playUi()
                    viewModel.rotateFacingClockwise()
                },
                icon = Icons.Default.Refresh,
                contentDescription = "Girar Bloco",
                size = 40.dp,
                accentColor = ButtonDefault
            )

            // 10. Girar Câmera + Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GlassIconButton(
                    onClick = {
                        SoundManager.playUi()
                        viewModel.rotateCamera()
                    },
                    icon = Icons.Default.RotateRight,
                    contentDescription = "Girar Câmera 90°",
                    size = 40.dp,
                    accentColor = ButtonDefault
                )

                CyberBadge(
                    label = "VISÃO",
                    value = viewModel.cameraOrientationLabel,
                    accentColor = RedstoneAccent
                )
            }

            // 11. Som Ligado / Mudo (sem emojis, ícone vetorial limpo)
            val isMuted = SoundManager.isMuted
            GlassIconButton(
                onClick = { viewModel.toggleSoundMute() },
                icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "Ativar Som" else "Mutar Som",
                isActive = isMuted,
                accentColor = if (isMuted) StatusError else ButtonDefault,
                size = 40.dp
            )

            // 12. Minimizar Barra Superior
            GlassIconButton(
                onClick = { viewModel.toggleTopBar() },
                icon = Icons.Default.KeyboardArrowUp,
                contentDescription = "Minimizar Barra Superior",
                size = 40.dp,
                accentColor = ButtonDefault
            )

            // 13. EXPORTAR .MCPACK (Destaque em Vermelho Redstone)
            GlassButton(
                onClick = {
                    SoundManager.playUi()
                    viewModel.showExportDialog = true
                },
                icon = Icons.Default.FileDownload,
                text = "EXPORTAR .MCPACK",
                isActive = true,
                accentColor = RedstoneAccent,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun BottomPalette(
    selectedCategory: BlockCategory,
    onCategorySelected: (BlockCategory) -> Unit,
    selectedBlockType: BlockType,
    onBlockTypeSelected: (BlockType) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF1B1D21))
            .border(
                width = 1.dp,
                color = Color(0xFF2E323A),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            // Abas de Categoria em formato de pílula + Botão de Minimizar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BlockCategory.entries.forEach { category ->
                        val isSelected = category == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) ButtonActive else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) BorderSelected else ButtonBorder,
                                    shape = CircleShape
                                )
                                .clickable { onCategorySelected(category) }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = category.label.uppercase(),
                                color = if (isSelected) TextWhite else TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                GlassIconButton(
                    onClick = onCollapse,
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Recolher Paleta de Blocos",
                    size = 36.dp,
                    accentColor = ButtonDefault
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Paleta deslizável com botões grandes de 52dp, ícone voxel 3D e borda branca de 2dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categoryBlocks = BlockType.entries.filter { it.category == selectedCategory && it != BlockType.AIR }
                for (blockType in categoryBlocks) {
                    val isSelected = blockType == selectedBlockType

                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) ButtonActive else ButtonDefault)
                            .border(
                                border = if (isSelected) {
                                    BorderStroke(2.dp, BorderSelected)
                                } else {
                                    BorderStroke(1.dp, ButtonBorder)
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onBlockTypeSelected(blockType) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BlockVoxelPreview(
                                type = blockType,
                                size = 30.dp
                            )

                            Text(
                                text = blockType.displayName.uppercase(),
                                color = TextWhite,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
