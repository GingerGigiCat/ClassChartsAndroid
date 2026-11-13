package com.gigi.cca.shared.api

import co.touchlab.kermit.Logger
import com.gigi.cca.shared.NetConfig
import com.gigi.cca.shared.model.ActivityPoint
import com.gigi.cca.shared.model.BehaviourSummary
import com.gigi.cca.shared.model.Homework
import com.gigi.cca.shared.model.Lesson
import com.gigi.cca.shared.model.Student
import com.gigi.cca.shared.netConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class InvalidLoginException(message: String) : Exception(message)
class SessionExpiredException : Exception("Your ClassCharts session expired")
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ApiException(message: String) : Exception(message)

/** dob is ISO yyyy-MM-dd. */
@Serializable
data class Credentials(val code: String, val dob: String)

@Serializable
data class Session(
    val cookies: Map<String, String>,
    val sessionId: String,
    val studentId: Int = 0,
    val student: Student? = null,
)

private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"

/** Pings rotate the session id; ClassCharts drops sessions after a few idle minutes. */
private val PING_INTERVAL = 150.seconds

/**
 * Student API client.
 *
 * Since April 2026 ClassCharts only accepts API calls that carry the `cc-session` cookie created
 * by the website login form, as well as `Authorization: Basic <session_id>`. The old
 * /apiv2student/login endpoint no longer works on its own. See
 * https://github.com/classchartsapi/api-docs/issues/17
 */
class ClassChartsApi(private val net: NetConfig = netConfig()) {
    private val client = HttpClient {
        followRedirects = false
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
    }
    private val authLock = Mutex()
    private var lastPing: TimeSource.Monotonic.ValueTimeMark? = null

    var credentials: Credentials? = null
        private set
    var session: Session? = null
        private set
    var onSessionChanged: (Session?) -> Unit = {}

    fun restore(credentials: Credentials?, session: Session?) {
        this.credentials = credentials
        this.session = session
        lastPing = null
    }

    fun clear() = restore(null, null)

    /** Logs in from scratch. Throws [InvalidLoginException] for a wrong code or date of birth. */
    suspend fun login(credentials: Credentials): Student = authLock.withLock {
        formLogin(credentials)
        this.credentials = credentials
        ping()
    }

    suspend fun homeworks(from: LocalDate, to: LocalDate): List<Homework> = call { s ->
        client.get("${net.root}/apiv2student/homeworks/${s.studentId}") {
            auth(s)
            parameter("display_date", "due_date")
            parameter("from", from.toString())
            parameter("to", to.toString())
        }
    }.data.items().mapNotNull(Parse::homework)

    suspend fun lessons(date: LocalDate): List<Lesson> = call { s ->
        client.get("${net.root}/apiv2student/timetable/${s.studentId}") {
            auth(s)
            parameter("date", date.toString())
        }
    }.data.items().map(Parse::lesson).sortedBy { it.start }

    suspend fun behaviour(from: LocalDate, to: LocalDate): BehaviourSummary = Parse.behaviour(
        call { s ->
            client.get("${net.root}/apiv2student/behaviour/${s.studentId}") {
                auth(s)
                parameter("from", from.toString())
                parameter("to", to.toString())
            }
        }.data
    )

    /** All behaviour points in the range, following ClassCharts' last_id pagination. */
    suspend fun activity(from: LocalDate, to: LocalDate, maxPages: Int = 15): List<ActivityPoint> {
        val all = LinkedHashMap<Long, ActivityPoint>()
        var lastId: Long? = null
        repeat(maxPages) {
            val page = call { s ->
                client.get("${net.root}/apiv2student/activity/${s.studentId}") {
                    auth(s)
                    parameter("from", from.toString())
                    parameter("to", to.toString())
                    lastId?.let { parameter("last_id", it.toString()) }
                }
            }.data.items().mapNotNull(Parse::activity)
            val nextLast = page.lastOrNull()?.id
            if (page.isEmpty() || nextLast == lastId) return all.values.toList()
            page.forEach { all[it.id] = it }
            lastId = nextLast
        }
        return all.values.toList()
    }

    /** Toggles a homework's ticked state. Takes homework.status.id, not homework.id. */
    suspend fun tickHomework(statusId: Int) {
        call { s ->
            client.get("${net.root}/apiv2student/homeworkticked/$statusId") {
                auth(s)
                parameter("studentId", s.studentId.toString())
            }
        }
    }

    // ---- Session handling ----

    private class Envelope(val data: JsonElement, val meta: JsonObject?)

    /** Runs an authenticated request, refreshing or re-creating the session as needed. */
    private suspend fun call(request: suspend (Session) -> HttpResponse): Envelope {
        val used = authLock.withLock { ensureSession() }
        return try {
            envelope(send { request(used) })
        } catch (e: SessionExpiredException) {
            val fresh = authLock.withLock {
                // Another request may already have logged back in.
                if (session?.sessionId == used.sessionId) relogin() else session!!
            }
            envelope(send { request(fresh) })
        }
    }

    private suspend fun ensureSession(): Session {
        val current = session ?: return relogin()
        val pingedRecently = lastPing?.let { it.elapsedNow() < PING_INTERVAL } == true
        if (pingedRecently && current.studentId != 0) return current
        return try {
            ping()
            session!!
        } catch (e: SessionExpiredException) {
            relogin()
        }
    }

