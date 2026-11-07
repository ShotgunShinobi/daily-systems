package com.dailysystems.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailysystems.app.domain.PriorityLabel
import com.dailysystems.app.ui.theme.AppTheme
import kotlinx.coroutines.delay

val CardShape = RoundedCornerShape(14.dp)

/** Label color from data, or the neutral "socket" grey for no label. */
@Composable
fun labelColor(label: PriorityLabel?): Color = label?.let { Color(it.colorArgb) } ?: AppTheme.colors.socket

/** White card with the prototype's soft shadow. */
@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .shadow(if (AppTheme.colors.isDark) 6.dp else 3.dp, CardShape, ambientColor = Color(0x22141C26), spotColor = Color(0x22141C26))
            .clip(CardShape)
            .background(AppTheme.colors.surface),
        content = content,
    )
}

/** Label chip: dot + name on a 14% tint with a 38% border (SPEC §5). Tapping opens the picker. */
@Composable
fun LabelChip(label: PriorityLabel?, onClick: (() -> Unit)?, modifier: Modifier = Modifier, maxWidth: Dp = 150.dp) {
    val c = labelColor(label)
    val name = label?.name ?: "No label"
    val shape = RoundedCornerShape(99.dp)
    Box(
        modifier
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = "Priority: $name" + if (onClick != null) ". Tap to change" else "" },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .widthIn(max = maxWidth)
                .clip(shape)
                .background(c.copy(alpha = 0.14f))
                .border(1.dp, c.copy(alpha = 0.38f), shape)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(c))
            Spacer(Modifier.width(6.dp))
            Text(name, style = TextStyles.chip, color = AppTheme.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Selectable option chip used in editors. */
@Composable
fun OptionChip(text: String, color: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(99.dp)
    val colors = AppTheme.colors
    Row(
        modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, if (selected) color else colors.line, shape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = TextStyles.chip.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) colors.ink else colors.muted,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Every editor shows labels as selectable chips (plus "No label" when allowed). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelOptions(
    labels: List<PriorityLabel>,
    selected: String?,
    onSelect: (String?) -> Unit,
    allowNone: Boolean = true,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEach { l -> OptionChip(l.name, Color(l.colorArgb), selected == l.id, { onSelect(l.id) }) }
        if (allowNone) {
            OptionChip("No label", AppTheme.colors.socket, selected == null || labels.none { it.id == selected }, { onSelect(null) })
        }
    }
}

/** Round check; the ring is tinted by the item's label color. */
@Composable
fun CheckCircle(checked: Boolean, color: Color, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics {
                contentDescription = label
                stateDescription = if (checked) "Done" else "Not done"
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (checked) color else Color.Transparent)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(AppIcons.Check, null, Modifier.size(14.dp), tint = Color.White)
        }
    }
}

@Composable
fun SectionHeader(text: String, count: Int? = null, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
        Text(text.uppercase(), style = AppTheme.type.sectionLabel, color = AppTheme.colors.muted)
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text("$count", style = AppTheme.type.monoSmall, color = AppTheme.colors.muted)
        }
    }
}

enum class ButtonKind { Primary, Secondary, Ghost, Danger, DangerArmed }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Secondary,
    icon: ImageVector? = null,
    small: Boolean = false,
    enabled: Boolean = true,
) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val (bg, fg, border) = when (kind) {
        ButtonKind.Primary -> Triple(c.ink, c.background, c.ink)
        ButtonKind.Secondary -> Triple(c.surface, c.ink, c.line)
        ButtonKind.Ghost -> Triple(Color.Transparent, c.accent, Color.Transparent)
        ButtonKind.Danger -> Triple(Color.Transparent, c.danger, c.danger.copy(alpha = 0.4f))
        ButtonKind.DangerArmed -> Triple(c.danger, c.surface, c.danger)
    }
    Row(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .clip(shape)
                .background(bg)
                .border(BorderStroke(1.dp, border), shape)
                .padding(horizontal = if (small) 11.dp else 14.dp, vertical = if (small) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(16.dp), tint = fg)
                Spacer(Modifier.width(6.dp))
            }
            Text(text, color = fg, style = TextStyles.button.copy(fontSize = if (small) 13.sp else 14.sp))
        }
    }
}

