package com.gigi.classchartsandroid
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
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

data class Homework(val title: String, val complete: Boolean, val teacher: String, val subject: String, val body: String, val dueDate: LocalDate? = null)

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