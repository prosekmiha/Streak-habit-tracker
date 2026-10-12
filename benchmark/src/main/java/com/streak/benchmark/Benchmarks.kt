package com.streak.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val PACKAGE = "com.streak.app.benchmark"
private const val ACTIVITY = "com.streak.app.MainActivity"

// Testni podatki: 200 navad, 5 let zapisov (DevSeeder v aplikaciji, gradnja benchmark).
// Enkrat na zagon instrumentacije, ker polnjenje traja.
object Seed {
    @Volatile private var done = false

    @Synchronized
    fun ensure() {
        if (done) return
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val marker = "/sdcard/Android/data/$PACKAGE/files/seed_status"
        device.executeShellCommand("am force-stop $PACKAGE")
        device.executeShellCommand("rm -f $marker")
        device.executeShellCommand("am start -W -n $PACKAGE/$ACTIVITY --ez dev_seed true --ei dev_seed_habits 200 --ei dev_seed_years 5")
        val deadline = System.currentTimeMillis() + SEED_TIMEOUT_MS
        var status = ""
        while (System.currentTimeMillis() < deadline) {
            status = device.executeShellCommand("cat $marker").trim()
            if (status.startsWith("SEED_")) break
            Thread.sleep(2_000)
        }
        check(status == "SEED_DONE") { "Polnjenje ni uspelo: '$status'" }
        device.executeShellCommand("am force-stop $PACKAGE")
        done = true
    }

    private const val SEED_TIMEOUT_MS = 600_000L
}

private fun MacrobenchmarkScope.waitForHome() {
    check(device.wait(Until.hasObject(By.textStartsWith("Habit ")), 20_000)) { "Domaci zaslon se ni nalozil" }
}

// Spodnja vrstica prikazuje le ikone; ime zavihka je opis (content-desc)
private fun MacrobenchmarkScope.tab(label: String) {
    (device.findObject(By.desc(label)) ?: device.findObject(By.text(label))).click()
}

private fun MacrobenchmarkScope.openStats() {
    tab("Stats")
    device.wait(Until.hasObject(By.text("Overview")), 10_000)
}

// Zagon: hladen, brez in z Baseline Profilom. TTID/TTFD (ReportDrawnWhen ob nalozenih navadah)
// in cas do nalozenega domacega zaslona (sekcija Home.ready).
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    @Before fun seed() = Seed.ensure()

    @Test fun coldStartNoCompilation() = startup(CompilationMode.None())

    @Test fun coldStartBaselineProfile() = startup(CompilationMode.Partial())

    private fun startup(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric(), TraceSectionMetric("Home.ready")),
        compilationMode = mode,
        startupMode = StartupMode.COLD,
        iterations = 5
    ) {
        pressHome()
        startActivityAndWait()
        waitForHome()
    }
}

// Domaci zaslon: cas do nalozenih podatkov ob odprtju in gladkost drsenja mreze
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class HomeBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    @Before fun seed() = Seed.ensure()

    @Test fun scrollHome() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric(), TraceSectionMetric("Home.ready")),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
        waitForHome()
        val grid = device.findObject(By.scrollable(true))
        grid.setGestureMargin(device.displayWidth / 5)
        repeat(3) { grid.fling(Direction.DOWN) }
        grid.fling(Direction.UP)
    }
}

// Statistika: pregled (koledar, tedenski povzetek) in zavihek navad (izbrana navada z vso zgodovino)
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class StatsBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    @Before fun seed() = Seed.ensure()

    @Test fun openStats() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(
            TraceSectionMetric("Stats.ready"),
            TraceSectionMetric("StatsHabits.ready"),
            FrameTimingMetric()
        ),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            waitForHome()
        }
    ) {
        openStats()
        device.findObject(By.text("Habits")).click()
        device.wait(Until.hasObject(By.text("Best")), 10_000)
        device.pressBack()
    }
}

// Baseline Profile: zagon, domaci zaslon z drsenjem, statistika, profil.
// Rezultat se kopira v app/src/main/baseline-prof.txt (docs/quality.md).
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Before fun seed() = Seed.ensure()

    @Test fun generate() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        waitForHome()
        device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
        openStats()
        device.findObject(By.text("Habits"))?.click()
        device.wait(Until.hasObject(By.text("Best")), 10_000)
        tab("Profile")
        device.wait(Until.hasObject(By.text("Theme")), 10_000)
        tab("Home")
        waitForHome()
    }
}
