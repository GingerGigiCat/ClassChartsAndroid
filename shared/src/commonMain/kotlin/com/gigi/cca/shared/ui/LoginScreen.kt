package com.gigi.cca.shared.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.gigi.cca.shared.data.DEMO_CODE
import com.gigi.cca.shared.data.Repository
import com.gigi.cca.shared.parseDate
import com.gigi.cca.shared.ui.icons.calendar_month
import com.gigi.cca.shared.ukFormat
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Composable
fun LoginScreen(repository: Repository) {
    var code by rememberSaveable { mutableStateOf("") }
    var dobText by rememberSaveable { mutableStateOf("") }
    val dob = parseDate(dobText)
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    val message by repository.loginMessage.collectAsState()
    val scope = rememberCoroutineScope()
    val isDemo = code.equals(DEMO_CODE, ignoreCase = true)
    val canSubmit = !loading && code.isNotBlank() && (dob != null || isDemo)

    val submit = {
        if (canSubmit) {
            loading = true
            error = null
            scope.launch {
                error = repository.login(code, dob)
                loading = false
            }
        }
    }

    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().dragToScroll(scrollState).verticalScroll(scrollState).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("ClassCharts", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Text(
                "Log in with your ClassCharts code and date of birth.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.filterNot(Char::isWhitespace).uppercase() },
                label = { Text("ClassCharts code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            Box {
                OutlinedTextField(
                    value = dob?.ukFormat() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date of birth") },
                    placeholder = { Text("DD/MM/YYYY") },
                    leadingIcon = { Icon(calendar_month, null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                // Read-only text fields swallow clicks, so catch them on top.
                Box(Modifier.matchParentSize().clickable { showPicker = true })
            }
            (error ?: message)?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { submit() }, enabled = canSubmit, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Log in")
                }
            }
            Text(
                "Just looking? Log in with the code DEMO.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showPicker) {
        DatePickerDialogFor(
            initial = dob ?: LocalDate(2010, 1, 1),
            inputMode = true,
            onDismiss = { showPicker = false },
            onPicked = { dobText = it.toString() },
        )
    }
}
