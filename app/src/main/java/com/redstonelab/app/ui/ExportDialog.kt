package com.redstonelab.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.redstonelab.app.exporter.McpackExporter
import com.redstonelab.app.model.CircuitGrid
import com.redstonelab.app.theme.*
import java.io.File

@Composable
fun ExportDialog(
    grid: CircuitGrid,
    initialCircuitName: String,
    onExportComplete: (File, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var circuitNameInput by remember { mutableStateOf(initialCircuitName) }
    var includeSmoothStoneBase by remember { mutableStateOf(true) }
    var showCommandsPreview by remember { mutableStateOf(false) }

    val sanitizedName = remember(circuitNameInput) {
        McpackExporter.sanitizeCircuitName(circuitNameInput)
    }

    val bounds = remember(grid) { grid.getBounds() }
    val totalBlocks = remember(grid) { grid.blockCount }

    val mcfunctionScript = remember(grid, sanitizedName, includeSmoothStoneBase) {
        McpackExporter.generateMcfunctionScript(grid, sanitizedName, includeSmoothStoneBase)
    }

    // Modal de Exportação
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassBox(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.90f)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = SurfaceBar,
            borderColor = BorderSubtle,
            borderWidth = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EXPORTAR .MCPACK PARA MINECRAFT BEDROCK",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "COMPATÍVEL COM BEDROCK 1.21+ (BEHAVIOR PACK AUTOMÁTICO)",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    GlassIconButton(
                        onClick = onDismiss,
                        icon = Icons.Default.Close,
                        contentDescription = "Fechar",
                        size = 40.dp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Nome do Circuito Input
                    Text(
                        text = "NOME DO CIRCUITO",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = circuitNameInput,
                        onValueChange = { circuitNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedstoneAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            cursorColor = RedstoneAccent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Identificador sanitizado: /function $sanitizedName",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Badges de Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberBadge(label = "BLOCOS", value = "$totalBlocks", accentColor = RedstoneAccent)
                        CyberBadge(
                            label = "DIMENSÕES",
                            value = "${bounds.sizeX}x${bounds.sizeY}x${bounds.sizeZ}",
                            accentColor = TextWhite
                        )
                        CyberBadge(label = "BEDROCK", value = "1.21+", accentColor = StatusSuccess)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Checkbox de Piso Base de Pedra Lisa
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceElevated)
                            .clickable { includeSmoothStoneBase = !includeSmoothStoneBase }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeSmoothStoneBase,
                            onCheckedChange = { includeSmoothStoneBase = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = RedstoneAccent,
                                uncheckedColor = TextSecondary,
                                checkmarkColor = TextWhite
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Incluir piso base de pedra lisa (smooth_stone)",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gera uma base sólida por baixo na camada Y: -1 antes de posicionar os circuitos",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botão para alternar visualização do código .mcfunction
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCommandsPreview = !showCommandsPreview }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showCommandsPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Visualizar Comandos",
                            tint = TextWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showCommandsPreview) "Ocultar comandos gerados" else "Visualizar comandos gerados (.mcfunction)",
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (showCommandsPreview) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CanvasBg)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = mcfunctionScript,
                                color = TextSecondary,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Bedrock Commands", mcfunctionScript)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Comandos copiados com sucesso!", Toast.LENGTH_SHORT).show()
                        },
                        icon = Icons.Default.ContentCopy,
                        text = "COPIAR COMANDOS",
                        modifier = Modifier.weight(1f),
                        accentColor = ElectricBlue
                    )

                    GlassButton(
                        onClick = {
                            try {
                                val packFile = McpackExporter.saveMcpackFile(
                                    context,
                                    grid,
                                    circuitNameInput,
                                    includeSmoothStoneBase
                                )
                                onExportComplete(packFile, sanitizedName)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro na exportação: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        icon = Icons.Default.FileDownload,
                        text = "EXPORTAR .MCPACK",
                        modifier = Modifier.weight(1.3f),
                        isActive = true,
                        accentColor = NeonCyan
                    )
                }
            }
        }
    }
}

@Composable
fun ExportSuccessDialog(
    packFile: File,
    sanitizedName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val spawnCommand = "/function $sanitizedName"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassBox(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = SurfaceBar,
            borderColor = StatusSuccess,
            borderWidth = 1.5.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header com sucesso
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PACOTE .MCPACK GERADO COM SUCESSO!",
                            color = StatusSuccess,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Salvo em: Download/RedstoneLab/${packFile.name}",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    GlassIconButton(
                        onClick = onDismiss,
                        icon = Icons.Default.Close,
                        contentDescription = "Fechar",
                        size = 40.dp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botão de Instalação em 1 Toque: ABRIR NO MINECRAFT
                GlassButton(
                    onClick = {
                        try {
                            val intent = McpackExporter.createOpenInMinecraftIntent(context, packFile)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Minecraft não pôde ser iniciado diretamente: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    icon = Icons.Default.PlayArrow,
                    text = "ABRIR NO MINECRAFT",
                    modifier = Modifier.fillMaxWidth(),
                    isActive = true,
                    accentColor = RedstoneAccent,
                    contentPadding = PaddingValues(vertical = 12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Caixa com comando pronto e botão de cópia
                Text(
                    text = "COMANDO DE SPAWN:",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CanvasBg)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = spawnCommand,
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    GlassButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Spawn Command", spawnCommand)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Comando '$spawnCommand' copiado!", Toast.LENGTH_SHORT).show()
                        },
                        icon = Icons.Default.ContentCopy,
                        text = "COPIAR",
                        accentColor = ButtonDefault,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mini guia de 3 passos curtos
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "COMO SPAWNAR O CIRCUITO NO SEU MUNDO:",
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Toque em 'ABRIR NO MINECRAFT' e ative o Pacote de Comportamento nas opções do mundo.\n" +
                                    "2. Entre no mundo e abra o chat.\n" +
                                    "3. Cole o comando $spawnCommand e aperte Enviar.",
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
