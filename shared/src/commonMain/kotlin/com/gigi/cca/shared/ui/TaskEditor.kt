package com.gigi.cca.shared.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.gigi.cca.shared.model.Homework
import com.gigi.cca.shared.parseDate
import com.gigi.cca.shared.plusDays
import com.gigi.cca.shared.relativeLabel
import com.gigi.cca.shared.today
import com.gigi.cca.shared.ui.icons.calendar_month
import kotlinx.datetime.LocalDate

/** Add or edit one of the user's own tasks. [existing] is null when adding a new one. */
@Composable
fun TaskEditorDialog(
    existing: Homework?,
    onDismiss: () -> Unit,
    onSave: (title: String, subject: String, due: LocalDate, notes: String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var subject by rememberSaveable { mutableStateOf(existing?.subject ?: "") }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var dueText by rememberSaveable { mutableStateOf(existing?.dueDate ?: today().plusDays(1).toString()) }
    var showPicker by remember { mutableStateOf(false) }
    val due = parseDate(dueText) ?: today()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New task" else "Edit task") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )
                Box {
                    OutlinedTextField(
                        value = due.relativeLabel(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Due") },
                        leadingIcon = { Icon(calendar_month, null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // Read-only text fields swallow clicks, so catch them on top.
                    Box(Modifier.matchParentSize().clickable { showPicker = true })
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, subject, due, notes) }, enabled = title.isNotBlank()) {
                Text(if (existing == null) "Add" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (showPicker) {
        DatePickerDialogFor(
            initial = due,
            inputMode = false,
            onDismiss = { showPicker = false },
            onPicked = { dueText = it.toString() },
        )
    }
}
