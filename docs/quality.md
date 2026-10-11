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
