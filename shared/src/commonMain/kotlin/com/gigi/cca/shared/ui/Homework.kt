package com.gigi.cca.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import com.gigi.cca.shared.ui.theme.ClassChartsAndroidTheme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import be.digitalia.compose.htmlconverter.HtmlStyle
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import com.gigi.cca.shared.ui.icons.link_2
import com.gigi.cca.shared.Attachment
import com.gigi.cca.shared.Homework
import com.gigi.cca.shared.RequestMaker
import com.gigi.cca.shared.openUriMime
import com.gigi.cca.shared.ui.icons.calendar_month
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeworkContent(homework: Homework, requestMaker: RequestMaker? = null, linkStyle: TextLinkStyles? = null, homeworksList: MutableList<Homework>? = null, onlyIncomplete: Boolean = true) {
    ClassChartsAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(15.dp)
            ) {
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
                    val colorScheme = MaterialTheme.colorScheme
                    var checkedChangeTrigger by remember {mutableStateOf(false)}
                    if (checkedChangeTrigger) {
                        LaunchedEffect(Dispatchers.IO) {
                            requestMaker?.tickHomework(homework.id!!)
                            homework.complete = !homework.complete
                        }
                    }
                    Checkbox(
                        checked = homework.complete,
                        onCheckedChange = { checkedChangeTrigger = true },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row() {
                    Card(
                        modifier = Modifier
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
                    Card(
                        modifier = Modifier
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
                if (homework.attachments != null) {
                    if ((homework.attachments as Collection<Any?>).isNotEmpty()) {
                        Spacer(Modifier.height(15.dp))
                        Text(
                            text = "Attachments",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(10.dp))
                        for (i in homework.attachments) {
                            HomeworkAttachmentCard(i)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeworkCard(homework: Homework, modifier: Modifier = Modifier, compact: Boolean = false, navigate: () -> Unit = {}, requestMaker: RequestMaker? = null, homeworksList: MutableList<Homework> = mutableListOf<Homework>(), onlyIncomplete: Boolean = true, linkStyle: TextLinkStyles? = null, triggerHomeworkListUpdate: () -> Unit = {}, homeworkListScope: CoroutineScope = CoroutineScope(
    Dispatchers.IO), toggleVisibility: () -> Unit = {}) {
    var subtitleText = ""
    var subtitleTextList = listOf<String>()
    if (homework.subject != "") subtitleTextList += homework.subject
    if (homework.teacher != "") subtitleTextList += homework.teacher
    if (homework.completionTime != "") subtitleTextList += homework.completionTime
    for (i in 0..subtitleTextList.size-1) {
        if (i == 0) {
            subtitleText += subtitleTextList.get(i)
        }
        else {
            subtitleText += " - "
            subtitleText += subtitleTextList.get(i)
        }
    }
    var checkedState by remember { mutableStateOf(homework.complete)}

    Card(modifier = modifier
        .fillMaxWidth()
        .padding(10.dp)
        .clickable(onClick = navigate)) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ){
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 10.dp) {
                    val colorScheme = MaterialTheme.colorScheme
                    Checkbox(
                        checked = checkedState, onCheckedChange =
                            {
                                checkedState = !checkedState
                                toggleVisibility()
                                homeworkListScope.launch {
                                    requestMaker?.tickHomework(
                                        homework.id!!,
                                        triggerHomeworkListUpdate
                                    )
                                }
                            }
                    )
                }
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
                            text = subtitleText,
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
                    text = subtitleText,
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

@Preview(showBackground = true, showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun HomeworkAttachmentCardPreview() {
    ClassChartsAndroidTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(Modifier
                .padding(innerPadding)
                .padding(10.dp)) {
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
    var attachmentExpanded by remember { mutableStateOf(false) }
    var outerCardModifier: Modifier = Modifier

    val openLink = {
        if (!attachmentExpanded) {
            attachmentExpanded = true
        }
        else {
            openUriMime(attachment.link)
        }
    }

    if (attachmentExpanded) {
        outerCardModifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    }
    else {
        outerCardModifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
            .height(80.dp)
    }
    Card(outerCardModifier.clickable(onClick = openLink)) {
        Row() {
            Card(
                Modifier
                    .width(80.dp)
                    .padding(5.dp)
                    .clickable(onClick = { openUriMime(attachment.link) }),
                colors = CardColors(
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer
                ),

                ) {
                var cardIcon: ImageVector
                if (attachment.isFile) {
                    cardIcon = calendar_month
                } else {
                    cardIcon = link_2
                }
                Image(
                    cardIcon, "link",
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.CenterHorizontally)
                        .height(50.dp).width(50.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary)
                )
            }
            Text(
                attachment.name,
                modifier = Modifier.padding(
                    start = 5.dp,
                    top = 10.dp,
                    bottom = 5.dp,
                    end = 10.dp
                ),
                onTextLayout = { textLayoutResult ->
                    if (!textLayoutResult.hasVisualOverflow) {
                        attachmentExpanded = true
                    }
                })
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun HomeworkContentPreview() {
    HomeworkContent(
        Homework(
            title = "Modal Jazz Improvisation",
            complete = true,
            teacher = "Mr. Teacher",
            subject = "Music",
            completionTime = "5 hours",
            body = htmlToAnnotatedString(
                "\n\n\n\n\n\n<p><b>TASK 1&nbsp; (2 hrs)</b></p>\n<p>Gain confidence improvising over two famous Modal Jazz\ncompositions by Miles Davis, 'So What' and 'Milestones'&nbsp;</p>\n<p>- Spend time playing and internalising the scales/ modes needed\nto improvise over the chords of each song</p>\n<p>- Spend time playing the scales/ chord tones over the chords\nchanges of the songs and getting a feel for the harmonic\nprogression of the song.&nbsp;</p>\n<p>- Spend time exploring and playing different swung\nrhythms&nbsp;</p>\n<p>- Spend time improvising and developing interesting ideas.</p>\n<p><b>BACKING TRACKS</b></p>\n<p><a href=\n\"https://www.youtube.com/watch?v=FSGWj22wV0U&amp;list=RDFSGWj22wV0U&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=FSGWj22wV0U&amp;list=RDFSGWj22wV0U&amp;start_radio=1</a></p>\n<p><a href=\n\"https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1</a></p>\n<p><br></p>\n<p><b>TASK 2 (2hrs)&nbsp;</b></p>\n<p>Start putting together a Powerpoint for Task 1 (b). Create two\nslides</p>\n<p>SLIDE 1 - Outline in detail the technical and musical\nrequirements needed to improvise in modal jazz. (Discuss everything\nincluding modes, chords scale relationships, chord changes in modal\njazz,&nbsp; rhythmic feel and articulation, phrasing, developing\nideas etc)&nbsp;</p>\n<p>SLIDE 2 - Reflect on/ analyse your ability and skills and set\nsome achievable aims for your improvising. Make sure you go into\ndetail and talk about technical specifics relating to your\ninstrument.&nbsp;</p>\n<p><br></p>\n<p><b>POWERPOINT</b> from class</p>\n<p><a href=\n\"https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1\"\ntarget=\n\"_blank\">https://www.youtube.com/watch?v=vk01tpTI3Ig&amp;list=RDvk01tpTI3Ig&amp;start_radio=1</a></p>\n<p><br></p>\n<p><br></p>\n\n",
                style = HtmlStyle(TextLinkStyles(
                    SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            )),
            issueDate = LocalDate.parse("2025-04-17"),
            dueDate = LocalDate.parse("2025-12-20")
        )
    )
}