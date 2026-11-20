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
import com.redstonelab.app.components.GlassBox
import com.redstonelab.app.components.GlassButton
import com.redstonelab.app.components.GlassIconButton
import com.redstonelab.app.model.BlockCategory
import com.redstonelab.app.model.BlockType
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

    // Garante inicialização nativa do SoundPool para baixa latência e 100% offline
    LaunchedEffect(Unit) {
        SoundManager.initialize(context)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBg)
    ) {
        // Inicializa o enquadramento e zoom do tabuleiro para ocupar ~80% da tela útil
        LaunchedEffect(constraints.maxWidth, constraints.maxHeight) {
            viewModel.initializeCamera(
                constraints.maxWidth.toFloat(),
                constraints.maxHeight.toFloat()
            )
        }

        // 1. Canvas Isométrico 2.5D com Câmera Rotativa Interativa e Renderizador Voxel 3D
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

        // 2. Barra Superior com Scroll Horizontal e Botões de Minimizar / Restaurar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = !viewModel.isTopBarMinimized,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                GlassTopBar(viewModel = viewModel)
            }

            if (viewModel.isTopBarMinimized) {
                GlassButton(
                    onClick = { viewModel.toggleTopBar() },
                    icon = Icons.Default.KeyboardArrowDown,
                    text = "MENU SUPERIOR",
                    accentColor = ButtonDefault,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // 3. Lateral Direita: Seletor de Camada Y & Inspetor de Bloco com Minimizar / Restaurar
        val sidePaddingTop = if (viewModel.isTopBarMinimized) 12.dp else 66.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = sidePaddingTop, end = 10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(
                visible = !viewModel.isSidePanelMinimized,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Seletor de Camadas Y em formato de pílula com botão de colapso
                    GlassBox(
                        shape = CircleShape,
                        backgroundColor = SurfaceBar,
                        borderColor = BorderSubtle
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            GlassIconButton(
                                onClick = { viewModel.setLayer(viewModel.activeLayerY - 1) },
                                icon = Icons.Default.Remove,
                                contentDescription = "Camada Abaixo",
                                size = 38.dp
                            )

                            CyberBadge(
                                label = "CAMADA",
                                value = "Y: ${viewModel.activeLayerY}",
                                accentColor = RedstoneAccent
                            )

                            GlassIconButton(
                                onClick = { viewModel.setLayer(viewModel.activeLayerY + 1) },
                                icon = Icons.Default.Add,
                                contentDescription = "Camada Acima",
                                size = 38.dp
                            )

                            GlassIconButton(
                                onClick = { viewModel.toggleSidePanel() },
                                icon = Icons.Default.ChevronRight,
                                contentDescription = "Ocultar Painel Lateral",
                                size = 38.dp,
                                accentColor = ButtonDefault
                            )
                        }
                    }

                    // Inspetor Rápido do Bloco Selecionado
                    AnimatedVisibility(
                        visible = viewModel.selectedBlockCoord != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val coord = viewModel.selectedBlockCoord
                        if (coord != null) {
                            val block = viewModel.circuitGrid.get(coord)
                            CircuitInspectorPanel(
                                selectedCoord = coord,
                                blockState = block,
                                cameraOrientationLabel = viewModel.cameraOrientationLabel,
                                isOnionSkinEnabled = viewModel.isOnionSkinEnabled,
                                showSignalNumbers = viewModel.showSignalNumbers,
                                onToggleOnionSkin = { viewModel.toggleOnionSkin() },
                                onToggleSignalNumbers = { viewModel.toggleSignalNumbers() },
                                onInteract = { viewModel.onTileClicked(coord, context) },
                                onRotate = {
                                    if (block != null && block.type.isDirectional) {
                                        viewModel.circuitGrid.set(block.copy(facing = block.facing.clockwise()))
                                        viewModel.stepSingleTick()
                                    }
                                },
                                onDelete = {
                                    viewModel.circuitGrid.remove(coord)
                                    viewModel.stepSingleTick()
                                },
                                onClose = {
                                    viewModel.clearSelectedBlockCoord()
                                }
                            )
                        }
                    }
                }
            }

            if (viewModel.isSidePanelMinimized) {
                GlassButton(
                    onClick = { viewModel.toggleSidePanel() },
                    icon = Icons.Default.Layers,
                    text = "Y: ${viewModel.activeLayerY}",
                    accentColor = ButtonDefault,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // 4. Barra Inferior: Abas de Categoria + Paleta com Minimizar / Restaurar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = !viewModel.isBottomPaletteMinimized,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                GlassBottomPalette(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    selectedBlockType = viewModel.selectedBlockType,
                    onBlockTypeSelected = { viewModel.setBlockType(it) },
                    onMinimize = { viewModel.toggleBottomPalette() }
                )
            }

            if (viewModel.isBottomPaletteMinimized) {
                GlassButton(
                    onClick = { viewModel.toggleBottomPalette() },
                    icon = Icons.Default.KeyboardArrowUp,
                    text = "PALETA DE BLOCOS",
                    accentColor = ButtonDefault,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                )
            }
        }

        // 5. Diálogos e Modais
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
}

