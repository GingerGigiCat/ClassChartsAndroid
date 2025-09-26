package com.gigi.classchartsandroid
import com.google.gson.Gson
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

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

class RequestMaker {
    private val client = OkHttpClient()
    var token: String? = null
    var studentloginresponse: Map<String, *>? = null
    constructor()

    constructor(id: String, dob: String) {
        val requestBody = FormBody.Builder()
            .add("code", id)
            .add("remember", "true")
            .add("recaptcha-token", "no-token-available")
            .add("dob", dob)
            .build()

        val request = Request.Builder()
            .url("https://www.classcharts.com/apiv2student/login")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw _root_ide_package_.okio.IOException("Unexpected code $response")

            val jsonResponse = JSONObject(response.body.string())
            studentloginresponse = jsonResponse.toMap()
        }

        token = studentloginresponse?.get("meta")?.get("session_id") as String?
    }

}