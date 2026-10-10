# Obvestila in alarmi

Vsa obvestila nastanejo na napravi. Ni strežnika, ni push obvestil in nobeni podatki ne zapustijo telefona.

## Vrste obvestil

| Obvestilo | Kanal | Kdaj | Pogoj |
|---|---|---|---|
| Opomnik navade | `reminders` (visoka pomembnost) | Ob vsakem nastavljenem času navade (do 8 na navado). | Navada je danes na sporedu in še ni opravljena (`PENDING` ali `PARTIAL`). Preskok, zamrznitev in dopust opomnika ne sprožijo. |
| Odlog | `reminders` | 15 min po pritisku "Čez 15 min". | Enako kot opomnik. |
| Jutranji povzetek | `briefing` (nizka) | Ob nastavljeni uri, privzeto 08:00. | Danes čaka vsaj ena navada. |
| Večerni opomnik | `nudge` (privzeta) | Ob nastavljeni uri, privzeto 20:00. | Vsaj ena navada je še neopravljena. Našteje do 3 imena in "še N". |
| Seja | `session`, `session_alerts` | Glej [sessions.md](sessions.md). | |

Dejanja opomnika:

- **"Opravljeno":** navado označi kot opravljeno brez odpiranja aplikacije. Pri časovnih navadah je namesto tega dejanje **"Začni sejo"**.
- **"Čez 15 min":** opomnik odloži za 15 minut.

Opomniki so združeni v skupino.

**Samodejno pospravljanje.** `ReminderDispatcher.syncWithProgress` opazuje napredek. Ko je navada opravljena kjerkoli (v aplikaciji, iz obvestila ali s sejo), se njen opomnik umakne. Ko je opravljeno vse, se umakne tudi večerni opomnik.

## Pravila (`domain/notify/NotificationRules`, unit testi)

- **Naslednja pojavitev ure** se izračuna v trenutnem časovnem pasu. Če ura zaradi premika na poletni čas ne obstaja, se premakne naprej (02:30 → 03:30). Ob ponovitvi ure se uporabi prva pojavitev, zato se opomnik ne sproži dvakrat.
- **Tihe ure** lahko prečkajo polnoč (22:00–07:00). Opomniki med tihimi urami pridejo brez zvoka, povzetek in večerni opomnik pa se ne pošljeta.
- **Dnevna omejitev** (brez, 3, 5 ali 10) velja za opomnike, povzetek in večerni opomnik. Števec (`NotificationLog`) se ponastavi z logičnim dnem.
- **"Dan" je logični dan.** Opomnik ob 01:00 pri nastavitvi "dan se konča ob 04:00" spada v prejšnji dan.

## Zasebnost

- Na zaklenjenem zaslonu je vedno prikazana javna različica brez imen navad (`VISIBILITY_PRIVATE` + `publicVersion`).
- Z nastavitvijo **"Skrij imena navad"** so imena skrita tudi v odklenjenem stanju: opomnik prikaže "Čas za navado", večerni opomnik pa le število.

## Razporejanje (`AlarmScheduler`)

- **Enkratni alarmi:** vsak alarm je enkraten in ob sprožitvi razporedi naslednjega. `RTC_WAKEUP` se uporablja za opomnike, povzetek in večerni opomnik, natančen alarm pa tudi za mejo logičnega dne.
- **Natančnost:** če je dovoljenje `SCHEDULE_EXACT_ALARM` dano, se uporabi `setExactAndAllowWhileIdle`. Brez dovoljenja se uporabi `setAndAllowWhileIdle`, ki deluje tudi v načinu Doze, vendar ga sistem lahko zamakne za nekaj minut.
- **Ponovno razporejanje** (`rescheduleAll`):
  - ob vsakem zagonu procesa, ker sistem alarme pobriše ob prisilni ustavitvi;
  - ob dogodkih, ki jih prejme `RescheduleReceiver`: `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED` in `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`;
  - ob spremembi nastavitev (meja dneva, ura povzetka ali večernega opomnika);
  - ob shranjevanju ali arhiviranju navade.

## Dovoljenja in baterija (Profil → Obvestila)

- **Dovoljenje za obvestila** (Android 13+): zahteva se v onboardingu. Če je trajno zavrnjeno, gumb odpre sistemske nastavitve.
- **Natančni opomniki** (Android 12+): `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. Ko uporabnik dovoljenje vklopi, se alarmi takoj prerazporedijo kot natančni. `USE_EXACT_ALARM` se ne uporablja, ker ga Play dovoli le budilkam in koledarjem.
- **Baterija:** povezava na seznam optimizacij baterije (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`). Neposredna zahteva za izjemo se ne uporablja, ker jo Play omejuje.
- **Obrazec navade** ob opomnikih opozori, če so obvestila izklopljena ali natančni alarmi niso dovoljeni.

## Preverjanje na emulatorju

```
adb shell dumpsys alarm | grep -A3 com.streak.app        # razporejeni alarmi in okno natančnosti
adb shell dumpsys notification --noredact | grep streak   # prikazana obvestila
```
