# Kakovost: dostopnost, posnetki zaslona, hitrost

## Dostopnost

- **Kartice navad:** vsaka je en združen element z opisom (ime, napredek), stanjem ("opravljeno, 5 dni zapored") in dejanji po meri: "Označi kot opravljeno" / "Dodaj 1" / "Začni sejo" ter "Možnosti navade" (dolg pritisk). Bralnik zaslona tako ne potrebuje kretenj.
- **Dotikalne tarče** so vsaj 48 dp. Okrogli gumbi imajo viden krog 44 dp v 48-dp tarči. Ročica spodnjih listov ima oznako in 48-dp višino. Compose sam razširi dotikalno območje majhnih elementov, a pregledovalniki merijo meje elementa, zato so tudi te vsaj 48 dp.
- **Velika pisava:** postavitve so preverjene pri 200 %.
  - Deljenje besed (`Hyphens.Auto`, `LineBreak.Paragraph`) je vklopljeno v celotni tipografiji, zato se besede ne lomijo sredi zloga (npr. "zapo-red").
  - Dnevi tedenskega traku imajo najmanjšo, ne fiksne velikosti.
  - Čipi na domačem zaslonu ostanejo v eni vrstici.
- **Oznake:** dejanja gradnika za izbiro časa so prevedena ("Povečaj"/"Zmanjšaj" namesto "+"/"−"). Okrasne ikone nimajo opisov.
- **Animacije:** sistemska nastavitev "Odstrani animacije" in izbira v aplikaciji (polne/zmanjšane/izklopljene) se upoštevata.

### Samodejni testi (`app/src/test/.../ui/AccessibilityTest.kt`)

- **Zakaj ne ATF:** Googlov Accessibility Test Framework pod Robolectricom ne deluje (ne zazna niti 20-dp gumba brez oznake), zato ga ne uporabljam.
- **Lastno preverjanje:** test pregleda semantično drevo. Vsak element z dejanjem mora imeti oznako in dotikalno območje vsaj 48 × 48 dp. Ločen test potrjuje, da preverjanje gumb brez oznake res ujame.
- **Kartica navade:** test preveri opis, stanje in obe dejanji po meri.

### Pregled na emulatorju

Skripta prek `uiautomator dump` na vsakem zaslonu poišče elemente z dejanjem brez oznake ali manjše od 48 dp. Delno vidne elemente ob robu drsnih seznamov prezre.

Pregledani zasloni: domov, naloge, statistika (oba zavihka), profil, dosežki, podatki, nastavitve obvestil, meni navade in list za dodajanje. Najdeni in odpravljeni sta bili dve napaki:
- 44-dp okrogla gumba;
- ročica lista brez oznake.

## Posnetki zaslona (Roborazzi)

`app/src/test/.../ui/ScreenshotTest.kt` pod Robolectricom (izvorni izris) posname prave gradnike:

| Posnetek | Vsebina |
|---|---|
| `home_light`, `home_dark` | Tedenski trak in kartice v vseh stanjih (delno, opravljeno, seja, preskočeno) |
| `home_font200` | Enako pri 200 % pisavi |
| `home_de`, `home_el`, `home_bg` | Dolge besede, grška pisava, cirilica (temna tema) |
| `components` | Vse različice gumbov, onemogočen gumb, značke |
| `mascot_poses` | Vse poze maskote |

- **Shramba:** referenčni posnetki so v `app/src/test/screenshots/`.
- **Snemanje:** `./gradlew recordRoborazziDebug`.
- **Primerjava:** `./gradlew verifyRoborazziDebug` (spodleti ob razliki).
- **Navadni testi:** `testDebugUnitTest` posnetke le izriše, brez primerjave.

## Hitrost (Macrobenchmark + Baseline Profile)

- **Testni podatki:** `DevSeeder` (le gradnji debug in benchmark) ustvari 200 navad in 5 let zapisov (≈ 230 000 vrstic). Sproži se z `am start ... --ez dev_seed true --ei dev_seed_habits 200 --ei dev_seed_years 5`; konec zapiše v `files/seed_status`.
- **Gradnja `benchmark`:** kot release (R8), a podpisana z debug ključem, `profileable`, brez zameglitve imen, z lastnim `applicationId` (`.benchmark`), da ne prepiše prave namestitve.
- **Merilne sekcije:** `ScreenReadyTrace` odpre asinhrono sledilno sekcijo ob prvi kompoziciji zaslona in jo zapre, ko so podatki naloženi (`Home.ready`, `Stats.ready`, `StatsHabits.ready`). `ReportDrawnWhen` na domačem zaslonu določi TTFD.
- **Modul `:benchmark`:** `StartupBenchmark` (hladen zagon brez in z Baseline Profilom), `HomeBenchmark` (topel zagon in drsenje mreže), `StatsBenchmark` (pregled in zavihek navad), `BaselineProfileGenerator`.

```
ANDROID_SERIAL=emulator-5554 ./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.streak.benchmark.StartupBenchmark,com.streak.benchmark.HomeBenchmark,com.streak.benchmark.StatsBenchmark
```

**Baseline Profile:** generator (`...class=com.streak.benchmark.BaselineProfileGenerator`) zapiše `BaselineProfileGenerator_generate-startup-prof.txt`, ki se kopira v `app/src/main/baseline-prof.txt`. Profil je treba osvežiti po večjih spremembah zagonske poti.

### Rezultati (emulator Pixel 7, API 36, 200 navad, 5 let; mediana)

| Meritev | Cilj | Rezultat |
|---|---|---|
| Domov naložen, topel zagon (`Home.ready`) | < 100 ms | 143 ms (min 93) |
| Statistika, pregled (`Stats.ready`) | < 300 ms | 42 ms |
| Statistika, zavihek navad (`StatsHabits.ready`) | < 300 ms | 33 ms |
| Hladen zagon TTID / TTFD, brez profila | | 1165 / 1482 ms |
| Hladen zagon TTID / TTFD, z Baseline Profilom | | 983 / 1207 ms |
| Drsenje domov, čas sličice CPU P50 / P90 | | 6,7 / 21,6 ms |

Optimizacije med meritvami:
- **Izračun stanj dni zunaj glavne niti:** `observeHabits` računa 200 navad × 6 tednov stanj na `Dispatchers.Default`.
- **Deljen tok navad:** tok ostane živ minuto po zadnjem zbiralcu, zato vrnitev na domači zaslon ne bere baze znova. Ob ustavitvi se zadnja vrednost zavrže.
- **Učinek na emulatorju:** topel Home.ready se je zmanjšal z 214 na 143 ms, hladen TTFD pa z 2271 na 1207 ms (skupaj z Baseline Profilom).

**Odstopanje:** domači zaslon pri 200 navadah na emulatorju še ni pod 100 ms. Preostanek je predvsem izris prve polne mreže (Compose ob prvi kompoziciji). Emulator je počasnejši in manj stabilen od prave naprave, zato je treba številke potrditi na telefonu.
