package com.gigi.classchartsandroid

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun DoDatePicker(state: DatePickerState = rememberDatePickerState(), disableDatePicker: (DatePickerState) -> Unit) {
    var editableState by remember { mutableStateOf(state) }
    DatePickerDialog(onDismissRequest = { disableDatePicker(editableState) },
        confirmButton = {TextButton({ disableDatePicker(editableState) }) { Text("OK") } },
        dismissButton = {TextButton({ disableDatePicker(state) }) { Text("Cancel") }},
        modifier = Modifier.verticalScroll(rememberScrollState())
    )
    {
        DatePicker(editableState)
    }
}