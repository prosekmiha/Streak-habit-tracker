# Lokalizacija

Streak podpira 24 uradnih jezikov EU. **Angleščina je privzeta** (`values/`), ostali jeziki so v `values-xx/`.

## Postopek

Nizi se ne urejajo v XML, ampak v izvornih datotekah `tools/i18n/<jezik>.txt`:

```
kljuc=besedilo
P kljuc=one:%d navada | two:%d navadi | few:%d navade | other:%d navad
A kljuc=prvi | drugi
```

`python tools/i18n/build.py` iz njih ustvari `res/values*/strings.xml` in preveri vsak jezik glede na `en.txt`:
- **ključi:** manjkajoči ali odvečni;
- **formatni argumenti:** enaki kot v izvirniku (`%d`, `%1$s`, `%%`);
- **množine:** natanko tiste kategorije, ki jih jezik potrebuje (tabela `PLURALS`, po CLDR);
- **nizi in seznami:** prazni nizi in napačno število elementov.

`--check` le preveri. Ubežne znake (`'`, `"`, `&`, `@` na začetku) doda skripta.

Nov niz:
1. Dodaj ga v `en.txt` in v vse ostale jezike.
2. Zaženi `build.py`.
3. Zaženi `./gradlew lintDebug`, ki preveri `MissingTranslation` in `MissingQuantity`.

## Pravila za prevode

- **Nevtralnost spola:** brez preteklika v 2. osebi ("si opravil/a") in pridevnikov, ki določajo spol uporabnika. Uporabljaj sedanjik, velelnik ali brezosebne oblike ("Kako je šlo?", "ki danes še niso opravljene"). Velja za vse jezike s slovničnim spolom (sl, hr, cs, sk, pl, bg, lt, lv, ro, fr, es, it, pt, el, mt, ga).
- **Tikanje:** neformalni nagovor povsod, kjer je v jeziku običajen za aplikacije (de "du", fr "tu", nl "je", …).
- **Brez nazivov nivojev:** samo "Nivo 5", ker bi bili nazivi spolno zaznamovani.
- **Množine:** vsako število pred samostalnikom gre v `<plurals>`, tudi če se v izvirniku ne spreminja. Kategorija `one` sme izpustiti število le, kadar v jeziku zajema samo 1 (npr. angleško "day in a row").
- **Imena navad, enote in opombe** so uporabniški podatki in se ne prevajajo.
- **Čas in datumi** se oblikujejo z `java.time` po jeziku naprave. Izjema sta "9:00" in "18:00" v izzivih, ki sta zapisana v nizu in ju prevod prilagodi zapisu jezika.

## Slovar

| en | sl | Opomba |
|---|---|---|
| habit | navada | |
| streak | veriga (v besedilu), "Streak" (ime aplikacije, widget) | |
| check off / mark as done | označi kot opravljeno | |
| skip / freeze / vacation | preskoči / zamrzni / dopust | ne prekinejo verige |
| session | seja | časovna seja z merilnikom časa |
| badge / level / XP | značka / nivo / XP | XP se ne prevaja |
| daily challenge | izziv dneva | |
| backup / restore | varnostna kopija / obnovi | |
| perfect day | popoln dan | vse načrtovane navade opravljene |

## Izbira jezika

- **Profil → Jezik:** "Jezik sistema" ali enega od 24 jezikov, razvrščenih po izvornem imenu (`Collator`). Pod imenom je ime v trenutnem jeziku.
- **Android 13+:** `LocaleManager`. Izbira je vidna tudi v sistemskih nastavitvah aplikacije (`generateLocaleConfig`).
- **Starejši Androidi:** izbira se shrani v SharedPreferences, `AppLocale.wrap` pa ovije Context aplikacije in aktivnosti.
- **AAB:** razdelitev po jezikih je izklopljena (`bundle.language.enableSplit = false`), da so ob izbiri v aplikaciji vsi jeziki nameščeni.

## Preverjanje (QA)

- [ ] `python tools/i18n/build.py` brez težav
- [ ] `./gradlew lintDebug` brez `MissingTranslation`, `MissingQuantity`, `PluralsCandidate`
- [ ] **Psevdojezik en-XA** (debug, Nastavitve → Sistem → Jeziki): dolgi nizi se ne prekrivajo (gumbi, čipi, tedenski trak, widgeti)
- [ ] **Psevdojezik ar-XB:** postavitev od desne proti levi ne razpade
- [ ] **Pisave:** grščina (el) in cirilica (bg) se izrišejo z naslovno pisavo (Roboto Flex pokriva oba)
- [ ] **Tedenski trak:** kratka imena dni so skrajšana na največ 4 znake (npr. lv "pirmd." → "Pirm")
- [ ] **Množine** za 1, 2, 3, 5, 11, 21, 101 v sl, pl, cs, lt, lv, ga, mt
- [ ] **Pregled domačih govorcev:** vsi prevodi razen sl in en so strojno pripravljeni brez pregleda. Posebej ga (irščina) in mt (malteščina).
