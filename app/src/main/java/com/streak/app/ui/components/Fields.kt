package com.streak.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.streak.app.ui.theme.StreakTheme

val FieldShape = RoundedCornerShape(18.dp)

// Oznaka nad poljem ("Ime", "Opis", "Intervali" ...)
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
        color = StreakTheme.palette.inkSoft,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp).semantics { heading() }
    )
}

// Vnosno polje: obrobljeno (primarna obroba ob fokusu) ali polnjeno sivo.
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
            placeholder = placeholder?.let { { Text(it, color = palette.inkSoft, style = MaterialTheme.typography.bodyLarge) } },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = FieldShape,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (filled) Color.Transparent else palette.border,
                focusedContainerColor = if (filled) palette.surfaceSoft else Color.Transparent,
                unfocusedContainerColor = if (filled) palette.surfaceSoft else Color.Transparent,
                errorBorderColor = palette.danger,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = palette.ink,
                unfocusedTextColor = palette.ink
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        )
    }
}

// Sivo polje z vrednostjo in puscico - odpre izbiro (sheet)
@Composable
fun SelectField(value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = StreakTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(FieldShape)
            .background(palette.surfaceSoft)
            .clickable(role = Role.DropdownList, onClick = onClick)
            .padding(horizontal = 18.dp)
    ) {
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = palette.ink, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = palette.ink)
    }
}

// Polje z ikono, majhno oznako in vrednostjo (datum, ura)
@Composable
fun InfoField(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    val palette = StreakTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(FieldShape)
            .background(palette.surfaceSoft)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = palette.ink, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f).padding(start = 10.dp, top = 8.dp, bottom = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = palette.inkSoft)
            Text(value, style = MaterialTheme.typography.titleSmall, color = palette.ink)
        }
        trailing?.invoke()
    }
}

// Crtkan gumb "Dodaj"
@Composable
fun DashedAddButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val border = StreakTheme.palette.border
    val primary = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRoundRect(
                    color = border,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = primary, modifier = Modifier.padding(start = 6.dp))
    }
}
