package com.streak.app.locale

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.text.Collator
import java.util.Locale

// Jezik aplikacije (neodvisen od jezika sistema).
// Android 13+: sistemski LocaleManager (izbira je vidna tudi v sistemskih nastavitvah
// aplikacije, sistem sam osvezi aktivnosti). Starejsi: izbira v SharedPreferences
// (sinhrono branje v attachBaseContext) in ovit Context aplikacije in aktivnosti.
object AppLocale {

    // 24 uradnih jezikov EU; angleščina je privzeta (values/)
    val SUPPORTED = listOf(
        "bg", "cs", "da", "de", "el", "en", "es", "et", "fi", "fr", "ga", "hr",
        "hu", "it", "lt", "lv", "mt", "nl", "pl", "pt", "ro", "sk", "sl", "sv"
    )

    private const val PREFS = "app_locale"
    private const val KEY = "tag"

    // Izbran jezik ali null (jezik sistema)
    fun current(context: Context): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales
                .takeIf { !it.isEmpty }?.get(0)?.language
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        }

    fun set(activity: Activity, tag: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit(commit = true) {
                if (tag == null) remove(KEY) else putString(KEY, tag)
            }
            // Ponovno ustvari aktivnost; Context aplikacije se osvezi ob naslednjem zagonu procesa
            activity.recreate()
        }
    }

    // Ovije Context z izbranim jezikom (le pred Androidom 13)
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    // Ime jezika v tem jeziku (npr. "Deutsch"), z veliko zacetnico
    fun nativeName(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
    }

    // Jeziki, razvrsceni po izvornem imenu po pravilih trenutnega jezika
    fun sorted(current: Locale): List<String> {
        val collator = Collator.getInstance(current)
        return SUPPORTED.sortedWith { a, b -> collator.compare(nativeName(a), nativeName(b)) }
    }
}
