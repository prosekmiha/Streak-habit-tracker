package com.streak.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.ui.theme.StreakTheme
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

data class TimePreset(val time: LocalTime, val label: String)

// Izbira ure: dve kolesci (ure, minute po 5) in hitre predloge
@Composable
fun TimePickerSheet(
    title: String,
    initial: LocalTime,
    presets: List<TimePreset>,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var hour by remember { mutableIntStateOf(initial.hour) }
    var minuteIndex by remember { mutableIntStateOf((initial.minute / 5).coerceIn(0, 11)) }
    var version by remember { mutableIntStateOf(0) } // sprozi ponovno pozicioniranje kolesc
    val palette = StreakTheme.palette
    val hourLabel = stringResource(R.string.picker_hour)
    val minuteLabel = stringResource(R.string.picker_minute)

    StreakSheet(onDismiss = onDismiss, title = title) {
        Box(Modifier.fillMaxWidth().height(WheelHeight), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(WheelItem)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surfaceSoft)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Wheel(
                    values = (0..23).map { "%02d".format(it) },
                    selected = hour,
                    resetKey = version,
                    label = hourLabel,
                    onSelected = { hour = it }
                )
                Text(":", style = MaterialTheme.typography.displayMedium, color = palette.ink, modifier = Modifier.padding(horizontal = 6.dp))
                Wheel(
                    values = (0..11).map { "%02d".format(it * 5) },
                    selected = minuteIndex,
                    resetKey = version,
                    label = minuteLabel,
                    onSelected = { minuteIndex = it }
                )
            }
        }
        if (presets.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
                presets.forEach { p ->
                    val isSel = p.time.hour == hour && p.time.minute == minuteIndex * 5
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) palette.daySelected else palette.surfaceSoft)
                            .clickable(role = Role.Button) {
                                hour = p.time.hour
                                minuteIndex = (p.time.minute / 5).coerceIn(0, 11)
                                version++
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(p.time.toString(), style = MaterialTheme.typography.titleSmall, color = if (isSel) palette.daySelectedInk else palette.ink)
                        if (p.label.isNotBlank()) {
                            Text(p.label, style = MaterialTheme.typography.labelSmall, color = if (isSel) palette.daySelectedInk.copy(alpha = .7f) else palette.inkSoft)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 22.dp)) {
            StreakButton(stringResource(R.string.cancel), onDismiss, Modifier.weight(1f), ButtonVariant.Secondary)
            StreakButton(confirmText, { onConfirm(LocalTime.of(hour, minuteIndex * 5)) }, Modifier.weight(1f))
        }
    }
}

private val WheelItem = 52.dp
private val WheelHeight = 156.dp

@Composable
private fun Wheel(values: List<String>, selected: Int, resetKey: Int, label: String, onSelected: (Int) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    val scope = rememberCoroutineScope()
    val palette = StreakTheme.palette
    val itemPx = with(LocalDensity.current) { WheelItem.roundToPx() }
    // Zaradi zgornjega odmika (ena vrstica) je sredinski element prvi vidni + zaokrozitev
    val centered by remember(itemPx) {
        derivedStateOf { state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0 }
    }

    // Kolesce porabi ves preostali zamah: sicer ga prevzame list in se ob hitrem
    // drsanju navzdol zapre
    val keepScrollInside = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = available
            override suspend fun onPostFling(consumed: Velocity, available: Velocity) = available
        }
    }

    LaunchedEffect(resetKey) { if (resetKey > 0) state.animateScrollToItem(selected) }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to centered }.collect { (scrolling, index) ->
            if (!scrolling) onSelected(index.coerceIn(0, values.lastIndex))
        }
    }

    val increaseLabel = stringResource(R.string.increase)
    val decreaseLabel = stringResource(R.string.decrease)
    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        contentPadding = PaddingValues(vertical = WheelItem),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .nestedScroll(keepScrollInside)
            .width(96.dp)
            .height(WheelHeight)
            .semantics {
                contentDescription = label
                stateDescription = values[centered.coerceIn(0, values.lastIndex)]
                customActions = listOf(
                    CustomAccessibilityAction(increaseLabel) { scope.launch { state.animateScrollToItem((centered + 1).coerceAtMost(values.lastIndex)) }; true },
                    CustomAccessibilityAction(decreaseLabel) { scope.launch { state.animateScrollToItem((centered - 1).coerceAtLeast(0)) }; true }
                )
            }
    ) {
        itemsIndexed(values) { index, v ->
            val isCenter = index == centered
            Box(Modifier.height(WheelItem), contentAlignment = Alignment.Center) {
                Text(
                    v,
                    style = if (isCenter) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineMedium,
                    color = if (isCenter) palette.ink else palette.inkSoft.copy(alpha = .45f)
                )
            }
        }
    }
}

