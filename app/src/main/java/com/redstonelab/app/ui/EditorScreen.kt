package com.redstonelab.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redstonelab.app.components.CyberBadge
import com.redstonelab.app.components.GlassBox
import com.redstonelab.app.components.GlassButton
import com.redstonelab.app.components.GlassIconButton
import com.redstonelab.app.model.BlockCategory
import com.redstonelab.app.model.BlockType
import com.redstonelab.app.renderer.IsometricCanvas
import com.redstonelab.app.theme.*
import com.redstonelab.app.viewmodel.EditorMode
import com.redstonelab.app.viewmodel.EditorTool
import com.redstonelab.app.viewmodel.EditorViewModel

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(BlockCategory.TRANSMISSION) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // 1. Canvas Isométrico 2.5D de Alta Performance (60 FPS)
        IsometricCanvas(
            grid = viewModel.circuitGrid,
            activeLayerY = viewModel.activeLayerY,
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

        // 2. Barra Superior em Paisagem
        GlassTopBar(
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )

        // 3. Lateral Direita: Seletor de Camada Y & Inspetor de Bloco
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 58.dp, end = 8.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Seletor de Camadas Y
            GlassBox(
                shape = RoundedCornerShape(12.dp),
                backgroundColor = SurfaceDark,
                borderColor = GlassBorderSubtle
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
                        size = 30.dp
                    )

                    CyberBadge(
                        label = "CAMADA",
                        value = "Y: ${viewModel.activeLayerY}",
                        accentColor = NeonCyan
                    )

                    GlassIconButton(
                        onClick = { viewModel.setLayer(viewModel.activeLayerY + 1) },
                        icon = Icons.Default.Add,
                        contentDescription = "Camada Acima",
                        size = 30.dp
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
                        }
                    )
                }
            }
        }

        // 4. Barra Inferior: Abas de Categoria + Paleta Deslizável
        GlassBottomPalette(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            selectedBlockType = viewModel.selectedBlockType,
            onBlockTypeSelected = { viewModel.setBlockType(it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )

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
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = SurfaceDark,
        borderColor = GlassBorderSubtle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Esquerda: Logo, Projetos e Presets
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.linearGradient(listOf(NeonCyan, ElectricBlue))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Logo",
                            tint = CyberBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "REDSTONELAB",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                GlassButton(
                    onClick = {
                        viewModel.refreshProjectsList(context)
                        viewModel.showProjectsDialog = true
                    },
                    icon = Icons.Default.FolderOpen,
                    text = "PROJETOS",
                    accentColor = ElectricBlue,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                )

                GlassButton(
                    onClick = { viewModel.showPresetsDialog = true },
                    icon = Icons.Default.MenuBook,
                    text = "EXEMPLOS",
                    accentColor = NeonBlueLight,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                )
            }

            // Centro: Simulação e Ticks
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // SIMULAR / PAUSAR
                val isSimRunning = viewModel.simulationState.isRunning
                GlassButton(
                    onClick = { viewModel.toggleSimulation() },
                    icon = if (isSimRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    text = if (isSimRunning) "PAUSAR" else "SIMULAR",
                    isActive = isSimRunning,
                    accentColor = if (isSimRunning) StatusSuccess else NeonCyan,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                )

                // +1 TICK
                GlassButton(
                    onClick = { viewModel.stepSingleTick() },
                    icon = Icons.Default.SkipNext,
                    text = "+1 TICK",
                    accentColor = ElectricBlue,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                )

                CyberBadge(
                    label = "TICKS",
                    value = "${viewModel.simulationState.totalTicks}",
                    accentColor = NeonCyan
                )
            }

            // Direita: Undo, Redo, Borracha, Girar, e EXPORTAR .MCPACK
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Desfazer / Refazer
                GlassIconButton(
                    onClick = { viewModel.undo() },
                    icon = Icons.Default.Undo,
                    contentDescription = "Desfazer",
                    size = 30.dp
                )

                GlassIconButton(
                    onClick = { viewModel.redo() },
                    icon = Icons.Default.Redo,
                    contentDescription = "Refazer",
                    size = 30.dp
                )

                // Borracha
                val isErasing = viewModel.selectedTool == EditorTool.ERASE
                GlassIconButton(
                    onClick = {
                        viewModel.setTool(if (isErasing) EditorTool.PLACE else EditorTool.ERASE)
                    },
                    icon = Icons.Default.Clear,
                    contentDescription = "Borracha",
                    isActive = isErasing,
                    accentColor = StatusError,
                    size = 30.dp
                )

                // Rotacionar Bloco
                GlassIconButton(
                    onClick = { viewModel.rotateFacingClockwise() },
                    icon = Icons.Default.RotateRight,
                    contentDescription = "Rotacionar Bloco",
                    size = 30.dp,
                    accentColor = ElectricBlue
                )

                // Botão destaque: EXPORTAR .MCPACK
                GlassButton(
                    onClick = { viewModel.showExportDialog = true },
                    icon = Icons.Default.FileDownload,
                    text = "EXPORTAR .MCPACK",
                    isActive = true,
                    accentColor = NeonCyan,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)
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
    modifier: Modifier = Modifier
) {
    GlassBox(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = SurfaceDark,
        borderColor = GlassBorderSubtle
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            // 4 Abas de Categoria
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BlockCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NeonCyan else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onCategorySelected(category) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = category.label.uppercase(),
                            color = if (isSelected) NeonCyan else TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Paleta deslizável com os blocos da categoria
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categoryBlocks = BlockType.entries.filter { it.category == selectedCategory && it != BlockType.AIR }
                for (blockType in categoryBlocks) {
                    val isSelected = blockType == selectedBlockType
                    GlassButton(
                        onClick = { onBlockTypeSelected(blockType) },
                        text = blockType.displayName.uppercase(),
                        isActive = isSelected,
                        accentColor = if (isSelected) NeonCyan else ElectricBlue,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
