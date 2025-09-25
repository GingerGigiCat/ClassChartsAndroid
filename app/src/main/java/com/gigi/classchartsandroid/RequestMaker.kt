package com.gigi.classchartsandroid
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request

class RequestMaker {
    private val client = OkHttpClient()
    var token: String? = null
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

            val jsonResponse = response.body.string()
            val moshi: Moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val jsonAdapter: JsonAdapter = moshi.adapter(User::class.java)
        }
    }

}