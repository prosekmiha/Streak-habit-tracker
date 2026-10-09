package com.streak.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.StreakTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

enum class WeekDayStatus { ALL_DONE, SOME_DONE, NONE, FUTURE }

data class WeekDayUi(val date: LocalDate, val status: WeekDayStatus, val isToday: Boolean)

// Vodoravni trak dni: opravljeni dnevi zeleni, izbran dan crn, ostali sivi.
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
        if (selectedIndex > 2) listState.scrollToItem(selectedIndex - 2)
    }
    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days, key = { it.date.toEpochDay() }) { day ->
            val isSelected = day.date == selected
            val bg = when {
                isSelected -> palette.daySelected
                day.status == WeekDayStatus.ALL_DONE -> palette.dayDone
                day.status == WeekDayStatus.SOME_DONE -> palette.dayDone.copy(alpha = 0.45f)
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
                    .width(58.dp)
                    .height(64.dp)
                    .background(bg, RoundedCornerShape(18.dp))
                    .then(
                        if (day.isToday && !isSelected) Modifier.border(1.5.dp, palette.ink, RoundedCornerShape(18.dp))
                        else Modifier
                    )
                    .clickable(role = Role.Tab) { onSelect(day.date) }
                    .semantics(mergeDescendants = true) {
                        this.selected = isSelected
                        contentDescription = "$fullDate, $stateLabel"
                    }
                    .padding(horizontal = 2.dp)
            ) {
                Text(
                    day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = fg
                )
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) },
                    style = MaterialTheme.typography.labelMedium,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}
