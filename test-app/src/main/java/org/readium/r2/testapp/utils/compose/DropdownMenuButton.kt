package org.readium.r2.testapp.utils.compose

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.runtime.*

@Composable
fun DropdownMenuButton(
    text: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    fun dismiss() { isExpanded = false }

    Button(
        onClick = { isExpanded = true },
    ) {
        text()
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            content(::dismiss)
        }
    }
}
