package com.dailysystems.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

/** Named text styles on top of the Material typography. */
object TextStyles {
    val chip: TextStyle @Composable get() = MaterialTheme.typography.labelMedium
    val button: TextStyle @Composable get() = MaterialTheme.typography.labelLarge
    val hint: TextStyle @Composable get() = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.sp)
    val input: TextStyle @Composable get() = MaterialTheme.typography.bodyLarge
    val body: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium
    val rowTitle: TextStyle @Composable get() = MaterialTheme.typography.bodyLarge
    val rowSub: TextStyle @Composable get() = MaterialTheme.typography.bodySmall
}

fun Modifier.onFocusChangedCompat(onChange: (Boolean) -> Unit): Modifier =
    onFocusChanged { onChange(it.isFocused) }

/** Dashed rounded outline (open slots, empty states). */
fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp, dash: Dp? = null): Modifier = drawBehind {
    val w = width.toPx()
    val d = (dash ?: (width * 3)).toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(w / 2, w / 2),
        size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(d, d * 0.8f))),
    )
}
