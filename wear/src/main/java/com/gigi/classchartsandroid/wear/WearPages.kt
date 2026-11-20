package com.gigi.classchartsandroid.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.SplitCheckboxButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import com.gigi.cca.shared.data.BehaviourRange
import com.gigi.cca.shared.data.Repository
import com.gigi.cca.shared.isWeekend
import com.gigi.cca.shared.model.Homework
import com.gigi.cca.shared.model.Lesson
import com.gigi.cca.shared.now
import com.gigi.cca.shared.parseDate
import com.gigi.cca.shared.parseDateTime
import com.gigi.cca.shared.plusDays
import com.gigi.cca.shared.relativeLabel
import com.gigi.cca.shared.shortLabel
import com.gigi.cca.shared.timeLabel
import com.gigi.cca.shared.today
import com.gigi.cca.shared.ui.descriptionText
import kotlinx.coroutines.delay

private val PositiveGreen = Color(0xFF81C784)

/** The three app pages side by side: swipe horizontally between them, scroll each vertically. */
@Composable
fun WearPages(repository: Repository, onOpenHomework: (Int) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    HorizontalPagerScaffold(pagerState = pagerState) {
        HorizontalPager(state = pagerState) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
                when (page) {
                    0 -> HomeworkPage(repository, onOpenHomework)
                    1 -> TimetablePage(repository)
                    else -> BehaviourPage(repository)
                }
            }
        }
    }
}

