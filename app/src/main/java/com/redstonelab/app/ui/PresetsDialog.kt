package com.redstonelab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.redstonelab.app.components.CyberBadge
import com.redstonelab.app.components.GlassBox
import com.redstonelab.app.components.GlassButton
import com.redstonelab.app.components.GlassIconButton
import com.redstonelab.app.model.CircuitPreset
import com.redstonelab.app.model.CircuitPresetsLibrary
import com.redstonelab.app.theme.*

@Composable
fun PresetsDialog(
    onPresetSelected: (CircuitPreset) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassBox(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = SurfaceDarkElevated,
            borderColor = GlassBorder,
            borderWidth = 1.5.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CIRCUIT PRESETS LIBRARY",
                            color = NeonCyan,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "CLASSIC & ADVANCED REDSTONE MODULES",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    GlassIconButton(
                        onClick = onDismiss,
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        size = 36.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(CircuitPresetsLibrary.presets) { preset ->
                        PresetCard(
                            preset = preset,
                            onLoad = {
                                onPresetSelected(preset)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: CircuitPreset,
    onLoad: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpaceDark.copy(alpha = 0.85f))
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onLoad)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                val diffColor = when (preset.difficulty) {
                    "Beginner" -> StatusSuccess
                    "Intermediate" -> NeonCyan
                    else -> StatusWarning
                }
                CyberBadge(label = "DIFF", value = preset.difficulty, accentColor = diffColor)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = preset.description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tags
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    preset.tags.take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberBlack)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag.uppercase(),
                                color = NeonCyanDim,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                GlassButton(
                    onClick = onLoad,
                    icon = Icons.Default.Download,
                    text = "LOAD CIRCUIT",
                    accentColor = NeonCyan,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
