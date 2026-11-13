@file:OptIn(ExperimentalTime::class)

package com.gigi.cca.shared

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val MILLIS_PER_DAY = 86_400_000L
private val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

fun LocalDate.plusDays(days: Int): LocalDate = plus(days, DateTimeUnit.DAY)

fun parseDate(text: String): LocalDate? = runCatching { LocalDate.parse(text.trim().take(10)) }.getOrNull()

/** Parses "2025-11-04T10:23:00+00:00" or "2025-11-04 10:23:00" as school-local wall-clock time. */
fun parseDateTime(text: String): LocalDateTime? =
    runCatching { LocalDateTime.parse(text.trim().take(19).replace(' ', 'T')) }.getOrNull()

val LocalDate.isWeekend: Boolean get() = dayOfWeek.ordinal >= 5

/** "Tue 28 Apr" */
fun LocalDate.shortLabel(): String =
    "${DAY_NAMES[dayOfWeek.ordinal].take(3)} $dayOfMonth ${MONTH_NAMES[month.ordinal].take(3)}"

/** "Tuesday 28 April", with the year added when it isn't this year. */
fun LocalDate.longLabel(): String {
    val base = "${DAY_NAMES[dayOfWeek.ordinal]} $dayOfMonth ${MONTH_NAMES[month.ordinal]}"
    return if (year != today().year) "$base $year" else base
}

fun LocalDate.relativeLabel(today: LocalDate = today()): String = when (today.daysUntil(this)) {
    0 -> "Today"
    1 -> "Tomorrow"
    -1 -> "Yesterday"
    else -> longLabel()
}

/** "28/04/2010" */
fun LocalDate.ukFormat(): String =
    "${dayOfMonth.toString().padStart(2, '0')}/${(month.ordinal + 1).toString().padStart(2, '0')}/$year"

fun LocalDateTime.timeLabel(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

fun LocalDate.startOfWeek(): LocalDate = plusDays(-dayOfWeek.ordinal)

fun LocalDate.startOfSchoolYear(): LocalDate =
    if (month.ordinal >= 8) LocalDate(year, 9, 1) else LocalDate(year - 1, 9, 1)

/** Material date pickers work in UTC-midnight milliseconds. */
fun LocalDate.toPickerMillis(): Long = toEpochDays().toLong() * MILLIS_PER_DAY

fun pickerMillisToDate(millis: Long): LocalDate =
    LocalDate(1970, 1, 1).plus(millis.floorDiv(MILLIS_PER_DAY), DateTimeUnit.DAY)
