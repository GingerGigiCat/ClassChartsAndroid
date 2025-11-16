package com.gigi.classchartsandroid

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gigi.classchartsandroid.ui.theme.ClassChartsAndroidTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

class MainActivity : ComponentActivity() {
    @Serializable
    object HomeworkListObject
    @Serializable
    data class HomeworkContentObject(
        val title: String,
        val complete: Boolean,
        val teacher: String,
        val subject: String,
        val body: String,
        val issueDate: String = "",
        val dueDate: String = "",
        val id: String = "",
        val completionTime: String,
        val attachments: String
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        )

        val requestMaker = RequestMaker("id", "dob")

        //val homeworksList: MutableList<Homework> = mutableListOf()

        setContent {
            val homeworksList = remember { mutableStateListOf<Homework>() }
            var showCompletedHomeworksChecked by remember { mutableStateOf(true) }
            val linkStyle = TextLinkStyles(
                style = SpanStyle(
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.secondary
                )
            )

            val navController = rememberNavController()
            NavHost(navController, startDestination = HomeworkListObject) {
                composable<HomeworkListObject> {
                    //HomeworkList(requestMaker = requestMaker, homeworksList = homeworksList, onlyIncomplete = true)
                    ClassChartsAndroidTheme {
                        Scaffold(modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) { innerPadding ->
                            Column(modifier = Modifier
                                .padding(innerPadding)
                            ) {
                                ShowCompletedHomeworksToggle(showCompletedHomeworksChecked, {showCompletedHomeworksChecked = it})
                                LazyColumn {
                                    requestMaker.refreshHomeworkList(homeworksList, showCompletedHomeworksChecked, linkStyle)
                                    itemsIndexed(
                                        items = homeworksList,
                                        key = {index, homework -> homework.id!! }
                                    ) { index, homework ->
                                        if (homework.dueDate.toString() != homeworksList[max(index-1, 0)].dueDate.toString() || index == 0) {
                                            DateDivider(homework.dueDate!!)
                                        }
                                        HomeworkCard(homework = homework, compact = false, navigate = {
                                            navController.navigate(
                                                HomeworkContentObject(
                                                    title = homework.title,
                                                    complete = homework.complete,
                                                    teacher = homework.teacher,
                                                    subject = homework.subject,
                                                    completionTime = homework.completionTime,
                                                    body = homework.rawBody!!,
                                                    issueDate = homework.issueDate.toString(),
                                                    dueDate = homework.dueDate.toString(),
                                                    id = homework.id!!,
                                                    attachments = Gson().toJson(homework.attachments)
                                                )) })
                                    }
                                }
                            }
                        }
                    }
                }
                composable<HomeworkContentObject> { backStackEntry ->
                    val homeworkContentObj: HomeworkContentObject = backStackEntry.toRoute()
                    val homework: Homework = Homework(
                        title = homeworkContentObj.title,
                        complete = homeworkContentObj.complete,
                        teacher = homeworkContentObj.teacher,
                        subject = homeworkContentObj.subject,
                        completionTime = homeworkContentObj.completionTime,
                        body = AnnotatedString.fromHtml(homeworkContentObj.body, linkStyles = linkStyle),
                        rawBody = homeworkContentObj.body,
                        issueDate = LocalDate.parse(homeworkContentObj.issueDate),
                        dueDate = LocalDate.parse(homeworkContentObj.dueDate),
                        id = homeworkContentObj.id,
                        attachments = Gson().fromJson(homeworkContentObj.attachments,
                            object: TypeToken<MutableList<Attachment>>() {}.type)
                    )
                    HomeworkContent(homework)
                }
            }


        }
    }
}

//data class Homework(val title: String, val complete: Boolean, val teacher: String, val subject: String, val body: String, val dueDate: LocalDate? = null)

@Composable
fun HomeworkList(requestMaker: RequestMaker, homeworksList: MutableList<Homework>, onlyIncomplete: Boolean) {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.secondary
        )
    )

    requestMaker.refreshHomeworkList(homeworksList, onlyIncomplete, linkStyle)

    for (homework in homeworksList) {
        HomeworkCard(homework = homework, compact = false)
    }
}

@Composable
fun DateDivider(date: LocalDate) {
    Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
        val today = LocalDate.now()
        var dateText = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))
        if (today.minusDays(1) == date) { dateText = "Yesterday" }
        else if (today == date) { dateText = "Today" }
        else if (today.plusDays(1) == date) { dateText = "Tomorrow" }
        Text(dateText, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)

    }
}


