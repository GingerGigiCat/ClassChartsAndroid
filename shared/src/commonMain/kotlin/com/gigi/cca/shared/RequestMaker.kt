package com.gigi.cca.shared
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLinkStyles
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.ColumnInfo
import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import arrow.core.Either
import be.digitalia.compose.htmlconverter.HtmlStyle
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import co.touchlab.kermit.Logger
//import com.google.gson.Gson
//import com.google.gson.JsonArray
//import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.request.cookie
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.UUID


@Serializable
data class Attachment(
    val name: String,
    val link: String,
    val isFile: Boolean
    )

data class Homework(
    val title: String,
    var complete: Boolean,
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

@Serializable
@Entity
data class Lesson(
    @ColumnInfo("teacher_name") val teacherName: String,
    @ColumnInfo("lesson_name") val lessonName: String,
    @ColumnInfo("subject_name") val subjectName: String,
    @ColumnInfo("is_alternative_lesson") val isAlternativeLesson: Boolean,
    @ColumnInfo("period_number") val periodNumber: String,
    @ColumnInfo("room_name") val roomName: String,
    @ColumnInfo("start_time") val startTime: String,
    @ColumnInfo("end_time") val endTime: String,
    @PrimaryKey val key: Int,
    @ColumnInfo("date") val date: String,
    @ColumnInfo("user_notes") val userNotes: String = "",
    @ColumnInfo("free_period") val freePeriod: Boolean = false
)

@Serializable
open class ScreenObject

@Serializable
object HomeworkListObject : ScreenObject()

@Serializable
@Entity
data class HomeworkContentObject(
    @ColumnInfo("title") val title: String,
    @ColumnInfo("complete") val complete: Boolean,
    @ColumnInfo("teacher") val teacher: String,
    @ColumnInfo("subject") val subject: String,
    @ColumnInfo("body") val body: String,
    @ColumnInfo("issue_date") val issueDate: String = "",
    @ColumnInfo("due_date") val dueDate: String = "",
    @PrimaryKey val id: String = "",
    @ColumnInfo("completion_time") val completionTime: String,
    @ColumnInfo("attachments") val attachments: String,
    @ColumnInfo("user_added") val userAdded: Boolean = false,
    @ColumnInfo("user_notes") val userNotes: String = "",
    @ColumnInfo("completion_state") val completionState: Int = 0
) : ScreenObject()


@Serializable
object LoginScreenObject : ScreenObject()

@Serializable
object TimetableScreenObject : ScreenObject()


@Serializable
@Entity
data class UserInfo(
    @PrimaryKey val id: Int = 0,
    @ColumnInfo("student_id") val studentId: String = "",
    @ColumnInfo("student_dob") val studentDob: String = "2000-01-01",
    @ColumnInfo("valid_login") val validLogin: Boolean = false,
    @ColumnInfo("last_online") val lastOnline: String = "2000-01-01"
)


@Dao
interface HomeworkDao {
    @Query("SELECT * FROM homeworkcontentobject WHERE NOT (complete AND :onlyIncomplete) ORDER BY due_date ASC")
    suspend fun getAll(onlyIncomplete: Boolean = false): MutableList<HomeworkContentObject>

    @Insert(onConflict = REPLACE)
    suspend fun insertAll(homeworks: MutableList<HomeworkContentObject>)

    @Query("UPDATE homeworkcontentobject SET complete = not complete WHERE id = :homeworkId")
    suspend fun tickHomework(homeworkId: String)
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lesson WHERE date = :date ORDER BY start_time ASC")
    suspend fun getDay(date: String = LocalDate.now().toString()): MutableList<Lesson>

    @Insert(onConflict = REPLACE)
    suspend fun insertDay(lessons: MutableList<Lesson>)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM userinfo WHERE id = :id")
    suspend fun getUserInfo(id: Int = 0): UserInfo?

    //@Query("""
    //    IF EXISTS (SELECT 1 FROM userinfo WHERE id = :id)
    //    BEGIN
    //        UPDATE userinfo SET student_id = :studentId WHERE id == :id
    //    ELSE
    //    BEGIN
    //        INSERT userinfo(id, student_id) VALUES(:id, :studentId)
    //    END
    //""")//
    @Query("INSERT INTO userinfo(id, student_id, student_dob, valid_login, last_online) VALUES(:id, :studentId, '2000-01-01', false, '2000-01-01') " +
            "ON CONFLICT(id) DO " +
            "UPDATE SET student_id = :studentId WHERE id == :id" +
            "")
    suspend fun setStudentId(studentId: String, id: Int = 0) //TODO: make it not have null for other fields when inseritng new

    //@Query("UPDATE userinfo SET student_dob = :studentDob WHERE id == :id")
    @Query("INSERT INTO userinfo(id, student_dob, student_id, valid_login, last_online) VALUES(:id, :studentDob, '', false, '2000-01-01') " +
            "ON CONFLICT(id) DO " +
            "UPDATE SET student_dob = :studentDob WHERE id == :id" +
            "")
    suspend fun setStudentDob(studentDob: String, id: Int = 0)

    //@Query("UPDATE userinfo SET valid_login = :validLogin WHERE id == :id")
    @Query("INSERT INTO userinfo(id, valid_login, student_id, student_dob, last_online) VALUES(:id, :validLogin, '', '2000-01-01', '2000-01-01') " +
            "ON CONFLICT(id) DO " +
            "UPDATE SET valid_login = :validLogin WHERE id == :id " +
            "")
    suspend fun setValidLogin(validLogin: Boolean, id: Int = 0)

    //@Query("UPDATE userinfo SET last_online = :lastOnline WHERE id == :id")
    @Query("INSERT INTO userinfo(id, last_online, student_id, student_dob, valid_login) VALUES(:id, :lastOnline, '', '2000-01-01', false) " +
            "ON CONFLICT(id) DO " +
            "UPDATE SET last_online = :lastOnline WHERE id == :id" +
            "")
    suspend fun setLastOnline(lastOnline: String, id: Int = 0)
}

@Database(entities = [HomeworkContentObject::class, Lesson::class, UserInfo::class], version = 1, exportSchema = false)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun homeworkDao(): HomeworkDao
    abstract fun lessonDao(): LessonDao
    abstract fun userDao(): UserDao
}

