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
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = SurfaceBar,
    borderColor: Color = BorderSubtle,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(
                border = BorderStroke(width = borderWidth, color = borderColor),
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
    accentColor: Color = RedstoneAccent,
    shape: Shape = CircleShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor = when {
        isActive && accentColor == RedstoneAccent -> RedstoneAccent
        isActive -> ButtonActive
        else -> ButtonDefault
    }

    val borderColor = when {
        isActive && accentColor == RedstoneAccent -> RedstoneAccentPressed
        isActive -> BorderSelected
        else -> ButtonBorder
    }

    Box(
        modifier = modifier
            .heightIn(min = 46.dp)
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
                indication = ripple(bounded = true, color = RedstoneAccentPressed),
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
                    tint = TextWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (icon != null && text != null) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (text != null) {
                Text(
                    text = text,
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
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
    accentColor: Color = RedstoneAccent,
    size: Dp = 46.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor = when {
        isActive && accentColor == RedstoneAccent -> RedstoneAccent
        isActive -> ButtonActive
        else -> ButtonDefault
    }

    val borderColor = when {
        isActive && accentColor == RedstoneAccent -> RedstoneAccentPressed
        isActive -> BorderSelected
        else -> ButtonBorder
    }

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
                indication = ripple(bounded = true, color = RedstoneAccentPressed),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextWhite,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun CyberBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = RedstoneAccent
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(ButtonDefault)
            .border(BorderStroke(1.dp, BorderSubtle), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            color = if (accentColor == RedstoneAccent) RedstoneBright else TextWhite,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
