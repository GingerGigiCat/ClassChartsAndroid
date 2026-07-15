package com.gigi.cca.shared

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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import co.touchlab.kermit.Logger
import com.gigi.cca.shared.ui.DateDivider
import com.gigi.cca.shared.ui.HomeworkCard
import com.gigi.cca.shared.ui.HomeworkContent
import com.gigi.cca.shared.ui.ShowCompletedHomeworksToggle
import com.gigi.cca.shared.ui.TimetableScreen
import com.gigi.cca.shared.ui.TodayMarker
import com.gigi.cca.shared.ui.theme.ClassChartsAndroidTheme
import com.gigi.cca.shared.ui.LogInScreen
import com.gigi.cca.shared.ui.icons.calendar_month
import com.gigi.cca.shared.ui.icons.list
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min


class App {
    var appContext: Any? = null
    constructor() {

    }

    constructor(context: Any?) {
        appContext = context
    }


    @Composable
    fun Ui() {
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val requestMaker = RequestMaker(appContext)
        //open class ScreenObject

        data class NavigationItem(
            val title: String,
            val icon: ImageVector,
            val route: ScreenObject
        )

        var studentId by remember { mutableStateOf(requestMaker.studentId) }
        var studentDob by remember { mutableStateOf(requestMaker.studentDob) }
        var loginResponse by remember {
            mutableStateOf<ErrorType>(ErrorWaiting()) // requestMaker.login(studentId, studentDob)
        } //TODO: Make this not runblocking and use the login sign from the db
        LaunchedEffect(Dispatchers.IO) {
            loginResponse = requestMaker.login(studentId, studentDob)
        }
        val homeworksList = remember { mutableStateListOf<Homework>() }
        var updateHomeworksColumnNeeded by remember { mutableStateOf(true) }
        var triggerHomeworkListUpdate by remember { mutableStateOf(true) }
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
            NavigationItem(
                "Homework",
                list,
                HomeworkListObject
            ),
            NavigationItem(
                "Timetable",
                calendar_month,
                TimetableScreenObject
            )
        )

        if (loginResponse is ErrorInvalidLogin) {
            startDestination = LoginScreenObject
        } // TODO: add handling for waiting and network error
        if (loginResponse is Success || loginResponse is ErrorWaiting) {
            startDestination = HomeworkListObject
        }
        Logger.d("LoginResponse") { loginResponse.toString() } // TODO: figure out why this is always error

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
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        bottomBar = navBar
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                        ) {
                            ShowCompletedHomeworksToggle(
                                showCompletedHomeworksChecked,
                                { showCompletedHomeworksChecked = it })
                            val colorScheme = MaterialTheme.colorScheme
                            LazyColumn {
                                if (updateHomeworksColumnNeeded) {
                                    updateHomeworksColumnNeeded = false
                                    Logger.d(
                                        "HomeworksListUpdate"
                                    )
                                    { "Updating the homework list due to db change" }
                                    val rawHomeworksList = runBlocking(Dispatchers.IO) {
                                        requestMaker.homeworkDao!!.getAll(
                                            showCompletedHomeworksChecked
                                        ) }
                                    homeworksList.clear()
                                    for (rawHomework in rawHomeworksList) {
                                        homeworksList += requestMaker.homeworkContentToNormalHomework(
                                            rawHomework,
                                            linkStyle
                                        )
                                    }
                                } else if (!triggerHomeworkListUpdate) {
                                    val rawHomeworksList =
                                        runBlocking(Dispatchers.IO) {
                                            requestMaker.homeworkDao!!.getAll(
                                                showCompletedHomeworksChecked
                                            )
                                        }
                                    homeworksList.clear()
                                    for (rawHomework in rawHomeworksList) {
                                        homeworksList += requestMaker.homeworkContentToNormalHomework(
                                            rawHomework,
                                            linkStyle
                                        )
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
                                    key = { index, homework -> homework.id!! }
                                ) { index, homework ->
                                    var cardVisible by remember { mutableStateOf(true) }
                                    var lonelyHomework =
                                        ((index == 0) || homework.dueDate != homeworksList[max(
                                            index - 1,
                                            0
                                        )].dueDate) && (index == homeworksList.size - 1 || homework.dueDate != homeworksList[min(
                                            index + 1,
                                            homeworksList.size - 1
                                        )].dueDate)
                                    AnimatedVisibility(visible = !(!cardVisible && lonelyHomework),) {
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

                                    AnimatedVisibility(
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically(),
                                        visible = cardVisible
                                    ) {
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
                val homework: Homework =
                    requestMaker.homeworkContentToNormalHomework(homeworkContentObj, linkStyle)
                HomeworkContent(homework)
            }
            composable<LoginScreenObject> { backStackEntry ->
                ClassChartsAndroidTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) { innerPadding ->
                        loginResponse = LogInScreen(Modifier.padding(innerPadding), requestMaker, {
                            navController.navigate(
                                HomeworkListObject
                            )
                        })
                    }
                }
            }
            composable<TimetableScreenObject> {
                TimetableScreen(navBar, requestMaker)
            }
        }
    }
}