@Composable
fun HomeworkCard(homework: Homework, modifier: Modifier = Modifier, compact: Boolean = false, navigate: () -> Unit = {}) {
    Card(modifier = modifier
        .fillMaxWidth()
        .padding(10.dp)
        .clickable(onClick = navigate)) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ){
                Checkbox(checked = homework.complete, onCheckedChange = null)
                Spacer(modifier = Modifier.width(15.dp))
                Column {
                    Text(
                        text = homework.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (compact) {
                        Text(
                            text = homework.subject + " - " + homework.teacher + " - " + homework.completionTime,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (!compact) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = homework.subject + " - " + homework.teacher + " - " + homework.completionTime,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = homework.body,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

//@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ShowCompletedHomeworksToggle(checked: Boolean, onToggle: (Boolean) -> Unit) {
    ClassChartsAndroidTheme {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(15.dp)) {
            Text(text = "Only show incomplete homework")
            Spacer(modifier = Modifier.weight(1f))

            Switch(checked,
                onCheckedChange = onToggle)
        }
    }
}


@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeworkAttachmentCardPreview() {
    ClassChartsAndroidTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Row(Modifier.padding(innerPadding).padding(10.dp)) {
                HomeworkAttachmentCard(
                    Attachment(
                        "verycoolfile.pdf",
                        "https://example.com/verycoolfile.pdf",
                        true
                    )
                )
                HomeworkAttachmentCard(
                    Attachment(
                        "https://example.com/verycoollink",
                        "https://example.com/verycoollink",
                        false
                    )
                )
            }
        }
    }
}

@Composable
fun HomeworkAttachmentCard(attachment: Attachment, modifier:Modifier = Modifier) {
    Card(Modifier.padding(10.dp).width(120.dp).height(100.dp)) {
        Column() {
            Card(
                Modifier.fillMaxWidth().weight(1f).padding(5.dp), colors = CardColors(
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Image(painterResource(R.drawable.link), "link",
                    modifier = Modifier.padding(10.dp).align(Alignment.CenterHorizontally))
            }
            Text("hello", modifier = Modifier.padding(start = 10.dp, top = 0.dp, bottom = 5.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeworkContentPreview() {
    HomeworkContent(Homework(
        title = "Modal Jazz Improvisation",
        complete = true,
        teacher = "Mr. Teacher",
        subject = "Music",
        completionTime = "5 hours",
        body = AnnotatedString.fromHtml("\n\n\n\n\n\n<p><b>TASK 1&nbsp; (2 hrs)</b></p>\n<p>Gain confidence improvising over two famous Modal Jazz\ncompositions by Miles Davis, 'So What' and 'Milestones'&nbsp;</p>\n<p>- Spend time playing and internalising the scales/ modes needed\nto improvise over the chords of each song</p>\n<p>- Spend time playing the scales/ chord tones over the chords\nchanges of the songs and getting a feel for the harmonic\nprogression of the song.&nbsp;</p>\n<p>- Spend time exploring and playing different swung\nrhythms&nbsp;</p>\n<p>- Spend time improvising and developing interesting ideas.</p>\n<p><b>BACKING TRACKS</b></p>\n<p><a href=\n\"https://www.youtube.com/watch?v=FSGWj22wV0U&amp;list=RDFSGWj22wV0U&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=FSGWj22wV0U&amp;list=RDFSGWj22wV0U&amp;start_radio=1</a></p>\n<p><a href=\n\"https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1</a></p>\n<p><br></p>\n<p><b>TASK 2 (2hrs)&nbsp;</b></p>\n<p>Start putting together a Powerpoint for Task 1 (b). Create two\nslides</p>\n<p>SLIDE 1 - Outline in detail the technical and musical\nrequirements needed to improvise in modal jazz. (Discuss everything\nincluding modes, chords scale relationships, chord changes in modal\njazz,&nbsp; rhythmic feel and articulation, phrasing, developing\nideas etc)&nbsp;</p>\n<p>SLIDE 2 - Reflect on/ analyse your ability and skills and set\nsome achievable aims for your improvising. Make sure you go into\ndetail and talk about technical specifics relating to your\ninstrument.&nbsp;</p>\n<p><br></p>\n<p><b>POWERPOINT</b> from class</p>\n<p><a href=\n\"https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1</a></p>\n<p><br></p>\n<p><br></p>\n\n", linkStyles = TextLinkStyles(
                SpanStyle(
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.primary
                )
                )),
        issueDate = LocalDate.parse("2025-04-17"),
        dueDate = LocalDate.parse("2025-12-20")
    ))
}

@Composable
fun HomeworkContent(homework: Homework) {
    ClassChartsAndroidTheme {
        Scaffold(modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) { innerPadding ->
            Column(modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(15.dp)) {
                Row() {
                    Column {
                        Text(
                            text = homework.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${homework.subject} - ${homework.teacher} - ${homework.completionTime}",
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Checkbox(checked = homework.complete, onCheckedChange = null,
                        modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row() {
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                        Text(
                            text = "Set ${homework.issueDate?.format(DateTimeFormatter.ofPattern("EEE dd MMM"))}", // eg. Tue 28 Apr
                            modifier = Modifier
                                .padding(6.dp)
                                .align(Alignment.CenterHorizontally),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Text(
                            text = "Due ${homework.dueDate?.format(DateTimeFormatter.ofPattern("EEE dd MMM"))}",
                            modifier = Modifier
                                .padding(6.dp)
                                .align(Alignment.CenterHorizontally),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = homework.body
                )
                Spacer(Modifier.height(15.dp))
                Text(
                    text = "Attachments",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}


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
