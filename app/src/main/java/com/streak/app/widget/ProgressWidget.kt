package com.streak.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.streak.app.R
import com.streak.app.domain.widget.TodaySummary
import com.streak.app.domain.widget.WidgetMood
import kotlinx.coroutines.flow.first

// Mali widget: obroc "3/5" in maskota, ki se odziva na napredek dneva
class ProgressWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val source = widgetEntryPoint(context).widgetRepository().today()
        val initial = source.first()
        provideContent {
            val state by source.collectAsState(initial)
            ProgressContent(state)
        }
    }

    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val WIDE = DpSize(180.dp, 110.dp)
        private val TALL = DpSize(110.dp, 170.dp)
    }
}

class ProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProgressWidget()
}

@Composable
private fun ProgressContent(state: TodayWidgetState) {
    val context = LocalContext.current
    val colors = WidgetColors(state.look.dark)
    val s = state.summary
    val size = LocalSize.current
    val wide = size.width >= 180.dp
    // Visok widget (npr. 2x2 na ozkem zaslonu): maskota nad obrocem
    val tall = !wide && size.height >= 170.dp
    val caption = progressCaption(context, s)
    val open = if (s.mood == WidgetMood.NO_HABITS) WidgetIntents.newHabit(context) else WidgetIntents.openApp(context)
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetBackground(colors.background)
            .clickable(actionStartActivity(open))
            .padding(WidgetPadding)
            .semantics { contentDescription = progressDescription(context, s) }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (tall && s.mood != WidgetMood.NO_HABITS) {
                MascotPose(s.mood, 64)
                Spacer(GlanceModifier.height(2.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (s.mood == WidgetMood.NO_HABITS) {
                    MascotPose(s.mood, 72)
                } else {
                    ProgressRing(s, colors.ink)
                    if (wide) {
                        Spacer(GlanceModifier.width(10.dp))
                        MascotPose(s.mood, 68)
                    }
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            Text(
                caption,
                maxLines = 1,
                style = TextStyle(color = colors.inkSoft, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            )
        }
    }
}

@Composable
private fun ProgressRing(s: TodaySummary, ink: ColorProvider) {
    val context = LocalContext.current
    Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.size(68.dp)) {
        Image(ImageProvider(WidgetBitmaps.ring(context, s.progress, 68.dp)), contentDescription = null, modifier = GlanceModifier.size(68.dp))
        Text(
            if (s.due > 0) "${s.done}/${s.due}" else "–",
            style = TextStyle(color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        )
    }
}

// Vnaprej zrenderirana poza 3D maskote
@Composable
internal fun MascotPose(mood: WidgetMood, sizeDp: Int) {
    val res = when (mood) {
        WidgetMood.NO_HABITS -> R.drawable.mascot_wave
        WidgetMood.REST_DAY -> R.drawable.mascot_sleep
        WidgetMood.NOT_STARTED -> R.drawable.mascot_idle
        WidgetMood.IN_PROGRESS -> R.drawable.mascot_thumbs
        WidgetMood.ALL_DONE -> R.drawable.mascot_cheer
    }
    Image(ImageProvider(res), contentDescription = null, modifier = GlanceModifier.size(sizeDp.dp))
}

internal fun progressCaption(context: Context, s: TodaySummary): String = when (s.mood) {
    WidgetMood.NO_HABITS -> context.getString(R.string.widget_add_first)
    WidgetMood.REST_DAY -> context.getString(R.string.widget_rest_day)
    WidgetMood.ALL_DONE -> context.getString(R.string.widget_all_done)
    else -> context.resources.getQuantityString(R.plurals.widget_remaining, s.remaining, s.remaining)
}

private fun progressDescription(context: Context, s: TodaySummary): String =
    if (s.due > 0) context.getString(R.string.widget_progress_a11y, s.done, s.due, progressCaption(context, s))
    else progressCaption(context, s)
