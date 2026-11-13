package com.gigi.cca.shared.data

import com.gigi.cca.shared.api.htmlPreview
import com.gigi.cca.shared.isWeekend
import com.gigi.cca.shared.model.ActivityPoint
import com.gigi.cca.shared.model.Attachment
import com.gigi.cca.shared.model.BehaviourData
import com.gigi.cca.shared.model.BehaviourSummary
import com.gigi.cca.shared.model.Homework
import com.gigi.cca.shared.model.Lesson
import com.gigi.cca.shared.model.ReasonCount
import com.gigi.cca.shared.model.TimelinePoint
import com.gigi.cca.shared.plusDays
import kotlinx.datetime.LocalDate

/** Offline sample data for the DEMO login (schools can't create test accounts). */
internal object Demo {
    private const val JAZZ = """<p><b>TASK 1 (2 hrs)</b></p>
<p>Gain confidence improvising over two famous Modal Jazz compositions by Miles Davis, 'So What' and 'Milestones'.</p>
<ul><li>Play and internalise the scales and modes needed for each song</li>
<li>Practise the chord tones over the changes and get a feel for the harmonic progression</li>
<li>Explore different swung rhythms</li></ul>
<p><b>BACKING TRACK</b></p>
<p><a href="https://www.youtube.com/watch?v=FSGWj22wV0U">https://www.youtube.com/watch?v=FSGWj22wV0U</a></p>
<p><b>TASK 2 (2 hrs)</b></p>
<p>Start a PowerPoint for Task 1 (b) with two slides: the technical requirements of modal jazz improvisation, and a reflection on your own skills with some achievable aims.</p>"""

    private const val RECORDING = """<ol><li>Complete the improvements noted in your feedback books. <b>General feedback for everyone:</b>
<ol><li>Check your word count</li><li>Compare the performance environment to a stadium</li><li>Use photos in the Health &amp; Safety section</li></ol></li>
<li><b><u>Rehearse in study and free time - you have 2 weeks until final recordings!</u></b></li></ol>
<p><span style="color: #000000;">Bring your instrument and headphones to every rehearsal.</span></p>
<p><span style="color: rgb(224, 62, 45);">Deadline: no extensions!</span> <span style="background-color: #fbeeb8;">Book a practice room at reception.</span></p>
<p><font color="#003366">Recording slots are on the noticeboard outside M02.</font></p>"""

    fun homeworks(today: LocalDate): List<Homework> = listOf(
        homework(1, "Vocabulary test prep", "Spanish", "Sra B Teacher", "<p>Learn the vocab on page 42.</p>", today.plusDays(-6), today.plusDays(-2), "30 minutes", ticked = true),
        homework(2, "Modal Jazz Improvisation", "Music", "Mr M Teacher", JAZZ, today.plusDays(-8), today, "5 hours"),
        homework(
            3, "Term 2 week 6", "Maths", "Mrs H Teacher", "<p>Complete in your booklet.</p>", today.plusDays(-1), today.plusDays(1), "20 minutes",
            attachments = listOf(Attachment("Term 2 week 6.pdf", "https://example.com/term-2-week-6.pdf", isFile = true)),
        ),
        homework(
            4, "Revision for Forces in Equilibrium Test", "Physics", "Mr D Teacher",
            "<p>Use the attached revision materials along with your class notes to revise for the Forces in Equilibrium test.</p>",
            today.plusDays(-4), today.plusDays(1), "60 minutes",
            attachments = listOf(
                Attachment("Revision Grids for Forces in Equilibrium.pdf", "https://example.com/revision-grids.pdf", isFile = true),
                Attachment("https://www.physicsandmathstutor.com", "https://www.physicsandmathstutor.com", isFile = false),
            ),
        ),
        homework(5, "Recording preparation", "Music", "Mr M Teacher", RECORDING, today.plusDays(-6), today.plusDays(4), "2 hours"),
    )

    private fun homework(
        id: Int, title: String, subject: String, teacher: String, html: String,
        issued: LocalDate, due: LocalDate, time: String, ticked: Boolean = false,
        attachments: List<Attachment> = emptyList(),
    ) = Homework(
        id = id, statusId = id, title = title, subject = subject, teacher = teacher,
        description = html, preview = htmlPreview(html), issueDate = issued.toString(), dueDate = due.toString(),
        completionTime = time, ticked = ticked, attachments = attachments,
    )

    private val SUBJECTS = listOf(
        Triple("Maths", "Mrs E Teacher", "L01"),
        Triple("Physics", "Mr D Teacher", "U07"),
        Triple("Music", "Mr M Teacher", "M02"),
        Triple("Spanish", "Sra B Teacher", "L12"),
        Triple("Computer Science", "Ms K Teacher", "C04"),
    )
    private val PERIODS = listOf(
        "1" to ("09:00" to "10:00"), "2" to ("10:05" to "11:05"),
        "3" to ("11:25" to "12:25"), "4" to ("13:15" to "14:15"), "5" to ("14:20" to "15:20"),
    )

    fun lessons(date: LocalDate): List<Lesson> {
        if (date.isWeekend) return emptyList()
        val tutor = Lesson("tut-$date", "Tutor Time", "12C/Tu", "Mx C Teacher", "U02", "Tutor", "Tut", date.toString(), "${date}T08:40:00", "${date}T09:00:00")
        return listOf(tutor) + PERIODS.mapIndexed { i, (period, times) ->
            val (subject, teacher, room) = SUBJECTS[(i + date.dayOfWeek.ordinal) % SUBJECTS.size]
            Lesson("$date-$period", subject, "12B/${subject.take(2)}1", teacher, room, "Period $period", period, date.toString(), "${date}T${times.first}:00", "${date}T${times.second}:00")
        }
    }

    fun behaviour(range: BehaviourRange, today: LocalDate): BehaviourData {
        val weeks = when (range) { BehaviourRange.WEEK -> 1; BehaviourRange.MONTH -> 5; BehaviourRange.YEAR -> 10 }
        val timeline = (weeks - 1 downTo 0).map { w ->
            val start = today.plusDays(-7 * w)
            TimelinePoint("Week ${weeks - w}", start.toString(), positive = 3 + (w * 7) % 5, negative = if (w % 3 == 1) 1 else 0)
        }
        val reasons = listOf("Excellent classwork", "Helping others", "Great effort", "Homework completed")
        val activity = (0 until timeline.sumOf { it.positive + it.negative }.coerceAtMost(25)).map { i ->
            val negative = i % 7 == 3
            val day = today.plusDays(-(i * 2))
            val (subject, teacher, room) = SUBJECTS[i % SUBJECTS.size]
            ActivityPoint(
                id = 1000L - i, type = "behaviour", polarity = if (negative) "negative" else "positive",
                reason = if (negative) "Late to lesson" else reasons[i % reasons.size], score = if (negative) -1 else 1,
                timestamp = "${day}T${10 + i % 5}:15:00", lesson = subject, teacher = teacher, room = room,
            )
        }
        val positive = activity.filter { it.score > 0 }.groupingBy { it.reason }.eachCount()
        val negative = activity.filter { it.score < 0 }.groupingBy { it.reason }.eachCount()
        return BehaviourData(
            BehaviourSummary(
                timeline = timeline,
                positiveReasons = positive.map { ReasonCount(it.key, it.value) }.sortedByDescending { it.count },
                negativeReasons = negative.map { ReasonCount(it.key, it.value) }.sortedByDescending { it.count },
            ),
            activity,
        )
    }
}
