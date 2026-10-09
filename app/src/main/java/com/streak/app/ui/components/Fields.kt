package com.streak.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.streak.app.ui.theme.StreakTheme

val FieldShape = RoundedCornerShape(16.dp)

// Oznaka nad poljem ("Ime", "Opis", "Intervali" ...)
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = StreakTheme.palette.inkSoft,
        modifier = modifier.padding(top = 18.dp, bottom = 8.dp).semantics { heading() }
    )
}

// Vnosno polje po dizajnu: obrobljeno (primarna obroba ob fokusu) ali polnjeno sivo.
@Composable
fun StreakTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    filled: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false
) {
    val palette = StreakTheme.palette
    Column(modifier) {
        if (label != null) FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 2,
            isError = isError,
            placeholder = placeholder?.let { { Text(it, color = palette.inkSoft) } },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = FieldShape,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (filled) Color.Transparent else palette.border,
                focusedContainerColor = if (filled) palette.surfaceSoft else Color.Transparent,
                unfocusedContainerColor = if (filled) palette.surfaceSoft else Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