/** Two-tap delete: the first tap arms it (turns red, "Tap again to delete"), the second confirms. */
@Composable
fun DeleteButton(label: String, onConfirm: () -> Unit, modifier: Modifier = Modifier, small: Boolean = false) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(3500)
            armed = false
        }
    }
    AppButton(
        text = if (armed) "Tap again to delete" else label,
        onClick = { if (armed) onConfirm() else armed = true },
        kind = if (armed) ButtonKind.DangerArmed else ButtonKind.Danger,
        modifier = modifier,
        small = small,
    )
}

@Composable
fun IconAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, tint: Color = AppTheme.colors.muted) {
    Box(
        modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(20.dp).alpha(if (enabled) 1f else 0.35f), tint = tint)
    }
}

/** Segmented control (filters, schedule type, Step/Slot). */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier
            .clip(shape)
            .background(c.surface2)
            .border(1.dp, c.line, shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, text) ->
            val on = value == selected
            Box(
                Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .then(if (on) Modifier.shadow(1.dp, RoundedCornerShape(8.dp)) else Modifier)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) c.surface else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(value) }
                    .semantics { this.selected = on }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text, style = TextStyles.button.copy(fontSize = 13.sp), color = if (on) c.ink else c.muted, maxLines = 1)
            }
        }
    }
}

/** Uppercase field caption used throughout editors. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = AppTheme.type.sectionLabel, color = AppTheme.colors.muted, modifier = modifier.padding(bottom = 6.dp))
}

@Composable
fun Field(label: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        content()
    }
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier, color: Color = AppTheme.colors.muted) {
    Text(text, style = TextStyles.hint, color = color, modifier = modifier)
}

/** Text input styled like the prototype: inset surface, hairline border, accent on focus. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    textStyle: TextStyle = TextStyles.input,
    singleLine: Boolean = true,
    minHeight: Dp = 44.dp,
    bordered: Boolean = true,
    background: Color = AppTheme.colors.surface2,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
) {
    val c = AppTheme.colors
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = textStyle.copy(color = c.ink),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .onFocusChangedCompat { focused = it },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = minHeight)
                    .then(
                        if (bordered) Modifier.clip(shape).background(background).border(1.dp, if (focused) c.accent else c.line, shape)
                        else Modifier,
                    )
                    .padding(if (bordered) contentPadding else PaddingValues(vertical = 6.dp)),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty()) Text(placeholder, style = textStyle, color = c.muted.copy(alpha = 0.8f))
                inner()
            }
        },
    )
}

/** Dashed-outline card for empty states. */
@Composable
fun EmptyCard(title: String?, body: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    val c = AppTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(c.surface)
            .dashedBorder(c.line, 14.dp, 1.5.dp)
            .padding(horizontal = 18.dp, vertical = 22.dp),
    ) {
        if (title != null) {
            Text(title, style = AppTheme.type.cardTitle, color = c.ink)
            Spacer(Modifier.height(6.dp))
        }
        Text(body, style = TextStyles.body, color = c.muted)
        Spacer(Modifier.height(14.dp))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), content = { actions() })
    }
}

@Composable
fun ExampleTag() {
    val c = AppTheme.colors
    Text(
        "example",
        style = AppTheme.type.monoSmall.copy(fontSize = 10.sp),
        color = c.muted,
        modifier = Modifier.border(1.dp, c.line, RoundedCornerShape(6.dp)).padding(horizontal = 4.dp),
    )
}

/** Thin progress bar. */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Box(modifier.height(6.dp).clip(RoundedCornerShape(6.dp)).background(c.line)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(6.dp)).background(c.accent))
    }
}
