package com.gigi.cca.shared

import androidx.compose.ui.platform.UriHandler

expect fun platform(): String

expect fun openUriMime(uri: String)