package com.streak.app.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.streak.app.MainActivity
import com.streak.app.R
import com.streak.app.ui.theme.Blurple
import com.streak.app.ui.theme.BlurpleLight
import com.streak.app.ui.theme.DarkPalette
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.LightPalette
import com.streak.app.ui.theme.Lime

// Barve widgetov po paleti aplikacije. Pri temi "po sistemu" barvo izbere zaganjalnik
// (dan/noc), pri izbrani temi pa je barva fiksna.
class WidgetColors(dark: Boolean?) {
    private val pick: (Color, Color) -> ColorProvider = when (dark) {
        null -> { day, night -> ColorProvider(day = day, night = night) }
        true -> { _, night -> ColorProvider(day = night, night = night) }
        false -> { day, _ -> ColorProvider(day = day, night = day) }
    }

    val background = pick(Color.White, Color(0xFF1C1D24))
    val ink = pick(LightPalette.ink, DarkPalette.ink)
    val inkSoft = pick(LightPalette.inkSoft, DarkPalette.inkSoft)
    val surfaceSoft = pick(LightPalette.surfaceSoft, DarkPalette.surfaceSoft)
    val accent = pick(Blurple, BlurpleLight)

    companion object {
        val Done: ColorProvider = ColorProvider(day = Lime, night = Lime)
        val OnPastel: ColorProvider = ColorProvider(day = InkOnPastel, night = InkOnPastel)
        val OnPastelSoft: ColorProvider = ColorProvider(day = InkOnPastel.copy(alpha = 0.7f), night = InkOnPastel.copy(alpha = 0.7f))
        val PastelButton: ColorProvider = ColorProvider(day = Color.White.copy(alpha = 0.6f), night = Color.White.copy(alpha = 0.6f))
        fun fixed(color: Color): ColorProvider = ColorProvider(day = color, night = color)
    }
}

val WidgetPadding: Dp = 14.dp

// Ozadje widgeta: na Androidu 12+ s sistemskim zaobljenjem, prej z zaobljeno obliko
fun GlanceModifier.widgetBackground(color: ColorProvider): GlanceModifier {
    val base = appWidgetBackground()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        base.background(color).cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        base.background(ImageProvider(R.drawable.widget_shape_rounded), colorFilter = ColorFilter.tint(color))
    }
}

// Okrogel gumb ali oznaka (deluje na vseh verzijah Androida)
fun GlanceModifier.circleBackground(color: ColorProvider): GlanceModifier =
    background(ImageProvider(R.drawable.widget_shape_circle), colorFilter = ColorFilter.tint(color))

// Odpiranje aplikacije z widgeta. Podatkovni URI loci namere med seboj (PendingIntent).
object WidgetIntents {
    fun openApp(context: Context): Intent = main(context, null, "streak://widget/open")

    fun newHabit(context: Context): Intent = main(context, MainActivity.ACTION_NEW_HABIT, "streak://widget/new")

    fun openHabit(context: Context, habitId: String): Intent =
        main(context, MainActivity.ACTION_OPEN_HABIT, "streak://widget/habit/$habitId").putExtra(MainActivity.EXTRA_HABIT_ID, habitId)

    fun openSession(context: Context, habitId: String): Intent =
        main(context, MainActivity.ACTION_OPEN_SESSION, "streak://widget/session/$habitId").putExtra(MainActivity.EXTRA_HABIT_ID, habitId)

    fun configure(context: Context, appWidgetId: Int): Intent =
        Intent(context, WidgetConfigActivity::class.java)
            .setData("streak://widget/configure/$appWidgetId".toUri())
            .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    private fun main(context: Context, action: String?, uri: String): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(action ?: Intent.ACTION_MAIN)
            .setData(uri.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}
