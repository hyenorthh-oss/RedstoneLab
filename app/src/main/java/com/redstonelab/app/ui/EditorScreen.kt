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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.redstonelab.app.model.Facing
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // 1. Isometric 2.5D Canvas
        IsometricCanvas(
            grid = viewModel.circuitGrid,
            activeLayerZ = viewModel.activeLayerZ,
            isLayerIsolationEnabled = viewModel.isLayerIsolationEnabled,
            selectedBlockCoord = viewModel.selectedBlockCoord,
            panOffset = viewModel.panOffset,
            zoom = viewModel.zoom,
            onPanChange = { viewModel.panOffset = it },
            onZoomChange = { viewModel.zoom = it },
            onTileClicked = { coord -> viewModel.onTileClicked(coord, context) },
            onTileLongClicked = { coord -> viewModel.onTileLongClicked(coord, context) },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Glassmorphic Futuristic Top Header Bar
        GlassTopBar(
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        // 3. Left Floating Tools & Layer Control Panel
        GlassLeftToolbar(
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
        )

        // 4. Right Block Inspector Panel (visible when a tile is selected)
        AnimatedVisibility(
            visible = viewModel.selectedBlockCoord != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 12.dp)
        ) {
            val coord = viewModel.selectedBlockCoord
            if (coord != null) {
                val block = viewModel.circuitGrid.get(coord)
                CircuitInspectorPanel(
                    selectedCoord = coord,
                    blockState = block,
                    onInteract = { viewModel.onTileClicked(coord, context) },
                    onRotate = {
                        if (block != null && block.type.isDirectional) {
                            val nextFacing = block.facing.clockwise()
                            viewModel.circuitGrid.set(block.copy(facing = nextFacing))
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

        // 5. Bottom Floating Component Palette
        GlassBottomPalette(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            selectedBlockType = viewModel.selectedBlockType,
            onBlockTypeSelected = { viewModel.setBlockType(it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        // 6. Modals
        if (viewModel.showPresetsDialog) {
            PresetsDialog(
                onPresetSelected = { preset -> viewModel.loadPreset(preset) },
                onDismiss = { viewModel.showPresetsDialog = false }
            )
        }

        if (viewModel.showExportDialog) {
            ExportDialog(
                grid = viewModel.circuitGrid,
                initialPackName = viewModel.circuitName,
                onDismiss = { viewModel.showExportDialog = false }
            )
        }
    }
}

@Composable
private fun GlassTopBar(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    GlassBox(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        backgroundColor = SurfaceDark,
        borderColor = GlassBorderSubtle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Branding & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(listOf(NeonCyan, ElectricBlue))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Logo",
                        tint = CyberBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "REDSTONELAB",
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = viewModel.circuitName.uppercase(),
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Simulation Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Play / Pause Button
                GlassButton(
                    onClick = { viewModel.toggleSimulation() },
                    icon = if (viewModel.simulationState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    text = if (viewModel.simulationState.isRunning) "PAUSE" else "RUN",
                    isActive = viewModel.simulationState.isRunning,
                    accentColor = if (viewModel.simulationState.isRunning) StatusSuccess else NeonCyan,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                )

                // Step 1 Tick Button
                GlassButton(
                    onClick = { viewModel.stepSingleTick() },
                    icon = Icons.Default.SkipNext,
                    text = "STEP",
                    accentColor = ElectricBlue,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                )

                // Reset Button
                GlassIconButton(
                    onClick = { viewModel.resetSimulation() },
                    icon = Icons.Default.Refresh,
                    contentDescription = "Reset Simulation",
                    size = 34.dp
                )

                // Ticks & Speed Badge
                CyberBadge(
                    label = "TICK",
                    value = "${viewModel.simulationState.totalTicks}",
                    accentColor = NeonCyan
                )

                CyberBadge(
                    label = "SPEED",
                    value = "${viewModel.simulationState.tickRateHz.toInt()} HZ",
                    accentColor = ElectricBlue
                )
            }

            // Actions: Presets & Export
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GlassButton(
                    onClick = { viewModel.showPresetsDialog = true },
                    icon = Icons.Default.FolderOpen,
                    text = "PRESETS",
                    accentColor = ElectricBlue,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                )

                GlassButton(
                    onClick = { viewModel.showExportDialog = true },
                    icon = Icons.Default.Share,
                    text = ".MCPACK",
                    isActive = true,
                    accentColor = NeonCyan,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                )

                GlassIconButton(
                    onClick = { viewModel.clearCircuit() },
                    icon = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Grid",
                    size = 34.dp,
                    accentColor = StatusError
                )
            }
        }
    }
}

@Composable
private fun GlassLeftToolbar(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    GlassBox(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        backgroundColor = SurfaceDark,
        borderColor = GlassBorderSubtle
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterVertically
        ) {
            // Mode Selectors
            Text(
                text = "TOOL",
                color = TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            GlassIconButton(
                onClick = { viewModel.setTool(EditorTool.INTERACT) },
                icon = Icons.Default.TouchApp,
                contentDescription = "Interact Mode",
                isActive = viewModel.selectedTool == EditorTool.INTERACT,
                accentColor = NeonCyan,
                size = 36.dp
            )

            GlassIconButton(
                onClick = { viewModel.setTool(EditorTool.PLACE) },
                icon = Icons.Default.Add,
                contentDescription = "Place Block Mode",
                isActive = viewModel.selectedTool == EditorTool.PLACE,
                accentColor = StatusSuccess,
                size = 36.dp
            )

            GlassIconButton(
                onClick = { viewModel.setTool(EditorTool.BREAK) },
                icon = Icons.Default.Close,
                contentDescription = "Break Block Mode",
                isActive = viewModel.selectedTool == EditorTool.BREAK,
                accentColor = StatusError,
                size = 36.dp
            )

            GlassIconButton(
                onClick = { viewModel.setTool(EditorTool.INSPECT) },
                icon = Icons.Default.Search,
                contentDescription = "Inspect Mode",
                isActive = viewModel.selectedTool == EditorTool.INSPECT,
                accentColor = ElectricBlue,
                size = 36.dp
            )

            Spacer(
                modifier = Modifier
                    .width(24.dp)
                    .height(1.dp)
                    .background(GlassBorderSubtle)
            )

            // Layer Selector
            Text(
                text = "LAYER",
                color = TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            GlassIconButton(
                onClick = { viewModel.setLayer(viewModel.activeLayerZ + 1) },
                icon = Icons.Default.KeyboardArrowUp,
                contentDescription = "Layer Up",
                size = 32.dp
            )

            CyberBadge(
                label = "Z",
                value = "${viewModel.activeLayerZ}",
                accentColor = NeonCyan
            )

            GlassIconButton(
                onClick = { viewModel.setLayer(viewModel.activeLayerZ - 1) },
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Layer Down",
                size = 32.dp
            )

            GlassIconButton(
                onClick = { viewModel.toggleLayerIsolation() },
                icon = if (viewModel.isLayerIsolationEnabled) Icons.Default.LayersClear else Icons.Default.Layers,
                contentDescription = "Toggle Layer Isolation",
                isActive = viewModel.isLayerIsolationEnabled,
                accentColor = NeonBlueLight,
                size = 32.dp
            )

            Spacer(
                modifier = Modifier
                    .width(24.dp)
                    .height(1.dp)
                    .background(GlassBorderSubtle)
            )

            // Facing Direction
            Text(
                text = "DIR",
                color = TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            GlassIconButton(
                onClick = { viewModel.rotateFacingClockwise() },
                icon = Icons.Default.RotateRight,
                contentDescription = "Rotate Direction",
                size = 32.dp,
                accentColor = ElectricBlue
            )

            Text(
                text = viewModel.selectedFacing.bedrockName.uppercase().take(1),
                color = NeonCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
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
        shape = RoundedCornerShape(14.dp),
        backgroundColor = SurfaceDark,
        borderColor = GlassBorderSubtle
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            // Category Tabs
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

            Spacer(modifier = Modifier.height(6.dp))

            // Block Palette Chips
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
                    GlassButton(
                        onClick = { onBlockTypeSelected(blockType) },
                        text = blockType.displayName.uppercase(),
                        isActive = isSelected,
                        accentColor = if (isSelected) NeonCyan else ElectricBlue,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
