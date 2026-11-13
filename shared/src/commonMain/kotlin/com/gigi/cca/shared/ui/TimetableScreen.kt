package com.gigi.cca.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.gigi.cca.shared.data.LoadState
import com.gigi.cca.shared.data.Repository
import com.gigi.cca.shared.isWeekend
import com.gigi.cca.shared.model.Lesson
import com.gigi.cca.shared.now
import com.gigi.cca.shared.parseDateTime
import com.gigi.cca.shared.plusDays
import com.gigi.cca.shared.relativeLabel
import com.gigi.cca.shared.timeLabel
import com.gigi.cca.shared.today
import com.gigi.cca.shared.ui.icons.AppIcons
import com.gigi.cca.shared.ui.icons.calendar_month
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlin.math.abs

private const val MIDDLE_PAGE = 5_000

@Composable
fun TimetableScreen(repository: Repository) {
    val today = remember { today() }
    val pagerState = rememberPagerState(initialPage = MIDDLE_PAGE) { MIDDLE_PAGE * 2 }
    val scope = rememberCoroutineScope()
    val lessons by repository.lessons.collectAsState()
    val states by repository.lessonStates.collectAsState()
    val currentDate = today.plusDays(pagerState.currentPage - MIDDLE_PAGE)
    var showPicker by remember { mutableStateOf(false) }
    val now by produceState(now()) {
        while (true) {
            delay(30_000)
            value = now()
        }
    }

    // Load the settled day plus its neighbours, so swiping shows cached lessons straight away.
    LaunchedEffect(pagerState.settledPage) {
        val date = today.plusDays(pagerState.settledPage - MIDDLE_PAGE)
        repository.loadDay(date)
        repository.loadDay(date.plusDays(1))
        repository.loadDay(date.plusDays(-1))
    }

    fun goTo(date: LocalDate) {
        val target = MIDDLE_PAGE + today.daysUntil(date)
        scope.launch {
            if (abs(target - pagerState.currentPage) > 3) pagerState.scrollToPage(target)
            else pagerState.animateScrollToPage(target)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            AppTopBar(
                "Timetable", repository,
                refreshing = states[currentDate.toString()]?.loading == true,
                onRefresh = { repository.loadDay(currentDate, force = true) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.contentWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { goTo(currentDate.plusDays(-1)) }) { Icon(AppIcons.ChevronLeft, "Previous day") }
                    TextButton(onClick = { showPicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(calendar_month, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(currentDate.relativeLabel(today), style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = { goTo(today) }, enabled = currentDate != today) { Text("Today") }
                    IconButton(onClick = { goTo(currentDate.plusDays(1)) }) { Icon(AppIcons.ChevronRight, "Next day") }
                }
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxSize().dragToSwipe(pagerState),
                ) { page ->
                    val date = today.plusDays(page - MIDDLE_PAGE)
                    val key = date.toString()
                    DayView(date, lessons[key], states[key], now, onRetry = { repository.loadDay(date, force = true) })
                }
        }
    }

    if (showPicker) {
        DatePickerDialogFor(
            initial = currentDate,
            inputMode = false,
            onDismiss = { showPicker = false },
            onPicked = { goTo(it) },
        )
    }
}

@Composable
private fun DayView(date: LocalDate, lessons: List<Lesson>?, state: LoadState?, now: LocalDateTime, onRetry: () -> Unit) {
    when {
        lessons == null && state?.error != null -> Column(Modifier.fillMaxSize().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ErrorBanner(state.error)
            TextButton(onClick = onRetry) { Text("Try again") }
        }
        lessons == null -> Box(Modifier.fillMaxSize().padding(top = 48.dp), contentAlignment = Alignment.TopCenter) {
            CircularProgressIndicator()
        }
        lessons.isEmpty() -> EmptyMessage(if (date.isWeekend) "No lessons. Enjoy the weekend!" else "No lessons")
        else -> Box(Modifier.fillMaxSize()) {
            val listState = rememberLazyListState()
            LazyColumn(
                Modifier.fillMaxSize().dragToScroll(listState),
                state = listState,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                state?.error?.let { centeredItem(key = "error") { ErrorBanner("Showing saved timetable. $it") } }
                itemsIndexed(lessons, key = { i, lesson -> "$i-${lesson.key}" }) { _, lesson ->
                    LessonRow(lesson, now, Modifier.contentWidth())
                }
            }
            ListScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson, now: LocalDateTime, modifier: Modifier = Modifier) {
    val start = parseDateTime(lesson.start)
    val end = parseDateTime(lesson.end)
    val current = start != null && end != null && now >= start && now < end
    val past = end != null && now >= end
    Row(modifier.height(IntrinsicSize.Min).alpha(if (past) 0.6f else 1f)) {
        Column(Modifier.width(52.dp).padding(top = 8.dp), horizontalAlignment = Alignment.End) {
            Text(
                lesson.periodNumber.ifBlank { lesson.periodName },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(start?.timeLabel() ?: "", style = MaterialTheme.typography.bodySmall)
            Text(end?.timeLabel() ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Card(
            modifier = Modifier.weight(1f),
            colors = if (current) {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                CardDefaults.cardColors()
            },
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(lesson.subject, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (current) Text("Now", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                val details = listOf(lesson.room, lesson.teacher).filter { it.isNotBlank() }.joinToString(" · ")
                if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodyMedium)
                if (lesson.lessonName.isNotBlank() && lesson.lessonName != lesson.subject) {
                    Text(lesson.lessonName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
