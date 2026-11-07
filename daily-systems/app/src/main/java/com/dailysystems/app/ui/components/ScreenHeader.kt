package com.dailysystems.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dailysystems.app.ui.theme.AppTheme

/** Top bar: optional mono eyebrow, big title, actions, and the tag icon that opens Labels. */
@Composable
fun ScreenHeader(
    title: String,
    onLabels: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(eyebrow.uppercase(), style = AppTheme.type.monoSmall, color = AppTheme.colors.muted, modifier = Modifier.padding(bottom = 2.dp))
            }
            Text(title, style = AppTheme.type.screenTitle, color = AppTheme.colors.ink, modifier = Modifier.semantics { heading() })
        }
        actions()
        IconAction(AppIcons.Tag, "Edit priority labels", onLabels)
    }
}
