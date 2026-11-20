package com.redstonelab.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.redstonelab.app.components.CyberBadge
import com.redstonelab.app.components.GlassBox
import com.redstonelab.app.components.GlassButton
import com.redstonelab.app.components.GlassIconButton
import com.redstonelab.app.exporter.BedrockPackExporter
import com.redstonelab.app.model.CircuitGrid
import com.redstonelab.app.theme.*

@Composable
fun ExportDialog(
    grid: CircuitGrid,
    initialPackName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var packName by remember { mutableStateOf(initialPackName) }
    val bounds = remember(grid) { grid.getBounds() }
    val totalBlocks = remember(grid) { grid.blockCount }

    val mcfunctionScript = remember(grid) {
        BedrockPackExporter.generateSpawnFunction(grid)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassBox(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
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
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EXPORT TO MINECRAFT BEDROCK",
                            color = NeonCyan,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "NATIVE .MCPACK BEHAVIOR PACKAGE",
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

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Circuit Name Input Field
                    Text(
                        text = "PACKAGE NAME",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = packName,
                        onValueChange = { packName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GlassBorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Circuit Metadata Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberBadge(label = "BLOCKS", value = "$totalBlocks", accentColor = NeonCyan)
                        CyberBadge(
                            label = "SIZE",
                            value = "${bounds.sizeX}x${bounds.sizeY}x${bounds.sizeZ}",
                            accentColor = ElectricBlue
                        )
                        CyberBadge(label = "BEDROCK", value = "1.20+", accentColor = StatusSuccess)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bedrock Instructions Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberBlack.copy(alpha = 0.6f))
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "BEDROCK IMPORT & DEPLOYMENT",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Tap 'SHARE / OPEN IN MINECRAFT' to launch Bedrock directly.\n" +
                                        "2. Enable this behavior pack in your Bedrock world settings.\n" +
                                        "3. Type '/function spawn' in chat to construct the circuit automatically at your coordinates.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Script Preview Box
                    Text(
                        text = "FUNCTIONS/SPAWN.MCFUNCTION PREVIEW",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF04060A))
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = mcfunctionScript,
                            color = NeonCyanDim,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Bedrock Spawn Script", mcfunctionScript)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Commands copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        icon = Icons.Default.ContentCopy,
                        text = "COPY COMMANDS",
                        modifier = Modifier.weight(1f),
                        accentColor = ElectricBlue
                    )

                    GlassButton(
                        onClick = {
                            try {
                                val packFile = BedrockPackExporter.exportToCacheFile(context, grid, packName)
                                val shareIntent = BedrockPackExporter.createShareIntent(context, packFile)
                                context.startActivity(Intent.createChooser(shareIntent, "Open with Minecraft or Share"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        icon = Icons.Default.Share,
                        text = "SHARE / OPEN IN MINECRAFT",
                        modifier = Modifier.weight(1.4f),
                        isActive = true,
                        accentColor = NeonCyan
                    )
                }
            }
        }
    }
}
