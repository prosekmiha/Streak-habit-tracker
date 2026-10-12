# Igrifikacija

XP, nivoji, značke in dnevni izzivi. Vse je **izpeljano iz zapisov** (`domain/gamification/Gamification.kt`, pokrito s testi), v bazi ni nobene nove tabele. To ima tri posledice:

- **Ni "kmetovanja":** odznačitev navade vrne njen XP, zato ga ni mogoče nabirati z odznačevanjem in ponovnim označevanjem.
- **Obnova:** po obnovi varnostne kopije je stanje enako kot prej.
- **Dodani podatki:** naknadni vpisi in uvoz štejejo enako kot sprotni vpisi (razen pri zgodnjih vpisih, glej spodaj).

## XP in nivoji

| Vir | XP |
|---|---|
| Opravljena navada (stanje `DONE`) | +10 |
| Popoln dan (vse načrtovane navade opravljene) | +20 |
| Opravljen izziv dneva | +30 |
| Seja: vsakih polnih 5 minut | +2 (največ 24 na sejo, tj. 1 ura) |

- **Prag nivoja n:** `50 · n · (n − 1)`. To pomeni: 2 → 100 XP, 3 → 300, 5 → 1000, 10 → 4500. Pri štirih navadah na dan je nivo 5 dosegljiv v približno treh tednih.
- **Nivoji nimajo nazivov:** samo "Nivo 5". Nazivi bi bili v slovenščini spolno zaznamovani.

## Dnevni izziv

Izziv je izbran po datumu (deterministično, nikoli isti dva dni zapored) in je izpolnljiv za vsakogar z vsaj eno navado:

| Izziv | Pogoj |
|---|---|
| Popoln dan | Vse današnje navade opravljene |
| Zgodnji začetek | Navada opravljena pred 9:00 |
| Trenutek premisleka | Opomba k opravljeni navadi |
| Prost večer | Vse opravljeno do 18:00 |

- **Lokalni čas vpisa:** "pred 9:00" in "do 18:00" uporabljata lokalni čas vpisa (UTC trenutek + zamik pasu ob vpisu). Štejeta le vpisi na isti koledarski dan, brez naknadnih vpisov.
- **Logični dan:** vpis ob 1:00 za prejšnji logični dan (pri "dan se konča ob 4:00") zato ni zgodnji.

## Značke (16)

- **Prvi korak**, **Sto kljukic** (100), **Tisoč kljukic** (1000): opravljene navade.
- **Teden / Mesec zapored, Stotica, Celo leto:** najdaljša veriga katerekoli navade (7 / 30 / 100 / 365 dni). Tedenska veriga šteje 7 dni na teden, mesečna 30.
- **Popoln dan**, **Popoln teden:** sedem popolnih dni zapored. Dnevi brez načrtovanih navad verige ne prekinejo.
- **Prva seja**, **Deset ur zbranosti:** čas v sejah.
- **Zgodnja ptica:** 10 navad pred 9:00.
- **Premislek:** 10 opomb.
- **Deset izzivov:** 10 opravljenih izzivov.
- **Nivo 5**, **Nivo 10**.

Neosvojene značke kažejo napredek (npr. "2 / 7"). Osvojeno značko je mogoče deliti kot sliko.

## Praznovanja in nabiralnik

`GameEvents` spremlja izračunano stanje in ga primerja s tem, kar je uporabnik že videl (`GameMemoryRepository` → `data/prefs/GameDataStore`, DataStore `gamification`):
- **Nova značka ali nivo:**
  - zapis v nabiralnik (zvonec na domačem zaslonu, pika ob neprebranih);
  - celozaslonsko praznovanje z maskoto. Značke verig samo v nabiralniku, ker mejnik verige že praznuje domači zaslon.
- **Opravljen izziv dneva:** zapis v nabiralnik in kratko sporočilo na dnu zaslona.
- **Brez ponavljanja:** ena značka se praznuje enkrat. Če uporabnik odznači navado in jo znova označi, se praznovanje ne ponovi.
- **Uvoz, obnova in brisanje:** potekajo v tihem načinu (`GameEvents.quietly`). Novo stanje se le zapomni, zato po obnovi ni plazu praznovanj za že osvojene značke.
- **Androidova varnostna kopija:** `GameDataStore` je vključen vanjo (`data_extraction_rules.xml`), tako da na novem telefonu praznovanja ostanejo enkratna.

## Deljenje

`ShareCard` nariše sliko 1080 × 1350 (maskota, značka, naslov, opis) na Canvas in jo deli prek FileProviderja (`cache/exports`) skupaj z besedilom. Enako deluje za mejnike verig, ki so prej delili le besedilo.

## Misel dneva

16 kratkih misli v `daily_thoughts` (izvirne, razen ene navedene). Vsak dan je druga in je enaka za ves dan.

## Zmogljivost

- **Poizvedbe:** `GameDao` vrača eno vrstico na dan (`GROUP BY epochDay`), ne vseh zapisov. Pri petih letih zapisov je to približno 1800 vrstic.
- **Izračun:** poteka na `Dispatchers.Default` in se deli med zasloni (`shareIn`).
