package com.redstonelab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redstonelab.app.components.CyberBadge
import com.redstonelab.app.components.GlassBox
import com.redstonelab.app.components.GlassButton
import com.redstonelab.app.model.BlockCoord
import com.redstonelab.app.model.BlockState
import com.redstonelab.app.theme.*

@Composable
fun CircuitInspectorPanel(
    selectedCoord: BlockCoord,
    blockState: BlockState?,
    onInteract: () -> Unit,
    onRotate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBox(
        modifier = modifier
            .width(280.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = SurfaceDarkElevated,
        borderColor = GlassBorder,
        borderWidth = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BLOCK INSPECTOR",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                CyberBadge(
                    label = "COORD",
                    value = "${selectedCoord.x}, ${selectedCoord.y}, ${selectedCoord.z}",
                    accentColor = ElectricBlue
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (blockState != null) {
                // Block Type Name
                Text(
                    text = blockState.type.displayName.uppercase(),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Power Level Progress Indicator
                val powerRatio = blockState.power / 15f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "POWER LEVEL",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${blockState.power} / 15",
                        color = if (blockState.power > 0) RedstoneFull else TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { powerRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (blockState.power > 0) RedstoneFull else RedstoneOff,
                    trackColor = CyberBlack
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Details Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CyberBadge(
                        label = "DIR",
                        value = blockState.facing.bedrockName.uppercase(),
                        accentColor = NeonCyan
                    )
                    if (blockState.type.isRepeater) {
                        CyberBadge(
                            label = "DELAY",
                            value = "${blockState.delay} T",
                            accentColor = StatusWarning
                        )
                    }
                    if (blockState.type.isComparator) {
                        CyberBadge(
                            label = "MODE",
                            value = if (blockState.isSubtractMode) "SUBTRACT" else "COMPARE",
                            accentColor = NeonBlueLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (blockState.type.isInteractive) {
                        GlassButton(
                            onClick = onInteract,
                            icon = Icons.Default.TouchApp,
                            text = "TRIGGER",
                            modifier = Modifier.weight(1f),
                            accentColor = NeonCyan,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    if (blockState.type.isDirectional) {
                        GlassButton(
                            onClick = onRotate,
                            icon = Icons.Default.RotateRight,
                            text = "ROTATE",
                            modifier = Modifier.weight(1f),
                            accentColor = ElectricBlue,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    GlassButton(
                        onClick = onDelete,
                        icon = Icons.Default.Delete,
                        modifier = Modifier.wrapContentWidth(),
                        accentColor = StatusError,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EMPTY AIR VOID",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
