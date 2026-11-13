package com.gigi.cca.shared.data

import co.touchlab.kermit.Logger
import com.gigi.cca.shared.KeyValueStore
import com.gigi.cca.shared.api.ClassChartsApi
import com.gigi.cca.shared.api.Credentials
import com.gigi.cca.shared.api.InvalidLoginException
import com.gigi.cca.shared.api.Session
import com.gigi.cca.shared.model.BehaviourData
import com.gigi.cca.shared.model.Homework
import com.gigi.cca.shared.model.Lesson
import com.gigi.cca.shared.model.Student
import com.gigi.cca.shared.plusDays
import com.gigi.cca.shared.startOfSchoolYear
import com.gigi.cca.shared.startOfWeek
import com.gigi.cca.shared.today
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val student: Student?, val demo: Boolean) : AuthState
}

data class LoadState(val loading: Boolean = false, val error: String? = null)

enum class BehaviourRange(val label: String) {
    WEEK("This week"),
    MONTH("Last 30 days"),
    YEAR("School year");

    fun from(today: LocalDate): LocalDate = when (this) {
        WEEK -> today.startOfWeek()
        MONTH -> today.plusDays(-30)
        YEAR -> today.startOfSchoolYear()
    }
}

const val DEMO_CODE = "DEMO"

private const val KEY_CREDENTIALS = "credentials"
private const val KEY_SESSION = "session"
private const val KEY_HOMEWORKS = "homeworks"
private const val KEY_LESSONS = "lessons"
private const val KEY_BEHAVIOUR = "behaviour"
private const val KEY_HIDE_COMPLETED = "hide_completed"
private val DATA_KEYS = listOf(KEY_CREDENTIALS, KEY_SESSION, KEY_HOMEWORKS, KEY_LESSONS, KEY_BEHAVIOUR)

