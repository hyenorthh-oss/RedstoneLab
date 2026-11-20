package com.redstonelab.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
    cameraOrientationLabel: String,
    isOnionSkinEnabled: Boolean,
    showSignalNumbers: Boolean,
    onToggleOnionSkin: () -> Unit,
    onToggleSignalNumbers: () -> Unit,
    onInteract: () -> Unit,
    onRotate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBox(
        modifier = modifier
            .width(260.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = SurfaceBar,
        borderColor = BorderSubtle,
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
                    text = "INSPETOR",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CyberBadge(
                        label = "CAM",
                        value = cameraOrientationLabel,
                        accentColor = RedstoneAccent
                    )
                    CyberBadge(
                        label = "POS",
                        value = "${selectedCoord.x}, ${selectedCoord.y}, ${selectedCoord.z}",
                        accentColor = TextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (blockState != null) {
                Text(
                    text = blockState.type.displayName.uppercase(),
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                val powerRatio = blockState.power / 15f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SINAL",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${blockState.power} / 15",
                        color = if (blockState.power > 0) RedstoneBright else TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                LinearProgressIndicator(
                    progress = { powerRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = if (blockState.power > 0) RedstoneBright else RedstoneOff,
                    trackColor = CanvasBg
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CyberBadge(
                        label = "DIR",
                        value = blockState.facing.bedrockName.uppercase(),
                        accentColor = RedstoneAccent
                    )
                    if (blockState.type.isRepeater) {
                        CyberBadge(
                            label = "TICKS",
                            value = "${blockState.delay} T",
                            accentColor = StatusWarning
                        )
                    }
                    if (blockState.type.isComparator) {
                        CyberBadge(
                            label = "MODO",
                            value = if (blockState.isSubtractMode) "SUBTR" else "COMP",
                            accentColor = TextWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (blockState.type.isInteractive) {
                        GlassButton(
                            onClick = onInteract,
                            icon = Icons.Default.TouchApp,
                            text = "ACIONAR",
                            modifier = Modifier.weight(1f),
                            accentColor = RedstoneAccent,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        )
                    }

                    if (blockState.type.isDirectional) {
                        GlassButton(
                            onClick = onRotate,
                            icon = Icons.Default.RotateRight,
                            text = "GIRAR",
                            modifier = Modifier.weight(1f),
                            accentColor = ButtonDefault,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
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
                Text(
                    text = "Bloco de Ar (Vazio)",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GlassButton(
                    onClick = onToggleOnionSkin,
                    text = if (isOnionSkinEnabled) "ONION: ON" else "ONION: OFF",
                    isActive = isOnionSkinEnabled,
                    accentColor = ButtonDefault,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                )

                GlassButton(
                    onClick = onToggleSignalNumbers,
                    text = if (showSignalNumbers) "NUM: ON" else "NUM: OFF",
                    isActive = showSignalNumbers,
                    accentColor = ButtonDefault,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }
}
