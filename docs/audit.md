# Phase 0: audit aplikacije Streak

Stanje na dan 2026-10-09, po commitih M0 (nadgradnja orodij, popravek kritičnega buga, odstranitev oblačnega stuba).

## 1. Arhitektura, odvisnosti, SDK, dovoljenja

| Področje | Stanje |
|---|---|
| Moduli | En modul `:app`, paket `com.streak.app` (~4200 vrstic Kotlina) |
| UI | Jetpack Compose + Material 3, ročno risani grafi (Canvas) |
| Arhitektura | MVVM + "clean" sloji: `ui` (ViewModeli s `StateFlow`) → `domain` (modeli, repository vmesniki, use-case-i) → `data` (Room, DataStore) |
| DI | Hilt (+ HiltWorkerFactory za WorkManager) |
| Shramba | Room `streak.db` v1 (tabele `habits`, `habit_logs`, `tasks`), DataStore `settings` (ime, tema) |
| Ozadje | AlarmManager (opomniki, polnočni reset), WorkManager (`DailyResetWorker`) |
| SDK | minSdk 26, compile/targetSdk **36** (nadgrajeno iz 35), Java 17, desugaring vklopljen |
| Orodja | AGP 8.13.2, Gradle 8.14.3, Kotlin 2.3.21, KSP2 2.3.6 (nadgrajeno). AGP 9 je namerno izpuščen, ker spremeni integracijo Kotlina, Hilt 2.59+ pa ga že zahteva. Hilt zato ostane na 2.57.2. |
| Testi | Pred auditom jih ni bilo. Zdaj: 11 karakterizacijskih testov streaka + Room regresijski test (Robolectric) |
| Velikost | Debug APK 21,8 MB (neoptimiziran, R8 izklopljen). Release z R8 bo bistveno manjši. |

**Odvisnosti:** core-ktx, lifecycle, activity-compose, Compose BOM 2026.01.01, material3, material-icons-extended, navigation-compose, Hilt (+ navigation-compose, work), Room, coroutines, WorkManager, DataStore, kotlinx-serialization, desugar_jdk_libs. Omrežnih knjižnic ni.

**Dovoljenja (manifest):** `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`. `USE_EXACT_ALARM` je odstranjen, ker je po Play politiki rezerviran za budilke in koledarje. `INTERNET` ni deklariran.

## 2. Podatkovni model in kje živijo podatki

- **Room v1, `exportSchema = false`** (sheme niso shranjene, migracije ne obstajajo):
  - `habits`: id, title, icon (ime Material ikone), colorHex, frequency (DAILY/WEEKLY/CUSTOM), targetPerWeek, reminderMinuteOfDay (en opomnik), reminderEnabled, isActive, createdAt, sortOrder
  - `habit_logs`: id, habitId (FK CASCADE), epochDay, status (DONE/SKIPPED/MISSED), completedAt, note. Unikatni indeks (habitId, epochDay).
  - `tasks`: id, title, dueEpochDay, dueMinuteOfDay, priority, isDone, isRecurring, recurrenceRule (RRULE niz), createdAt
- **DataStore `settings`:** `user_name`, `theme_mode`. Ključ `cloud_sync` je odstranjen.
- **Strežnik:** ga ni. `data/remote` je bil prazen Supabase stub in je odstranjen.
- **Datoteke:** jih ni. JSON izvoz je bil le prikazan v dialogu, ni se shranil.
- `SeedCallback` ob prvem ustvarjanju baze vstavi 3 vzorčne navade in 2 nalogi (slovenski nizi v kodi).

## 3. Tabela vrzeli (feature gap)

Legenda: ✅ obstaja · 🟡 delno · ❌ manjka

