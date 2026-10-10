# Streak — sledilnik navad

Sledilnik navad, ki deluje popolnoma brez povezave: brez računov, strežnikov, oglasov in analitike. Vsi podatki ostanejo na napravi.
Zgrajen je v Kotlinu z Jetpack Compose (Material 3), arhitektura je MVVM + Clean, DI pa Hilt.

## Funkcionalnosti
- **Onboarding**: dobrodošlica z maskoto, ime, dovoljenje za obvestila, izbira predlog navad.
- **Domov**: pozdrav glede na del dneva, tedenski trak (dotik dneva omogoči naknadni vpis), mreža pastelnih kartic.
  - Dotik kartico označi; pri količinskih navadah doda +1.
  - Dolg pritisk odpre meni: preskoči, zamrzni, opomba, uredi, arhiviraj.
- **Navade**:
  - Cilj je lahko da/ne, količina z enoto ali trajanje.
  - Urnik je lahko vsak dan, izbrani dnevi ali X-krat na teden oziroma mesec.
  - Druge nastavitve: kategorija, del dneva, več opomnikov, začetek in konec, arhiv.
- **Streak**: preskoki, zamrznitve in dopust verige ne prekinejo. Nastaviti je mogoče "dan se konča ob" (npr. 04:00 za pozne ure) in prvi dan tedna.
- **Statistika**:
  - Pregled: tedenski povzetek in mesečni koledar z naknadnim vpisom.
  - Navade: trenutni streak in rekord, skupno število, uspešnost, najboljši dan v tednu, zadnjih 8 tednov, heatmap.
- **Seje**: časovne seje, ki tečejo tudi v ozadju, z obvestilom s štoparico (Premor/Končaj), opozorilom ob cilju, ambientnimi zvoki, generiranimi na napravi (rjavi šum, dež, valovi, ogenj), in zgodovino v podrobnostih navade. Bližnjice na ikoni aplikacije omogočajo hiter začetek seje ali nove navade.
- **Obvestila**:
  - Opomniki ob več časih na navado pridejo le, če navada danes še ni opravljena. Iz obvestila jo lahko označiš kot opravljeno ali jo odložiš za 15 min.
  - Jutranji povzetek in večerni opomnik z neopravljenimi navadami; večerni se umakne, ko je vse opravljeno.
  - Tihe ure, dnevna omejitev in skrivanje imen. Zaklenjen zaslon nikoli ne pokaže imen navad.
  - Zaslon s stanjem dovoljenj (obvestila, natančni alarmi, baterija). Alarmi se prerazporedijo po ponovnem zagonu, posodobitvi ter spremembi ure ali časovnega pasu.
- **Naloge**: enkratna opravila z rokom, prioriteto in ponavljanjem.
- **Profil**: ime, tema, animacije, prvi dan tedna, "dan se konča ob", obvestila, arhivirane navade, izvoz JSON in "Preračunaj vse".

## Zagon
1. Odpri mapo v **Android Studio**.
2. Zaženi na emulatorju ali napravi (minSdk 26, targetSdk 36).

Iz ukazne vrstice:

```
./gradlew assembleDebug          # gradnja
./gradlew testDebugUnitTest      # unit in Robolectric testi
./gradlew lintDebug              # lint
```

## Dokumentacija
- [docs/audit.md](docs/audit.md) — Phase 0 audit, tabela vrzeli, tveganja, fazni načrt
- [docs/data-model.md](docs/data-model.md) — Room shema, logični dan, izračun streaka, migracije
- [docs/alarms.md](docs/alarms.md) — opomniki, povzetek, večerni opomnik, tihe ure, omejitev, razporejanje in dovoljenja
- [docs/sessions.md](docs/sessions.md) — trajne seje, obvestilo, opozorilo ob cilju, ambientni zvoki, bližnjice
- [docs/mascot-pipeline.md](docs/mascot-pipeline.md) — 3D maskota: Blender generator, Filament, fallbacki

## Struktura
- `domain` — modeli, use-case-i (streak, vpis, izvoz), `time` (logični dan, zaznava skokov ure), `streak` (izpeljava stanja dneva)
- `data` — Room (entitete, DAO, `DatabaseGuard`), DataStore nastavitve, repozitoriji, `StreakRecalculator` (predpomnilnik streakov in dnevni povzetki)
- `notifications`, `work` — alarmi, opomniki, menjava logičnega dne
- `session`, `shortcuts` — kontroler seje, obvestilo, ambientna storitev in sintetizator, bližnjice
- `ui` — `onboarding`, `home`, `addhabit`, `tasks`, `addtask`, `stats`, `settings`, `mascot` (3D maskota prek Filamenta + WebP poze, glej `docs/mascot-pipeline.md`), `components`, `theme`
- `tools/fonts` — ponovljiva gradnja fontov (Roboto Flex, Manrope; OFL)
