package com.dailysystems.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dailysystems.app.ui.theme.AppTheme

/**
 * Grip for drag-to-reorder. Pass the list's draggable-handle modifier as [modifier]. Screen-reader
 * users get "Move up" / "Move down" actions instead of dragging.
 */
@Composable
fun DragHandle(label: String, modifier: Modifier, onMoveUp: (() -> Unit)?, onMoveDown: (() -> Unit)?) {
    Box(
        modifier
            .size(40.dp, 48.dp)
            .semantics {
                contentDescription = "Drag to reorder $label"
                customActions = listOfNotNull(
                    onMoveUp?.let { f -> CustomAccessibilityAction("Move up") { f(); true } },
                    onMoveDown?.let { f -> CustomAccessibilityAction("Move down") { f(); true } },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.Grip, null, Modifier.size(20.dp), tint = AppTheme.colors.muted)
    }
}