/**
 * Single source of truth for the UI. Everything is shown from the on-disk cache straight away
 * and refreshed in the background, so screens never block on the network.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Repository(
    private val store: KeyValueStore,
    private val api: ClassChartsApi = ClassChartsApi(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    /** Serialises disk writes so they land in the order they were made. */
    private val storeDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private var demo = false
    private var homeworkJob: Job? = null
    private var homeworksFetchedAt: TimeSource.Monotonic.ValueTimeMark? = null
    private val fetchedDays = mutableSetOf<String>()
    private val fetchedBehaviour = mutableSetOf<BehaviourRange>()
    private val behaviourJobs = mutableMapOf<BehaviourRange, Job>()

    private val _auth = MutableStateFlow<AuthState>(AuthState.Loading)
    val auth: StateFlow<AuthState> = _auth
    val loginMessage = MutableStateFlow<String?>(null)

    private val _homeworks = MutableStateFlow<List<Homework>>(emptyList())
    val homeworks: StateFlow<List<Homework>> = _homeworks
    private val _homeworkState = MutableStateFlow(LoadState())
    val homeworkState: StateFlow<LoadState> = _homeworkState
    private val _hideCompleted = MutableStateFlow(true)
    val hideCompleted: StateFlow<Boolean> = _hideCompleted

    private val _lessons = MutableStateFlow<Map<String, List<Lesson>>>(emptyMap())
    val lessons: StateFlow<Map<String, List<Lesson>>> = _lessons
    private val _lessonStates = MutableStateFlow<Map<String, LoadState>>(emptyMap())
    val lessonStates: StateFlow<Map<String, LoadState>> = _lessonStates

    private val _behaviour = MutableStateFlow<Map<BehaviourRange, BehaviourData>>(emptyMap())
    val behaviour: StateFlow<Map<BehaviourRange, BehaviourData>> = _behaviour
    private val _behaviourStates = MutableStateFlow<Map<BehaviourRange, LoadState>>(emptyMap())
    val behaviourStates: StateFlow<Map<BehaviourRange, LoadState>> = _behaviourStates

    init {
        api.onSessionChanged = { persist(KEY_SESSION, it) }
        scope.launch(storeDispatcher) { restore() }
    }

    private fun restore() {
        _hideCompleted.value = load<Boolean>(KEY_HIDE_COMPLETED) ?: true
        val creds = load<Credentials>(KEY_CREDENTIALS)
        if (creds == null) {
            _auth.value = AuthState.LoggedOut
            return
        }
        demo = creds.code == DEMO_CODE
        val session = load<Session>(KEY_SESSION)
        _homeworks.value = load(KEY_HOMEWORKS) ?: if (demo) Demo.homeworks(today()) else emptyList()
        _lessons.value = load(KEY_LESSONS) ?: emptyMap()
        _behaviour.value = load(KEY_BEHAVIOUR) ?: emptyMap()
        api.restore(creds, session)
        _auth.value = AuthState.LoggedIn(session?.student, demo)
    }

    /** Returns an error message, or null on success. */
    suspend fun login(code: String, dob: LocalDate?): String? {
        val cleanCode = code.trim().uppercase()
        loginMessage.value = null
        if (cleanCode == DEMO_CODE) {
            startSession(Credentials(DEMO_CODE, "2000-01-01"), null, isDemo = true)
            return null
        }
        if (cleanCode.isEmpty()) return "Enter your ClassCharts code."
        if (dob == null) return "Pick your date of birth."
        val creds = Credentials(cleanCode, dob.toString())
        return try {
            val student = api.login(creds)
            startSession(creds, student, isDemo = false)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Login failed" }
            e.message ?: "Login failed."
        }
    }

    private fun startSession(creds: Credentials, student: Student?, isDemo: Boolean) {
        clearData()
        demo = isDemo
        if (isDemo) _homeworks.value = Demo.homeworks(today())
        persist(KEY_CREDENTIALS, creds)
        _auth.value = AuthState.LoggedIn(student, isDemo)
    }

    fun logout(message: String? = null) {
        api.clear()
        clearData()
        scope.launch(storeDispatcher) { DATA_KEYS.forEach(store::remove) }
        loginMessage.value = message
        _auth.value = AuthState.LoggedOut
    }

    private fun clearData() {
        homeworkJob?.cancel()
        behaviourJobs.values.forEach { it.cancel() }
        behaviourJobs.clear()
        homeworksFetchedAt = null
        fetchedDays.clear()
        fetchedBehaviour.clear()
        _homeworks.value = emptyList()
        _homeworkState.value = LoadState()
        _lessons.value = emptyMap()
        _lessonStates.value = emptyMap()
        _behaviour.value = emptyMap()
        _behaviourStates.value = emptyMap()
        DATA_KEYS.filter { it != KEY_CREDENTIALS && it != KEY_SESSION }.forEach { key ->
            scope.launch(storeDispatcher) { store.remove(key) }
        }
    }

    // ---- Homework ----

    /** With force = false this only refreshes if the list is more than a couple of minutes old. */
    fun refreshHomeworks(force: Boolean = true) {
        if (demo || homeworkJob?.isActive == true) return
        if (!force && homeworksFetchedAt?.let { it.elapsedNow() < 2.minutes } == true) return
        _homeworkState.value = LoadState(loading = true)
        homeworkJob = scope.launch {
            val t = today()
            val list = guarded({ _homeworkState.value = LoadState(error = it) }) {
                api.homeworks(t.plusDays(-45), t.plusDays(366))
            } ?: return@launch
            val sorted = list.sortedWith(compareBy({ it.dueDate }, { it.subject }, { it.title }))
            _homeworks.value = sorted
            homeworksFetchedAt = TimeSource.Monotonic.markNow()
            _homeworkState.value = LoadState()
            persist(KEY_HOMEWORKS, sorted)
        }
    }

    /** Optimistic: the tick shows instantly and is undone if ClassCharts rejects it. */
    fun toggleHomework(id: Int) {
        val homework = _homeworks.value.firstOrNull { it.id == id } ?: return
        setTicked(id, !homework.ticked)
        if (demo) return
        scope.launch {
            guarded({ message ->
                setTicked(id, homework.ticked)
                _homeworkState.update { it.copy(error = "Couldn't update \"${homework.title}\". $message") }
            }) { api.tickHomework(homework.statusId) }
        }
    }

    private fun setTicked(id: Int, ticked: Boolean) {
        _homeworks.update { list -> list.map { if (it.id == id) it.copy(ticked = ticked) else it } }
        persist(KEY_HOMEWORKS, _homeworks.value)
    }

    fun setHideCompleted(hide: Boolean) {
        _hideCompleted.value = hide
        persist(KEY_HIDE_COMPLETED, hide)
    }

    // ---- Timetable ----

    /** Loads a day once per app session (cached days show instantly meanwhile). */
    fun loadDay(date: LocalDate, force: Boolean = false) {
        val key = date.toString()
        if (demo) {
            if (key !in _lessons.value) _lessons.update { it + (key to Demo.lessons(date)) }
            return
        }
        if (_lessonStates.value[key]?.loading == true) return
        if (!force && key in fetchedDays) return
        fetchedDays += key
        _lessonStates.update { it + (key to LoadState(loading = true)) }
        scope.launch {
            val list = guarded({ msg -> _lessonStates.update { it + (key to LoadState(error = msg)) } }) {
                api.lessons(date)
            } ?: return@launch
            _lessons.update { it + (key to list) }
            _lessonStates.update { it - key }
            val t = today()
            val keep = t.plusDays(-60).toString()..t.plusDays(90).toString()
            persist(KEY_LESSONS, _lessons.value.filterKeys { it in keep })
        }
    }

    // ---- Behaviour ----

    fun loadBehaviour(range: BehaviourRange, force: Boolean = false) {
        if (demo) {
            if (range !in _behaviour.value) _behaviour.update { it + (range to Demo.behaviour(range, today())) }
            return
        }
        if (behaviourJobs[range]?.isActive == true) return
        if (!force && range in fetchedBehaviour) return
        fetchedBehaviour += range
        _behaviourStates.update { it + (range to LoadState(loading = true)) }
        behaviourJobs[range] = scope.launch {
            val t = today()
            val data = guarded({ msg -> _behaviourStates.update { it + (range to LoadState(error = msg)) } }) {
                val from = range.from(t)
                val to = t.plusDays(1)
                BehaviourData(api.behaviour(from, to), api.activity(from, to))
            } ?: return@launch
            _behaviour.update { it + (range to data) }
            _behaviourStates.update { it - range }
            persist(KEY_BEHAVIOUR, _behaviour.value)
        }
    }

    // ---- Helpers ----

    private suspend fun <T> guarded(onError: (String) -> Unit, block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: InvalidLoginException) {
        logout("Please log in again. ${e.message}")
        null
    } catch (e: Exception) {
        Logger.w(e) { "Request failed" }
        onError(e.message ?: "Something went wrong.")
        null
    }

    private inline fun <reified T> load(key: String): T? = store.get(key)?.let {
        runCatching { json.decodeFromString(serializer<T>(), it) }
            .onFailure { e -> Logger.w(e) { "Ignoring unreadable cache entry $key" } }
            .getOrNull()
    }

    private inline fun <reified T> persist(key: String, value: T?) {
        val text = if (value == null) null else json.encodeToString(serializer<T>(), value)
        scope.launch(storeDispatcher) {
            if (text == null) store.remove(key) else store.put(key, text)
        }
    }
}
