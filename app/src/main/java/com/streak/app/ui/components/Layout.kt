package com.streak.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.StreakTheme

// Glava podzaslona: X/nazaj in naslov (kot "Zacni novo navado")
@Composable
fun ScreenTopBar(
    title: String,
    navIcon: ImageVector,
    navDescription: String,
    onNav: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
    ) {
        RoundIconButton(navIcon, navDescription, onNav, container = Color.Transparent)
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = StreakTheme.palette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 4.dp).semantics { heading() }
        )
        actions()
    }
}

// Velik naslov glavnega zaslona ("Statistika", "Naloge")
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier.fillMaxWidth()) {
        Text(
            text,
            style = MaterialTheme.typography.displaySmall,
            color = StreakTheme.palette.ink,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        trailing()
    }
}

// Pastelna ploscica s stevilko in oznako
@Composable
fun KpiTile(value: String, label: String, color: Color, modifier: Modifier = Modifier, big: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Text(value, style = if (big) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall, color = InkOnPastel, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = InkOnPastel)
    }
}

// Skupina nastavitev (siva kartica z vrsticami)
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(StreakTheme.palette.surfaceSoft)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        content = content
    )
}

@Composable
fun SettingsRow(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit,
    value: String? = null,
    showDivider: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    val palette = StreakTheme.palette
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onClick)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(iconBg)
            ) { Icon(icon, contentDescription = null, tint = InkOnPastel, modifier = Modifier.size(18.dp)) }
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.ink, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            if (trailing != null) trailing() else {
                if (value != null) Text(value, style = MaterialTheme.typography.labelLarge, color = palette.inkSoft)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = palette.inkSoft)
            }
        }
        if (showDivider) HorizontalDivider(color = palette.border)
    }
}

enum class BadgeKind { Ring, Progress, Done, Play }

// Znacka v kotu kartice: prazen krog, krog napredka, crna kljukica ali gumb za sejo
@Composable
fun StatusBadge(kind: BadgeKind, ink: Color, modifier: Modifier = Modifier, progress: Float = 0f, size: Dp = 28.dp) {
    val palette = StreakTheme.palette
    when (kind) {
        BadgeKind.Ring -> Box(modifier.size(size - 2.dp).border(2.5.dp, ink, CircleShape))
        BadgeKind.Progress -> Canvas(modifier.size(size)) {
            val w = 3.dp.toPx()
            val inset = w / 2
            drawArc(ink.copy(alpha = .25f), 0f, 360f, false, Offset(inset, inset), Size(this.size.width - w, this.size.height - w), style = Stroke(w))
            drawArc(ink, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), Size(this.size.width - w, this.size.height - w), style = Stroke(w, cap = StrokeCap.Round))
        }
        BadgeKind.Done -> Box(modifier.size(size - 2.dp).clip(CircleShape).background(palette.ink), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(17.dp))
        }
        BadgeKind.Play -> Box(modifier.size(size + 2.dp).clip(CircleShape).background(InkOnPastel), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

// Crn trak napredka
@Composable
fun ProgressTrack(progress: Float, modifier: Modifier = Modifier, color: Color = StreakTheme.palette.ink, track: Color = StreakTheme.palette.surfaceSoft) {
    Box(modifier.heightIn(min = 8.dp).clip(RoundedCornerShape(4.dp)).background(track)) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .heightIn(min = 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
    }
}

// Ploscica z barvnim ozadjem in ikono (npr. v glavi lista)
@Composable
fun IconTile(icon: ImageVector, background: Color, modifier: Modifier = Modifier, size: Dp = 52.dp, tint: Color = InkOnPastel) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(background)
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

// Razsiri element cez vodoravni odmik starsa (npr. trak dni do robov zaslona)
fun Modifier.bleed(horizontal: Dp): Modifier = this.layout { measurable, constraints ->
    val extra = (horizontal * 2).roundToPx()
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra)
    )
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}
