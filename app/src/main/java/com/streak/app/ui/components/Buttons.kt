package com.streak.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.ui.theme.Blurple
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.StreakTheme

val PillShape = RoundedCornerShape(20.dp)

enum class ButtonVariant { Primary, White, Secondary, Dark, Danger }

// Gumb po dizajnu: visok 56, zaobljen 20, brez sence in Material valovanja barv.
@Composable
fun StreakButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 56.dp
) {
    val palette = StreakTheme.palette
    val (baseBg, baseFg) = when (variant) {
        ButtonVariant.Primary -> MaterialTheme.colorScheme.primary to Color.White
        ButtonVariant.White -> Color.White to InkOnPastel
        ButtonVariant.Secondary -> palette.surfaceSoft to palette.ink
        ButtonVariant.Dark -> palette.ink to MaterialTheme.colorScheme.background
        ButtonVariant.Danger -> palette.dangerSoft to palette.danger
    }
    // Onemogoceno stanje z barvo, ne z alpha slojem: preklop sloja ob omogocitvi je v
    // listih (ModalBottomSheet) pustil gumb brez ozadja do naslednjega izrisa
    val fg = if (enabled) baseFg else baseFg.copy(alpha = baseFg.alpha * 0.55f)
    val bg = if (enabled) baseBg else baseBg.copy(alpha = baseBg.alpha * 0.55f)
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(PillShape)
            .background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// Besedilni gumb (npr. "Preskoci", "Ne zdaj")
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, underline: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            textDecoration = if (underline) androidx.compose.ui.text.style.TextDecoration.Underline else null
        )
    }
}

// Okrogel gumb z ikono (44 dp, sivo ozadje)
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = StreakTheme.palette.surfaceSoft,
    tint: Color = StreakTheme.palette.ink,
    size: Dp = 44.dp
) {
    // Dotikalna tarca vsaj 48 dp (pregledovalniki dostopnosti merijo meje elementa),
    // viden krog pa ostane velik kot v dizajnu
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.coerceAtLeast(48.dp))
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size.coerceAtLeast(44.dp)).clip(CircleShape).background(container)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

// Segmentni izbirnik: sivo ozadje, izbrani segment crn
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    container: Color = StreakTheme.palette.surfaceSoft,
    height: Dp = 44.dp
) {
    val palette = StreakTheme.palette
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .padding(5.dp)
            .selectableGroup()
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = height)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) palette.daySelected else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(option) }
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) palette.daySelectedInk else palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Cip (kategorija, prioriteta ...). Izbran = crn ali obroba pri barvnih cipih.
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    val palette = StreakTheme.palette
    val shape = RoundedCornerShape(14.dp)
    val bg = color ?: if (selected) palette.daySelected else palette.surfaceSoft
    val fg = if (color != null) InkOnPastel else if (selected) palette.daySelectedInk else palette.ink
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(bg)
            .then(if (color != null && selected) Modifier.border(3.dp, palette.ink, shape) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.selectableGroup()
    ) {
        options.forEach { option -> ChoiceChip(label(option), option == selected, { onSelect(option) }) }
    }
}

// Stevec s crnima okroglima gumboma
@Composable
fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    container: Color = StreakTheme.palette.surfaceSoft,
    ink: Color = StreakTheme.palette.ink,
    buttonSize: Dp = 48.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(container)
            .padding(8.dp)
    ) {
        StepButton(Icons.Rounded.Remove, stringResource(R.string.decrease), onMinus, buttonSize)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = ink)
            if (caption != null) Text(caption, style = MaterialTheme.typography.labelMedium, color = StreakTheme.palette.inkSoft)
        }
        StepButton(Icons.Rounded.Add, stringResource(R.string.increase), onPlus, buttonSize)
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, onClick: () -> Unit, size: Dp) {
    val palette = StreakTheme.palette
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(palette.ink)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(24.dp))
    }
}

// Stikalo po dizajnu
@Composable
fun StreakSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    val knobOffset by animateDpAsState(if (checked) 26.dp else 4.dp, label = "knob")
    Box(
        modifier = modifier
            .size(width = 56.dp, height = 34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (checked) Blurple else StreakTheme.palette.border)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Box(
            Modifier
                .padding(start = knobOffset, top = 4.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// Indikator korakov (onboarding)
@Composable
fun StepIndicator(step: Int, total: Int, description: String, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i < step) MaterialTheme.colorScheme.primary else StreakTheme.palette.border)
            )
        }
    }
}
