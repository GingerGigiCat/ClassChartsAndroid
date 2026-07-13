package com.gigi.classchartsandroid

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gigi.cca.shared.ui.DateDivider
import com.gigi.cca.shared.ErrorInvalidLogin
import com.gigi.cca.shared.Homework
import com.gigi.cca.shared.ui.HomeworkCard
import com.gigi.cca.shared.ui.HomeworkContent
import com.gigi.cca.shared.RequestMaker
import com.gigi.cca.shared.ScreenObject
import com.gigi.cca.shared.HomeworkContentObject
import com.gigi.cca.shared.HomeworkListObject
import com.gigi.cca.shared.ui.ShowCompletedHomeworksToggle
import com.gigi.cca.shared.Success
import com.gigi.cca.shared.ui.TimetableScreen
import com.gigi.cca.shared.ui.TodayMarker
import com.gigi.cca.shared.ui.theme.ClassChartsAndroidTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

// Features to add:
// Tickable homeworks DONE but make it work on the actual homework page
// Timetable DONE but add swiping between days DONE
// Login page DONE
// Add notes to homeworks
// Half tick homeworks
// User addable tasks
// Shading of incomplete homeworks
// fix networkonmainthread
// make it not hang when you do anything // TODO: ALSO DOING THIS
// figure out why the calendar is laggy to open
// implement loading wheels and nice error handling
// Export your timetable and then import to a friend? because storing friend's classcharts code directly is kind of a bit very insecure
// Animations! like for ticking off a homework so it doesn't just abruptly disappear DONE
// Make opening microsoft documents not crash it
// Login with microsoft??
// Make the timetable show free periods?
// Offline mode // TODO: THIS
// Add not terrible wearos support
// Add desktop/web app support
// Add notification support (new homework set, homeworks due tomorrow etc)