@Composable
private fun GlassTopBar(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    GlassBox(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        backgroundColor = SurfaceBar,
        borderColor = BorderSubtle
    ) {
        // Row com scroll horizontal suave para NUNCA cortar botões (especialmente EXPORTAR .MCPACK)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Grupo 1: Logo, Projetos e Exemplos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(RedstoneAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Logo",
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "REDSTONELAB v2",
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                GlassButton(
                    onClick = {
                        viewModel.refreshProjectsList(context)
                        viewModel.showProjectsDialog = true
                    },
                    icon = Icons.Default.FolderOpen,
                    text = "PROJETOS",
                    accentColor = ButtonDefault,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                )

                GlassButton(
                    onClick = { viewModel.showPresetsDialog = true },
                    icon = Icons.Default.MenuBook,
                    text = "EXEMPLOS",
                    accentColor = ButtonDefault,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            // Grupo 2: Rotação Isométrica do Tabuleiro, Simulação e Ticks
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botão de Girar Tabuleiro 360° em passos de 90° + Indicador
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GlassIconButton(
                        onClick = { viewModel.rotateCamera() },
                        icon = Icons.Default.RotateRight,
                        contentDescription = "Girar Tabuleiro 90°",
                        size = 46.dp,
                        accentColor = ButtonDefault
                    )

                    CyberBadge(
                        label = "VISÃO",
                        value = viewModel.cameraOrientationLabel,
                        accentColor = RedstoneAccent
                    )
                }

                // SIMULAR / PAUSAR
                val isSimRunning = viewModel.simulationState.isRunning
                GlassButton(
                    onClick = { viewModel.toggleSimulation() },
                    icon = if (isSimRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    text = if (isSimRunning) "PAUSAR" else "SIMULAR",
                    isActive = isSimRunning,
                    accentColor = RedstoneAccent,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )

                // +1 TICK
                GlassButton(
                    onClick = { viewModel.stepSingleTick() },
                    icon = Icons.Default.SkipNext,
                    text = "+1 TICK",
                    accentColor = ButtonDefault,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                )

                CyberBadge(
                    label = "TICKS",
                    value = "${viewModel.simulationState.totalTicks}",
                    accentColor = TextWhite
                )
            }

            // Grupo 3: Undo, Redo, Borracha, Girar Bloco e EXPORTAR .MCPACK
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GlassIconButton(
                    onClick = { viewModel.undo() },
                    icon = Icons.Default.Undo,
                    contentDescription = "Desfazer",
                    size = 46.dp
                )

                GlassIconButton(
                    onClick = { viewModel.redo() },
                    icon = Icons.Default.Redo,
                    contentDescription = "Refazer",
                    size = 46.dp
                )

                val isErasing = viewModel.selectedTool == EditorTool.ERASE
                GlassIconButton(
                    onClick = {
                        viewModel.setTool(if (isErasing) EditorTool.PLACE else EditorTool.ERASE)
                    },
                    icon = Icons.Default.Clear,
                    contentDescription = "Borracha",
                    isActive = isErasing,
                    accentColor = StatusError,
                    size = 46.dp
                )

                GlassIconButton(
                    onClick = { viewModel.rotateFacingClockwise() },
                    icon = Icons.Default.Refresh,
                    contentDescription = "Girar Bloco",
                    size = 46.dp,
                    accentColor = ButtonDefault
                )

                // Botão de Destaque em Vermelho Redstone Fosco: EXPORTAR .MCPACK
                GlassButton(
                    onClick = { viewModel.showExportDialog = true },
                    icon = Icons.Default.FileDownload,
                    text = "EXPORTAR .MCPACK",
                    isActive = true,
                    accentColor = RedstoneAccent,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                )
            }

            // Grupo 4: Som (Mutar/Desmutar) e Minimizar Menu Superior
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isMuted = SoundManager.isMuted
                GlassIconButton(
                    onClick = { viewModel.toggleSoundMute() },
                    icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Ativar Som" else "Mutar Som",
                    isActive = isMuted,
                    accentColor = if (isMuted) StatusError else ButtonDefault,
                    size = 46.dp
                )

                GlassIconButton(
                    onClick = { viewModel.toggleTopBar() },
                    icon = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Minimizar Barra Superior",
                    size = 46.dp,
                    accentColor = ButtonDefault
                )
            }
        }
    }
}

@Composable
private fun GlassBottomPalette(
    selectedCategory: BlockCategory,
    onCategorySelected: (BlockCategory) -> Unit,
    selectedBlockType: BlockType,
    onBlockTypeSelected: (BlockType) -> Unit,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBox(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = SurfaceBar,
        borderColor = BorderSubtle
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
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
                                .padding(horizontal = 14.dp, vertical = 6.dp)
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
                    onClick = onMinimize,
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimizar Paleta de Blocos",
                    size = 38.dp,
                    accentColor = ButtonDefault
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Paleta deslizável com botões grandes de 54dp, ícone voxel 3D e borda branca de 2dp
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
                            .height(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) ButtonActive else ButtonDefault)
                            .border(
                                border = if (isSelected) {
                                    BorderStroke(2.dp, BorderSelected)
                                } else {
                                    BorderStroke(1.dp, ButtonBorder)
                                },
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onBlockTypeSelected(blockType) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Ícone Voxel 3D do bloco
                            BlockVoxelPreview(
                                type = blockType,
                                size = 32.dp
                            )

                            // Nome do Bloco em tipografia limpa sem emojis
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
