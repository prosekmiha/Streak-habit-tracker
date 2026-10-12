package com.streak.app.ui.review

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.review.Chronotype
import com.streak.app.domain.review.Review
import com.streak.app.domain.review.ReviewPeriod
import com.streak.app.domain.review.ReviewRange
import com.streak.app.ui.achievements.ShareCard
import com.streak.app.ui.achievements.ShareCardSpec
import com.streak.app.ui.components.KpiTile
import com.streak.app.ui.components.RoundIconButton
import com.streak.app.ui.components.ScreenTopBar
import com.streak.app.ui.components.SegmentedControl
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.MascotView
import com.streak.app.ui.mood.MoodUi
import com.streak.app.ui.theme.Blurple
import com.streak.app.ui.theme.Lavender
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Mint
import com.streak.app.ui.theme.Peach
import com.streak.app.ui.theme.Sky
import com.streak.app.ui.theme.StreakTheme
import com.streak.app.ui.theme.Sun
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ReviewScreen(onBack: () -> Unit, viewModel: ReviewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    ReviewContent(
        state = state,
        onBack = onBack,
        onPeriod = viewModel::setPeriod,
        onPrev = viewModel::previous,
        onNext = viewModel::next,
        onShare = { review -> scope.launch { ShareCard.share(context, shareSpec(context, review)) } }
    )
}

// Pregled meseca/leta: kartice s povzetkom in deljenje kot slika
@Composable
fun ReviewContent(
    state: ReviewUiState,
    onBack: () -> Unit,
    onPeriod: (ReviewPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onShare: (Review) -> Unit
) {
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    val range = state.range
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScreenTopBar(stringResource(R.string.review_title), Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack)
        SegmentedControl(
            ReviewPeriod.entries,
            range?.period ?: ReviewPeriod.MONTH,
            { stringResource(if (it == ReviewPeriod.MONTH) R.string.review_month else R.string.review_year) },
            onPeriod
        )
        if (range != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                RoundIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.review_previous), onPrev)
                Text(
                    periodLabel(range, locale),
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                if (state.canGoNext) {
                    RoundIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.review_next), onNext)
                } else {
                    Box(Modifier.size(48.dp))
                }
            }
        }

        val review = state.review ?: return@Column
        if (review.isEmpty) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                MascotView(MascotMood.THINKING, Modifier.size(width = 150.dp, height = 162.dp))
                Text(stringResource(R.string.review_empty), style = MaterialTheme.typography.bodyLarge, color = palette.ink, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
            }
            return@Column
        }
        ReviewCards(review, locale)
        StreakButton(stringResource(R.string.review_share), { onShare(review) }, Modifier.padding(top = 8.dp), icon = Icons.Rounded.IosShare)
    }
}

@Composable
private fun ReviewCards(review: Review, locale: Locale) {
    // Glavna kartica
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Blurple)
            .padding(20.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Text(review.doneCount.toString(), style = MaterialTheme.typography.displayMedium, color = Color.White)
        Text(stringResource(R.string.review_done_label), style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(stringResource(R.string.review_active_days, review.activeDays), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .85f), modifier = Modifier.padding(top = 6.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KpiTile(review.perfectDays.toString(), stringResource(R.string.review_perfect_days), Lime, Modifier.weight(1f), big = true)
        KpiTile(review.longestPerfectRun.toString(), stringResource(R.string.review_longest_run), Sun, Modifier.weight(1f), big = true)
    }
    review.topHabit?.let { habit ->
        KpiTile(habit.title, stringResource(R.string.review_top_habit, review.topHabitDays), parseHexColor(habit.colorHex), Modifier.fillMaxWidth())
    }
    // Imena dni in dela dneva so lahko dolga (tudi pri vecji pisavi), zato cez celo sirino
    review.bestWeekday?.let {
        KpiTile(it.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { c -> c.titlecase(locale) }, stringResource(R.string.review_best_weekday), Sky, Modifier.fillMaxWidth())
    }
    review.chronotype?.let {
        KpiTile(stringResource(chronotypeLabel(it)), stringResource(R.string.review_chronotype), Lavender, Modifier.fillMaxWidth())
    }
    if (review.sessionCount > 0) {
        val minutes = (review.sessionSeconds / 60).toInt()
        KpiTile(stringResource(R.string.review_hours_minutes, minutes / 60, minutes % 60), stringResource(R.string.review_focus, review.sessionCount), Mint, Modifier.fillMaxWidth())
    }
    review.moodAverage?.let { avg ->
        KpiTile(String.format(locale, "%.1f / 5", avg), stringResource(R.string.review_mood, review.moodEntries), MoodUi.color(avg.roundToInt().coerceIn(MoodUi.levels)), Modifier.fillMaxWidth())
    }
    if (review.cleanDays > 0) {
        KpiTile(review.cleanDays.toString(), stringResource(R.string.review_clean_days), Peach, Modifier.fillMaxWidth())
    }
}

private fun chronotypeLabel(c: Chronotype) = when (c) {
    Chronotype.EARLY_BIRD -> R.string.review_chrono_early
    Chronotype.DAYTIME -> R.string.review_chrono_day
    Chronotype.NIGHT_OWL -> R.string.review_chrono_night
}

fun periodLabel(range: ReviewRange, locale: Locale): String = when (range.period) {
    ReviewPeriod.MONTH -> "${range.start.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }} ${range.start.year}"
    ReviewPeriod.YEAR -> range.start.year.toString()
}

// Slika za deljenje: le stevila in obdobje, brez imen navad
private fun shareSpec(context: Context, review: Review): ShareCardSpec {
    val locale = context.resources.configuration.locales[0]
    val period = periodLabel(review.range, locale)
    return ShareCardSpec(
        label = period.uppercase(locale),
        title = context.getString(R.string.review_share_title),
        subtitle = context.getString(R.string.review_share_subtitle, review.perfectDays, review.longestPerfectRun),
        badgeColor = Lime,
        badgeText = review.doneCount.toString(),
        shareText = context.getString(R.string.review_share_text, period, review.doneCount, review.perfectDays)
    )
}
