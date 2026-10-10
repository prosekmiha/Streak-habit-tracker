# Časovne seje

Seja (meditacija, branje, vadba ...) teče neodvisno od zaslona. Preživi zapustitev aplikacije, zaklenjen zaslon, smrt procesa in tudi ponovni zagon naprave.

## Arhitektura

| Del | Vloga |
|---|---|
| `domain/session/ActiveSession` | Čisto stanje seje in izračun časa (unit testi v `ActiveSessionTest`). |
| `data/session/SessionStore` | Shrani sejo v DataStore `active_session` ob vsaki spremembi. Hrani tudi zadnji izbrani zvok. |
| `session/SessionController` | Edini vir resnice (singleton). Ob vsaki spremembi uskladi obvestilo, alarm za cilj in zvok. |
| `session/SessionNotifier` | Trajno obvestilo s štoparico in dejanji Premor/Nadaljuj ter Končaj, poleg tega opozorilo ob cilju. |
| `session/SessionReceiver` | Obdela dejanja iz obvestila in alarm za cilj. |
| `session/AmbientService` + `AmbientSynth` | Ambientni zvok kot foreground storitev `mediaPlayback`, ki teče le med predvajanjem. |
| `shortcuts/ShortcutPublisher` | Dinamične bližnjice "Začni sejo" za časovne navade. |

Zaslon seje, pasica na domačem zaslonu, obvestilo in bližnjice so le različni pogledi na isti `SessionController`.

## Merjenje časa

Vsak trenutek se zapiše z dvema urama: `elapsedRealtime`, ki je monotona in neodvisna od spremembe ure ali časovnega pasu, ter stensko uro. Poleg tega se zapiše `Settings.Global.BOOT_COUNT`.

- **Brez ponovnega zagona** se čas meri z monotono uro, zato sprememba ure ali DST nanj ne vpliva.
- **Po ponovnem zagonu** (spremenjen boot count) se uporabi stenska ura. Rezultat nikoli ni negativen, tudi če je bila ura vmes premaknjena nazaj.
- **Pavza** prišteje tekoči del k `accumulatedMs`.

Ob koncu se zapiše `HabitSession` (zgodovina je v podrobnostih navade). Pri časovnih navadah se trajanje prišteje k današnjemu napredku. Seje, krajše od 10 s, se ne shranijo.

## Obnova

- **Zagon procesa:** `StreakApplication` pokliče `restore()`, ki obnovi obvestilo in alarm.
- **Ponovni zagon naprave:** `BootReceiver` pokliče `restore()`.
- **Zvok se po obnovi ne zažene sam**, da ob ponovnem zagonu telefona ne zaigra nepričakovano. Znova zaigra, ko uporabnik odpre sejo ali jo nadaljuje iz obvestila.
- Ob posodobitvi aplikacije sistem pobriše obvestila in alarme. Obnovijo se ob naslednjem zagonu aplikacije.

## Opozorilo ob cilju

Opozorilo ob cilju sproži prvi od treh virov:

1. **Odprt zaslon seje** zazna cilj sam (obvestilo z zvokom, uporabno z zaprtimi očmi).
2. **Med predvajanjem zvoka** proces živi, zato `AmbientService` sproži cilj točno.
3. **Alarm** `ELAPSED_REALTIME_WAKEUP`: z dovoljenjem `SCHEDULE_EXACT_ALARM` je natančen, brez njega pa ga sistem lahko zamakne. Ko dovoljenje manjka, zaslon seje ponudi bližnjico do sistemske nastavitve. Po vrnitvi se alarm prerazporedi.

Opozorilo se pokaže le enkrat na sejo (`goalNotified`).

## Ambientni zvoki

Vsi zvoki so generirani v realnem času na napravi (44,1 kHz, mono, `AudioTrack` float), brez zvočnih datotek in brez omrežja:

| Zvok | Sestava |
|---|---|
| Rjavi šum | Integriran beli šum, mehko nizkoprepustno filtriran. |
| Dež | Visokoprepustni roza šum s počasnim nihanjem jakosti in naključnimi kapljami. |
| Valovi | Rjavi in roza šum z ovojnico vala (7–12 s, hiter dvig, počasen odtok) in filtrom, ki sledi valu. |
| Ogenj | Nizko bobnenje, rahlo šumenje in prasketanje v skupinah. |

Zvok se vklopi v 3 s in izklopi v 1,2 s, med zvoki se pretaplja 1,5 s. Mehka omejitev prepreči pokanje. Audio focus deluje takole: ob trajni izgubi se zvok izklopi, ob začasni pa utiša. `AmbientVoiceTest` preverja glasnost, odsotnost NaN in rezanja ter značaj zvokov (dež svetlejši od rjavega šuma, valovi z utripom).

## Bližnjice

- **Statična:** "Nova navada" (`res/xml/shortcuts.xml`).
- **Dinamične:** do 3 "Začni sejo: <navada>" za časovne navade, posodobljene ob vsaki spremembi navad. Uporaba se sporoči launcherju (`reportShortcutUsed`).
- Pripete bližnjice izbrisanih ali arhiviranih navad se onemogočijo.

Hkrati lahko teče le ena seja. Če uporabnik začne drugo, ga dialog vpraša, ali naj odpre tekočo sejo ali jo shrani in začne novo.
