# Streak — habit tracker

Offline-first sledilnik navad z dnevnimi opomniki, koledarjem in vizualnimi statistikami.
Zgrajen v Kotlinu + Jetpack Compose (Material 3), arhitektura MVVM + Clean.

## Funkcionalnosti
- **Danes**: krožni progress ring, kartice navad s pikami zadnjih 7 dni in odkljukanjem, današnje naloge
- **Koledar**: mesečni grid, dnevi obarvani po deležu opravljenih navad
- **Statistike**: trenutni/rekordni streak, mesečni %, tedenski stolpčni graf, letni heatmap (GitHub-style)
- **Več**: ime, preklop teme (sistem/svetla/temna), urejanje in brisanje navad, izvoz v JSON, placeholder za oblak
- **Dodajanje**: navada (ikona, barva, frekvenca, cilj/teden, opomnik z uro) in naloga (datum, ura, prioriteta, ponavljanje z RRULE)
- **Opomniki**: AlarmManager dnevno; če navada ni opravljena, escalation po 30 in 60 min (tiho → zvok → persistent)
- **Dnevni reset**: WorkManager ob polnoči označi včerajšnje neopravljene navade kot MISSED in pripravi jutranji briefing
- Ob ponovnem zagonu telefona (BOOT_COMPLETED) se alarmi znova razporedijo

## Zagon
1. Odpri mapo v **Android Studio** (Ladybug ali novejši).
2. Počakaj na Gradle sync (prvič potegne odvisnosti).
3. Zaženi na emulatorju ali napravi z **API 33+** (zaradi `POST_NOTIFICATIONS`; aplikacija deluje že od API 26).
4. Ob prvem zagonu se vstavijo 3 vzorčne navade z zgodovino, da je UI takoj poln.

> Iz ukazne vrstice: `./gradlew assembleDebug` (potrebna sta JDK 17+ in internet za prvi sync).

## Potrebna dovoljenja
- `POST_NOTIFICATIONS` — prikaz opomnikov (API 33+, vpraša ob zagonu)
- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` — točni dnevni alarmi
- `RECEIVE_BOOT_COMPLETED` — obnovitev alarmov po ponovnem zagonu
- `VIBRATE` — vibracija pri opomniku

Za testiranje opomnikov nastavi uro opomnika nekaj minut v prihodnost; za escalation pusti navado neoznačeno.

## Arhitektura
```
ui (Compose, ViewModeli s StateFlow)
   └── domain (modeli, repository vmesniki, use-case-i: streak, toggle, export)
        └── data (Room baza – edini vir resnice, DataStore, Supabase stub)
```
Izračun streaka je v `domain/usecase/ComputeStreakUseCase.kt`:
DONE poveča verigo, MISSED jo prekine, SKIPPED je ne prekine; pri WEEKLY/CUSTOM šteje tedenski cilj.

## Priklop Supabase (kasneje)
1. Dodaj odvisnost `io.github.jan-tennert.supabase:postgrest-kt` (+ ktor client) v `app/build.gradle.kts`.
2. Implementiraj `data/remote/SupabaseRemoteDataSource.kt` (trenutno prazni stubi z TODO):
   inicializiraj `SupabaseClient` (Project URL + anon key) in napiši `push*/pull*` klice na tabeli `habits` in `tasks`.
3. V repozitorijih po lokalnem zapisu pokliči `RemoteDataSource`, ko je sinhronizacija vklopljena (stikalo v Nastavitvah).
4. UI sloja ni treba spreminjati — vse teče preko vmesnika `RemoteDataSource`.

## Struktura
- `data/local` — Room entitete, DAO-ji, baza, seed
- `data/prefs` — DataStore (ime, tema, sync)
- `domain` — modeli, use-case-i, vmesniki
- `notifications` — AlarmScheduler, ReminderReceiver, BootReceiver, NotificationHelper
- `work` — DailyResetWorker + scheduler
- `ui/*` — ekrani + ViewModeli, `ui/components` ročno risane komponente (Canvas grafi)
