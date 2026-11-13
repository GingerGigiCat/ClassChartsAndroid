package com.gigi.cca.shared

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation

@OptIn(ExperimentalComposeUiApi::class, ExperimentalBrowserHistoryApi::class)
fun main() {
    ComposeViewport("app") {
        // Same approach as the molten platform: the address bar follows the current screen,
        // and the browser's back/forward buttons and links like #timetable work.
        App().Ui(onNavHostReady = { it.bindToBrowserNavigation() })
    }
}
