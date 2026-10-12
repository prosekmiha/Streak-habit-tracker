# Varnost in zasebnost

Streak deluje brez povezave: brez računa, strežnika, oglasov in analitike. Aplikacija nima dovoljenja `INTERNET`, zato podatki tehnično ne morejo zapustiti naprave drugače kot prek dejanj uporabnika (izvoz, deljenje) ali Androidove varnostne kopije.

## Baza

- **Brez lastnega šifriranja:** baza (`streak.db`) je navadna SQLite baza v zasebnem prostoru aplikacije. Ščiti jo Androidov peskovnik aplikacij in šifriranje shrambe naprave.
- **Starejše šifrirane baze:** različice s SQLCipher so bazo šifrirale. Takšna baza se ob prvem zagonu odmakne (`streak.db.encrypted-*`), začne se s prazno, aplikacija pa ponudi obnovo iz samodejnega posnetka (`LegacyEncryptedDatabase`). Ostanki ključa (`no_backup/db.key`, Keystore `streak_db_wrap`) se izbrišejo.
- **Kopija pred migracijo** (`DatabaseGuard`) se naredi pred vsako nadgradnjo sheme.

## Zasebni zaslon

- **Stikalo:** Profil → Zasebni zaslon, privzeto izklopljeno.
- **Učinek:** `FLAG_SECURE` in `setRecentsScreenshotEnabled(false)` preprečita posnetke zaslona in skrijeta vsebino v pregledu odprtih aplikacij.
- **Brez zaklepa s kodo:** aplikacija nima lastnega zaklepa (koda ali biometrija). Za zaščito skrbi zaklep zaslona telefona.

## Varnostne kopije

| Pot | Vsebina | Zaščita |
|---|---|---|
| Ročni izvoz (JSON) | Vse navade, zapisi, seje, dopusti, naloge in nastavitve brez varnostnih podatkov | Neobvezno geslo: PBKDF2-HMAC-SHA256 (600.000 iteracij) in AES-256-GCM. GCM zazna napačno geslo in vsako spremembo datoteke. |
| CSV | Zapisi za preglednice | Brez šifriranja. Polja, ki bi jih preglednica izvedla kot formulo, so zaščitena. |
| Androidova varnostna kopija | `files/snapshot/latest.json` in nastavitve | Google Drive kopija je na Androidu 9+ šifrirana z zaklepom zaslona naprave. Posnetek je mogoče izklopiti (Podatki → Androidova varnostna kopija). |

- **Česa ni v Androidovi varnostni kopiji:** baze, tekoče seje in dnevnika obvestil. Pravila (`data_extraction_rules.xml`, `backup_rules.xml`) vključujejo le navedeni datoteki.
- **Uvoz** preveri obliko, verzijo in sklice. Neznana polja prezre, zapise brez navade izpusti, iteracije PBKDF2 pa so omejene, da tuja datoteka ne more "zamrzniti" naprave. Zamenjava podatkov poteka v eni transakciji (vse ali nič).
- **Prazna baza nikoli ne prepiše posnetka.** Po namestitvi na nov telefon se tako kopija ne izgubi, aplikacija pa obnovo ponudi sama.

## Obvestila

- **Zaklenjen zaslon:** vedno prikaže javno različico brez imen navad.
- **"Skrij imena navad":** imena skrije tudi v odklenjenem stanju.

## Gradnja

- **R8:** release uporablja R8 (`isMinifyEnabled`, `isShrinkResources`). Pravila za Filament (JNI) in obliko varnostne kopije so v `app/proguard-rules.pro`.
- **Preverjeno na emulatorju z R8 buildom:** zagon, 3D maskota in izvoz JSON z nespremenjenimi imeni polj.
- **Dovoljenja:**
  - `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`;
  - `FOREGROUND_SERVICE` in `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, samo med ambientnim zvokom.
