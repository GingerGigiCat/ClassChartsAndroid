package com.gigi.classchartsandroid.wear

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.gigi.cca.shared.data.AuthState
import com.gigi.cca.shared.data.DEMO_CODE
import com.gigi.cca.shared.data.Repository
import com.gigi.cca.shared.parseDate
import com.gigi.cca.shared.ukFormat
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate

@Composable
fun WearApp(repository: Repository) {
    MaterialTheme {
        AppScaffold {
            val auth by repository.auth.collectAsState()
            when (auth) {
                AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                AuthState.LoggedOut -> LoginFlow(repository)
                is AuthState.LoggedIn -> MainFlow(repository)
            }
        }
    }
}

/** Swipe left/right between homework, timetable and behaviour; swipe right on a detail to go back. */
@Composable
private fun MainFlow(repository: Repository) {
    val navController = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(navController, startDestination = "pages") {
        composable("pages") {
            WearPages(repository, onOpenHomework = { navController.navigate("homework/$it") })
        }
        composable("homework/{id}") { entry ->
            HomeworkDetail(repository, entry.arguments?.getString("id")?.toIntOrNull())
        }
    }
}

@Composable
private fun LoginFlow(repository: Repository) {
    val navController = rememberSwipeDismissableNavController()
    var dobText by rememberSaveable { mutableStateOf("") }
    SwipeDismissableNavHost(navController, startDestination = "login") {
        composable("login") {
            LoginScreen(repository, dob = parseDate(dobText), onPickDob = { navController.navigate("dob") })
        }
        composable("dob") {
            DatePicker(
                initialDate = (parseDate(dobText) ?: LocalDate(2010, 1, 1)).toJavaLocalDate(),
                onDatePicked = {
                    dobText = it.toKotlinLocalDate().toString()
                    navController.popBackStack()
                },
                maxValidDate = java.time.LocalDate.now(),
            )
        }
    }
}

@Composable
private fun LoginScreen(repository: Repository, dob: LocalDate?, onPickDob: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val message by repository.loginMessage.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    fun logIn(withCode: String) {
        loading = true
        error = null
        scope.launch {
            error = repository.login(withCode, dob)
            loading = false
        }
    }

    ScreenScaffold(
        scrollState = listState,
        scrollIndicator = { ScrollIndicator(listState) },
    ) { padding ->
        ScalingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader { Text("ClassCharts") } }
            item {
                // Tapping this opens the watch keyboard.
                BasicTextField(
                    value = code,
                    onValueChange = { code = it.filterNot(Char::isWhitespace).uppercase() },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    decorationBox = { field ->
                        Box(contentAlignment = Alignment.Center) {
                            if (code.isEmpty()) {
                                Text("Code", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            field()
                        }
                    },
                )
            }
            item {
                Button(
                    onClick = onPickDob,
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(dob?.ukFormat() ?: "Date of birth") },
                )
            }
            (error ?: message)?.let { text ->
                item {
                    Text(
                        text,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item {
                Button(
                    onClick = { logIn(code) },
                    enabled = !loading && code.isNotBlank() && dob != null,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (loading) "Logging in…" else "Log in") },
                )
            }
            item {
                Button(
                    onClick = { logIn(DEMO_CODE) },
                    enabled = !loading,
                    colors = ButtonDefaults.outlinedButtonColors(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = !loading),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Try the demo") },
                )
            }
        }
    }
}