| # | Funkcionalnost | Stanje | Opomba |
|---|---|---|---|
| 1 | Onboarding (ime, jezik, obvestila, zaklep, predloge, obnova) | ❌ | Dovoljenje za obvestila se zahteva takoj ob zagonu |
| 2 | Home: pozdrav, tedenski trak, mreža kartic, long-press meni | 🟡 | Pozdrav in kartice obstajajo (seznam, ne mreža). Tedenskega traku, long-press menija in zvonca ni. |
| 3 | Ustvari/uredi navado | 🟡 | Naslov, ikona (17), barva, frekvenca, cilj/teden, 1 opomnik. Manjkajo opis, tip cilja (količina/čas), urnik po dnevih, kategorija, del dneva, več opomnikov, začetek/konec. |
| 4 | Časovna seja (timer, nasveti, zvoki, obvestilo) | ❌ | |
| 5 | Streak logika: zamrznitev, dopust, backfill, delni napredek, arhiv | 🟡 | DONE/SKIPPED/MISSED. Backfill je mogoč prek koledarja. Zamrznitve, dopusta, količin in arhiva (le `isActive`) ni. |
| 6 | Lokalna obvestila in alarmi | 🟡 | En dnevni alarm na navado + eskalacija 30/60 min, BOOT_COMPLETED, preverjanje exact alarmov. Manjkajo kanali nudge/seja/značke, quiet hours, cap, skrivanje imen, reschedule ob spremembi časovnega pasu ali posodobitvi. |
| 7 | Widgeti (Glance) | ❌ | |
| 8 | Statistika | 🟡 | Trenutni/rekordni streak, mesečni %, tedenski stolpci, letni heatmap, koledar. Manjkajo trendi po navadi, najboljši dan in tedenski povzetek. |
| 9 | Gamifikacija | ❌ | |
| 10 | Varnost (zaklep, šifriranje, FLAG_SECURE) | ❌ | |
| 11 | Nastavitve | 🟡 | Ime, tema, upravljanje navad, izvoz. Ostalo manjka. |
| 12 | Integracije (Health Connect, Wear OS, Assistant) | ❌ | Načrtovano za v2 |
| 13 | Opombe, predloge, kategorije, CSV uvoz | 🟡 | Polje `note` obstaja v bazi, a ga UI ne uporablja |
| — | Izvoz/uvoz, varnostna kopija | 🟡 | JSON izvoz vsebuje le navade (brez logov!) in naloge, brez sheme. Uvoza ni. |
| — | Lokalizacija | 🟡 | 79 vrstic `strings.xml`, vse v slovenščini v privzetem `values/`. ~20 hardkodiranih nizov. |
| — | Dostopnost | 🟡 | Ni sistematične semantike. Stanje opravljenosti je pogosto prikazano le z barvo. |
| — | 3D / animacije | ❌ | |

## 4. Deli, odvisni od omrežja

Teh ni več. Supabase stub, stikalo za sinhronizacijo in pripadajoči nizi so odstranjeni (commit "Remove cloud sync stub"). FCM, analitike, Crashlytics, oglasov in plačil ni bilo.

## 5. Tveganja

| Tveganje | Resnost | Ukrep |
|---|---|---|
| **Urejanje navade je izbrisalo vso zgodovino** (`REPLACE` + `ON DELETE CASCADE`) | Kritično | ✅ Popravljeno z `@Upsert` + regresijski test |
| JSON izvoz ne vsebuje logov, zato varnostna kopija ni mogoča | Visoka | M5: polni izvoz/uvoz s `schemaVersion` |
| `exportSchema = false`, brez migracij: vsaka sprememba sheme bi zrušila aplikacijo | Visoka | M1: v2 kot nova osnova (podatki niso pomembni, aplikacija ni objavljena), izvožene sheme, testirane migracije |
| `DailyResetWorker` ob polnoči piše MISSED. Ni "day ends at", ob spremembi časovnega pasu ali ure lahko nastanejo napačni MISSED zapisi. | Visoka | M1: MISSED se ne zapisuje več, izpelje se iz urnika; `DayClock` |
| Streak: med dnevi brez zapisa se rekord ne prekine (QUIRK v testih). Tedni so odvisni od `Locale.getDefault()`, `isNextWeek` je približek. | Srednja | M1: nov izračun + testi (DST, potovanja, meja leta) |
| Alarmi: po spremembi časovnega pasu, ure ali posodobitve aplikacije se ne prerazporedijo. `requestCode` iz `hashCode()` lahko kolidira. | Srednja | M4: `RescheduleReceiver`, stabilni ID-ji iz baze |
| Auto Backup: pravila so prazna, varnostno se kopira vse. Ob kasnejšem šifriranju s Keystore ključem bi obnovljena baza bila neberljiva. | Srednja | M5: snapshot + eksplicitna pravila |
| Zmogljivost: `observeHabitsWithLogs` naloži **vse** loge vseh navad in izračuna streak ob vsaki spremembi. Pri 5 letih × 200 navad je to ~365 000 vrstic. | Srednja | M1: `streak_cache`, `daily_aggregates`, okenske poizvedbe |
| Dostopnost: barva kot edini nosilec stanja, ni contentDescription | Srednja | M2/M10 |
| Lokalizacija: privzeti jezik je slovenščina, hardkodirani nizi | Srednja | M9 |
| Play: Data safety obrazec, deklaracija exact alarm, FGS tip za sejo z zvokom | Nizka | M3/M4, `docs/privacy-policy.md` |
| Font iz dizajna ("Obviously") je plačljiv in ne pokriva cirilice/grščine | Nizka | Brezplačna OFL alternativa (M2) |

