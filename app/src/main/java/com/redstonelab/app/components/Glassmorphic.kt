package com.redstonelab.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
    shape: Shape = RoundedCornerShape(14.dp),
    backgroundColor: Color = GlassSurface,
    borderColor: Color = GlassBorderSubtle,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = shape, spotColor = NeonCyan.copy(alpha = 0.2f))
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.85f),
                        backgroundColor.copy(alpha = 0.70f)
                    )
                )
            )
            .border(
                border = BorderStroke(
                    width = borderWidth,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            borderColor.copy(alpha = 0.7f),
                            borderColor.copy(alpha = 0.2f)
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
    shape: Shape = RoundedCornerShape(10.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor = if (isActive) accentColor else GlassBorderSubtle
    val bgColor = if (isActive) accentColor.copy(alpha = 0.22f) else GlassSurface

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
                indication = rememberRipple(bounded = true, color = accentColor),
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
                    modifier = Modifier.size(18.dp)
                )
            }
            if (icon != null && text != null) {
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (text != null) {
                Text(
                    text = text,
                    color = if (isActive) accentColor else TextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
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
    size: Dp = 40.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor = if (isActive) accentColor else GlassBorderSubtle
    val bgColor = if (isActive) accentColor.copy(alpha = 0.24f) else GlassSurface

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
                indication = rememberRipple(bounded = true, color = accentColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) accentColor else TextPrimary,
            modifier = Modifier.size(20.dp)
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
            .clip(RoundedCornerShape(6.dp))
            .background(CyberBlack.copy(alpha = 0.8f))
            .border(BorderStroke(1.dp, GlassBorderSubtle), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = value,
            color = accentColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
