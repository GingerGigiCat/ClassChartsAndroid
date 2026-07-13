package com.gigi.classchartsandroid

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.gigi.cca.shared.App
import com.gigi.cca.shared.ui.DateDivider
import com.gigi.cca.shared.Homework
import com.gigi.cca.shared.ui.HomeworkCard
import com.gigi.cca.shared.ui.theme.ClassChartsAndroidTheme
import java.time.LocalDate

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

        //val homeworksList: MutableList<Homework> = mutableListOf()

        setContent {
            App()
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


