package com.dailysystems.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailysystems.app.ui.theme.AppTheme

/** −/+ counter (slot size 1–9, repeat interval). */
@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, min: Int = 1, max: Int = 9, unit: String = "to-dos") {
    val c = AppTheme.colors
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, c.line, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−", "Fewer", enabled = value > min) { onChange(value - 1) }
        Text("$value", style = AppTheme.type.mono, color = c.ink, modifier = Modifier.width(22.dp).semantics { contentDescription = "$value $unit" }, textAlign = TextAlign.Center)
        StepperButton("+", "More", enabled = value < max) { onChange(value + 1) }
    }
}

@Composable
private fun StepperButton(symbol: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp, 40.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = TextStyles.button.copy(fontSize = 18.sp), color = if (enabled) AppTheme.colors.ink else AppTheme.colors.line)
    }
}

/** Day toggle used for weekdays and month days. */
@Composable
fun RowScope.DayToggle(text: String, on: Boolean, onClick: () -> Unit, description: String = text, weight: Float = 1f) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .weight(weight)
            .height(44.dp)
            .clip(shape)
            .background(if (on) c.ink else Color.Transparent)
            .border(1.dp, if (on) c.ink else c.line, shape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { selected = on; contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppTheme.type.mono.copy(fontSize = 13.sp), color = if (on) c.background else c.muted)
    }
}

