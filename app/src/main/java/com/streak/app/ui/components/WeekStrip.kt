package com.streak.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.StreakTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class WeekDayStatus { ALL_DONE, SOME_DONE, NONE, FUTURE }

data class WeekDayUi(val date: LocalDate, val status: WeekDayStatus, val isToday: Boolean)

private val LimeSoft = Color(0xFFDCF5B6)

// Vodoravni trak dni: opravljeni zeleni, delno svetlo zeleni, izbran crn, ostali sivi.
@Composable
fun WeekStrip(
    days: List<WeekDayUi>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val locale = LocalConfiguration.current.locales[0]
    val palette = StreakTheme.palette
    val selectedIndex = days.indexOfFirst { it.date == selected }
    LaunchedEffect(days.size) {
        if (selectedIndex > 3) listState.scrollToItem(selectedIndex - 3)
    }
    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days, key = { it.date.toEpochDay() }) { day ->
            val isSelected = day.date == selected
            val shape = RoundedCornerShape(18.dp)
            val bg = when {
                isSelected -> palette.daySelected
                day.status == WeekDayStatus.ALL_DONE -> palette.dayDone
                day.status == WeekDayStatus.SOME_DONE -> LimeSoft
                else -> palette.surfaceSoft
            }
            val fg = when {
                isSelected -> palette.daySelectedInk
                day.status == WeekDayStatus.ALL_DONE || day.status == WeekDayStatus.SOME_DONE -> InkOnPastel
                day.status == WeekDayStatus.FUTURE -> palette.inkSoft
                else -> palette.ink
            }
            val stateLabel = stringResource(
                when (day.status) {
                    WeekDayStatus.ALL_DONE -> R.string.week_day_all_done
                    WeekDayStatus.SOME_DONE -> R.string.week_day_some_done
                    WeekDayStatus.NONE -> R.string.week_day_none
                    WeekDayStatus.FUTURE -> R.string.week_day_future
                }
            )
            val fullDate = day.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    // Najmanjsa velikost: pri veliki pisavi dan zraste namesto da bi se besedilo odrezalo
                    .widthIn(min = 56.dp)
                    .heightIn(min = 66.dp)
                    .clip(shape)
                    .background(bg)
                    .then(if (day.isToday && !isSelected) Modifier.border(2.dp, palette.ink, shape) else Modifier)
                    .selectable(selected = isSelected, role = Role.Tab) { onSelect(day.date) }
                    .semantics(mergeDescendants = true) { contentDescription = "$fullDate, $stateLabel" }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, color = fg)
                Text(
                    shortDayName(day.date.dayOfWeek, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}