## 6. Vizualna sredstva in 3D pristop

**Inventar:** le adaptivna launcher ikona (vektor). Maskote, ilustracij, fontov, Lottie/Rive in 3D izvornih datotek ni. Animacije so le osnovne Compose animacije.

**Maskota na priloženih slikah** je tuje avtorsko delo (Dribbble predloga), zato se ne kopira. Nastane **originalen** lik v podobnem stilu: oranžna krogla z "rubber-hose" okončinami, belimi rokavicami in supergami.

**Predlagan 3D pristop:**
- **Generiranje:** Python skripta za Blender 4.3 (`tools/mascot/build_mascot.py`, headless). Proceduralno zgradi model, armaturo in vse animacije ter izvozi `assets/models/mascot.glb`. Druga skripta zrenderira WebP poze za widgete, obvestila in fallback. Vse je ponovljivo iz repozitorija, model pa je kasneje mogoče zamenjati z ročno izdelanim brez spremembe kode.
- **Izgled:** ploskovne barve (`KHR_materials_unlit`) in črna obroba (inverted hull), kar ustreza ilustrativnemu stilu dizajna.
- **Render:** Filament prek SceneView (Compose API), zaprt za vmesnik `MascotRenderer`. Fallbacka sta sprite sekvenca in statična poza.
- **Ocena vpliva:**

| Postavka | Ocena |
|---|---|
| Filament/SceneView native knjižnice | ~4–6 MB na ABI (AAB split, uporabnik prenese le svoj ABI) |
| `mascot.glb` (brez tekstur, Draco) | < 1 MB |
| WebP poze in sekvence | ~1–2 MB |
| GPU/baterija | Rendering le, ko je maskota vidna. 30 fps v varčnem načinu, pavza v ozadju ali ob pregrevanju. |

## 7. Fazni načrt

Vsak milestone se konča z zelenim buildom, testi in commitom ter je samostojno uporaben.

| Milestone | Vsebina | Ocena |
|---|---|---|
| **M0** ✅ | Git, nadgradnja orodij, testna infrastruktura, karakterizacijski testi, kritični popravek, odstranitev oblaka, ta audit | — |
| **M1** ✅ | Room v2 (nov model), `DayClock`, nov streak izračun s testi, agregati, `DatabaseGuard`, razširjene nastavitve | velik |
| **M2** ✅ | Dizajn sistem (barve, font, komponente), novi zasloni: onboarding, Home, nova navada, statistika, naloge, profil; mesto za maskoto | velik |
| **M3** ✅ | Časovne seje (timer, ki preživi process death, ongoing obvestilo, ambientni zvoki, bližnjice) | srednji |
| **M4** ✅ | Obvestila in alarmi (več opomnikov, nudge, kanali, reschedule, quiet hours, zasebnost) | srednji |
| **M5** ✅ | Izvoz/uvoz JSON/CSV, geslo, Auto Backup + snapshot, SQLCipher, FLAG_SECURE, R8. Zaklep s kodo je bil narejen in nato na željo uporabnika odstranjen. | velik |
| **M6** ✅ | Glance widgeti (mali, srednji, streak, količina) | srednji |
| **M7** ✅ | 3D maskota: Blender generator, poze, SceneView renderer, state machine, sprožilci, fallbacki | velik |
| **M8** ✅ | Gamifikacija (XP, nivoji, značke, izzivi, deljenje) + razširjena statistika | srednji |
| **M9** ✅ | 24 EU jezikov, plurals, per-app language, pseudolokalizacija | srednji |
| **M10** ✅ | Dostopnost, screenshot testi, Macrobenchmark + Baseline Profile | srednji |
| v2 | Health Connect, Wear OS, App Actions, koledar, CSV uvoz iz drugih aplikacij, lock-screen widgeti, 3D značke in outfiti | — |

**Rollback strategija:** vsak milestone je ločen niz commitov. Ker aplikacija ni objavljena, se staged rollout in Play Console koraki začnejo šele pred prvo izdajo (opisani bodo v `docs/roadmap.md`).
