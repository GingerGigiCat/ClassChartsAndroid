package com.gigi.cca.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.platform.LocalUriHandler
import co.touchlab.kermit.Logger
import com.gigi.cca.shared.usesTouchInput
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gigi.cca.shared.data.Repository
import com.gigi.cca.shared.pickerMillisToDate
import com.gigi.cca.shared.toPickerMillis
import com.gigi.cca.shared.ui.icons.AppIcons
import kotlinx.datetime.LocalDate

/** Widest that content gets, so it stays readable in wide desktop and web windows. */
val ContentMaxWidth = 760.dp

/**
 * Caps the width of one piece of content. Lists themselves stay full width (with items centred),
 * so the empty space either side still scrolls.
 */
fun Modifier.contentWidth(): Modifier = widthIn(max = ContentMaxWidth).fillMaxWidth()

/** A list item centred at [ContentMaxWidth]. */
fun LazyListScope.centeredItem(key: Any? = null, content: @Composable () -> Unit) {
    item(key = key) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.contentWidth()) { content() }
        }
    }
}

/**
 * Pull-to-refresh on touch screens. With a mouse it's a plain box with a progress bar, because
 * wheel-scrolling past the top would pull the indicator down and leave it stuck there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val touch = remember { usesTouchInput() }
    if (touch) {
        PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier, content = content)
    } else {
        Box(modifier) {
            content()
            if (isRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

/**
 * ClassCharts attachments are Google Cloud signed links that force a download. For the older
 * (V2) link style the download flag isn't part of the signature, so it can be switched to
 * "inline" and PDFs, images and text open in the browser instead. Raw spaces and quotes are
 * escaped too, as desktop Java refuses to open links containing them.
 */
internal fun browserFriendlyUrl(url: String): String {
    var result = url
    val key = "response-content-disposition="
    val start = result.indexOf(key)
    val signedV2 = "GoogleAccessId=" in result && "X-Goog-Signature" !in result
    if (signedV2 && start >= 0) {
        val valueStart = start + key.length
        if (result.regionMatches(valueStart, "attachment", 0, "attachment".length, ignoreCase = true)) {
            result = result.substring(0, valueStart) + "inline" + result.substring(valueStart + "attachment".length)
        }
    }
    return result.replace(" ", "%20").replace("\"", "%22")
}

/** Opens a link or attachment in the browser (or the matching app on Android). */
@Composable
fun rememberUrlOpener(): (String) -> Unit {
    val handler = LocalUriHandler.current
    return remember(handler) {
        { url ->
            runCatching { handler.openUri(browserFriendlyUrl(url)) }
                .onFailure { Logger.w(it) { "Couldn't open $url" } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, repository: Repository, refreshing: Boolean, onRefresh: () -> Unit) {
    var confirmLogout by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(title) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        actions = {
            IconButton(onClick = onRefresh, enabled = !refreshing) { Icon(AppIcons.Refresh, "Refresh") }
            IconButton(onClick = { confirmLogout = true }) { Icon(AppIcons.Logout, "Log out") }
        },
    )
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            text = { Text("Your saved homework, timetable and behaviour will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; repository.logout() }) { Text("Log out") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialogFor(
    initial: LocalDate?,
    inputMode: Boolean,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.toPickerMillis(),
        initialDisplayMode = if (inputMode) DisplayMode.Input else DisplayMode.Picker,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPicked(pickerMillisToDate(it)) }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state)
    }
}
