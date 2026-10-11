"""Ustvari res/values*/strings.xml iz tools/i18n/<jezik>.txt in preveri prevode.

Oblika vrstic (UTF-8):
    kljuc=besedilo
    P kljuc=one:besedilo | few:besedilo | other:besedilo
    A kljuc=prvi | drugi | tretji
Prazne vrstice in vrstice z # so komentarji.

en.txt je privzeti jezik (values/). Preverjanje za vsak jezik:
  - manjkajoci in odvecni kljuci,
  - enaki formatni argumenti (%d, %1$s, %%) kot v angleskem izvirniku,
  - mnozine imajo vse kategorije, ki jih jezik potrebuje (CLDR),
  - nizi niso prazni.
Uporaba: python tools/i18n/build.py [--check]
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = os.path.join(ROOT, "tools", "i18n")
RES = os.path.join(ROOT, "app", "src", "main", "res")

# Ne prevajajo se (le oblika ali ime)
NON_TRANSLATABLE = {
    "app_name", "greeting_with_comma", "habit_progress_quantity", "notif_settings_cap_value",
    "badge_progress", "badge_progress_hours", "xp_total", "challenge_pending",
}

# Kategorije mnozine, ki jih Android (ICU/CLDR) uporablja za stevila
PLURALS = {
    "en": ["one", "other"], "de": ["one", "other"], "nl": ["one", "other"], "sv": ["one", "other"],
    "da": ["one", "other"], "fi": ["one", "other"], "et": ["one", "other"], "el": ["one", "other"],
    "bg": ["one", "other"], "hu": ["one", "other"],
    "fr": ["one", "many", "other"], "es": ["one", "many", "other"], "it": ["one", "many", "other"],
    "pt": ["one", "many", "other"],
    "cs": ["one", "few", "many", "other"], "sk": ["one", "few", "many", "other"],
    "pl": ["one", "few", "many", "other"], "lt": ["one", "few", "many", "other"],
    "lv": ["zero", "one", "other"], "ro": ["one", "few", "other"], "hr": ["one", "few", "other"],
    "sl": ["one", "two", "few", "other"], "ga": ["one", "two", "few", "many", "other"],
    "mt": ["one", "two", "few", "many", "other"],
}

ARG = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[sdfx%]")


def parse(path):
    entries = {}
    order = []
    with open(path, encoding="utf-8") as f:
        for n, line in enumerate(f, 1):
            line = line.rstrip("\n")
            if not line.strip() or line.startswith("#"):
                continue
            kind = "s"
            if line.startswith("P "):
                kind, line = "p", line[2:]
            elif line.startswith("A "):
                kind, line = "a", line[2:]
            if "=" not in line:
                raise SystemExit(f"{path}:{n}: manjka '='")
            key, value = line.split("=", 1)
            key = key.strip()
            if key in entries:
                raise SystemExit(f"{path}:{n}: podvojen kljuc {key}")
            if kind == "p":
                forms = {}
                for part in value.split(" | "):
                    q, text = part.split(":", 1)
                    forms[q.strip()] = text.strip()
                entries[key] = ("p", forms)
            elif kind == "a":
                entries[key] = ("a", [v.strip() for v in value.split(" | ")])
            else:
                entries[key] = ("s", value.strip())
            order.append(key)
    return entries, order


def esc(text):
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = text.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')
    if text[:1] in ("@", "?"):
        text = "\\" + text
    return text


def args(text):
    found = ARG.findall(text)
    # %d in %1$d sta enakovredna, ce je argument en sam
    return sorted(a.replace("1$", "") if len([x for x in found if x != "%%"]) == 1 else a for a in found)


def check(lang, entries, base):
    problems = []
    for key, (kind, value) in base.items():
        if key in NON_TRANSLATABLE:
            continue
        if key not in entries:
            problems.append(f"manjka {key}")
            continue
        k2, v2 = entries[key]
        if k2 != kind:
            problems.append(f"{key}: napacna vrsta")
            continue
        if kind == "s":
            if not v2:
                problems.append(f"{key}: prazno")
            elif args(v2) != args(value):
                problems.append(f"{key}: argumenti {args(v2)} != {args(value)}")
        elif kind == "p":
            need = PLURALS[lang]
            missing = [q for q in need if q not in v2]
            extra = [q for q in v2 if q not in need]
            if missing:
                problems.append(f"{key}: manjkajo kategorije {missing}")
            if extra:
                problems.append(f"{key}: odvecne kategorije {extra}")
            ref = args(value["other"])
            for q, t in v2.items():
                # "one" lahko izpusti stevilo, kjer zajema le 1 (npr. "one day")
                if args(t) != ref and not (q in ("one", "zero", "two") and args(t) == []):
                    problems.append(f"{key}[{q}]: argumenti {args(t)} != {ref}")
        elif kind == "a" and len(v2) != len(value):
            problems.append(f"{key}: {len(v2)} elementov namesto {len(value)}")
    for key in entries:
        if key not in base:
            problems.append(f"odvecen kljuc {key}")
        elif key in NON_TRANSLATABLE and lang != "en":
            problems.append(f"{key} se ne prevaja")
    return problems


def write(lang, entries, order):
    folder = "values" if lang == "en" else f"values-{lang}"
    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             f"<!-- Ustvarjeno s tools/i18n/build.py iz tools/i18n/{lang}.txt. Ne urejaj rocno. -->",
             '<resources xmlns:tools="http://schemas.android.com/tools">']
    for key in order:
        if key not in entries:
            continue
        kind, value = entries[key]
        if kind == "s":
            attr = ' translatable="false"' if key in NON_TRANSLATABLE else ""
            lines.append(f'    <string name="{key}"{attr}>{esc(value)}</string>')
        elif kind == "p":
            # Stevilo je prikazano posebej (npr. velika stevilka na widgetu)
            ignore = ' tools:ignore="ImpliedQuantity"' if all("%" not in t for t in value.values()) else ""
            lines.append(f'    <plurals name="{key}"{ignore}>')
            for q in ("zero", "one", "two", "few", "many", "other"):
                if q in value:
                    lines.append(f'        <item quantity="{q}">{esc(value[q])}</item>')
            lines.append("    </plurals>")
        else:
            lines.append(f'    <string-array name="{key}">')
            lines.extend(f"        <item>{esc(v)}</item>" for v in value)
            lines.append("    </string-array>")
    lines.append("</resources>")
    out = os.path.join(RES, folder)
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, "strings.xml"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")


def main():
    only_check = "--check" in sys.argv
    base, order = parse(os.path.join(SRC, "en.txt"))
    langs = sorted(f[:-4] for f in os.listdir(SRC) if f.endswith(".txt"))
    failed = False
    for lang in langs:
        entries, _ = parse(os.path.join(SRC, f"{lang}.txt"))
        problems = check(lang, entries, base)
        if problems:
            failed = True
            print(f"[{lang}] {len(problems)} tezav:")
            for p in problems[:40]:
                print("   ", p)
        if not only_check:
            write(lang, entries, order)
    print(f"{len(langs)} jezikov: {', '.join(langs)}")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
