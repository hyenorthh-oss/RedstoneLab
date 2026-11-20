package com.redstonelab.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redstonelab.app.theme.*

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = MatteSurfaceElevated,
    borderColor: Color = MatteBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = shape, spotColor = NeonCyan.copy(alpha = 0.15f))
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.92f),
                        backgroundColor.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                border = BorderStroke(
                    width = borderWidth,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            borderColor.copy(alpha = 0.9f),
                            borderColor.copy(alpha = 0.4f)
                        )
                    )
                ),
                shape = shape
            ),
        content = content
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String? = null,
    isActive: Boolean = false,
    accentColor: Color = NeonCyan,
    shape: Shape = RoundedCornerShape(50),
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor = if (isActive) accentColor else MatteBorder
    val bgColor = if (isActive) accentColor.copy(alpha = 0.22f) else MatteSurface

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(
                border = BorderStroke(
                    width = if (isActive) 1.5.dp else 1.dp,
                    color = borderColor
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = accentColor),
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.wrapContentSize()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = if (isActive) accentColor else TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (icon != null && text != null) {
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (text != null) {
                Text(
                    text = text,
                    color = if (isActive) accentColor else TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    accentColor: Color = NeonCyan,
    size: Dp = 36.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor = if (isActive) accentColor else MatteBorder
    val bgColor = if (isActive) accentColor.copy(alpha = 0.24f) else MatteSurface

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                border = BorderStroke(
                    width = if (isActive) 1.5.dp else 1.dp,
                    color = borderColor
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = accentColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) accentColor else TextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun CyberBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = NeonCyan
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MatteBackground.copy(alpha = 0.9f))
            .border(BorderStroke(1.dp, MatteBorderSubtle), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = value,
            color = accentColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