val Context.appDataStore: DataStore<Preferences> by preferencesDataStore("settings")
class MainActivity : ComponentActivity() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    //open class ScreenObject

    @Serializable
    object LoginScreenObject : ScreenObject()

    @Serializable
    object TimetableScreenObject : ScreenObject()

    data class NavigationItem(
        val title: String,
        val icon: ImageVector,
        val route: ScreenObject
    )

    companion object {
        lateinit var instance: MainActivity
            private set
        fun isInstanceInitialised() = ::instance.isInitialized
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        enableEdgeToEdge()

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        )
        val requestMaker = RequestMaker()

        //val homeworksList: MutableList<Homework> = mutableListOf()

        setContent {
            var studentId by remember { mutableStateOf(requestMaker.studentId) }
            var studentDob by remember { mutableStateOf(requestMaker.studentDob) }
            var loginResponse by remember { mutableStateOf(runBlocking { requestMaker.login(studentId, studentDob) }) }
            val homeworksList = remember { mutableStateListOf<Homework>() }
            var updateHomeworksColumnNeeded by remember { mutableStateOf(true) }
            var triggerHomeworkListUpdate by remember {mutableStateOf(true)}
            var showCompletedHomeworksChecked by remember { mutableStateOf(true) }
            val linkStyle = TextLinkStyles(
                style = SpanStyle(
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.secondary
                )
            )
            //requestMaker.listLessons(LocalDate.now())
            val navController = rememberNavController()
            var startDestination: ScreenObject = LoginScreenObject
            var selectedDestination by rememberSaveable { mutableIntStateOf(0) }
            val destinations = listOf(
                NavigationItem("Homework",
                    ImageVector.vectorResource(R.drawable.ico_list),
                    HomeworkListObject),
                NavigationItem("Timetable",
                    ImageVector.vectorResource(R.drawable.ico_calendar_month),
                    TimetableScreenObject)
            )

            if (loginResponse is ErrorInvalidLogin) {
                startDestination = LoginScreenObject
            } // TODO: add handling for waiting and network error
            if (loginResponse is Success) {
                startDestination = HomeworkListObject
            }
            Log.d("LoginResponse", loginResponse.toString()) // TODO: figure out why this is always error

            val navBar = @Composable {
                NavigationBar {
                    destinations.forEachIndexed { index, item: NavigationItem ->
                        NavigationBarItem(
                            selected = (index == selectedDestination),
                            onClick = {
                                if (index != selectedDestination) {
                                    navController.navigate(item.route)
                                    selectedDestination = index
                                }
                            },
                            icon = {
                                Icon(item.icon, item.title)
                            },
                            label = { Text(item.title) }
                        )
                    }
                }
            }

            NavHost(navController, startDestination = startDestination) {
                composable<HomeworkListObject> {
                    val homeworkListScope = rememberCoroutineScope()
                    //HomeworkList(requestMaker = requestMaker, homeworksList = homeworksList, onlyIncomplete = true)
                    ClassChartsAndroidTheme {
                        Scaffold(modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.surfaceContainerLow, bottomBar = navBar) { innerPadding ->
                            Column(modifier = Modifier
                                .padding(innerPadding)
                            ) {
                                ShowCompletedHomeworksToggle(
                                    showCompletedHomeworksChecked,
                                    { showCompletedHomeworksChecked = it })
                                val colorScheme = MaterialTheme.colorScheme
                                LazyColumn {
                                    if (updateHomeworksColumnNeeded) {
                                        updateHomeworksColumnNeeded = false
                                        Log.d("HomeworksListUpdate", "Updating the homework list due to db change")
                                        val rawHomeworksList = requestMaker.homeworkDao.getAll(showCompletedHomeworksChecked)
                                        homeworksList.clear()
                                        for (rawHomework in rawHomeworksList) {
                                            homeworksList += requestMaker.homeworkContentToNormalHomework(rawHomework, linkStyle)
                                        }
                                    }
                                    else if (!triggerHomeworkListUpdate){
                                        val rawHomeworksList = requestMaker.homeworkDao.getAll(showCompletedHomeworksChecked)
                                        homeworksList.clear()
                                        for (rawHomework in rawHomeworksList) {
                                            homeworksList += requestMaker.homeworkContentToNormalHomework(rawHomework, linkStyle)
                                        }
                                        homeworkListScope.launch(Dispatchers.IO) {
                                            requestMaker.refreshHomeworkList(
                                                showCompletedHomeworksChecked,
                                                linkStyle,
                                                colorScheme,
                                                { updateHomeworksColumnNeeded = true })
                                        }
                                    }
                                    if (triggerHomeworkListUpdate) {

                                        homeworkListScope.launch(Dispatchers.IO) {
                                            requestMaker.refreshHomeworkList(
                                                showCompletedHomeworksChecked,
                                                linkStyle,
                                                colorScheme,
                                                { updateHomeworksColumnNeeded = true }
                                            )
                                        }
                                        triggerHomeworkListUpdate = false
                                    }

                                    itemsIndexed(
                                        items = homeworksList,
                                        key = {index, homework -> homework.id!! }
                                    ) { index, homework ->
                                        var cardVisible by remember { mutableStateOf(true) }
                                        var lonelyHomework = ((index == 0) || homework.dueDate != homeworksList[max(index-1, 0)].dueDate) && (index == homeworksList.size-1 || homework.dueDate != homeworksList[min(index+1, homeworksList.size-1)].dueDate)
                                        AnimatedVisibility(visible=!(!cardVisible && lonelyHomework), ) {
                                            Column {
                                                if (homework.dueDate != homeworksList[max(
                                                        index - 1,
                                                        0
                                                    )].dueDate || index == 0 // if date is different to the previous one
                                                ) {
                                                    if (homework.dueDate!! >= LocalDate.now() && (homeworksList[max(
                                                            index - 1,
                                                            0
                                                        )].dueDate!! < LocalDate.now() || index == 0)
                                                    ) {
                                                        TodayMarker()
                                                    }
                                                    if (homework.dueDate != LocalDate.now()) {
                                                        DateDivider(
                                                            homework.dueDate
                                                                ?: LocalDate.ofEpochDay(0)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        AnimatedVisibility(enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically(), visible=cardVisible) {
                                            HomeworkCard(
                                                homework = homework, compact = false,
                                                navigate = {
                                                    navController.navigate(
                                                        requestMaker.normalHomeworkToHomeworkContent(
                                                            homework
                                                        )
                                                    )
                                                },
                                                requestMaker = requestMaker,
                                                homeworksList = homeworksList,
                                                onlyIncomplete = showCompletedHomeworksChecked,
                                                linkStyle = linkStyle,
                                                triggerHomeworkListUpdate = {
                                                    triggerHomeworkListUpdate = true
                                                },
                                                homeworkListScope = homeworkListScope,
                                                toggleVisibility = {
                                                    if (showCompletedHomeworksChecked) cardVisible =
                                                        !cardVisible
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                composable<HomeworkContentObject> { backStackEntry ->
                    val homeworkContentObj: HomeworkContentObject = backStackEntry.toRoute()
                    val homework: Homework = requestMaker.homeworkContentToNormalHomework(homeworkContentObj, linkStyle)
                    HomeworkContent(homework)
                }
                composable<LoginScreenObject> { backStackEntry ->
                    ClassChartsAndroidTheme {
                        Scaffold(modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.surfaceContainerLow) { innerPadding ->
                            loginResponse = LogInScreen(Modifier.padding(innerPadding), requestMaker, {navController.navigate(
                                HomeworkListObject
                            )})
                        }
                    }
                }
                composable<TimetableScreenObject> {
                    TimetableScreen(navBar)
                }
            }
        }
    }
}

//data class Homework(val title: String, val complete: Boolean, val teacher: String, val subject: String, val body: String, val dueDate: LocalDate? = null)

val moreunusedcodewhichprobablyshouldbemadeintoanactualmodule = """
@Composable
fun HomeworkList(requestMaker: RequestMaker, homeworksList: MutableList<Homework>, onlyIncomplete: Boolean) {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.secondary
        )
    )
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(Dispatchers.IO) {
        requestMaker.refreshHomeworkList(onlyIncomplete, linkStyle, colorScheme)
    }

    for (homework in homeworksList) {
        HomeworkCard(homework = homework, compact = false, requestMaker = requestMaker)
    }
}
"""


@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GreetingPreview() {
    ClassChartsAndroidTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                //ShowCompletedHomeworksToggle(true)
                HomeworkCard(
                    homework = Homework(
                        title = "Modal Jazz Improvisation",
                        complete = true,
                        teacher = "Mr. Teacher",
                        subject = "Music",
                        body = AnnotatedString.fromHtml("this is a music homework you have to do a lot of work for this because obviously of course you do what more would you expect from homework and this is supposed to be a really loioooonmg description explaining everything you need to do for the task like questyion a question b question cquestion d and all of thsose so that it can show what happens when the content is long, hopefully it will collapse the text and then you can see the whole thing when you clickk on me but who knows")
                    ),
                    compact = true
                )
                DateDivider(LocalDate.parse("2025-05-04"))
                HomeworkCard(
                    homework = Homework(
                        title = "Modal Jazz Improvisation",
                        complete = false,
                        teacher = "Mr. Teacher",
                        subject = "Music",
                        body = AnnotatedString.fromHtml("this is a music homework you have to do a lot of work for this because obviously of course you do what more would you expect from homework and this is supposed to be a really loioooonmg description explaining everything you need to do for the task like questyion a question b question cquestion d and all of thsose so that it can show what happens when the content is long, hopefully it will collapse the text and then you can see the whole thing when you clickk on me but who knows")
                    ),
                    compact = false
                )
            }
        }
    }
}


