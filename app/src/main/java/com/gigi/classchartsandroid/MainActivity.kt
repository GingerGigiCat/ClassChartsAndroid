package com.gigi.classchartsandroid

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigi.classchartsandroid.ui.theme.ClassChartsAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClassChartsAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeworkCard(
                        homework = Homework("Music", "this is a music homework you have to do a lot of work for this because obviously of course you do what more would you expect from homework and this is supposed to be a really loioooonmg description explaining everything you need to do for the task like questyion a question b question cquestion d and all of thsose so that it can show what happens when the content is long, hopefully it will collapse the text and then you can see the whole thing when you clickk on me but who knows"),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

data class Homework(val title: String, val body: String) //val startDate: String, val endDate: String)

@Composable
fun HomeworkCard(homework: Homework, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(10.dp)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = homework.title, style = typography.titleLarge)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = homework.body)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GreetingPreview() {
    ClassChartsAndroidTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            HomeworkCard(
                homework = Homework("Music", "this is a music homework you have to do a lot of work for this because obviously of course you do what more would you expect from homework and this is supposed to be a really loioooonmg description explaining everything you need to do for the task like questyion a question b question cquestion d and all of thsose so that it can show what happens when the content is long, hopefully it will collapse the text and then you can see the whole thing when you clickk on me but who knows"),
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}