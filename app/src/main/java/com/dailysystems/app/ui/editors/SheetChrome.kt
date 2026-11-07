package com.dailysystems.app.ui.editors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.dailysystems.app.ui.components.AppButton
import com.dailysystems.app.ui.components.ButtonKind
import com.dailysystems.app.ui.theme.AppTheme

/** Editors close with "Done" (no Save buttons: everything already saved). */
@Composable
fun SheetScaffold(eyebrow: String, onDone: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(eyebrow.uppercase(), style = AppTheme.type.sectionLabel, color = AppTheme.colors.muted, modifier = Modifier.weight(1f))
            AppButton("Done", onDone, kind = ButtonKind.Ghost, modifier = Modifier.testTag("sheet-done"))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content,
        )
    }
}
