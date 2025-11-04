package com.gigi.classchartsandroid
import android.util.Log
import android.view.textclassifier.TextLinks
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
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


data class Homework(val title: String,
                    val complete: Boolean,
                    val teacher: String,
                    val subject: String,
                    val body: AnnotatedString,
                    val rawBody: String? = null,
                    val issueDate: LocalDate? = null,
                    val dueDate: LocalDate? = null,
                    val id: String? = null)

class RequestMaker {
    private val client = OkHttpClient()
    val gson = Gson()
    var sessionId: String? = null
    var studentId: String? = null
    var studentLoginResponse: JsonObject? = null
    var name: String = ""
    var f_name: String = ""
    var l_name: String = ""

    constructor()

    constructor(id: String, dob: String) {
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


        client.newCall(request).execute().use { response -> // TODO: Error here
            if (!response.isSuccessful) print(response)//throw _root_ide_package_.okio.IOException("Unexpected code $response")
            studentLoginResponse = gson.fromJson(response.body?.string(), JsonObject::class.java)
            sessionId = studentLoginResponse?.getAsJsonObject("meta")?.get("session_id")?.asString
            studentId = id
        }
    }

    constructor(session_id: String) {
        sessionId = session_id

        if (!studentPing()) throw IOException("Ping no work, check internet? Or reauth")
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
                studentId = jsonResponse?.getAsJsonObject("data")?.getAsJsonObject("user")?.get("id")?.asString
                return true
            }
            catch (e: Error) {
                return false
            }
        }
    }


    fun refreshHomeworkList(homeworksList: MutableList<Homework>, onlyIncomplete: Boolean, linkStyle: TextLinkStyles) {
        homeworksList.clear()
        for (i in getHomeworks()!!) {
            val isComplete = yesno_to_truefalse(i.asJsonObject.get("status")!!.asJsonObject.get("ticked")!!.asString)
            if (!onlyIncomplete || !isComplete) {
                homeworksList += Homework(
                    title = i.asJsonObject.get("title")!!.asString,
                    complete = isComplete,
                    teacher = i.asJsonObject.get("teacher")!!.asString,
                    subject = i.asJsonObject.get("subject")!!.asString,
                    body = AnnotatedString.fromHtml(i.asJsonObject.get("description")!!.asString, linkStyles = linkStyle),
                    rawBody = i.asJsonObject.get("description")!!.asString,
                    issueDate = LocalDate.parse(i.asJsonObject.get("issue_date")!!.asString),
                    dueDate = LocalDate.parse(i.asJsonObject.get("due_date")!!.asString),
                    id = i.asJsonObject.get("status")!!.asJsonObject.get("id")!!.asString
                )
            }
        }
    }

    fun getHomeworks(startDate: LocalDate = LocalDate.now().minusDays(28),
                     endDate: LocalDate = LocalDate.now().plusDays(28)): JsonArray? {
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
            try {
                return jsonResponse.getAsJsonArray("data")
            }
            catch (e: Error) {
                return null
            }
        }
    }

}