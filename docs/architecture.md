# Arhitektura

MVVM s čistimi sloji v treh modulih. Prevajalnik sam prepreči, da bi domena uvozila Android ali Room in da bi podatkovna plast uvozila UI.

```
            ┌──────────► :core:domain ◄──────────┐
            │            modeli, pravila,        │
:app ───────┤            use-case-i, vmesniki    │ api
 ui,        │                                    │
 platforma  └──────────► :core:data ─────────────┘
 (alarmi, seje,          Room, DataStore,
 widgeti), DI            implementacije vmesnikov
```

## Moduli

| Modul | Vsebina | Odvisnosti |
|---|---|---|
| `:core:domain` | Modeli, use-case-i, pravila (streak, logični dan, igrifikacija, obvestila, varnostna kopija) in vmesniki, ki jih implementira aplikacija | Samo Kotlin, coroutines, kotlinx.serialization, `javax.inject` (Dagger generira tovarne že tu) |
| `:core:domain` `testFixtures` | Skupni fake-i (`FakeHabitRepository`, `FakeReminderScheduler` ...), `testHabit`, `FakeDayClock`, `MainDispatcherRule` | Uporabljajo jih testi domene in aplikacije |
| `:core:data` | Room (SQLCipher), DataStore, repozitoriji, `DatabaseGuard`, Hilt moduli za bazo in repozitorije, izvozene sheme (`core/data/schemas`) | `:core:domain` (api) |
| `:app` | UI (Compose), Androidove storitve (alarmi, obvestila, seje, widgeti, varnostna kopija), zagon, DI za storitve | `:core:domain`, `:core:data` |
| `:benchmark` | Macrobenchmark in Baseline Profile | `:app` |

Paketi ostajajo `com.streak.app.domain.*`, zato premik modula ni spremenil nobenega imena.

## Pravila odvisnosti

1. **Domena ne pozna Androida.** V `:core:domain` ni `android.*`, `androidx.*`, Rooma ali `Context`. Ta modul je JVM knjižnica, zato takega importa sploh ni mogoče prevesti.
2. **UI pozna le domeno.** ViewModeli prejmejo vmesnike iz domene (`HabitRepository`, `ReminderScheduler`, `SessionController`, `BackupService` ...), nikoli razredov iz `data` ali Androidovih storitev. Edina izjema je `BackupFiles` (SAF `Uri`), ki je vmesnik v `:app`.
3. **ViewModel ne prejme `Context`.** Nize iz virov (npr. predloge navad) razreši vmesnik (`HabitTemplateCatalog`), sistemska stanja pa `PermissionStatus`.
4. **Podatkovna plast ne pozna aplikacije.** `:core:data` ne vidi `R`, `BuildConfig`, UI-ja ne storitev.
5. **Implementacije se povežejo v `di/`.** `RepositoryModule` in `DatabaseModule` (v `:core:data`) povežeta podatkovne vmesnike z Room/DataStore, `PlatformModule` (v `:app`) pa Androidove storitve. Zamenjava implementacije je ena vrstica.

## Vmesniki (porti) in implementacije

| Vmesnik (`:core:domain`) | Implementacija | Kaj |
|---|---|---|
| `repository/HabitRepository` | `data/repository/HabitRepositoryImpl` | Navade, zapisi, dopusti, seje (Room) |
| `repository/TaskRepository` | `data/repository/TaskRepositoryImpl` | Naloge |
| `repository/SettingsRepository` | `data/prefs/SettingsDataStore` | Nastavitve |
| `repository/GameRepository` | `data/repository/GameRepositoryImpl` | Izpeljano stanje igrifikacije |
| `repository/GameMemoryRepository` | `data/prefs/GameDataStore` | Kaj je uporabnik že videl, nabiralnik |
| `session/ActiveSessionStore` | `data/prefs/SessionDataStore` | Trajna tekoča seja |
| `notify/NotificationLog` | `data/prefs/NotificationLogDataStore` | Dnevna omejitev obvestil |
| `time/DayClock` | `data/time/SystemDayClock` | Logični dan |
| `notify/ReminderScheduler` | `notifications/AlarmScheduler` | Alarmi (AlarmManager) |
| `notify/PermissionStatus` | `notifications/AndroidPermissionStatus` | Obvestila, natančni alarmi, baterija |
| `session/SessionController` | `session/AndroidSessionController` | Tekoča seja, obvestilo, zvok |
| `backup/BackupService` | `backup/BackupManager` | Izvoz, uvoz, posnetek, brisanje |
| `health/DatabaseHealthMonitor` | `data/local/DatabaseGuard` | Stanje baze po migraciji / preverjanju |