/** A watch screen with a curved scrolling list and scroll indicator. */
@Composable
private fun WearList(
    state: ScalingLazyListState = rememberScalingLazyListState(),
    content: androidx.wear.compose.foundation.lazy.ScalingLazyListScope.() -> Unit,
) {
    ScreenScaffold(
        scrollState = state,
        scrollIndicator = { ScrollIndicator(state) },
    ) { padding ->
        ScalingLazyColumn(state = state, contentPadding = padding, modifier = Modifier.fillMaxSize(), content = content)
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---- Homework: scroll down through due dates ----

@Composable
private fun HomeworkPage(repository: Repository, onOpen: (Int) -> Unit) {
    val homeworks by repository.homeworks.collectAsState()
    val state by repository.homeworkState.collectAsState()
    val today = remember { today() }
    val todayKey = today.toString()
    LaunchedEffect(Unit) { repository.refreshHomeworks(force = false) }

    // Anything still to do, plus finished work that isn't due yet.
    val groups = homeworks.filter { !it.ticked || it.dueDate >= todayKey }.groupBy { it.dueDate }.toList()

    WearList {
        item { ListHeader { Text("Homework") } }
        if (groups.isEmpty()) item { Message(if (state.loading) "Loading…" else "All caught up!") }
        state.error?.let { item { Message(it) } }
        groups.forEach { (dateKey, items) ->
            val overdue = dateKey < todayKey
            item(key = "due-$dateKey") {
                ListHeader {
                    Text(
                        if (overdue) "Overdue" else "Due ${parseDate(dateKey)?.relativeLabel(today) ?: dateKey}",
                        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            items(items, key = { it.id }) { homework -> HomeworkButton(homework, repository, onOpen) }
        }
        item {
            Button(
                onClick = { repository.refreshHomeworks() },
                enabled = !state.loading,
                colors = ButtonDefaults.filledTonalButtonColors(),
                label = { Text(if (state.loading) "Refreshing…" else "Refresh") },
            )
        }
    }
}

/** Tap the text to open it, or the tick box to mark it done. */
@Composable
private fun HomeworkButton(homework: Homework, repository: Repository, onOpen: (Int) -> Unit) {
    SplitCheckboxButton(
        checked = homework.ticked,
        onCheckedChange = { repository.toggleHomework(homework.id) },
        toggleContentDescription = "Done",
        onContainerClick = { onOpen(homework.id) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(homework.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = {
            Text(
                if (homework.custom) "My task" else homework.subject,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
fun HomeworkDetail(repository: Repository, id: Int?) {
    val homeworks by repository.homeworks.collectAsState()
    val homework = homeworks.firstOrNull { it.id == id }
    val paragraphs = remember(homework?.description) {
        homework?.descriptionText()?.split("\n")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    }
    WearList {
        if (homework == null) {
            item { Message("This homework isn't available any more.") }
            return@WearList
        }
        item { ListHeader { Text(if (homework.custom) "My task" else homework.subject.ifBlank { "Homework" }) } }
        item {
            Text(homework.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        item { Message("Due ${parseDate(homework.dueDate)?.shortLabel() ?: "?"}") }
        item {
            CheckboxButton(
                checked = homework.ticked,
                onCheckedChange = { repository.toggleHomework(homework.id) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (homework.ticked) "Completed" else "Mark as done") },
            )
        }
        items(paragraphs) { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (homework.attachments.isNotEmpty()) {
            val n = homework.attachments.size
            item { Message("$n attachment${if (n == 1) "" else "s"}. Open on your phone.") }
        }
    }
}

// ---- Timetable: scroll down through the days ----

@Composable
private fun TimetablePage(repository: Repository) {
    val today = remember { today() }
    val days = remember(today) { (0 until 28).map { today.plusDays(it) }.filterNot { it.isWeekend } }
    val lessons by repository.lessons.collectAsState()
    val states by repository.lessonStates.collectAsState()
    val now by produceState(now()) {
        while (true) {
            delay(30_000)
            value = now()
        }
    }

    WearList {
        days.forEach { day ->
            val key = day.toString()
            item(key = "day-$key") {
                // Days load as they scroll into view.
                LaunchedEffect(key) { repository.loadDay(day) }
                ListHeader { Text(day.relativeLabel(today)) }
            }
            val dayLessons = lessons[key]
            when {
                dayLessons == null -> item(key = "wait-$key") {
                    Message(states[key]?.error?.let { "Couldn't load this day." } ?: "Loading…")
                }
                dayLessons.isEmpty() -> item(key = "none-$key") { Message("No lessons") }
                else -> items(dayLessons, key = { "$key-${it.key}" }) { LessonCard(it, now) }
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson, now: kotlinx.datetime.LocalDateTime) {
    val start = parseDateTime(lesson.start)
    val end = parseDateTime(lesson.end)
    val current = start != null && end != null && now >= start && now < end
    TitleCard(
        title = { Text(lesson.subject, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        time = { Text(start?.timeLabel() ?: "") },
        subtitle = {
            Text(
                listOf(lesson.room, lesson.teacher).filter { it.isNotBlank() }.joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = if (current) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
                subtitleColor = MaterialTheme.colorScheme.onPrimaryContainer,
                timeColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---- Behaviour ----

@Composable
private fun BehaviourPage(repository: Repository) {
    val behaviour by repository.behaviour.collectAsState()
    var rangeIndex by rememberSaveable { mutableStateOf(0) }
    val range = BehaviourRange.entries[rangeIndex]
    val data = behaviour[range]
    LaunchedEffect(range) { repository.loadBehaviour(range) }

    WearList {
        item { ListHeader { Text("Behaviour") } }
        item {
            // Tap to switch between this week, the last 30 days and the school year.
            Button(
                onClick = { rangeIndex = (rangeIndex + 1) % BehaviourRange.entries.size },
                colors = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier.fillMaxWidth(),
                label = { Text(range.label) },
            )
        }
        if (data == null) {
            item { Message("Loading…") }
            return@WearList
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("+${data.summary.positive}", style = MaterialTheme.typography.numeralSmall, color = PositiveGreen)
                Text("−${data.summary.negative}", style = MaterialTheme.typography.numeralSmall, color = MaterialTheme.colorScheme.error)
            }
        }
        if (data.activity.isEmpty()) item { Message("No points in this period") }
        items(data.activity.take(20), key = { it.id }) { point ->
            TitleCard(
                title = { Text(point.reason.ifBlank { point.type }, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                time = {
                    Text(
                        if (point.score > 0) "+${point.score}" else point.score.toString(),
                        color = when (point.polarity) {
                            "positive" -> PositiveGreen
                            "negative" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                subtitle = {
                    Text(
                        listOfNotNull(parseDateTime(point.timestamp)?.date?.shortLabel(), point.lesson.ifBlank { null }).joinToString(" · "),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
