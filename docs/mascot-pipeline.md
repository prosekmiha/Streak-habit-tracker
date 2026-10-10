# Maskota: pipeline in runtime

Maskota je originalen lik (oranžna krogla z "rubber-hose" okončinami, rokavicami in supergami). V celoti jo zgradi skripta v repozitoriju, zato je ponovljiva in jo je mogoče kadarkoli zamenjati z ročno modeliranim GLB z enakimi imeni animacij, brez spremembe kode.

## Generiranje (Blender 4.3, headless)

```
"C:\Program Files\Blender Foundation\Blender 4.3\blender.exe" -b -P tools/mascot/build_mascot.py
# opcije: -- --glb <pot> --renders <mapa> --no-render
python tools/mascot/inspect_glb.py      # proračun: <= 15k trikotnikov, <= 2 MB
python tools/mascot/contact_sheet.py    # pregled vseh poz na eni sliki
```

Skripta naredi naslednje:

1. **Model.** Proceduralno zgradi telo, oči z zenicami, obrvi in pet oblik ust (`smile/open/frown/flat/o`). Usta se preklapljajo s skaliranjem kosti, saj glTF shape keyev ne animira zanesljivo po akcijah. Roke in noge so skinned cevi s smoothstep utežmi. Črna obroba je narejena z metodo inverted hull (backface culling, `doubleSided = false`).
2. **Armatura.** Kosti so `root`, `body`, `eye/brow/arm_up/arm_lo/hand/leg_up/leg_lo/foot` (L/R) in `mouth.*`. Trdni deli so parentani na kosti.
3. **Animacije** (glTF actions): `idle`, `wave`, `cheer`, `sad`, `think`, `sleep`, `meditate`, `thumbs`, `walk`. Vse so zanke.
4. **Izvoz** v `app/src/main/assets/models/mascot.glb`: ~14,8k trikotnikov, ~0,5 MB, brez tekstur. V APK-ju je nestisnjen (`noCompress += "glb"`).
5. **Render poz** z EEVEE: 480×520 WebP s prozornim ozadjem, ena poza na animacijo, shranjene v `res/drawable-nodpi/mascot_*.webp` (~14 KB vsaka).

## Runtime (`ui/mascot/`)

| Datoteka | Vloga |
|---|---|
| `MascotView.kt` | Javni composable `MascotView(mood, modifier, description, hero)`. Preslika stanje v animacijo (`animationFor`) in pozo (`poseDrawable`). |
| `MascotScene.kt` | Minimalni Filament viewer: `TextureView`, prozorno ozadje, gltfio animator s cross-fade 0,35 s, zanka prek Choreographerja. |
| `DeviceCapabilities.kt` | Odloči, ali naprava zmore 3D in ali naj se izris omeji na 30 fps. |
| `MascotMood.kt` | 14 stanj z opisi za bralnike zaslona. |

**Kdaj 3D in kdaj slika.** 3D se izriše le, ko so izpolnjeni vsi pogoji:

- `hero = true`, kar velja le na dobrodošlici (`WAVE`), pri praznovanju mejnika (`CHEER`) in med sejo (`MEDITATING`);
- animacije so nastavljene na `FULL` (sistemski `ANIMATOR_DURATION_SCALE = 0` jih izklopi);
- naprava ima GLES 3.0+, ni označena kot `isLowRamDevice`, ni v varčevanju z baterijo in nima `THERMAL_STATUS_SEVERE`. Pri `MODERATE` se izris omeji na 30 fps.

V vseh ostalih primerih se prikaže WebP poza. Na seznamih, obrazcih, v statistiki in nastavitvah je maskota vedno statična.

**Doslednost.** Kamera v aplikaciji je enaka Blenderjevi: 86 mm (vertikalni FOV 23,64°), pozicija (1,1; 2,4; 9,8) v glTF koordinatah, cilj (0; 1,32; 0). Luči so uravnane z linearnim tone mapperjem tako, da se barva telesa ujema z renderjem (#F28A3D, MascotOrange). Prehod med 3D in sliko zato ni opazen.

**Življenjski cikel.**

- Izris teče samo v stanju `RESUMED`; ob `ON_PAUSE` se zanka ustavi.
- Ko composable zapusti kompozicijo (`AndroidView.onRelease`), se sprostijo vsi GPU viri: swap chain, asset, materiali, view, scene in renderer.
- Filament `Engine` in prebran GLB ostaneta v procesu za naslednjo uporabo.

## Mapiranje stanj

| Stanje | Animacija / poza |
|---|---|
| IDLE | idle |
| WAVE | wave |
| WALK | walk |
| HAPPY, CHEER, STRETCHING | cheer |
| SAD | sad |
| THINKING, WORKING | think |
| SLEEPING, FREEZE | sleep |
| MEDITATING | meditate |
| THUMBS_UP, VACATION | thumbs |

## Proračun

| Postavka | Velikost |
|---|---|
| Filament + gltfio (native, na ABI) | arm64 ~5,7 MB, armeabi-v7a ~4,4 MB. Z AAB uporabnik prenese le svoj ABI. |
| `mascot.glb` | ~0,5 MB |
| 9 WebP poz | ~130 KB |

Filament je zaklenjen na **1.75.1**, ker 1.76+ zahteva `compileSdk 37`, ki ga AGP 8.13 še ne podpira. Pri nadgradnji na AGP 9 se lahko dvigne.

## Naslednji koraki

- **M6:** widgeti uporabijo iste WebP poze.
- **M8:** `MascotDirector` (vrsta dogodkov, ena reakcija na dogodek) in sprožilci iz use-caseov: prva navada, vse opravljeno, izgubljen streak, zamrznitev, dopust, backup.
