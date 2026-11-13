package com.gigi.cca.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gigi.cca.shared.data.Repository
import com.gigi.cca.shared.isDesktopApp
import com.gigi.cca.shared.model.Attachment
import com.gigi.cca.shared.model.Homework
import com.gigi.cca.shared.parseDate
import com.gigi.cca.shared.relativeLabel
import com.gigi.cca.shared.shortLabel
import com.gigi.cca.shared.today
import com.gigi.cca.shared.ui.icons.AppIcons
import com.gigi.cca.shared.ui.icons.description
import com.gigi.cca.shared.ui.icons.link_2
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** One row of the homework list. */
private sealed interface ListRow {
    val key: String

    data class Header(val dateKey: String, val label: String, val overdue: Boolean) : ListRow {
        override val key get() = "header-$dateKey"
    }

    data class TodayBar(val date: LocalDate) : ListRow {
        override val key get() = "today"
    }

    data class Item(val homework: Homework) : ListRow {
        override val key get() = "homework-${homework.id}"
    }
}

/**
 * Groups homework by due date, with the "Today" bar where today falls (it doubles as the header
 * for anything due today).
 */
private fun buildRows(homeworks: List<Homework>, today: LocalDate): List<ListRow> = buildList {
    val todayKey = today.toString()
    var todayAdded = false
    homeworks.groupBy { it.dueDate }.forEach { (dateKey, items) ->
        if (!todayAdded && dateKey >= todayKey) {
            add(ListRow.TodayBar(today))
            todayAdded = true
        }
        if (dateKey != todayKey) {
            add(ListRow.Header(dateKey, parseDate(dateKey)?.relativeLabel(today) ?: "No due date", dateKey < todayKey && items.any { !it.ticked }))
        }
        items.forEach { add(ListRow.Item(it)) }
    }
    if (!todayAdded && isNotEmpty()) add(ListRow.TodayBar(today))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeworkListScreen(repository: Repository, onOpen: (Int) -> Unit) {
    val homeworks by repository.homeworks.collectAsState()
    val state by repository.homeworkState.collectAsState()
    val hideCompleted by repository.hideCompleted.collectAsState()
    val today = remember { today() }
    val todayKey = today.toString()
    val scope = rememberCoroutineScope()
    // Keep just-ticked cards on screen briefly so you see the tick before they slide away.
    val justTicked = remember { mutableStateListOf<Int>() }

    LaunchedEffect(Unit) { repository.refreshHomeworks(force = false) }

    val visible = homeworks.filter { !hideCompleted || !it.ticked || it.id in justTicked }
    val rows = remember(visible, today) { buildRows(visible, today) }
    val todayIndex = rows.indexOfFirst { it is ListRow.TodayBar }
    val toDo = homeworks.count { !it.ticked && it.dueDate >= todayKey }

    val listState = rememberLazyListState()
    var scrolledToToday by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(todayIndex >= 0) {
        if (!scrolledToToday && todayIndex >= 0) {
            listState.scrollToItem(todayIndex)
            scrolledToToday = true
        }
    }
    val todayOnScreen by remember(todayIndex) {
        derivedStateOf { todayIndex < 0 || listState.layoutInfo.visibleItemsInfo.any { it.index == todayIndex } }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            AppTopBar("Homework", repository, refreshing = state.loading, onRefresh = { repository.refreshHomeworks() })
        },
        floatingActionButton = {
            AnimatedVisibility(visible = !todayOnScreen, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                val above = listState.firstVisibleItemIndex > todayIndex
                ExtendedFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(todayIndex) } },
                    icon = { Icon(if (above) AppIcons.ArrowUp else AppIcons.ArrowDown, null) },
                    text = { Text("Today") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.contentWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (toDo == 0) "All caught up" else "$toDo to do",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = hideCompleted,
                    onClick = { repository.setHideCompleted(!hideCompleted) },
                    label = { Text("Hide completed") },
                )
            }
            state.error?.let { ErrorBanner(it, Modifier.contentWidth()) }

            RefreshBox(isRefreshing = state.loading, onRefresh = { repository.refreshHomeworks() }, modifier = Modifier.fillMaxSize()) {
                HomeworkList(rows, listState, emptyText = when {
                    state.loading -> "Loading homework…"
                    homeworks.isEmpty() -> "No homework yet. Press refresh to check again."
                    else -> "No homework left to do."
                }, onOpen = onOpen, onToggle = { homework ->
                    if (!homework.ticked) {
                        justTicked += homework.id
                        scope.launch {
                            delay(900)
                            justTicked -= homework.id
                        }
                    }
                    repository.toggleHomework(homework.id)
                })
                ListScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeworkList(
    rows: List<ListRow>,
    listState: LazyListState,
    emptyText: String,
    onOpen: (Int) -> Unit,
    onToggle: (Homework) -> Unit,
) {
    // Full width so the empty space beside the cards scrolls too; each row is centred.
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().dragToScroll(listState),
        contentPadding = PaddingValues(bottom = 88.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (rows.isEmpty()) centeredItem(key = "empty") { EmptyMessage(emptyText) }
        rows.forEach { row ->
            when (row) {
                is ListRow.Header -> stickyHeader(key = row.key) { DueHeader(row.label, row.overdue) }
                is ListRow.TodayBar -> stickyHeader(key = row.key) { TodayBar(row.date) }
                is ListRow.Item -> item(key = row.key) {
                    HomeworkCard(
                        homework = row.homework,
                        modifier = Modifier.animateItem().contentWidth(),
                        onOpen = { onOpen(row.homework.id) },
                        onToggle = { onToggle(row.homework) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DueHeader(label: String, overdue: Boolean) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = when {
                    overdue -> "Overdue · $label"
                    label == "Tomorrow" || label == "Yesterday" -> "Due ${label.lowercase()}"
                    else -> "Due $label"
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.contentWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}

/** The bar marking where today is in the list. */
@Composable
private fun TodayBar(date: LocalDate) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.contentWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Today", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(date.shortLabel(), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun Homework.subtitle(): String =
    listOf(subject, teacher, completionTime).filter { it.isNotBlank() }.joinToString(" · ")

@Composable
private fun HomeworkCard(homework: Homework, onOpen: () -> Unit, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(if (homework.ticked) 0.6f else 1f)
    Card(
        onClick = onOpen,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp).alpha(alpha),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 14.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = homework.ticked, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    homework.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (homework.ticked) TextDecoration.LineThrough else null,
                )
                Text(
                    homework.subtitle(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (homework.preview.isNotBlank()) {
                    Text(
                        homework.preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (homework.attachments.isNotEmpty()) {
                    val n = homework.attachments.size
                    Text(
                        "$n attachment${if (n == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkDetailScreen(repository: Repository, id: Int, onBack: () -> Unit) {
    val homeworks by repository.homeworks.collectAsState()
    val homework = homeworks.firstOrNull { it.id == id }
    val openUrl = rememberUrlOpener()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(homework?.subject?.ifBlank { null } ?: "Homework", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        if (homework == null) {
            EmptyMessage("This homework isn't available any more.", Modifier.padding(padding))
            return@Scaffold
        }
        val body = rememberHomeworkBody(homework.description)
        val scrollState = rememberScrollState()
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize().dragToScroll(scrollState).verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.contentWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(homework.title, style = MaterialTheme.typography.headlineSmall)
                    Text(homework.subtitle(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoPill(
                            "Set ${parseDate(homework.issueDate)?.shortLabel() ?: "?"}",
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer,
                            Modifier.weight(1f),
                        )
                        InfoPill(
                            "Due ${parseDate(homework.dueDate)?.shortLabel() ?: "?"}",
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                            Modifier.weight(1f),
                        )
                    }
                    Card(
                        onClick = { repository.toggleHomework(homework.id) },
                        colors = if (homework.ticked) {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            CardDefaults.cardColors()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = homework.ticked, onCheckedChange = { repository.toggleHomework(homework.id) })
                            Text(if (homework.ticked) "Completed" else "Mark as completed", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    if (body.text.isNotBlank()) {
                        // On desktop, dragging (or a finger on a touchscreen) should scroll, not select.
                        if (isDesktopApp) {
                            Text(body, style = MaterialTheme.typography.bodyLarge)
                        } else {
                            SelectionContainer { Text(body, style = MaterialTheme.typography.bodyLarge) }
                        }
                    }
                    if (homework.attachments.isNotEmpty()) {
                        Text("Attachments", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        homework.attachments.forEach { attachment ->
                            AttachmentRow(attachment) { openUrl(attachment.url) }
                        }
                    }
                    Spacer(Modifier.size(24.dp))
                }
            }
            ColumnScrollbar(scrollState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}

@Composable
private fun InfoPill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@Composable
private fun AttachmentRow(attachment: Attachment, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (attachment.isFile) description else link_2,
                contentDescription = if (attachment.isFile) "File" else "Link",
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                attachment.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
