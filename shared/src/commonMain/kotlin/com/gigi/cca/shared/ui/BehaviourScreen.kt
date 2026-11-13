package com.gigi.cca.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gigi.cca.shared.data.BehaviourRange
import com.gigi.cca.shared.data.Repository
import com.gigi.cca.shared.model.ActivityPoint
import com.gigi.cca.shared.model.BehaviourSummary
import com.gigi.cca.shared.model.ReasonCount
import com.gigi.cca.shared.model.TimelinePoint
import com.gigi.cca.shared.parseDate
import com.gigi.cca.shared.parseDateTime
import com.gigi.cca.shared.shortLabel
import com.gigi.cca.shared.timeLabel
import com.gigi.cca.shared.ui.theme.BehaviourColors
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviourScreen(repository: Repository) {
    var rangeIndex by rememberSaveable { mutableIntStateOf(0) }
    val range = BehaviourRange.entries[rangeIndex]
    val all by repository.behaviour.collectAsState()
    val states by repository.behaviourStates.collectAsState()
    val data = all[range]
    val state = states[range]
    val loading = state?.loading == true

    LaunchedEffect(range) { repository.loadBehaviour(range) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            AppTopBar("Behaviour", repository, refreshing = loading, onRefresh = { repository.loadBehaviour(range, force = true) })
        },
    ) { padding ->
        RefreshBox(
            isRefreshing = loading,
            onRefresh = { repository.loadBehaviour(range, force = true) },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            val listState = rememberLazyListState()
            LazyColumn(
                Modifier.fillMaxSize().dragToScroll(listState),
                state = listState,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                centeredItem(key = "ranges") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BehaviourRange.entries.forEachIndexed { i, r ->
                            FilterChip(selected = i == rangeIndex, onClick = { rangeIndex = i }, label = { Text(r.label) })
                        }
                    }
                }
                state?.error?.let { centeredItem(key = "error") { ErrorBanner(it, Modifier.padding(horizontal = 0.dp)) } }
                if (data == null) {
                    if (state?.error == null) {
                        centeredItem(key = "loading") {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        }
                    }
                    return@LazyColumn
                }
                centeredItem(key = "summary") { SummaryCard(data.summary) }
                if (data.summary.timeline.size > 1) centeredItem(key = "chart") { TimelineCard(data.summary.timeline) }
                if (data.summary.positiveReasons.isNotEmpty()) {
                    centeredItem(key = "positive") { ReasonsCard("Positive", data.summary.positiveReasons, BehaviourColors.positive) }
                }
                if (data.summary.negativeReasons.isNotEmpty()) {
                    centeredItem(key = "negative") { ReasonsCard("Negative", data.summary.negativeReasons, BehaviourColors.negative) }
                }
                centeredItem(key = "activity-title") {
                    Text("Activity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                }
                if (data.activity.isEmpty()) centeredItem(key = "activity-empty") { EmptyMessage("No behaviour points in this period") }
                items(data.activity, key = { "activity-${it.id}" }) { ActivityRow(it, Modifier.contentWidth()) }
            }
            ListScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}

@Composable
private fun SummaryCard(summary: BehaviourSummary) {
    val positive = summary.positive
    val negative = summary.negative
    val positiveColor = BehaviourColors.positive
    val negativeColor = BehaviourColors.negative
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row {
                Stat("Positive", positive, positiveColor, Modifier.weight(1f))
                Stat("Negative", negative, negativeColor, Modifier.weight(1f))
            }
            val total = positive + negative
            if (total > 0) {
                Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                    if (positive > 0) Box(Modifier.weight(positive.toFloat()).fillMaxHeight().background(positiveColor))
                    if (negative > 0) Box(Modifier.weight(negative.toFloat()).fillMaxHeight().background(negativeColor))
                }
                Text(
                    "${positive * 100 / total}% positive",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = color)
        Text(value.toString(), style = MaterialTheme.typography.displaySmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TimelineCard(timeline: List<TimelinePoint>) {
    val positiveColor = BehaviourColors.positive
    val negativeColor = BehaviourColors.negative
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val maxValue = max(timeline.maxOf { max(it.positive, it.negative) }, 1)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Week by week", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val slot = size.width / timeline.size
                val barWidth = (slot * 0.6f).coerceAtMost(24.dp.toPx())
                val middle = size.height / 2
                timeline.forEachIndexed { i, point ->
                    val x = i * slot + (slot - barWidth) / 2
                    val up = middle * point.positive / maxValue
                    val down = middle * point.negative / maxValue
                    drawRect(positiveColor, Offset(x, middle - up), Size(barWidth, up))
                    drawRect(negativeColor, Offset(x, middle), Size(barWidth, down))
                }
                drawLine(axisColor, Offset(0f, middle), Offset(size.width, middle), 1.dp.toPx())
            }
            Spacer(Modifier.height(4.dp))
            Row {
                val labelStyle = MaterialTheme.typography.bodySmall
                val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                Text(timeline.first().let { parseDate(it.start)?.shortLabel() ?: it.label }, style = labelStyle, color = labelColor)
                Spacer(Modifier.weight(1f))
                Text(timeline.last().let { parseDate(it.start)?.shortLabel() ?: it.label }, style = labelStyle, color = labelColor)
            }
        }
    }
}

@Composable
private fun ReasonsCard(title: String, reasons: List<ReasonCount>, color: Color) {
    val top = reasons.maxOf { it.count }.coerceAtLeast(1)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = color)
            reasons.take(8).forEach { reason ->
                Column {
                    Row {
                        Text(reason.reason, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(reason.count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { reason.count.toFloat() / top },
                        color = color,
                        trackColor = color.copy(alpha = 0.15f),
                        drawStopIndicator = {},
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(point: ActivityPoint, modifier: Modifier = Modifier) {
    val color = when (point.polarity) {
        "positive" -> BehaviourColors.positive
        "negative" -> BehaviourColors.negative
        else -> MaterialTheme.colorScheme.outline
    }
    val badge = when {
        point.score > 0 -> "+${point.score}"
        point.score < 0 -> point.score.toString()
        else -> "•"
    }
    Card(modifier) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(point.reason.ifBlank { point.type.replace('_', ' ') }, style = MaterialTheme.typography.bodyLarge)
                val details = listOf(point.teacher, point.lesson).filter { it.isNotBlank() }.joinToString(" · ")
                if (details.isNotEmpty()) {
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (point.note.isNotBlank()) Text(point.note, style = MaterialTheme.typography.bodySmall)
            }
            parseDateTime(point.timestamp)?.let { time ->
                Text(
                    "${time.date.shortLabel()}\n${time.timeLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
