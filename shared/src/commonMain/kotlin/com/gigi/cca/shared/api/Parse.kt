package com.gigi.cca.shared.api

import com.gigi.cca.shared.model.ActivityPoint
import com.gigi.cca.shared.model.Attachment
import com.gigi.cca.shared.model.BehaviourSummary
import com.gigi.cca.shared.model.Homework
import com.gigi.cca.shared.model.Lesson
import com.gigi.cca.shared.model.ReasonCount
import com.gigi.cca.shared.model.Student
import com.gigi.cca.shared.model.TimelinePoint
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

// ClassCharts is loose with types (numbers as strings, `meta: []` when empty, objects keyed by
// index instead of arrays), so everything is read defensively by hand.

internal fun JsonObject.str(key: String): String = (this[key] as? JsonPrimitive)?.contentOrNull?.trim() ?: ""

internal fun JsonObject.int(key: String): Int? =
    (this[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.contentOrNull?.trim()?.toIntOrNull() }

internal fun JsonObject.long(key: String): Long? =
    (this[key] as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.trim()?.toLongOrNull() }

internal fun JsonObject.bool(key: String): Boolean {
    val p = this[key] as? JsonPrimitive ?: return false
    return p.booleanOrNull ?: (p.contentOrNull == "1" || p.contentOrNull == "yes")
}

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonElement?.items(): List<JsonObject> = when (this) {
    is JsonArray -> mapNotNull { it as? JsonObject }
    is JsonObject -> values.mapNotNull { it as? JsonObject }
    else -> emptyList()
}

private val TAG = Regex("<[^>]+>")
private val WHITESPACE = Regex("\\s+")

internal fun htmlPreview(html: String): String = TAG.replace(html, " ")
    .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
    .replace("&quot;", "\"").replace("&#39;", "'").replace("&rsquo;", "’")
    .replace(WHITESPACE, " ").trim().take(240)

internal object Parse {
    fun student(o: JsonObject) = Student(
        id = o.int("id") ?: 0,
        name = o.str("name"),
        firstName = o.str("first_name"),
        lastName = o.str("last_name"),
    )

    fun homework(o: JsonObject): Homework? {
        val status = o.obj("status") ?: return null
        val statusId = status.int("id") ?: return null
        val links = o["validated_links"].items().mapNotNull { l ->
            val url = l.str("link").ifEmpty { l.str("validated_link") }
            if (url.isEmpty()) null else Attachment(name = url, url = url, isFile = false)
        }
        val files = o["validated_attachments"].items().mapNotNull { a ->
            val url = a.str("file").ifEmpty { a.str("validated_file") }
            if (url.isEmpty()) null
            else Attachment(name = a.str("filename").ifEmpty { a.str("file_name") }.ifEmpty { "Attachment" }, url = url, isFile = true)
        }
        val timeValue = o.str("completion_time_value")
        val description = o.str("description")
        return Homework(
            id = o.int("id") ?: statusId,
            statusId = statusId,
            title = o.str("title").ifEmpty { "Untitled homework" },
            subject = o.str("subject"),
            teacher = o.str("teacher"),
            lesson = o.str("lesson"),
            description = description,
            preview = htmlPreview(description),
            issueDate = o.str("issue_date").take(10),
            dueDate = o.str("due_date").take(10),
            completionTime = if (timeValue.isNotEmpty()) "$timeValue ${o.str("completion_time_unit")}".trim() else "",
            ticked = status.str("ticked") == "yes",
            attachments = files + links,
        )
    }

    fun lesson(o: JsonObject) = Lesson(
        key = "${o.str("key")}-${o.str("start_time")}",
        subject = o.str("subject_name").ifEmpty { o.str("lesson_name") },
        lessonName = o.str("lesson_name"),
        teacher = o.str("teacher_name"),
        room = o.str("room_name"),
        periodName = o.str("period_name"),
        periodNumber = o.str("period_number"),
        date = o.str("date").take(10),
        start = o.str("start_time"),
        end = o.str("end_time"),
        isAlternative = o.bool("is_alternative_lesson"),
    )

    fun behaviour(data: JsonElement): BehaviourSummary {
        val o = data as? JsonObject ?: return BehaviourSummary()
        fun reasons(key: String) = o.obj(key)?.entries
            ?.map { (reason, count) -> ReasonCount(reason, (count as? JsonPrimitive)?.intOrNull ?: 0) }
            ?.filter { it.count > 0 }
            ?.sortedByDescending { it.count }
            .orEmpty()
        return BehaviourSummary(
            timeline = o["timeline"].items().map {
                TimelinePoint(it.str("name"), it.str("start").take(10), it.int("positive") ?: 0, it.int("negative") ?: 0)
            },
            positiveReasons = reasons("positive_reasons"),
            negativeReasons = reasons("negative_reasons"),
        )
    }

    fun activity(o: JsonObject): ActivityPoint? = ActivityPoint(
        id = o.long("id") ?: return null,
        type = o.str("type"),
        polarity = o.str("polarity"),
        reason = o.str("reason"),
        score = o.int("score") ?: 0,
        timestamp = o.str("timestamp_custom_time").ifEmpty { o.str("timestamp") },
        lesson = o.str("lesson_name"),
        teacher = o.str("teacher_name"),
        room = o.str("room_name"),
        note = htmlPreview(o.str("note")),
    )
}