data class DatePreset(val date: LocalDate, val label: String)

// Izbira datuma: hitre izbire in mesecna mreza (crn krog = izbrano, obroba = danes)
@Composable
fun DatePickerSheet(
    title: String,
    initial: LocalDate,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    presets: List<DatePreset> = emptyList(),
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    extraAction: Pair<String, () -> Unit>? = null
) {
    var selected by remember { mutableStateOf(initial) }
    var month by remember { mutableStateOf(YearMonth.from(initial)) }
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]

    StreakSheet(onDismiss = onDismiss, title = title) {
        if (presets.isNotEmpty() || extraAction != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                presets.forEach { p ->
                    ChoiceChip(p.label, selected == p.date, { selected = p.date; month = YearMonth.from(p.date) })
                }
                extraAction?.let { (label, action) -> ChoiceChip(label, false, { action(); onDismiss() }) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous_month), { month = month.minusMonths(1) })
            Text(
                "${month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }} ${month.year}",
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            RoundIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next_month), { month = month.plusMonths(1) })
        }
        MonthGrid(
            month = month,
            firstDayOfWeek = firstDayOfWeek,
            modifier = Modifier.padding(top = 8.dp)
        ) { date ->
            val enabled = (minDate == null || !date.isBefore(minDate)) && (maxDate == null || !date.isAfter(maxDate))
            val isSel = date == selected
            val desc = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isSel) palette.daySelected else androidx.compose.ui.graphics.Color.Transparent)
                    .then(if (date == today && !isSel) Modifier.border(2.dp, palette.ink, CircleShape) else Modifier)
                    .clickable(enabled = enabled, role = Role.Button) { selected = date }
                    .semantics { this.selected = isSel; contentDescription = desc }
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = when {
                        isSel -> palette.daySelectedInk
                        !enabled -> palette.inkSoft.copy(alpha = .4f)
                        else -> palette.ink
                    }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 16.dp)) {
            StreakButton(stringResource(R.string.cancel), onDismiss, Modifier.weight(1f), ButtonVariant.Secondary)
            StreakButton(stringResource(R.string.pick), { onConfirm(selected) }, Modifier.weight(1f))
        }
    }
}

// Mreza meseca z glavo dni v tednu; vsebino celice nariše klicatelj
@Composable
fun MonthGrid(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    modifier: Modifier = Modifier,
    cellSpacing: androidx.compose.ui.unit.Dp = 2.dp,
    cell: @Composable (LocalDate) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val palette = StreakTheme.palette
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            (0L..6L).map { firstDayOfWeek.plus(it) }.forEach { d ->
                Text(
                    shortDayName(d, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                )
            }
        }
        val first = month.atDay(1)
        val leading = ((first.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
        val cells = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = cellSpacing)) {
                (week + List(7 - week.size) { null }).forEach { date ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (date != null) cell(date)
                    }
                }
            }
        }
    }
}

// Kratko ime dneva brez pike ("pon." -> "Pon"); najvec 4 znaki, da se prilega
// tedenskemu traku tudi v jezikih z dolgimi okrajsavami (npr. latviscina "pirmd.")
fun shortDayName(day: DayOfWeek, locale: java.util.Locale): String =
    day.getDisplayName(TextStyle.SHORT_STANDALONE, locale).trimEnd('.').take(4).replaceFirstChar { it.titlecase(locale) }

// Začetek tedna (za hitre izbire, npr. "ponedeljek")
fun nextWeekStart(today: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate =
    today.with(TemporalAdjusters.next(firstDayOfWeek))