    private suspend fun relogin(): Session {
        val creds = credentials ?: throw InvalidLoginException("You've been logged out.")
        Logger.i("ClassChartsApi") { "Session expired, logging in again" }
        formLogin(creds)
        ping()
        return session!!
    }

    /** Logs in through the website form, which is what sets the cc-session cookie. */
    private suspend fun formLogin(creds: Credentials) {
        val dob = LocalDate.parse(creds.dob).let {
            "${it.dayOfMonth.toString().padStart(2, '0')}/${(it.month.ordinal + 1).toString().padStart(2, '0')}/${it.year}"
        }
        val response = send {
            client.submitForm(
                url = "${net.root}/student/login",
                formParameters = parameters {
                    append("_method", "POST")
                    append("code", creds.code.uppercase())
                    append("dob", dob)
                    append("remember_me", "1")
                    append("recaptcha-token", "no-token-available")
                },
            ) { browserHeaders() }
        }
        val cookies = readCookies(response)
        val credentialsCookie = cookies["student_session_credentials"]
        if (credentialsCookie == null) {
            val body = runCatching { response.bodyAsText() }.getOrDefault("")
            throw when {
                "Invalid credentials" in body -> InvalidLoginException("That ClassCharts code and date of birth don't match.")
                "Too many" in body -> InvalidLoginException("Too many login attempts. Wait a few minutes and try again.")
                statusOf(response) in 500..599 -> ApiException("ClassCharts is having problems (${statusOf(response)}). Try again later.")
                else -> InvalidLoginException("Login failed. Check your code and date of birth, or wait a few minutes if you've tried several times.")
            }
        }
        val sessionId = runCatching {
            Json.parseToJsonElement(credentialsCookie.decodeURLQueryComponent()).jsonObject.str("session_id")
        }.getOrNull()
        if (sessionId.isNullOrEmpty()) throw ApiException("ClassCharts logged in but didn't send a session.")
        session = Session(cookies = cookies, sessionId = sessionId, studentId = session?.studentId ?: 0)
        lastPing = null
    }

    /** Refreshes the session id and fetches the student. */
    private suspend fun ping(): Student {
        val current = session ?: throw SessionExpiredException()
        val response = send {
            client.submitForm(
                url = "${net.root}/apiv2student/ping",
                formParameters = parameters { append("include_data", "true") },
            ) { auth(current) }
        }
        val env = envelope(response)
        val user = (env.data as? JsonObject)?.obj("user") ?: throw ApiException("ClassCharts sent an unexpected response.")
        val student = Parse.student(user)
        val updated = current.copy(
            sessionId = env.meta?.str("session_id")?.ifEmpty { null } ?: current.sessionId,
            studentId = student.id,
            student = student,
            cookies = current.cookies + readCookies(response),
        )
        session = updated
        lastPing = TimeSource.Monotonic.markNow()
        onSessionChanged(updated)
        return student
    }

    private fun HttpRequestBuilder.browserHeaders() {
        header(HttpHeaders.Accept, "application/json, text/javascript, */*; q=0.01")
        header("X-Requested-With", "XMLHttpRequest")
        // Browsers won't let us set these; the web proxy adds them instead.
        if (!net.viaProxy) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Referrer, "https://www.classcharts.com/mobile/student")
        }
    }

    private fun HttpRequestBuilder.auth(s: Session) {
        browserHeaders()
        header(HttpHeaders.Authorization, "Basic ${s.sessionId}")
        header(net.cookieHeader, s.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
    }

    private fun statusOf(response: HttpResponse): Int =
        response.headers["X-CC-Status"]?.toIntOrNull() ?: response.status.value

    private fun readCookies(response: HttpResponse): Map<String, String> =
        response.headers.getAll(net.setCookieHeader).orEmpty()
            .flatMap { header -> if (net.viaProxy) header.split(";") else listOf(header.substringBefore(";")) }
            .map { it.trim() }
            .filter { '=' in it && !it.endsWith("=deleted") }
            .associate { it.substringBefore("=") to it.substringAfter("=") }

    private suspend fun envelope(response: HttpResponse): Envelope {
        val status = statusOf(response)
        // A redirect to session.tes.com (or a 401/403) means the session is gone.
        if (status in 300..399 || status == 401 || status == 403) throw SessionExpiredException()
        val text = runCatching { response.bodyAsText() }.getOrDefault("")
        val obj = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()
            ?: throw ApiException("ClassCharts sent an unexpected response ($status).")
        val success = (obj["success"] as? JsonPrimitive)?.let { it.intOrNull ?: if (it.booleanOrNull == true) 1 else 0 } ?: 0
        if (success != 1) {
            val error = (obj["error"] as? JsonPrimitive)?.contentOrNull
            if (obj.int("expired") == 1 || error?.contains("not logged in", ignoreCase = true) == true) {
                throw SessionExpiredException()
            }
            throw ApiException(error ?: "ClassCharts returned an error.")
        }
        return Envelope(obj["data"] ?: JsonNull, obj["meta"] as? JsonObject)
    }

    private suspend fun send(block: suspend () -> HttpResponse): HttpResponse = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Logger.w(e) { "Network request failed" }
        throw NetworkException("Couldn't reach ClassCharts. Check your internet connection.", e)
    }
}
