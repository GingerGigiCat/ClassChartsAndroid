package com.gigi.cca.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Student(
    val id: Int,
    val name: String = "",
    val firstName: String = "",
    val lastName: String = "",
)

@Serializable
data class Attachment(val name: String, val url: String, val isFile: Boolean)

@Serializable
data class Homework(
    val id: Int,
    /** homework.status.id, which is what the tick endpoint wants. */
    val statusId: Int,
    val title: String,
    val subject: String = "",
    val teacher: String = "",
    val lesson: String = "",
    /** Raw HTML from ClassCharts. */
    val description: String = "",
    /** Plain-text snippet of the description, precomputed so list scrolling stays cheap. */
    val preview: String = "",
    val issueDate: String = "",
    val dueDate: String = "",
    val completionTime: String = "",
    val ticked: Boolean = false,
    val attachments: List<Attachment> = emptyList(),
    /** A task the user added themselves. These only live on this device and have negative ids. */
    val custom: Boolean = false,
    /** The user's own text for a custom task (its description is this as HTML). */
    val notes: String = "",
)

@Serializable
data class Lesson(
    val key: String,
    val subject: String,
    val lessonName: String = "",
    val teacher: String = "",
    val room: String = "",
    val periodName: String = "",
    val periodNumber: String = "",
    val date: String = "",
    val start: String = "",
    val end: String = "",
    val isAlternative: Boolean = false,
)

@Serializable
data class ReasonCount(val reason: String, val count: Int)

@Serializable
data class TimelinePoint(val label: String, val start: String, val positive: Int, val negative: Int)

@Serializable
data class BehaviourSummary(
    val timeline: List<TimelinePoint> = emptyList(),
    val positiveReasons: List<ReasonCount> = emptyList(),
    val negativeReasons: List<ReasonCount> = emptyList(),
) {
    val positive: Int
        get() = timeline.sumOf { it.positive }.takeIf { it > 0 } ?: positiveReasons.sumOf { it.count }
    val negative: Int
        get() = timeline.sumOf { it.negative }.takeIf { it > 0 } ?: negativeReasons.sumOf { it.count }
}

@Serializable
data class ActivityPoint(
    val id: Long,
    val type: String = "",
    /** "positive", "negative", "blank" or "". */
    val polarity: String = "",
    val reason: String = "",
    val score: Int = 0,
    val timestamp: String = "",
    val lesson: String = "",
    val teacher: String = "",
    val room: String = "",
    val note: String = "",
)

@Serializable
data class BehaviourData(val summary: BehaviourSummary, val activity: List<ActivityPoint>)
