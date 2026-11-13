package com.gigi.cca.shared

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ClassCharts",
        state = rememberWindowState(width = 1000.dp, height = 760.dp),
    ) {
        App().Ui()
    }
}
