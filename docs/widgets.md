# Widgeti

Štirje widgeti za domači zaslon, zgrajeni z **Jetpack Glance** (`androidx.glance:glance-appwidget`). Koda je v paketu `widget`, logika izbire in vrstnega reda pa v `domain/widget/WidgetSummary.kt` (z unit testi).

| Widget | Privzeta velikost | Vsebina | Dotik |
|---|---|---|---|
| **Napredek dneva** (`ProgressWidget`) | 2×2 | Obroč "3/5", maskota glede na napredek, "Še 2 navadi" / "Vse opravljeno!" | Odpre aplikacijo (brez navad: nova navada) |
| **Današnje navade** (`HabitsWidget`) | 4×2, poljubno večji | Glava "Danes 3/5" in toliko vrstic, kolikor jih gre v višino | Vrstica: enako kot dotik kartice v aplikaciji |
| **Streak navade** (`StreakWidget`) | 2×2 | Barva navade, trenutna veriga, enota (dni/tedni/meseci), ime, rekord (če je dovolj višine), kljukica, če je danes opravljeno | Odpre podrobnosti navade |
| **Količina** (`QuantityWidget`) | 2×2 | Ime, "3 / 8 kozarcev", vrstica napredka, gumba −1 in +1 | Gumba vpišeta, ostalo odpre podrobnosti |

## Dotik vrstice v widgetu "Današnje navade"

- **da/ne:** opravi oziroma odznači.
- **količina:** +1 (opravljena se odznači).
- **trajanje:** odpre sejo v aplikaciji (časovnik teče v aplikaciji).

Vrstni red sledi domačemu zaslonu:
1. neopravljene po delu dneva (jutro, popoldne, večer, kadarkoli);
2. tedenske/mesečne, ki danes niso nujne;
3. opravljene;
4. preskočene, zamrznjene in dopust.

Ko ni ničesar več za narediti, widget namesto seznama pokaže maskoto in sporočilo:
- "Vse opravljeno!";
- "Danes ni ničesar na sporedu";
- "Dodaj prvo navado".

Vpisi z widgeta gredo skozi isti `ToggleHabitUseCase` kot v aplikaciji in vedno veljajo za današnji logični dan (upošteva "dan se konča ob").

## Zaščita pred skokom ure

Če `ClockSanity` zazna skok datuma, aplikacija pred vpisom vpraša, ali je datum pravilen. Widget tega ne more vprašati, zato v tem stanju vsak dotik samo odpre aplikacijo.

## Izbira navade

Widgeta Streak in Količina ob dodajanju odpreta `WidgetConfigActivity`:
- Količina ponudi le navade s ciljem po količini.
- Izbira se shrani v Glance stanje widgeta (`WidgetKeys.HabitId`).
- Na Androidu 12+ je widget mogoče znova nastaviti (`reconfigurable`).
- Če je navada izbrisana, arhivirana ali spremeni tip cilja, widget pokaže "Tapni in izberi navado".

## Zasebnost

**Profil → Skrij podrobnosti na widgetih:**
- **Današnje navade:** ne pokaže seznama, le število preostalih navad in maskoto.
- **Streak:** namesto ikone in imena navade pokaže plamen.
- **Količina:** ne pokaže imena in ikone.

Aplikacija nima zaklepa, zato widgeti nimajo zaklenjenega stanja.

## Osveževanje

- **Med delovanjem procesa:** `WidgetUpdater.run()` spremlja navade in nastavitve (tema, skrivanje podrobnosti, zadnji znani datum) in po 300 ms mirovanja kliče `updateAll()`. Tudi seja Glance sama spremlja podatke (`collectAsState`), zato je sprememba vidna takoj.
- **Menjava dneva:**
  - `DailyResetWorker` po preračunu streakov osveži widgete;
  - ob vsakem zagonu procesa se widgeti osvežijo s prvo emisijo;
  - zagon procesa sprožijo tudi alarm ob meji dne, obvestila in sprememba ure ali pasu.
- **Brez periodičnega osveževanja:** `updatePeriodMillis=0`, zato widgeti ne praznijo baterije.

Preverjeno na emulatorju (API 36): sprememba časovnega pasu na naslednji dan je osvežila widgete. To je delovalo tudi, ko je bil proces aplikacije prej ubit.

## Videz

- **Barve:** sledijo paleti aplikacije. Pri temi "po sistemu" barvo izbere zaganjalnik (dan/noč), pri izbrani temi je fiksna.
- **Barva navade:** widgeta Streak in Količina uporabljata pastelno barvo navade s temnim besedilom (enak kontrast v obeh temah).
- **Ikone navad:** RemoteViews ne zna izrisati Compose vektorjev, zato `WidgetBitmaps` iste Material ikone kot v aplikaciji nariše v majhne bitne slike (predpomnjene). Obroč napredka je narisan enako.
- **Maskota:** uporabljene so vnaprej zrenderirane WebP poze (`mascot_*`):
  - wave: ni navad;
  - sleep: prost dan;
  - idle: še nič opravljeno;
  - thumbs: delno opravljeno;
  - cheer: vse opravljeno.
- **Zaobljeni robovi:** na Androidu 12+ sistemsko zaobljenje, na starejših zaobljena oblika z barvo.
- **Predogledi v izbirniku:** posnetki pravih widgetov z emulatorja (`drawable-nodpi/widget_*_preview.webp`).

## Dostopnost

- Vsaka vrstica ima opis dejanja, npr. "Označi kot opravljeno: Branje" ali "Dodaj 1: Voda, 2 / 8 kozarcev".
- Gumbi so veliki vsaj 44 dp, vrstice 48 dp.
- Widget napredka bralniku zaslona prebere "Opravljeno 1 od 4. Še 3 navade".

## Ploščici v hitrih nastavitvah

- **Navada** (`HabitTileService`): izbrana navada da/ne se preklopi, količina dobi +1 (ista logika kot widget, `WidgetActions.tap`). Izbrati je mogoče le navade da/ne in količinske; brez izbire dotik odpre aplikacijo.
- **Seja** (`SessionTileService`): med tekočo sejo dotik sejo ustavi ali nadaljuje, sicer odpre zaslon seje izbrane časovne navade (začetek je v aplikaciji zaradi izbire zvoka in spora z drugo sejo).
- Izbira je v Profil → Ploščice v hitrih nastavitvah (`AppSettings.tileHabitId`/`tileSessionHabitId`). Na Androidu 13+ je tam gumb za sistemsko dodajanje ploščice.
- Zasebnost: ploščica je vidna tudi na zaklenjenem zaslonu, zato z nastavitvijo "Skrij imena v obvestilih" pokaže splošen naslov. Na zaklenjeni napravi z zaklepom dejanje najprej zahteva odklep (`unlockAndRun`).
- Osveževanje: `TileUpdater.requestAll()` se kliče ob vsaki osvežitvi widgetov in ob spremembi seje.

## Ni v tem krogu

Veliki widget (teden), widget s časovnikom, citat dneva in widgeti za zaklenjen zaslon (glej fazni načrt v `docs/audit.md`).
