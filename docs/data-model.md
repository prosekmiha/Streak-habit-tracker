# Podatkovni model (Room v4)

Shema je izvožena v `core/data/schemas/com.streak.app.data.local.StreakDatabase/4.json` (starejše verzije so v isti mapi).

## Tabele

| Tabela | Namen | Ključ / indeksi |
|---|---|---|
| `habits` | Navada: naslov, opis, ikona, barva, kategorija, tip cilja (`YES_NO`/`QUANTITY`/`DURATION`/`QUIT`) s ciljno vrednostjo in enoto, urnik (`DAILY`/`WEEKDAYS`/`TIMES_PER_WEEK`/`TIMES_PER_MONTH` + bitna maska dni + X na obdobje), del dneva, začetek/konec, `archivedAt` | PK `id`, indeks `archivedAt` |
| `habit_reminders` | Več opomnikov na navado (minuta v dnevu, vklopljen) | FK → habits (CASCADE), indeks `habitId` |
| `completions` | En zapis na navado in logični dan: status (`DONE`/`PARTIAL`/`SKIPPED`/`FROZEN`/`RELAPSE`), vrednost (pri `RELAPSE` število zdrsov), UTC trenutek vpisa, zamik pasu ob vpisu, opomba, `manuallyEdited` (backfill) | FK → habits (CASCADE), unikatno (`habitId`, `epochDay`), indeks `epochDay` |
| `sessions` | Zaključene časovne seje | FK → habits (CASCADE), indeksa `habitId`, `epochDay` |
| `streak_cache` | Predpomnjen streak na navado + dan, za katerega velja | PK/FK `habitId` |
| `daily_aggregates` | Načrtovane/opravljene navade po dnevih (koledar, heatmap) | PK `epochDay` |
| `vacations` | Obdobja dopusta (veljajo za vse navade) | PK `id` |
| `tasks` | Naloge: rok (datum, neobvezna ura), prioriteta, ponavljanje (preprost RRULE), opravljeno; od v4 še `reminders` (opomniki, kodirani z `TaskReminder.encode`, npr. `B60;D1@1200;R30@540-1260`) | PK `id` |
| `mood_entries` | Razpoloženje na logični dan (od v3): razpoloženje 1–5, neobvezna energija 1–5, opomba, trenutek vpisa | PK `epochDay` |

## Ključna pravila

- **"Zgrešeno" se ne shranjuje.** Stanje dneva (`DayState`) izpelje `DayEvaluator` iz urnika, zapisa, dopusta in trajanja navade. Sprememba urnika ali zamik ure zato ne pustita napačnih zapisov.
- **Logični dan:** `LogicalDay` z nastavitvijo "dan se konča ob" (00:00–06:00). Vpis ob 01:30 pri meji 04:00 pripada prejšnjemu dnevu. Meja je definirana z `ZonedDateTime`, zato so DST dnevi (23 h / 25 h) in DST "luknje" obravnavani dosledno.
- **Potovanja:** vsak zapis hrani lokalni datum (vir resnice za streak), UTC trenutek in zamik pasu ob vpisu, zato se zgodovina ob menjavi pasu ne premakne.
- **Prihodnji dnevi** so blokirani (`ToggleHabitUseCase` vrne `FUTURE_BLOCKED`). Vpis za pretekli dan je označen z `manuallyEdited`.
- **Skoki ure:** `ClockSanity` zazna velik skok naprej (> 3 dni) ali kakršenkoli skok nazaj. Opozorilo prikaže UI (M2); podatki se nikoli ne brišejo.
- **Arhiv namesto brisanja:** arhivirana navada izgine s seznama, zgodovina ostane. V dnevnih povzetkih šteje do dneva arhiviranja.

## Izračun streaka (`ComputeStreakUseCase`)

- **Dnevni urniki** (enota dan): `DONE` podaljša verigo (tudi na dan izven urnika). `SKIPPED`, `FROZEN`, dopust, dnevi izven urnika ali trajanja navade so nevtralni. Načrtovan pretekli dan brez `DONE` (tudi `PARTIAL`) verigo prekine. Današnji dan je "v teku", dokler ne mine.
- **Opuščanje** (`QUIT`, vedno dnevno): vsak aktiven dan brez zapisa je čist (`DONE`), tudi današnji in med dopustom. Zdrs (`RELAPSE`) verigo prekine. `totalDone` je število čistih dni; ti prinesejo XP (`GameDao.observeCleanDays`) in štejejo v popolne dni. Dotik kartice ali widgeta zdrsa ne vpiše, vpis je mogoč le v listu z dejanji.
- **Obdobni urniki** (enota teden/mesec, prvi dan tedna iz nastavitev): obdobje je izpolnjeno pri ≥ zahtevanem številu `DONE` dni. Zahtevano število se sorazmerno zmanjša za nevtralne dni, zamrznitev pa zaščiti celo obdobje.
- **Delež:** zadnjih 30 dni (opravljeni / opravljeni + zgrešeni) oziroma zadnja 4 zaključena obdobja.
- Na popolnih zgodovinah se ujema s starim izračunom (`StreakEquivalenceTest`). Edina namerna razlika je, da dan brez zapisa zdaj prekine tudi rekord.

## Predpomnilnik in povzetki (`StreakRecalculator`)

| Dogodek | Posodobitev |
|---|---|
| Vpis/izbris zapisa | Streak te navade + povzetek tega dne |
| Sprememba/arhiv/izbris navade | Streak navade + povzetki od začetka navade do danes |
| Menjava logičnega dne (alarm na meji dne, sistemski broadcast ob spremembi ure/pasu, zagon) | Zastareli streaki + povzetki od zadnjega izračuna |
| Dopust, "Preračunaj vse" | Vse |

Domači zaslon bere le predpomnjene streake in zapise zadnjih 35 dni. Podrobnosti posamezne navade berejo celotno zgodovino le te navade.

## Migracije in varnost podatkov

- v1 ni imela izvožene sheme in produkcijskih podatkov, zato je prehod **1 → 2 edini dovoljeni destruktivni prehod** (`fallbackToDestructiveMigrationFrom(1)`). Splošnega destruktivnega fallbacka ni.
- **2 → 3** doda tabelo `mood_entries` (`MIGRATION_2_3`). `DatabaseMigrationTest` jo preveri z dejanskimi podatki prek `MigrationTestHelper` (sheme so testni asset; v Robolectricu je treba podati absolutno pot baze).
- **3 → 4** doda stolpec `tasks.reminders` (`TEXT NOT NULL DEFAULT ''`, `MIGRATION_3_4`). Opomniki so vezani na nalogo in se ne poizvedujejo ločeno, zato niso v svoji tabeli.
- Vsaka nova verzija mora dodati migracijo v `StreakDatabase.MIGRATIONS`. `DatabaseMigrationTest` preveri, da migracija obstaja za vsak korak in da je shema izvožena.
- `DatabaseGuard` pred vsako migracijo kopira bazo (+ WAL) v `no_backup/db-backups` in obdrži zadnje 3 kopije. Odpre bazo v ozadju in ob napaki obnovi kopijo. Enkrat na 7 dni, 10 s po zagonu, izvede `PRAGMA quick_check`. Stanje (`DatabaseHealth`) bo UI prikazal v M2/M5 s ponudbo uvoza varnostne kopije.
