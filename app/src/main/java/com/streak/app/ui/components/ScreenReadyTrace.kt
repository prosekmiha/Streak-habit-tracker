package com.streak.app.ui.components

import android.os.Build
import android.os.Trace
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import java.util.concurrent.atomic.AtomicInteger

private val cookies = AtomicInteger()

// Asinhrona sledilna sekcija od prve kompozicije zaslona do trenutka, ko so podatki nalozeni
// (vkljucno z branjem iz baze). Macrobenchmark jo meri s TraceSectionMetric (docs/quality.md).
// Asinhrone sekcije obstajajo od API 29; na starejsih sistemih se nic ne belezi.
@Composable
fun ScreenReadyTrace(name: String, ready: Boolean) {
    val section = remember { Section(name, cookies.incrementAndGet()) }
    DisposableEffect(section) {
        section.begin()
        onDispose { section.end() }
    }
    LaunchedEffect(ready) { if (ready) section.end() }
}

private class Section(val name: String, val cookie: Int) {
    private var ended = false

    fun begin() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Trace.beginAsyncSection(name, cookie)
    }

    fun end() {
        if (ended) return
        ended = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Trace.endAsyncSection(name, cookie)
    }
}
