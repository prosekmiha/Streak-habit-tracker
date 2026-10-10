package com.streak.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.streak.app.R
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.MascotView
import com.streak.app.ui.theme.StreakTheme

val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

// Spodnji list po dizajnu: bel, radij 32, rocaj, zatemnjeno ozadje; neobvezen naslov z X
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    showClose: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = StreakTheme.palette
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = SheetShape,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = palette.scrim,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 14.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.border)
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.ink,
                        modifier = Modifier.weight(1f).semantics { heading() }
                    )
                    if (showClose) RoundIconButton(Icons.Rounded.Close, stringResource(R.string.close), onDismiss)
                }
            }
            content()
        }
    }
}

// Izbira ene moznosti v listu (urnik, prvi dan tedna, animacije ...)
@Composable
fun <T> OptionSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    description: (@Composable (T) -> String?)? = null
) {
    val palette = StreakTheme.palette
    StreakSheet(onDismiss = onDismiss, title = title) {
        Column(Modifier.selectableGroup()) {
            options.forEach { option ->
                val isSelected = option == selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) palette.daySelected else palette.surfaceSoft)
                        .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(option); onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(label(option), style = MaterialTheme.typography.titleSmall, color = if (isSelected) palette.daySelectedInk else palette.ink)
                        description?.invoke(option)?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = if (isSelected) palette.daySelectedInk.copy(alpha = .75f) else palette.inkSoft)
                        }
                    }
                    if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, tint = palette.daySelectedInk)
                }
            }
        }
    }
}

// Dialog po dizajnu: kartica z radijem 32, neobvezna maskota nad naslovom, dejanja v stolpcu
@Composable
fun StreakDialog(
    onDismiss: () -> Unit,
    title: String,
    text: String,
    mascot: MascotMood? = null,
    actions: @Composable ColumnScope.() -> Unit
) {
    val palette = StreakTheme.palette
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.padding(horizontal = 20.dp).padding(top = if (mascot != null) 60.dp else 0.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 22.dp, end = 22.dp, bottom = 20.dp, top = if (mascot != null) 76.dp else 24.dp)
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = palette.ink, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                Text(
                    text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)
                )
                actions()
            }
            if (mascot != null) {
                MascotView(
                    mascot,
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-60).dp)
                        .size(width = 124.dp, height = 134.dp)
                )
            }
        }
    }
}

// Vrstica dejanja v dialogu (polna sirina z razmikom)
@Composable
fun ColumnScope.DialogAction(text: String, variant: ButtonVariant, onClick: () -> Unit) {
    StreakButton(text, onClick, variant = variant, height = 54.dp)
    Spacer(Modifier.height(8.dp))
}

@Composable
fun SheetActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color = StreakTheme.palette.ink) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = tint)
    }
}
