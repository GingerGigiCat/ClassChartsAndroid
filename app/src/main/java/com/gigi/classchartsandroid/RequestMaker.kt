package com.gigi.classchartsandroid
import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.toLowerCase
import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.Either
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

fun JSONObject.toMap(): Map<String, *> = keys().asSequence().associateWith { // borrowed from stackoverflow, converts json to a kotlin friendly object
    when (val value = this[it])
    {
        is JSONArray ->
        {
            val map = (0 until value.length()).associate { Pair(it.toString(), value[it]) }
            JSONObject(map).toMap().values.toList()
        }
        is JSONObject -> value.toMap()
        JSONObject.NULL -> null
        else            -> value
    }
}

@Serializable
data class Attachment(
    val name: String,
    val link: String,
    val isFile: Boolean
    )

data class Homework(
    val title: String,
    val complete: Boolean,
    val teacher: String,
    val subject: String,
    val completionTime: String = "no time",
    val body: AnnotatedString,
    val rawBody: String? = null,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val id: String? = null,
    val attachments: MutableList<Attachment>? = null
    )

data class Lesson(
    val teacherName: String,
    val lessonName: String,
    val subjectName: String,
    val isAlternativeLesson: Boolean,
    val periodNumber: String,
    val roomName: String,
    val startTime: String,
    val endTime: String,
    val key: Int
)


open class ErrorType

class ErrorInvalidLogin : ErrorType()
class Success: ErrorType()
class ErrorNetwork: ErrorType()
class ErrorText : ErrorType()
class ErrorWaiting : ErrorType()


class RequestMaker {
    private val client = OkHttpClient()
    val gson = Gson()
    var sessionId: String? = null
    var studentId: String? = null
    var studentDob: String? = null
    var studentLoginResponse: JsonObject? = null
    var name: String = ""
    var f_name: String = ""
    var l_name: String = ""

    val STUDENT_ID = stringPreferencesKey("student_id")
    val STUDENT_DOB = stringPreferencesKey("student_dob")

    fun idFlow(): Flow<String> = MainActivity.instance.appDataStore.data.map { preferences ->
        preferences[STUDENT_ID] ?: ""
    }

    fun dobFlow(): Flow<String> = MainActivity.instance.appDataStore.data.map { preferences ->
        preferences[STUDENT_DOB] ?: ""
    }