Poimenovanje: vmesnik nosi ime vloge. Implementacija nosi ime tehnologije (`SettingsDataStore`, `AlarmScheduler`, `AndroidSessionController`), le pri Room repozitorijih je pripona `Impl`.

## Use-case-i

Use-case nastane, **ko ista poslovna operacija potrebuje več kot en repozitorij ali storitev in jo kliče več kot en ViewModel**. Primer: shranjevanje navade mora razporediti tudi alarme. Preprosto branje ali pisanje ene vrednosti (npr. `settingsRepository.update { ... }`) gre iz ViewModela neposredno v repozitorij; ovoj brez logike bi bil le šum.

| Use-case | Kaj naredi |
|---|---|
| `ToggleHabitUseCase` | Vpisi za logični dan (preklop, stanje, napredek, backfill, zaklep prihodnosti) |
| `ComputeStreakUseCase` | Izračun streaka in stanj dni |
| `SaveHabitUseCase` | Shrani navado in na novo razporedi opomnike |
| `SetHabitArchivedUseCase` | Arhiv/vrnitev skupaj s preklicem/razporeditvijo opomnikov |
| `DeleteHabitUseCase` | Izbris skupaj s preklicem opomnikov |

Igrifikacija ima pravila v `Gamification` (čista funkcija) in zaznavo novih dogodkov v `GameEvents`. Oboje je v domeni in testirano brez Androida.

## Zasloni

Vsak zaslon ima `XxxScreen.kt` (vstopni composable, povezava z ViewModelom), `XxxViewModel.kt` in po potrebi datoteke z gradniki (`HomeContent.kt`, `HabitFormSections.kt`, `OnboardingSteps.kt`). Preslikave domene v UI model, ki ne potrebujejo stanja, so ločene čiste funkcije (`HomeUiMapping`), zato jih je mogoče testirati brez ViewModela. Ločeni deli istega zaslona imajo lahko svoj ViewModel (`HomeGameViewModel` za praznovanja in nabiralnik).

## Zagon procesa

`StreakApplication` samo pokliče `AppStartup.start()`. Ta odpre bazo, obnovi alarme in sejo ter zažene dolgožive tokove (widgeti, igrifikacija, samodejni posnetek, menjava dneva).

## Testi

| Kje | Kaj | Kako |
|---|---|---|
| `core/domain/src/test` | Pravila, use-case-i | Čisti JVM, fake-i iz `testFixtures` |
| `core/data/src/test` | Room, migracije, DAO, repozitoriji | Robolectric, baza v pomnilniku |
| `app/src/test/.../ui` | ViewModeli, UI preslikave | JVM + `MainDispatcherRule`, fake-i namesto mockk |
| `app/src/test/.../ui` (Roborazzi) | Posnetki zaslona, dostopnost | Robolectric |

## Nova funkcija: vrstni red

1. Model in pravila v `:core:domain` (+ unit test).
2. Če potrebuje shrambo ali sistemsko storitev: vmesnik v domeni, implementacija v `:core:data` (shramba) ali v `:app` (sistemska storitev), vezava v `di/`.
3. Če operacija združuje več virov in jo kliče več zaslonov: use-case.
4. ViewModel prejme le vmesnike/use-case-e; test z fake-i iz `testFixtures`.
5. Zaslon: `XxxScreen` + gradniki v ločenih datotekah, ko datoteka preseže ~300 vrstic.

## Opombe k gradnji

- Hilt 2.57 prinese `kotlin-metadata-jvm`, ki ne bere metapodatkov Kotlin 2.3. Ker so Hilt moduli zdaj v knjižnici `:core:data`, ima `:app` v `ksp` novejšo različico (`libs.kotlin.metadata.jvm`). Ob nadgradnji Hilta preveri, ali je to še potrebno.
- Pravila R8 za JSON nabiralnika so v `core/data/consumer-rules.pro`, za varnostno kopijo pa v `app/proguard-rules.pro` (domena je JVM knjižnica brez lastnih pravil).

## Naslednji koraki, ko bo projekt rasel

- **DAO-ji kot `internal`**: UI danes ne uvaža `data` po dogovoru. Ko bo `BackupManager` bral prek vmesnika na ravni domene, so lahko DAO-ji in entitete `internal` v `:core:data`.
- **Moduli po funkcijah** (`:feature:home`, `:feature:stats` ...), ko bo več razvijalcev ali ko bo gradnja počasna. Pri eni osebi in ~170 datotekah bi to prineslo več režije kot koristi.
