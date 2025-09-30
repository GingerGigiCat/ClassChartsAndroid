package com.gigi.classchartsandroid

import android.R
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.widget.ToggleButton
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigi.classchartsandroid.ui.theme.ClassChartsAndroidTheme
import java.time.LocalDate
import java.util.Date
import kotlin.math.truncate
import com.gigi.classchartsandroid.RequestMaker.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        )

        val requestMaker = RequestMaker("ID", "DOB")
        val homeworksList: MutableList<Homework> = mutableListOf()
        for (i in requestMaker.getHomeworks()!!) {
            homeworksList += Homework(
                title = i.asJsonObject.get("title").asString,
                complete = false,
                teacher = i.asJsonObject.get("teacher").asString,
                subject = i.asJsonObject.get("subject").asString,
                body = i.asJsonObject.get("description").asString
            )
        }
        setContent {
            ClassChartsAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)
                        .verticalScroll(rememberScrollState()
                    )) {
                        ShowCompletedHomeworksToggle()
                        for (i in homeworksList) {
                            HomeworkCard(homework = i, compact = true)
                        }
                        HomeworkCard(
                            homework = Homework(
                                title = "Modal Jazz Improvisation",
                                complete = true,
                                teacher = "Mr. Teacher",
                                subject = "Music",
                                body = "this is a music homework you have to do a lot of work " +
                                        "for this because obviously of course you do what more " +
                                        "would you expect from homework and this is supposed to " +
                                        "be a really loioooonmg description explaining everything " +
                                        "you need to do for the task like questyion a question b " +
                                        "question cquestion d and all of thsose so that it can show " +
                                        "what happens when the content is long, hopefully it will " +
                                        "collapse the text and then you can see the whole thing " +
                                        "when you clickk on me but who knows"
                            ),
                            compact = true
                        )
                        HomeworkCard(
                            homework = Homework(
                                title = "Modal Jazz Improvisation",
                                complete = false,
                                teacher = "Mr. Teacher",
                                subject = "Music",
                                body = "this is a music homework you have to do a lot of work " +
                                        "for this because obviously of course you do what more " +
                                        "would you expect from homework and this is supposed to " +
                                        "be a really loioooonmg description explaining everything " +
                                        "you need to do for the task like questyion a question b " +
                                        "question cquestion d and all of thsose so that it can show " +
                                        "what happens when the content is long, hopefully it will " +
                                        "collapse the text and then you can see the whole thing " +
                                        "when you clickk on me but who knows"
                            ),
                            compact = false
                        )
                    }
                }
            }
        }
    }
}

//data class Homework(val title: String, val complete: Boolean, val teacher: String, val subject: String, val body: String, val dueDate: LocalDate? = null)

@Composable
fun HomeworkCard(homework: Homework, modifier: Modifier = Modifier, compact: Boolean = false) {
    Card(modifier = modifier.fillMaxWidth().padding(10.dp)) {
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
                            text = homework.subject + " - " + homework.teacher,
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
                    text = homework.subject + " - " + homework.teacher,
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

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ShowCompletedHomeworksToggle() {
    ClassChartsAndroidTheme {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(15.dp)) {
            Text(text = "Only show incomplete homework")
            Spacer(modifier = Modifier.weight(1f))
            Switch(true, onCheckedChange = null)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GreetingPreview() {
    ClassChartsAndroidTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                ShowCompletedHomeworksToggle()
                HomeworkCard(
                    homework = Homework(
                        title = "Modal Jazz Improvisation",
                        complete = true,
                        teacher = "Mr. Teacher",
                        subject = "Music",
                        body = "this is a music homework you have to do a lot of work for this because obviously of course you do what more would you expect from homework and this is supposed to be a really loioooonmg description explaining everything you need to do for the task like questyion a question b question cquestion d and all of thsose so that it can show what happens when the content is long, hopefully it will collapse the text and then you can see the whole thing when you clickk on me but who knows"
                    ),
                    compact = true
                )
                HomeworkCard(
                    homework = Homework(
                        title = "Modal Jazz Improvisation",
                        complete = false,
                        teacher = "Mr. Teacher",
                        subject = "Music",
                        body = "this is a music homework you have to do a lot of work for this because obviously of course you do what more would you expect from homework and this is supposed to be a really loioooonmg description explaining everything you need to do for the task like questyion a question b question cquestion d and all of thsose so that it can show what happens when the content is long, hopefully it will collapse the text and then you can see the whole thing when you clickk on me but who knows"
                    ),
                    compact = false
                )
            }
        }
    }
}