    suspend fun writeId(id: String) {
        MainActivity.instance.appDataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[STUDENT_ID] = id
                studentId = id
            }
        }
    }

    suspend fun writeDob(dob: String) {
        MainActivity.instance.appDataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[STUDENT_DOB] = dob
                studentDob = dob
            }
        }
    }

    constructor() {

    }

    constructor(id: String?, dob: String?) {
        var id: String = id?: runBlocking { idFlow().first() }
        var dob: String = dob?: runBlocking { dobFlow().first() }

        val requestBody = FormBody.Builder()
            .add("code", id)
            .add("remember", "true")
            .add("recaptcha-session_id", "no-session_id-available")
            .add("dob", dob)
            .build()

        val request = Request.Builder()
            .url("https://www.classcharts.com/apiv2student/login")
            .post(requestBody)
            .build()


        client.newCall(request).execute().use { response -> // was an Error here
            if (!response.isSuccessful) print(response)//throw _root_ide_package_.okio.IOException("Unexpected code $response")
            studentLoginResponse = gson.fromJson(response.body?.string(), JsonObject::class.java)
            sessionId = studentLoginResponse?.getAsJsonObject("meta")?.get("session_id")?.asString
            runBlocking {
                writeId(id)
                writeDob(dob)
            }
        }
    }

    constructor(session_id: String) {
        sessionId = session_id

        if (!studentPing()) throw IOException("Ping no work, check internet? Or reauth")
    }

    suspend fun login(id: String? = null, dob: String? = null): ErrorType {
        var id: String = id?: ""
        var dob: String = dob?: ""
        if (id == "") {
            // Log.d("DataStoredID", idFlow().first())
            id = idFlow().first()
        }
        if (dob == "") {
            dob = dobFlow().first()
        }
        //id = "demo"

        if (id.lowercase() == "demo") {
            sessionId = "demo"
            writeId("demo")
            writeDob(dob)
            return Success()
        }

        val requestBody = FormBody.Builder()
            .add("code", id)
            .add("remember", "true")
            .add("recaptcha-session_id", "no-session_id-available")
            .add("dob", dob)
            .build()

        val request = Request.Builder()
            .url("https://www.classcharts.com/apiv2student/login")
            .post(requestBody)
            .build()


        client.newCall(request).execute().use { response -> // was an Error here
            if (!response.isSuccessful) return ErrorNetwork() //throw _root_ide_package_.okio.IOException("Unexpected code $response")
            studentLoginResponse = gson.fromJson(response.body?.string(), JsonObject::class.java)
            try {
                sessionId =
                    studentLoginResponse?.getAsJsonObject("meta")?.get("session_id")?.asString
            }
            catch (e: Exception) {
                return ErrorInvalidLogin()
            }
            writeId(id)
            writeDob(dob)
        }
        return Success()
    }


    fun yesnoToTruefalse(yesno: String): Boolean {
        if (yesno == "yes") return true
        else return false
    }

    fun studentPing(): Boolean {
        val requestBody = FormBody.Builder()
            .add("include_data", "true")
            .build()

        val request = Request.Builder()
            .url("https://www.classcharts.com/apiv2student/ping")
            .header("Authorization", "Basic $sessionId")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw _root_ide_package_.okio.IOException("Unexpected code $response")
            val jsonResponse = gson.fromJson(response.body?.string(), JsonObject::class.java)
            try {
                sessionId = jsonResponse?.getAsJsonObject("meta")?.get("session_id")?.asString
                // studentId = jsonResponse?.getAsJsonObject("data")?.getAsJsonObject("user")?.get("id")?.asString
                return true
            }
            catch (e: Error) {
                return false
            }
        }
    }


    fun refreshHomeworkList(homeworksList: MutableList<Homework>, onlyIncomplete: Boolean, linkStyle: TextLinkStyles, colorScheme: ColorScheme) {
        homeworksList.clear()
        if (sessionId == "demo") {
            homeworksList += Homework(
                title = "Modal Jazz Improvisation",
                complete = false,
                teacher = "Mr M Teacher",
                subject = "Music",
                completionTime = "5 hours",
                body = AnnotatedString.fromHtml("\n\n\n\n\n\n<p><b>TASK 1&nbsp; (2 hrs)</b></p>\n<p>Gain confidence improvising over two famous Modal Jazz\ncompositions by Miles Davis, 'So What' and 'Milestones'&nbsp;</p>\n<p>- Spend time playing and internalising the scales/ modes needed\nto improvise over the chords of each song</p>\n<p>- Spend time playing the scales/ chord tones over the chords\nchanges of the songs and getting a feel for the harmonic\nprogression of the song.&nbsp;</p>\n<p>- Spend time exploring and playing different swung\nrhythms&nbsp;</p>\n<p>- Spend time improvising and developing interesting ideas.</p>\n<p><b>BACKING TRACKS</b></p>\n<p><a href=\n\"https://www.youtube.com/watch?v=FSGWj22wV0U&amp;list=RDFSGWj22wV0U&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=FSGWj22wV0U&amp;list=RDFSGWj22wV0U&amp;start_radio=1</a></p>\n<p><a href=\n\"https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1</a></p>\n<p><br></p>\n<p><b>TASK 2 (2hrs)&nbsp;</b></p>\n<p>Start putting together a Powerpoint for Task 1 (b). Create two\nslides</p>\n<p>SLIDE 1 - Outline in detail the technical and musical\nrequirements needed to improvise in modal jazz. (Discuss everything\nincluding modes, chords scale relationships, chord changes in modal\njazz,&nbsp; rhythmic feel and articulation, phrasing, developing\nideas etc)&nbsp;</p>\n<p>SLIDE 2 - Reflect on/ analyse your ability and skills and set\nsome achievable aims for your improvising. Make sure you go into\ndetail and talk about technical specifics relating to your\ninstrument.&nbsp;</p>\n<p><br></p>\n<p><b>POWERPOINT</b> from class</p>\n<p><a href=\n\"https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1</a></p>\n<p><br></p>\n<p><br></p>\n\n",
                    linkStyles = TextLinkStyles(
                    SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = colorScheme.primary
                    )
                )),
                rawBody = "\n\n\n\n\n\n<p><b>TASK 1&nbsp; (2 hrs)</b></p>\n<p>Gain confidence improvising over two famous Modal Jazz\ncompositions by Miles Davis, 'So What' and 'Milestones'&nbsp;</p>\n<p>- Spend time playing and internalising the scales/ modes needed\nto improvise over the chords of each song</p>\n<p>- Spend time playing the scales/ chord tones over the chords\nchanges of the songs and getting a feel for the harmonic\nprogression of the song.&nbsp;</p>\n<p>- Spend time exploring and playing different swung\nrhythms&nbsp;</p>\n<p>- Spend time improvising and developing interesting ideas.</p>\n<p><b>BACKING TRACKS</b></p>\n<p><a href=\n\"https://www.youtube.com/watch?v=FSGWj22wV0U&amp;list=RDFSGWj22wV0U&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=FSGWj22wV0U&amp;list=RDFSGWj22wV0U&amp;start_radio=1</a></p>\n<p><a href=\n\"https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1</a></p>\n<p><br></p>\n<p><b>TASK 2 (2hrs)&nbsp;</b></p>\n<p>Start putting together a Powerpoint for Task 1 (b). Create two\nslides</p>\n<p>SLIDE 1 - Outline in detail the technical and musical\nrequirements needed to improvise in modal jazz. (Discuss everything\nincluding modes, chords scale relationships, chord changes in modal\njazz,&nbsp; rhythmic feel and articulation, phrasing, developing\nideas etc)&nbsp;</p>\n<p>SLIDE 2 - Reflect on/ analyse your ability and skills and set\nsome achievable aims for your improvising. Make sure you go into\ndetail and talk about technical specifics relating to your\ninstrument.&nbsp;</p>\n<p><br></p>\n<p><b>POWERPOINT</b> from class</p>\n<p><a href=\n\"https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1</a></p>\n<p><br></p>\n<p><br></p>\n\n",
                issueDate = LocalDate.now().minusDays(8),
                dueDate = LocalDate.now(),
                id = "879867"
            )
            homeworksList += Homework(title="Term 2 week 6", complete=false, teacher="Mrs H Teacher", subject="Maths", completionTime="20 minutes", body=AnnotatedString("Complete in your booklet"), rawBody="Complete in your booklet", issueDate=LocalDate.now().minusDays(1), dueDate=LocalDate.now().plusDays(1), id="767539988", attachments=mutableListOf(Attachment(name="Term 2 week 6.pdf", link="https://attachments.classcharts.com/h/186700/401f6c71e04b551d7b3f2d84c016afdb_20251209_122813.pdf?GoogleAccessId=attachments-classcharts%40edukey-classcharts.iam.gserviceaccount.com&Expires=1765590527&Signature=k4zDUbWzt283HeF826n2KCNNBHJb8e8UmSWCNMVdPsXv9%2BJaubAt6d%2BriAO3cedMWfCXI8IEVPBZYrUrKH7G9kjeH8fsmayyb0ZpgZXASCLM9xoYH%2B0iK%2BD5j5Y0NgKiQEsxOrO5JvIoQkM5hpaheRbmNMzpqumFv8cqV5JmhRBkqnEfdJDIaQygFhD70Gw7%2BCx6Co%2BIehK0M%2FXDcmh5zcLVTB2yAw3s35ZX0YF91SGUEwUcNf2dqSfRRhSsjRfkZbI8IFVVx38AB0vB%2FRpe8dA3JFDJ4dLrbUB5DQ6nIQeaAQgai7lbpIhf5xA9m2thNzUrHnq9UbBFKmURZNsXFg%3D%3D&response-content-disposition=attachment; filename=\"Term+2+week+6.pdf\"", isFile=true)))
            homeworksList += Homework(title="Revision for Forces in Equilibrium Test", complete=false, teacher="Mr D Teacher", subject="Physics", completionTime="60 minutes", body=AnnotatedString("Use the attached revision materials along with your class notes to revise for Forces in Equilibrium Test."), rawBody="\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "Use the attached revision materials along with your class notes to\n" +
                    "revise for Forces in Equilibrium Test.\n" +
                    "\n", issueDate=LocalDate.now().minusDays(4), dueDate=LocalDate.now().plusDays(1), id="736381326", attachments=mutableListOf(Attachment(name="Static_calculations_.pdf", link="https://attachments.classcharts.com/h/186700/44e36a500057a8e3aac78e4a328e3943_20251031_142537.pdf?GoogleAccessId=attachments-classcharts%40edukey-classcharts.iam.gserviceaccount.com&Expires=1765607973&Signature=SUa6jk4APJLCDzYDBIff5F8WvyP1WpVASSS9EKd5FSYnloGopFop%2BUxL8Mz7G3sSsFKvTYGAzGZa7eYMUfWYHcKQ%2FqWheIQCc698yCay5EIdA1TEDnR7vj9mE8nRh9%2B0carPcQ5snzXeo%2Fc3mob9uWYsUpAJVQ6ka9QsKhDnvJ8T6G9GI4xlQ8FERBldrmPkzWHCfyTOXoqf%2BLeY2N11HTUie4zzIjYsMHSFLE7n6elK6ddESyzePKC1zBt9mqMYGrK47CTdkPNPXibFY3ulHB7fCLYc6fVLIyYQHKWgKYoko8HLcTIWTKQ60YR%2FGLTBI%2FWQb75GtNJF3gMaNDk5qw%3D%3D&response-content-disposition=attachment; filename=\"Static_calculations_.pdf\"", isFile=true), Attachment(name="Revision Grids for Forces in Equilibrium.pdf", link="https://attachments.classcharts.com/h/186700/df9a95fbb96b6be9fff9a7da4c1fc920_20251031_142552.pdf?GoogleAccessId=attachments-classcharts%40edukey-classcharts.iam.gserviceaccount.com&Expires=1765607973&Signature=bzqj5c%2B%2BsbuofuhJ8uvW%2FMHhPFLcAnQovqLf7ZcohAWzON7mhnp4f5geqVB41%2FFs7xKjVv26BtHUL%2FTO%2BJWfpEYbC4LpxXNkuSNspddjFBwerwJhO%2FRHk3C0l1MLzuWrPLuu6D1MocqqAshEXLgSbx4IH6ldUy%2Fe9fWwuZAZ2EUS6zc%2FGKssjntdRrl0qIWAH2vLg%2B9Y%2FHelFSuzKjOshzkm4lUxlPFU0rXNadjzZ5iUDCXGVFdD5jPwjixmpUAQzIJzl1lvvAnCzvwDE1ZEqLEUMsOy8M0YzKCAgACZspZhvEbs%2ByvmixaBcsQ%2Frjtejoj%2FLNiEeGvCNx1PYCTVAA%3D%3D&response-content-disposition=attachment; filename=\"Revision+Grids+for+Forces+in+Equilibrium.pdf\"", isFile=true), Attachment(name="Revision Grids for Forces in Equilibrium_MS.pdf", link="https://attachments.classcharts.com/h/186700/afce3edcef6c8f4b69e90dfb1e69b8e1_20251031_142600.pdf?GoogleAccessId=attachments-classcharts%40edukey-classcharts.iam.gserviceaccount.com&Expires=1765607973&Signature=okHxBPBFQf21vj7WLEfRXuCzJ%2F1haSzszpMvg%2BCAhhFcxXvsOVE1qyVwaPVcWJ30Av698yi4YgAHZdtj%2FRtjCaRXbAgt7C3rhwib%2FvO3XKAaA5FyF8wSthve2tM%2FZe%2F8z2gZaGjYjiKE88z4cv00CYHahx7QJXnedRJZRq1SkL%2FkaKJnWKO3SBzcWN9XBD0ERsLtUuDDTZlH34aE81dLw1KuhKoFepncNQTHwkiw0E8IX2%2BvMN68rcCVXqXorW986nB0W6ThZvoaa9oOo3iGPxubVSNzMeA%2Bk7FrjVSw2k7Ing%2BU82hAyELFx7ZXB9FMocU122OllOxeot3JY28FmQ%3D%3D&response-content-disposition=attachment; filename=\"Revision+Grids+for+Forces+in+Equilibrium_MS.pdf\"", isFile=true), Attachment(name="Practice questions chapter 6 ANSWERS.pdf", link="https://attachments.classcharts.com/h/186700/ebab40e394ab4347208b712a256062f0_20251104_005108.pdf?GoogleAccessId=attachments-classcharts%40edukey-classcharts.iam.gserviceaccount.com&Expires=1765607973&Signature=fiG2aJOn9CIFv%2F8oHL7CmREh1jNpzGe0fj2MiVoFumw4Oo2UN83yIYyJkaSb%2BkKbG7ih9tRDkvQb4EKEagxlfLvM2CoIfIBcYWcMgYg7CF7voE7qSEo67kMeyEYjb9kwThtK4fm79F%2BwZLahqWmxsZebKs1Smdnh%2F2K4J6TiHBjQwnmfMrhOqoLqaTHXkB7oIq9pCfb2edOzbIUIu%2BWu7qQWW01wN6s%2FncML66PWiYvP9zHidhs88E5DtAXSyEQoP41xWtXZGKL3RQ%2BJVLS8OecMX%2FBM1bewnQfs4lyDsFERb2op9yPJEqX8lcr3%2BXfiigjXtpZ5Rt4PecZ%2FownWOg%3D%3D&response-content-disposition=attachment; filename=\"Practice+questions+chapter+6+ANSWERS.pdf\"", isFile=true)))
            val rawText = """
            <ol>
            <li>Complete improvements as noted in your feedback books and based
                    on what we discuss in class on Monday 3rd.&nbsp; <b>GENERAL
            FEEDBACK for EVERYONE</b> includes:
            <ol>
            <li>Check your word count</li>
            <li>COMPARE the performance environment to, say, a Stadium and note
            what differences are required compared to a Lunch Canteen</li>
            <li>Use more "I am to" or "I will"</li>
            <li>Use PHOTOS in the Health & Safety section, and generally use
            photos where it can save you word</li>
            <li>In the OWN ABILITY section, please refer more clearly to
            MUSICAL CHALLENGES i.e things that are tricky/difficult in your
            pieces that require you to rehearse in detail.&nbsp; Say what the
            challenge is, and how you'll overcome it to become a more skilled
            musician</li>
            </ol>
            </li>
            <li><b><u>Rehearse in study and free time - you have 2 weeks until
            final recordings!</u></b></li>
            </ol>
            """
            homeworksList += Homework(title="Music", complete=false, teacher="Mr M Teacher", subject="Music", completionTime="2 hours",
                body=AnnotatedString.fromHtml(rawText,
                    linkStyles = TextLinkStyles(
                SpanStyle(
                    textDecoration = TextDecoration.Underline,
                    color = colorScheme.primary
                ))),
                rawBody=rawText, issueDate=LocalDate.now().minusDays(6), dueDate=LocalDate.now().plusDays(4), id="736253715", attachments=mutableListOf())
        }
        else {
            val homeworks = getHomeworks()
            if (homeworks != null) {
                for (i in homeworks) {
                    val isComplete =
                        yesnoToTruefalse(i.asJsonObject.get("status")!!.asJsonObject.get("ticked")!!.asString)
                    if (!onlyIncomplete || !isComplete) {
                        var attachments: MutableList<Attachment> = mutableListOf()

                        for (i in i.asJsonObject.getAsJsonArray("validated_links")) {
                            attachments += Attachment(
                                name = i.asJsonObject.get("link")!!.asString,
                                link = i.asJsonObject.get("link")!!.asString,
                                isFile = false
                            )
                        }

                        for (i in i.asJsonObject.getAsJsonArray("validated_attachments")) {
                            attachments += Attachment(
                                name = i.asJsonObject.get("filename")!!.asString,
                                link = i.asJsonObject.get("file")!!.asString,
                                isFile = true
                            )
                        }

                        homeworksList += Homework(
                            title = i.asJsonObject.get("title")!!.asString,
                            complete = isComplete,
                            teacher = i.asJsonObject.get("teacher")!!.asString,
                            subject = i.asJsonObject.get("subject")!!.asString,
                            completionTime = "${i.asJsonObject.get("completion_time_value")!!.asString} ${
                                i.asJsonObject.get(
                                    "completion_time_unit"
                                )!!.asString
                            }",
                            body = AnnotatedString.fromHtml(
                                i.asJsonObject.get("description")!!.asString,
                                linkStyles = linkStyle
                            ),
                            rawBody = i.asJsonObject.get("description")!!.asString,
                            issueDate = LocalDate.parse(i.asJsonObject.get("issue_date")!!.asString),
                            dueDate = LocalDate.parse(i.asJsonObject.get("due_date")!!.asString),
                            id = i.asJsonObject.get("status")!!.asJsonObject.get("id")!!.asString,
                            attachments = attachments
                        )
                    }
                }
            }
        }
    }

    fun getHomeworks(startDate: LocalDate = LocalDate.now().minusDays(45),
                     endDate: LocalDate = LocalDate.now().plusDays(366)): JsonArray? {
        // To get current date: LocalDate.now()

        val url = "https://www.classcharts.com/apiv2student/homeworks/$studentId".toHttpUrlOrNull()!!
            .newBuilder()
            .addQueryParameter("display_date", "due_date")
            .addQueryParameter("from", startDate.toString())
            .addQueryParameter("to", endDate.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Basic $sessionId")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw _root_ide_package_.okio.IOException("Unexpected code $response")
            val jsonResponse = gson.fromJson(response.body?.string(), JsonObject::class.java)
            Log.d("HomeworkData", jsonResponse.toString())
            try {
                return jsonResponse.getAsJsonArray("data")
            }
            catch (e: Error) {
                Log.e("uh oh in getHomeworks", e.toString())
                return null
            }
        }
    }

    fun tickHomework(id: String? = studentId) {
        val url = "https://www.classcharts.com/apiv2student/homeworkticked/$id".toHttpUrlOrNull()!!
            .newBuilder()
            .addQueryParameter("studentId", studentId)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Basic $sessionId")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw _root_ide_package_.okio.IOException("Unexpected code $response")
        }
    }

    fun listLessons(date: LocalDate): Either<MutableList<Lesson>, ErrorType> {
        var lessonList = mutableListOf<Lesson>()

        if (sessionId == "demo") {
            lessonList += Lesson(teacherName="Mx C Teacher", lessonName="12C/Tu", subjectName="Tutor Time", isAlternativeLesson=false, periodNumber="Tut", roomName="U02", startTime="${LocalDate.now().toString()}T08:40:00+00:00", endTime="${LocalDate.now().toString()}T09:00:00+00:00", key=1157931873)
            lessonList += Lesson(teacherName="Mrs E Teacher", lessonName="12D/Ma1", subjectName="Maths", isAlternativeLesson=false, periodNumber="1", roomName="L01", startTime="${LocalDate.now().toString()}T09:00:00+00:00", endTime="${LocalDate.now().toString()}T10:00:00+00:00", key=1192664549)
            lessonList += Lesson(teacherName="Ms H Teacher", lessonName="12D/Ma1", subjectName="Maths", isAlternativeLesson=false, periodNumber="2", roomName="L06", startTime="${LocalDate.now().toString()}T10:05:00+00:00", endTime="${LocalDate.now().toString()}T11:05:00+00:00", key=1192664561)
            lessonList += Lesson(teacherName="Mr D Teacher", lessonName="12B/Ph1", subjectName="Physics", isAlternativeLesson=false, periodNumber="3", roomName="U07", startTime="${LocalDate.now().toString()}T11:25:00+00:00", endTime="${LocalDate.now().toString()}T12:25:00+00:00", key=1157937234)
            lessonList += Lesson(teacherName="Miss K Teacher", lessonName="12B/Ph1", subjectName="Physics", isAlternativeLesson=false, periodNumber="4", roomName="U09", startTime="${LocalDate.now().toString()}T12:30:00+00:00", endTime="${LocalDate.now().toString()}T13:30:00+00:00", key=1157941107)
        }
        else {
            val url =
                "https://www.classcharts.com/apiv2student/timetable/$studentId".toHttpUrlOrNull()!!
                    .newBuilder()
                    .addQueryParameter("date", date.toString())
                    .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Basic $sessionId")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Either.Right(ErrorNetwork())
                val jsonResponse = gson.fromJson(response.body?.string(), JsonObject::class.java)
                for (i in jsonResponse.getAsJsonArray("data")) {
                    val l = i.asJsonObject
                    lessonList += Lesson(
                        teacherName = l.get("teacher_name")?.toString()?.replace("\"", "")
                            ?: "Mx. Teacher",
                        lessonName = l.get("lesson_name")?.toString()?.replace("\"", "")
                            ?: "Lesson",
                        subjectName = l.get("subject_name")?.toString()?.replace("\"", "")
                            ?: "Subject",
                        isAlternativeLesson = l.get("is_alternative_lesson")?.toString()
                            ?.toBoolean() ?: false,
                        periodNumber = l.get("period_number")?.toString()?.replace("\"", "") ?: "0",
                        roomName = l.get("room_name")?.toString()?.replace("\"", "") ?: "Room",
                        startTime = l.get("start_time")?.toString()?.replace("\"", "")
                            ?: "1970-01-01T00:00:00+00:00",
                        endTime = l.get("end_time")?.toString()?.replace("\"", "")
                            ?: "1970-01-01T00:00:00+00:00",
                        key = l.get("key")?.toString()?.toInt() ?: 0
                    )
                }
            }
        }
        return Either.Left(lessonList)

    }

}