@Suppress("KotlinNoActualForExpect") // actuals given by compiler
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect fun getDatabaseBuilder(context: Any? = null): RoomDatabase.Builder<AppDatabase>

fun getRoomDatabase(context: Any? = null): AppDatabase {
    return getDatabaseBuilder(context)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

open class ErrorType

class ErrorInvalidLogin : ErrorType()
class Success: ErrorType()
class ErrorNetwork: ErrorType()
class ErrorText : ErrorType()
class ErrorWaiting : ErrorType()


class RequestMaker {
    var sessionId: String? = null
    var appContext: Any? = null
    var studentId: String? = null
    var studentDob: String? = null
    var studentLoginResponse: JsonObject? = null
    var name: String = ""
    var f_name: String = ""
    var l_name: String = ""
    val timer = Timer()

    val cookieJarbutnolongerimportantbecausenomoreokhttp = """object: CookieJar {
        var theCookies: List<Cookie> = listOf<Cookie>()

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val sendCookies = listOf(Cookie.Builder()
                .name("student_session_credentials")
                .value("%7B%22remember_me%22%3Atrue%2C%22session_id%22%3A%22$sessionId%22%7D")
                .expiresAt(0)
                .domain("www.classcharts.com")
                .build()
            ) + theCookies
            Logger.d(tag="mmmcookies", messageString = sendCookies.toString())
            return sendCookies
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            Logger.d(tag="newcookies", messageString=cookies.toString())
            theCookies = cookies
        }
    }
    """

    private val client = HttpClient() {
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        //followRedirects = false //useful for debugging
    }

    val STUDENT_ID = stringPreferencesKey("student_id")
    val STUDENT_DOB = stringPreferencesKey("student_dob")
    val LOGIN_SUCCESS = booleanPreferencesKey("login_success")

    //val roomDb = Room.databaseBuilder(
    //    MainActivity.instance,
    //    AppDatabase::class.java,
    //    "maindb")
    //    .allowMainThreadQueries()
    //    .build()

    var roomDb: AppDatabase? = null
    var homeworkDao: HomeworkDao? = null
    var lessonDao: LessonDao? = null
    var userDao: UserDao? = null

    constructor(context: Any? = null) {
        appContext = context

        roomDb = getRoomDatabase(appContext)
        homeworkDao = roomDb!!.homeworkDao()
        lessonDao = roomDb!!.lessonDao()
        userDao = roomDb!!.userDao()
    }


    suspend fun login(id: String? = null, dob: String? = null): ErrorType {
        var id: String = id?: ""
        var dob: String = dob?: ""
        var userInfo = userDao!!.getUserInfo()
        if (userInfo == null) {
            userDao!!.setStudentId(id)
            userDao!!.setStudentDob(dob)
            userInfo = userDao!!.getUserInfo()
        }
        if (id == "") {
            // Log.d("DataStoredID", idFlow().first())
            Logger.d("DataStored") {userInfo.toString()}
            id = userInfo!!.studentId
        }
        if (dob == "") {
            dob = userInfo!!.studentDob
        }
        //id = "demo"

        if (id.lowercase() == "demo" || sessionId == "demo") {
            sessionId = "demo"
            userDao!!.setStudentId("demo")
            userDao!!.setStudentDob(dob)

            if (homeworkDao!!.getAll().size == 0) {
                var homeworksList = mutableListOf<HomeworkContentObject>()
                homeworksList += HomeworkContentObject(
                    title = "Modal Jazz Improvisation",
                    complete = false,
                    teacher = "Mr M Teacher",
                    subject = "Music",
                    completionTime = "5 hours",
                    body = "\n\n\n\n\n\n<p><b>TASK 1&nbsp; (2 hrs)</b></p>\n<p>Gain confidence improvising over two famous Modal Jazz\ncompositions by Miles Davis, 'So What' and 'Milestones'&nbsp;</p>\n<p>- Spend time playing and internalising the scales/ modes needed\nto improvise over the chords of each song</p>\n<p>- Spend time playing the scales/ chord tones over the chords\nchanges of the songs and getting a feel for the harmonic\nprogression of the song.&nbsp;</p>\n<p>- Spend time exploring and playing different swung\nrhythms&nbsp;</p>\n<p>- Spend time improvising and developing interesting ideas.</p>\n<p><b>BACKING TRACKS</b></p>\n<p><a href=\n\"https://www.youtube.com/watch?v=FSGWj22wV0U&amp;list=RDFSGWj22wV0U&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=FSGWj22wV0U&amp;list=RDFSGWj22wV0U&amp;start_radio=1</a></p>\n<p><a href=\n\"https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1</a></p>\n<p><br></p>\n<p><b>TASK 2 (2hrs)&nbsp;</b></p>\n<p>Start putting together a Powerpoint for Task 1 (b). Create two\nslides</p>\n<p>SLIDE 1 - Outline in detail the technical and musical\nrequirements needed to improvise in modal jazz. (Discuss everything\nincluding modes, chords scale relationships, chord changes in modal\njazz,&nbsp; rhythmic feel and articulation, phrasing, developing\nideas etc)&nbsp;</p>\n<p>SLIDE 2 - Reflect on/ analyse your ability and skills and set\nsome achievable aims for your improvising. Make sure you go into\ndetail and talk about technical specifics relating to your\ninstrument.&nbsp;</p>\n<p><br></p>\n<p><b>POWERPOINT</b> from class</p>\n<p><a href=\n\"https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1</a></p>\n<p><br></p>\n<p><br></p>\n\n",
                    issueDate = LocalDate.now().minusDays(8).toString(),
                    dueDate = LocalDate.now().toString(),
                    id = "879867",
                    attachments = "[]"
                )
                homeworksList += normalHomeworkToHomeworkContent(
                    Homework(
                        title = "Term 2 week 6",
                        complete = false,
                        teacher = "Mrs H Teacher",
                        subject = "Maths",
                        completionTime = "20 minutes",
                        body = AnnotatedString("Complete in your booklet"),
                        rawBody = "Complete in your booklet",
                        issueDate = LocalDate.now().minusDays(1),
                        dueDate = LocalDate.now().plusDays(1),
                        id = "767539988",
                        attachments = mutableListOf(
                            Attachment(
                                name = "Term 2 week 6.pdf",
                                link = "https://attachments.classcharts.com/h/186700/401f6c71e04b551d7b3f2d84c016afdb_20251209_122813.pdf?GoogleAccessId=attachments-classcharts%40edukey-classcharts.iam.gserviceaccount.com&Expires=1765590527&Signature=k4zDUbWzt283HeF826n2KCNNBHJb8e8UmSWCNMVdPsXv9%2BJaubAt6d%2BriAO3cedMWfCXI8IEVPBZYrUrKH7G9kjeH8fsmayyb0ZpgZXASCLM9xoYH%2B0iK%2BD5j5Y0NgKiQEsxOrO5JvIoQkM5hpaheRbmNMzpqumFv8cqV5JmhRBkqnEfdJDIaQygFhD70Gw7%2BCx6Co%2BIehK0M%2FXDcmh5zcLVTB2yAw3s35ZX0YF91SGUEwUcNf2dqSfRRhSsjRfkZbI8IFVVx38AB0vB%2FRpe8dA3JFDJ4dLrbUB5DQ6nIQeaAQgai7lbpIhf5xA9m2thNzUrHnq9UbBFKmURZNsXFg%3D%3D&response-content-disposition=attachment; filename=\"Term+2+week+6.pdf\"",
                                isFile = true
                            )
                        )
                    )
                )
                homeworksList += normalHomeworkToHomeworkContent(
                    Homework(
                        title = "Revision for Forces in Equilibrium Test",
                        complete = false,
                        teacher = "Mr D Teacher",
                        subject = "Physics",
                        completionTime = "60 minutes",
                        body = AnnotatedString("Use the attached revision materials along with your class notes to revise for Forces in Equilibrium Test."),
                        rawBody = "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Use the attached revision materials along with your class notes to\n" +
                                "revise for Forces in Equilibrium Test.\n" +
                                "\n",
                        issueDate = LocalDate.now().minusDays(4),
                        dueDate = LocalDate.now().plusDays(1),
                        id = "736381326",
                        attachments = mutableListOf(
                            Attachment(
                                name = "Static_calculations_.pdf",
                                link = "https://attachments.classcharts.com/h/186700/44e36a500057a8e3aac78e4a328e3943_20251031_142537.pdf?GoogleAccessId=attachments-classcharts%40edukey-classcharts.iam.gserviceaccount.com&Expires=1765607973&Signature=SUa6jk4APJLCDzYDBIff5F8WvyP1WpVASSS9EKd5FSYnloGopFop%2BUxL8Mz7G3sSsFKvTYGAzGZa7eYMUfWYHcKQ%2FqWheIQCc698yCay5EIdA1TEDnR7vj9mE8nRh9%2B0carPcQ5snzXeo%2Fc3mob9uWYsUpAJVQ6ka9QsKhDnvJ8T6G9GI4xlQ8FERBldrmPkzWHCfyTOXoqf%2BLeY2N11HTUie4zzIjYsMHSFLE7n6elK6ddESyzePKC1zBt9mqMYGrK47CTdkPNPXibFY3ulHB7fCLYc6fVLIyYQHKWgKYoko8HLcTIWTKQ60YR%2FGLTBI%2FWQb75GtNJF3gMaNDk5qw%3D%3D&response-content-disposition=attachment; filename=\"Static_calculations_.pdf\"",
                                isFile = true
                            ),
                            Attachment(
                                name = "Revision Grids for Forces in Equilibrium.pdf",
                                link = "https://attachments.classcharts.com/h/186700/df9a95fbb96b6be9fff9a7da4c1fc920_20251031_142552.pdf?GoogleAccessId=attachments-classcharts%40edukey-classcharts.iam.gserviceaccount.com&Expires=1765607973&Signature=bzqj5c%2B%2BsbuofuhJ8uvW%2FMHhPFLcAnQovqLf7ZcohAWzON7mhnp4f5geqVB41%2FFs7xKjVv26BtHUL%2FTO%2BJWfpEYbC4LpxXNkuSNspddjFBwerwJhO%2FRHk3C0l1MLzuWrPLuu6D1MocqqAshEXLgSbx4IH6ldUy%2Fe9fWwuZAZ2EUS6zc%2FGKssjntdRrl0qIWAH2vLg%2B9Y%2FHelFSuzKjOshzkm4lUxlPFU0rXNadjzZ5iUDCXGVFdD5jPwjixmpUAQzIJzl1lvvAnCzvwDE1ZEqLEUMsOy8M0YzKCAgACZspZhvEbs%2ByvmixaBcsQ%2Frjtejoj%2FLNiEeGvCNx1PYCTVAA%3D%3D&response-content-disposition=attachment; filename=\"Revision+Grids+for+Forces+in+Equilibrium.pdf\"",
                                isFile = true
                            ),
                            Attachment(
                                name = "Revision Grids for Forces in Equilibrium_MS.pdf",
                                link = "https://attachments.classcharts.com/h/186700/afce3edcef6c8f4b69e90dfb1e69b8e1_20251031_142600.pdf?GoogleAccessId=attachments-classcharts%40edukey-classcharts.iam.gserviceaccount.com&Expires=1765607973&Signature=okHxBPBFQf21vj7WLEfRXuCzJ%2F1haSzszpMvg%2BCAhhFcxXvsOVE1qyVwaPVcWJ30Av698yi4YgAHZdtj%2FRtjCaRXbAgt7C3rhwib%2FvO3XKAaA5FyF8wSthve2tM%2FZe%2F8z2gZaGjYjiKE88z4cv00CYHahx7QJXnedRJZRq1SkL%2FkaKJnWKO3SBzcWN9XBD0ERsLtUuDDTZlH34aE81dLw1KuhKoFepncNQTHwkiw0E8IX2%2BvMN68rcCVXqXorW986nB0W6ThZvoaa9oOo3iGPxubVSNzMeA%2Bk7FrjVSw2k7Ing%2BU82hAyELFx7ZXB9FMocU122OllOxeot3JY28FmQ%3D%3D&response-content-disposition=attachment; filename=\"Revision+Grids+for+Forces+in+Equilibrium_MS.pdf\"",
                                isFile = true
                            ),
                            Attachment(
                                name = "Practice questions chapter 6 ANSWERS.pdf",
                                link = "https://attachments.classcharts.com/h/186700/ebab40e394ab4347208b712a256062f0_20251104_005108.pdf?GoogleAccessId=attachments-classcharts%40edukey-classcharts.iam.gserviceaccount.com&Expires=1765607973&Signature=fiG2aJOn9CIFv%2F8oHL7CmREh1jNpzGe0fj2MiVoFumw4Oo2UN83yIYyJkaSb%2BkKbG7ih9tRDkvQb4EKEagxlfLvM2CoIfIBcYWcMgYg7CF7voE7qSEo67kMeyEYjb9kwThtK4fm79F%2BwZLahqWmxsZebKs1Smdnh%2F2K4J6TiHBjQwnmfMrhOqoLqaTHXkB7oIq9pCfb2edOzbIUIu%2BWu7qQWW01wN6s%2FncML66PWiYvP9zHidhs88E5DtAXSyEQoP41xWtXZGKL3RQ%2BJVLS8OecMX%2FBM1bewnQfs4lyDsFERb2op9yPJEqX8lcr3%2BXfiigjXtpZ5Rt4PecZ%2FownWOg%3D%3D&response-content-disposition=attachment; filename=\"Practice+questions+chapter+6+ANSWERS.pdf\"",
                                isFile = true
                            )
                        )
                    )
                )
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
                homeworksList += HomeworkContentObject(
                    title = "Music",
                    complete = false,
                    teacher = "Mr M Teacher",
                    subject = "Music",
                    completionTime = "2 hours",
                    body = rawText,
                    issueDate = LocalDate.now().minusDays(6).toString(),
                    dueDate = LocalDate.now().plusDays(4).toString(),
                    id = "736253715",
                    attachments = "[]"
                )
                homeworkDao!!.insertAll(homeworksList)
                return Success()
            }
        }

        try {
            dob = LocalDate.parse(dob).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
        catch (e: Exception) {
            Logger.w(e.toString())
            Logger.w("dob is empty probably, or not in a good date format")
        }

        //val requestBody = FormBody.Builder()
        //    .add("code", id)
        //    .add("remember", "true")
        //    .add("recaptcha-token", "no-token-available")
        //    .add("dob", dob)
        //    .build()

        //val request = HTTPRequestBuilder()
        //    .url()
        //    .post(requestBody)
        //    .build()


        val response = client.post("https://www.classcharts.com/apiv2student/login") {
            /*url {
                parameters.append("code", id)
                parameters.append("remember", "true")
                parameters.append("recaptcha-token", "no-token-available")
                parameters.append("dob", dob)
            }*/
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(FormDataContent(Parameters.build {
                append("code", id)
                append("remember_me", "1")
                append("recaptcha-token", "no-token-available")
                append("dob", dob)
            }))
            //cookie("student_session_credentials", "%7B%22remember_me%22%3Atrue%2C%22session_id%22%3A%22$id%22%7D")
        }

        if (!(response.status.value in 200..302)) return ErrorNetwork() //throw _root_ide_package_.okio.IOException("Unexpected code $response")
        studentLoginResponse = response.body()//gson.fromJson(response.bodyAsText(), JsonObject::class)
        Logger.d("StudentIDInLoginFunc") {id}
        Logger.d(tag="RealLoginResponseRaw", messageString=studentLoginResponse.toString())
        try {
            sessionId =
                studentLoginResponse?.get("meta")?.jsonObject?.get("session_id")?.toString()?.trim('"')
        }
        catch (e: Exception) {
            Logger.w(e.toString())
            return ErrorInvalidLogin()
        }
        userDao!!.setStudentId(id)
        userDao!!.setStudentDob(dob)
        studentId = id
        studentDob = dob

        userDao!!.setValidLogin(true)
        return Success()
    }


    fun yesnoToTruefalse(yesno: String): Boolean {
        if (yesno == "yes") return true
        else return false
    }

    fun homeworkContentToNormalHomework(homeworkContentObj: HomeworkContentObject, linkStyle: TextLinkStyles): Homework {
        return Homework(
            title = homeworkContentObj.title,
            complete = homeworkContentObj.complete,
            teacher = homeworkContentObj.teacher,
            subject = homeworkContentObj.subject,
            completionTime = homeworkContentObj.completionTime,
            body = htmlToAnnotatedString(homeworkContentObj.body, style = HtmlStyle(linkStyle)),
            rawBody = homeworkContentObj.body,
            issueDate = LocalDate.parse(homeworkContentObj.issueDate),
            dueDate = LocalDate.parse(homeworkContentObj.dueDate),
            id = homeworkContentObj.id,
            attachments = Json.decodeFromString(homeworkContentObj.attachments)
        )
    }

    fun normalHomeworkToHomeworkContent(homework: Homework): HomeworkContentObject {
        return HomeworkContentObject(
            title = homework.title,
            complete = homework.complete,
            teacher = homework.teacher,
            subject = homework.subject,
            completionTime = homework.completionTime,
            body = homework.rawBody!!,
            issueDate = homework.issueDate.toString(),
            dueDate = homework.dueDate.toString(),
            id = homework.id!!,
            attachments = Json.encodeToString(homework.attachments)
        )
    }

    val nothingimportantignorethisitscommentedoutbecauseitwasntbeingused = """
    fun studentPing(): Boolean { // Updates cookies maybe
        val response = client.get("https://www.classcharts.com/apiv2student/ping") {
            url {
                parameters.append("include_data", "true")
                headers.append("Authorization", "Basic $sessionId")
                
            }
        }
        
        //val requestBody = FormBody.Builder()
        //    .add("include_data", "true")
        //    .build()

        val request = Request.Builder()
            .url("https://www.classcharts.com/apiv2student/ping")
            .header("Authorization", "Basic $sessionId")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw _root_ide_package_.okio.IOException("Unexpected code {DOLLARSIGNbutitwasinterferingwiththecommentingout}response")
            val jsonResponse = gson.fromJson(response.body?.string(), JsonObject::class.java)
            try {
                //sessionId = jsonResponse?.getAsJsonObject("meta")?.get("session_id")?.asString
                // studentId = jsonResponse?.getAsJsonObject("data")?.getAsJsonObject("user")?.get("id")?.asString
                return true
            }
            catch (e: Error) {
                return false
            }
        }
    }
    """

    suspend fun getHomeworks(startDate: LocalDate = LocalDate.now().minusDays(45),
                             endDate: LocalDate = LocalDate.now().plusDays(366)): JsonObject? {
        // To get current date: LocalDate.now()

        //val url = "https://www.classcharts.com/apiv2student/homeworks/$studentId".toHttpUrlOrNull()!!
        //    .newBuilder()
        //    .addQueryParameter("display_date", "due_date")
        //    .addQueryParameter("from", startDate.toString())
        //    .addQueryParameter("to", endDate.toString())
        //    .build()

        //val request = Request.Builder()
        //    .url(url)
        //    .header("Authorization", "Basic $sessionId")
        //    .build()

        login("", "")
        Logger.d("CookiesInHomeworks") {runBlocking{client.cookies("https://www.classcharts.com").toString()}}

        val response = client.get("https://www.classcharts.com/apiv2student/homeworks/$studentId") {
            url {
                parameters.append("display_date", "due_date")
                parameters.append("from", startDate.toString())
                parameters.append("to", endDate.toString())

                headers.append("Authorization", "Basic $sessionId")
            }
            cookie("student_session_credentials", "%7B%22remember_me%22%3Atrue%2C%22session_id%22%3A%22$sessionId%22%7D")
        }
        Logger.d("SessionIDHomeworks") {sessionId.toString()}
        Logger.d("HomeworkResponse") { runBlocking{response.body<JsonObject>().toString()}}
        if (!(response.status.value in 200..299)) return null

        return response.body<JsonObject>().get("data")?.jsonObject
    }

    suspend fun refreshHomeworkList(onlyIncomplete: Boolean, linkStyle: TextLinkStyles, colorScheme: ColorScheme, onFinish: () -> Unit = {}) {
        val homeworksList = mutableListOf<Homework>()
        if (sessionId != "demo") {
            val homeworks = getHomeworks()
            if (homeworks != null) {
                for (i in homeworks) {
                    val isComplete =
                        yesnoToTruefalse(i.value.jsonObject.get("status")?.jsonObject?.get("ticked")?.toString()?: "no")
                    if (true) { //(Want everything in the database) //if (!onlyIncomplete || !isComplete) {
                        var attachments: MutableList<Attachment> = mutableListOf()


                        for (j in i.value.jsonObject.get("validated_links")?.jsonArray?: mutableListOf<JsonObject>()) {
                            attachments += Attachment(
                                name = j.jsonObject.get("link").toString()?: "unknown-site.com",
                                link = j.jsonObject.get("link").toString()?: "https://example.com",
                                isFile = false
                            )
                        }

                        for (l in i.value.jsonObject.get("validated_attachments")?.jsonArray?: mutableListOf<JsonObject>()) {
                            attachments += Attachment(
                                name = l.jsonObject.get("filename").toString()?: "UnknownFile",
                                link = l.jsonObject.get("file").toString()?: "https://example.com",
                                isFile = true
                            )
                        }

                        homeworksList += Homework(
                            title = (i.value.jsonObject.get("title")?.toString())?: "",
                            complete = isComplete,
                            teacher = (i.value.jsonObject.get("teacher")?.toString()?: ""),
                            subject = (i.value.jsonObject.get("subject")?.toString()?: ""),
                            completionTime = (
                                    if (i.value.jsonObject.get("completion_time_value")?.toString() != "") {
                                        i.value.jsonObject.get("completion_time_value")?.toString() + " " + i.value.jsonObject.get("completion_time_unit")?.toString()
                                    } else ""),
                            body = htmlToAnnotatedString(
                                (i.value.jsonObject.get("description")?.toString()?: "No description"),
                                style = HtmlStyle(linkStyle)
                            ),
                            rawBody = (i.value.jsonObject.get("description")?.toString()?: "No description"),
                            issueDate = LocalDate.parse(i.value.jsonObject.get("issue_date")?.toString()?: "1990-01-01"),
                            dueDate = LocalDate.parse(i.value.jsonObject.get("due_date")?.toString()?: "2200-01-01"),
                            id = i.value.jsonObject.get("status")?.jsonObject?.get("id")?.toString()?:UUID.randomUUID().toString(),
                            attachments = attachments
                        )
                    }
                }
                val rawHomeworksList = mutableListOf<HomeworkContentObject>()
                for (homework in homeworksList) {
                    rawHomeworksList += normalHomeworkToHomeworkContent(homework)
                }
                homeworkDao!!.insertAll(rawHomeworksList)
                onFinish()
            }
        }
        else {
            onFinish()
        }
    }



    suspend fun tickHomework(homeworkId: String, onFinish: () -> Unit = {}) {
        if (sessionId == "demo") {
            homeworkDao!!.tickHomework(homeworkId)
        }
        val response = client.get("https://www.classcharts.com/apiv2student/homeworkticked/$homeworkId") {
            url {
                parameters.append("studentId", studentId?: "")
                headers.append("Authorization", "Basic $sessionId")
            }
        }

        if (response.status.value in 200..299) {}//do something to convey an error and maybe have a queue of operations yet to be performed, i'm not sure yet

        //val url = "https://www.classcharts.com/apiv2student/homeworkticked/$id".toHttpUrlOrNull()!!
        //    .newBuilder()
        //    .addQueryParameter("studentId", studentId)
        //    .build()

        //val request = Request.Builder()
        //    .url(url)
        //    .addHeader("Authorization", "Basic $sessionId")
        //    .build()

        //client.newCall(request).execute().use { response ->
        //    if (!response.isSuccessful) throw _root_ide_package_.okio.IOException("Unexpected code $response")
        //}
        onFinish()
    }

    suspend fun listLessons(date: LocalDate): Either<MutableList<Lesson>, ErrorType> {
        var lessonList = mutableListOf<Lesson>()

        if (sessionId == "demo") {
            lessonList += Lesson(teacherName="Mx C Teacher", lessonName="12C/Tu", subjectName="Tutor Time", isAlternativeLesson=false, periodNumber="Tut", roomName="U02", startTime="${LocalDate.now().toString()}T08:40:00+00:00", endTime="${LocalDate.now().toString()}T09:00:00+00:00", key=1157931873, date=LocalDate.now().toString())
            lessonList += Lesson(teacherName="Mrs E Teacher", lessonName="12D/Ma1", subjectName="Maths", isAlternativeLesson=false, periodNumber="1", roomName="L01", startTime="${LocalDate.now().toString()}T09:00:00+00:00", endTime="${LocalDate.now().toString()}T10:00:00+00:00", key=1192664549, date=LocalDate.now().toString())
            lessonList += Lesson(teacherName="Ms H Teacher", lessonName="12D/Ma1", subjectName="Maths", isAlternativeLesson=false, periodNumber="2", roomName="L06", startTime="${LocalDate.now().toString()}T10:05:00+00:00", endTime="${LocalDate.now().toString()}T11:05:00+00:00", key=1192664561, date=LocalDate.now().toString())
            lessonList += Lesson(teacherName="Mr D Teacher", lessonName="12B/Ph1", subjectName="Physics", isAlternativeLesson=false, periodNumber="3", roomName="U07", startTime="${LocalDate.now().toString()}T11:25:00+00:00", endTime="${LocalDate.now().toString()}T12:25:00+00:00", key=1157937234, date=LocalDate.now().toString())
            lessonList += Lesson(teacherName="Miss K Teacher", lessonName="12B/Ph1", subjectName="Physics", isAlternativeLesson=false, periodNumber="4", roomName="U09", startTime="${LocalDate.now().toString()}T12:30:00+00:00", endTime="${LocalDate.now().toString()}T13:30:00+00:00", key=1157941107, date=LocalDate.now().toString())
        }
        else {
            val response = client.get("https://www.classcharts.com/apiv2student/timetable/$studentId") {
                url {
                    parameters.append("date", date.toString())
                    headers.append("Authorization", "Basic $sessionId")
                }
            }

            login("", "")


            if (response.status.value !in 200..299) return Either.Right(ErrorNetwork())
            val jsonResponse = response.body<JsonObject>()
            for (i in jsonResponse.get("data")?.jsonArray?: mutableListOf<JsonObject>()) {
                val l = i.jsonObject
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
                    key = l.get("key")?.toString()?.toInt() ?: 0,
                    date = l.get("date")?.toString()?.replace("\"", "")?: "1970-01-01"
                )
            }

        }
        lessonDao!!.insertDay(lessonList)
        return Either.Left(lessonList)

    }

}

fun getMillisForLocalDate(date: LocalDate): Long {
    return date.toEpochDay() * 24 * 60 * 60 * 1000
}