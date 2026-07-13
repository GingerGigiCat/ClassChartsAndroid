package com.gigi.cca.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gigi.cca.shared.ui.icons.calendar_month
import com.gigi.cca.shared.ui.theme.ClassChartsAndroidTheme
import org.jetbrains.compose.resources.painterResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.invoke

@Composable
fun DateDivider(date: LocalDate, modifier: Modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
    Row(modifier = modifier) {
        val today = LocalDate.now()
        var dateText = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))
        if (today.minusDays(1) == date) {
            dateText = "Yesterday"
        } else if (today == date) {
            dateText = "Today"
        } else if (today.plusDays(1) == date) {
            dateText = "Tomorrow"
        }
        Text(
            dateText,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium
        )

    }
}


@Composable
fun TodayMarker() {
    ClassChartsAndroidTheme {
        Card(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            colors = CardColors(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                "Today",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 10.dp)
            )
        }
    }
}

@Composable
fun DatePickerButton(getLocalDate: () -> LocalDate, onClick: () -> Unit, label: String = "Date of birth", modifier: Modifier = Modifier.Companion) {
    OutlinedTextField(
        getLocalDate()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
        onValueChange = {},
        modifier
            .padding(start = 15.dp, end = 15.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        label = { Text(label) },
        enabled = false,
        colors = TextFieldDefaults.colors(
            disabledTextColor = LocalContentColor.current,
            disabledContainerColor = Color.Transparent,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledPlaceholderColor = LocalContentColor.current,
            disabledIndicatorColor = MaterialTheme.colorScheme.outline,
            disabledSupportingTextColor = MaterialTheme.colorScheme.outline,
            disabledPrefixColor = MaterialTheme.colorScheme.outline
        ),
        leadingIcon = {
            Image(
                calendar_month,
                contentDescription = "Calendar icon",
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(25.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary)
            )
        }
    )
}

@Composable
fun DoDatePicker(state: DatePickerState = rememberDatePickerState(), disableDatePicker: (DatePickerState) -> Unit) {
    var editableState by remember { mutableStateOf(state) }
    DatePickerDialog(
        onDismissRequest = { disableDatePicker(editableState) },
        confirmButton = { TextButton({ disableDatePicker(editableState) }) { Text("OK") } },
        dismissButton = { TextButton({ disableDatePicker(state) }) { Text("Cancel") } },
        modifier = Modifier.verticalScroll(rememberScrollState())
    )
    {
        DatePicker(editableState)
    }
}