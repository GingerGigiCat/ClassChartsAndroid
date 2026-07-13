package com.gigi.cca.shared.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import arrow.core.Either
import co.touchlab.kermit.Logger
import com.gigi.cca.shared.ErrorType
import com.gigi.cca.shared.Lesson
import com.gigi.cca.shared.RequestMaker
import com.gigi.cca.shared.getMillisForLocalDate
import com.gigi.cca.shared.ui.theme.ClassChartsAndroidTheme
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.plusAssign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(navBar: @Composable () -> Unit = @Composable {}) {
    val lessonsList = remember { mutableStateListOf<Lesson>() }
    var dateState = rememberDatePickerState(initialDisplayMode = DisplayMode.Picker)
    val getLocalDateObjectForSelected = { LocalDate.ofEpochDay(dateState.selectedDateMillis!!.toLong() / (24 * 60 * 60 * 1000)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var lessonsListResponse by remember { mutableStateOf<Either<MutableList<Lesson>, ErrorType>?>(null) }
    val requestMaker by remember { mutableStateOf(RequestMaker()) }

    if (dateState.selectedDateMillis == null) dateState.selectedDateMillis =
        getMillisForLocalDate(LocalDate.now())

    if ( requestMaker.sessionId == "demo" ) {
        lessonsList += Lesson(
            teacherName = "Mx C Teacher",
            lessonName = "12C/Tu",
            subjectName = "Tutor Time",
            isAlternativeLesson = false,
            periodNumber = "Tut",
            roomName = "U02",
            startTime = "${LocalDate.now().toString()}T08:40:00+00:00",
            endTime = "${LocalDate.now().toString()}T09:00:00+00:00",
            key = 1157931873,
            date = LocalDate.now().toString()
        )
        lessonsList += Lesson(
            teacherName = "Mrs E Teacher",
            lessonName = "12D/Ma1",
            subjectName = "Maths",
            isAlternativeLesson = false,
            periodNumber = "1",
            roomName = "L01",
            startTime = "${LocalDate.now().toString()}T09:00:00+00:00",
            endTime = "${LocalDate.now().toString()}T10:00:00+00:00",
            key = 1192664549,
            date = LocalDate.now().toString()
        )
        lessonsList += Lesson(
            teacherName = "Ms H Teacher",
            lessonName = "12D/Ma1",
            subjectName = "Maths",
            isAlternativeLesson = false,
            periodNumber = "2",
            roomName = "L06",
            startTime = "${LocalDate.now().toString()}T10:05:00+00:00",
            endTime = "${LocalDate.now().toString()}T11:05:00+00:00",
            key = 1192664561,
            date = LocalDate.now().toString()
        )
        lessonsList += Lesson(
            teacherName = "Mr D Teacher",
            lessonName = "12B/Ph1",
            subjectName = "Physics",
            isAlternativeLesson = false,
            periodNumber = "3",
            roomName = "U07",
            startTime = "${LocalDate.now().toString()}T11:25:00+00:00",
            endTime = "${LocalDate.now().toString()}T12:25:00+00:00",
            key = 1157937234,
            date = LocalDate.now().toString()
        )
        lessonsList += Lesson(
            teacherName = "Miss K Teacher",
            lessonName = "12B/Ph1",
            subjectName = "Physics",
            isAlternativeLesson = false,
            periodNumber = "4",
            roomName = "U09",
            startTime = "${LocalDate.now().toString()}T12:30:00+00:00",
            endTime = "${LocalDate.now().toString()}T13:30:00+00:00",
            key = 1157941107,
            date = LocalDate.now().toString()
        )
    }
    else {
        if (requestMaker.sessionId == null) {
            Logger.d("Slow") {"Login"}
            runBlocking { requestMaker.login(null, null) }
        }
    }


    ClassChartsAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            bottomBar = navBar
        ) { innerPadding ->
            val box = BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val maxHeight: Dp = maxHeight
                val buttonHeight = remember { mutableStateOf(0.dp) }

                Column(
                    Modifier.padding(innerPadding).padding(start = 10.dp, end = 10.dp, top = 10.dp)
                        .verticalScroll(rememberScrollState()).fillMaxSize()
                ) {
                    var leftSizeDp by remember { mutableStateOf(10.dp) }
                    val pageCount = 100000
                    val pagerState =
                        rememberPagerState(pageCount = { pageCount }, initialPage = pageCount / 2)
                    var middlePageDate by remember { mutableStateOf(LocalDate.now()) }
                    var lastCurrentPage = pageCount / 2

                    DatePickerButton(
                        getLocalDateObjectForSelected,
                        { showDatePicker = true },
                        "Date",
                        Modifier.onGloballyPositioned({
                            buttonHeight.value = with(density) { it.size.height.toDp() }
                        })
                    )
                    Spacer(Modifier.height(8.dp))
                    if (showDatePicker) DoDatePicker(dateState, { theState ->
                        dateState.selectedDateMillis = theState.selectedDateMillis
                        showDatePicker = false

                        //middlePageDate = getLocalDateObjectForSelected()
                        pagerState.requestScrollToPage((pageCount / 2 + getLocalDateObjectForSelected().toEpochDay() - middlePageDate.toEpochDay()).toInt())
                        //pagerState.requestScrollToPage(pageCount / 2)
                    })
                    Logger.d("Height") {buttonHeight.toString()}
                    HorizontalPager(
                        pagerState,
                        modifier = Modifier.fillMaxWidth()
                            .defaultMinSize(minHeight = maxHeight - buttonHeight.value * 3 - 8.dp),
                        verticalAlignment = Alignment.Top,
                        pageSize = PageSize.Fill
                    ) { page ->
                        Column(Modifier.padding(start = 5.dp, end = 5.dp).fillMaxSize()) {
                            var localLessonsListResponse by remember {
                                mutableStateOf(
                                    lessonsListResponse
                                )
                            }
                            var lastMiddlePageDate = LocalDate.of(1, 1, 1)
                            localLessonsListResponse = lessonsListResponse
                            var localDate by remember { mutableStateOf(LocalDate.now()) }
                            val localLessons = remember { mutableStateListOf<Lesson>() }
                            localDate = middlePageDate.plusDays((page - pageCount / 2).toLong())

                            if (middlePageDate != lastMiddlePageDate) {
                                var isInitial by remember { mutableStateOf(true) }
                                if (isInitial) {
                                    isInitial = false
                                    Logger.d("Slow") {"ListLessonsLocal"}
                                    localLessonsListResponse =
                                        runBlocking { requestMaker.listLessons(localDate) } // TODO: Make the timetable not blocking
                                    if (localLessonsListResponse != null) {
                                        if (localLessonsListResponse!!.isLeft()) {
                                            if (localLessonsListResponse!!.leftOrNull() != null) {
                                                localLessons.clear()
                                                localLessons.addAll(
                                                    localLessonsListResponse!!.leftOrNull()
                                                        ?: mutableListOf<Lesson>()
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (pagerState.currentPage != lastCurrentPage) {
                                dateState.selectedDateMillis =
                                    getMillisForLocalDate(middlePageDate.plusDays(((pagerState.currentPage - pageCount / 2).toLong()))) // TODO: page here used to be the currentPage from the datepicker but using page makes it worse, i think i need some combination of the two
                                lastCurrentPage = pagerState.currentPage
                            }


                            DateDivider(
                                middlePageDate.plusDays(((pagerState.currentPage - pageCount / 2).toLong())),
                                Modifier.padding(horizontal = 0.dp, vertical = 5.dp)
                            )
                            Spacer(Modifier.height(8.dp))

                            if (localLessons.size == 0) {
                                Text("No lessons", color = MaterialTheme.colorScheme.onSurface)
                            }

                            for (lesson in localLessons) {
                                val startTime =
                                    LocalDateTime.parse(lesson.startTime.substring(0, 19))
                                val endTime = LocalDateTime.parse(lesson.endTime.substring(0, 19))
                                Row {
                                    Column {
                                        Text(lesson.periodNumber)
                                        Text(startTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                                        Text(
                                            LocalDateTime.parse(lesson.endTime.substring(0, 19))
                                                .format(DateTimeFormatter.ofPattern("HH:mm"))
                                        )
                                    }
                                    Spacer(Modifier.width(15.dp))
                                    var cardColors = CardDefaults.cardColors()
                                    if (startTime <= LocalDateTime.now() && LocalDateTime.now() <= endTime) {
                                        cardColors = CardColors(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.onPrimaryContainer,
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Card(Modifier.fillMaxWidth(), colors = cardColors) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text("${lesson.subjectName} - ${lesson.lessonName}")
                                            Text(lesson.teacherName)
                                            Text(lesson.roomName)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                            }
                        }

                    }
                }
            }
        }
    }
}