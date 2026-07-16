package com.gigi.cca.shared.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.gigi.cca.shared.ErrorInvalidLogin
import com.gigi.cca.shared.ErrorType
import com.gigi.cca.shared.RequestMaker
import com.gigi.cca.shared.ui.theme.ClassChartsAndroidTheme
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

@Preview(showBackground = true, showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun LogInScreenPreview() {
    ClassChartsAndroidTheme {
        Scaffold(Modifier.fillMaxSize()) { innerPadding ->
            LogInScreen(Modifier.padding(innerPadding), RequestMaker())
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogInScreen(modifier:Modifier = Modifier, requestMaker: RequestMaker, navigate: () -> Unit = {}): ErrorType {
    var dateState = rememberDatePickerState(initialDisplayMode = DisplayMode.Input)
    var showDatePicker by remember { mutableStateOf(false) }
    var studentId by remember { mutableStateOf(requestMaker.studentId?: "") }
    val getLocalDateObjectForSelected = { LocalDate.ofEpochDay(dateState.selectedDateMillis!!.toLong() / (24 * 60 * 60 * 1000)) }
    var goToHomeworkList by remember { mutableStateOf(false) }
    var loginResponse: ErrorType = ErrorInvalidLogin()
    var doReturn by remember { mutableStateOf(true) }
    if (dateState.selectedDateMillis == null) dateState.selectedDateMillis = 1230768000000

    Column(modifier.padding(10.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(30.dp))
        Text(
            "Login",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(15.dp)
        )
        Text(
            "Login with classcharts code and DoB",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 18.dp)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            studentId,
            onValueChange = {studentId = it},
            Modifier.padding(start = 15.dp, end = 15.dp).fillMaxWidth(),
            label = { Text("Classcharts code") })
        Spacer(Modifier.height(10.dp))
        DatePickerButton(getLocalDateObjectForSelected, { showDatePicker = true })
        if (showDatePicker) DoDatePicker(dateState, { theState ->
            dateState = theState
            showDatePicker = false
        })
        Spacer(Modifier.height(15.dp))
        Row {
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    runBlocking {
                        loginResponse = requestMaker.login(
                            code = studentId,
                            dob = getLocalDateObjectForSelected().toString()
                        )
                    }
                    Logger.d("RealLoginResponse") {loginResponse.toString()}
                    navigate()
                    doReturn = false
                    doReturn = true
                }, content = { Text("Login") }, modifier = Modifier
                .padding(end = 15.dp), colors = ButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.inversePrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
    if (doReturn) return loginResponse
    else return loginResponse
}