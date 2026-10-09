"""Zgradi fonta aplikacije iz Google Fonts virov (OFL).

- streak_display.ttf: Roboto Flex, omejen na osi wght 600-1000 in wdth 112-151
  (ostale osi pripete na privzeto), za siroke krepke naslove.
- streak_text.ttf: Manrope (wght 200-800) za besedilo.

Oba sta zmanjsana na latinico (vkljucno z razsirjeno), cirilico in grscino,
kar pokrije vseh 24 uradnih jezikov EU. Skripta na koncu preveri pokritost.

Uporaba:  pip install fonttools  &&  python tools/fonts/build_fonts.py
"""
import pathlib
import urllib.request

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "app/src/main/res/font"
CACHE = pathlib.Path(__file__).resolve().parent / "out"
BASE = "https://raw.githubusercontent.com/google/fonts/main/ofl/"
SOURCES = {
    "RobotoFlex.ttf": "robotoflex/RobotoFlex%5BGRAD,XOPQ,XTRA,YOPQ,YTAS,YTDE,YTFI,YTLC,YTUC,opsz,slnt,wdth,wght%5D.ttf",
    "Manrope.ttf": "manrope/Manrope%5Bwght%5D.ttf",
}
UNICODES = ("U+0000-024F,U+0259,U+02BB-02BC,U+02C6-02DD,U+0300-036F,U+0370-03FF,"
            "U+0400-045F,U+0490-0491,U+1E00-1EFF,U+2000-206F,U+20AC,U+2116,U+2122,"
            "U+2190-2193,U+2212,U+2215,U+FEFF,U+FFFD")
CHECK = {
    "latin": "ČčŠšŽžĆćĐđŁłŃńŚśŹźŻżĄąĘęȘșȚțĂăÂâÎîĀāĒēĪīŪūĶķĻļŅņĢģĖėĮįŲųŰűŐőĦħĠġĊċÁáÉéÍíÓóÚúÝýĎďĚěŇňŘřŤťŮůÄäÖöÜüßÅåÆæØøÕõÑñÇç€",
    "cyrillic": "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЬЮЯабвгдежзийклмнопрстуфхцчшщъьюя",
    "greek": "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩαβγδεζηθικλμνξοπρστυφχψωάέήίόύώϊϋΐΰς",
}


def fetch(name: str) -> pathlib.Path:
    CACHE.mkdir(exist_ok=True)
    path = CACHE / name
    if not path.exists():
        urllib.request.urlretrieve(BASE + SOURCES[name], path)
    return path


def subset_font(font: TTFont, dst: pathlib.Path) -> None:
    opts = subset.Options()
    opts.layout_features = ["*"]
    opts.name_IDs = ["*"]
    opts.notdef_outline = True
    s = subset.Subsetter(opts)
    s.populate(unicodes=subset.parse_unicodes(UNICODES))
    s.subset(font)
    font.save(dst)


def check(path: pathlib.Path) -> None:
    cmap = TTFont(path).getBestCmap()
    for script, chars in CHECK.items():
        missing = [c for c in chars if ord(c) not in cmap]
        if missing:
            raise SystemExit(f"{path.name}: manjkajo znaki ({script}): {''.join(missing)}")
    print(f"{path.name}: {path.stat().st_size} B, pokritost OK")


def main() -> None:
    flex = TTFont(fetch("RobotoFlex.ttf"))
    limits = {a.axisTag: a.defaultValue for a in flex["fvar"].axes}
    limits["wdth"] = (112, 151)
    limits["wght"] = (600, 1000)
    # Delno instanciran font shranimo in ponovno nalozimo (subsetter ne dela z lenimi tabelami)
    partial = CACHE / "RobotoFlex-partial.ttf"
    instancer.instantiateVariableFont(flex, limits).save(partial)
    subset_font(TTFont(partial), OUT / "streak_display.ttf")
    subset_font(TTFont(fetch("Manrope.ttf")), OUT / "streak_text.ttf")
    check(OUT / "streak_display.ttf")
    check(OUT / "streak_text.ttf")


if __name__ == "__main__":
    main()
