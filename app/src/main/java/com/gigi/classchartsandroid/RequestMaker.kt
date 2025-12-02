package com.gigi.classchartsandroid
import android.app.Application
import android.content.Context
import android.util.Log
import android.view.textclassifier.TextLinks
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
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


    fun yesno_to_truefalse(yesno: String): Boolean {
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


    fun refreshHomeworkList(homeworksList: MutableList<Homework>, onlyIncomplete: Boolean, linkStyle: TextLinkStyles) {
        homeworksList.clear()
        val homeworks = getHomeworks()
        if (homeworks != null) {
            for (i in homeworks) {
                val isComplete = yesno_to_truefalse(i.asJsonObject.get("status")!!.asJsonObject.get("ticked")!!.asString)
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
                        completionTime = "${i.asJsonObject.get("completion_time_value")!!.asString} ${i.asJsonObject.get("completion_time_unit")!!.asString}",
                        body = AnnotatedString.fromHtml(i.asJsonObject.get("description")!!.asString, linkStyles = linkStyle),
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

    fun getHomeworks(startDate: LocalDate = LocalDate.now().minusDays(45),
                     endDate: LocalDate = LocalDate.now().plusDays(366)): JsonArray? {
        // To get current date: LocalDate.now()

        val requestBody = FormBody.Builder()
            .build()

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

    fun tickHomework(id: String) {
        val requestBody = FormBody.Builder()
            .build()

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
}