# Ruppi

[![](https://img.shields.io/badge/powered_by-dsh-4D6BFE?style=flat-square&logo=deepseek&logoColor=white)](https://github.com/deepseek-ai/deepseek-harness)

**English** · [日本語](README.ja.md) · [简体中文](README.zh-CN.md)

A Minecraft **26.3** port of [rubi](https://github.com/keve1227/rubi) by keve1227 (GPL-3.0).
Ruppi draws ruby annotations — furigana / reading aids — for any text the client renders.

Minecraft 1.20 → 26.3 required a new rendering pipeline (the 26.3 client no longer has
`GuiGraphics`), so this is a re-implementation of rubi's behaviour rather than a mechanical
bump. See [PORTING.md](PORTING.md) for the porting notes.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.20.1+
- Fabric API (any 26.3 build)
- Java 25

Client-side only.

## Usage

Write the annotation inline in any string the client draws (language files, chat, item names, NBT text):

    §^text(ruby)

`§^` opens the annotation, the base text is everything up to the parentheses, the reading is what is
inside them:

    §^漢字(かんじ) と §^Tokyo(とうきょう)

The annotation occupies a single text slot whose width follows the active mode, so layout stays
stable when the mode changes.

Four modes live in **Options → Accessibility → Rubies** (persisted as `ruppi.renderMode` in
`options.txt`):

| Mode | Drawing |
| --- | --- |
| Above | base at 80 %, reading at 50 % above it |
| Below | base at 80 %, reading at 50 % below it |
| Replace | reading only, full size |
| Hidden | base only, full size |

### Annotating vanilla strings

Resource packs stack above mod resources, and the vanilla pack wins over a mod-provided
`en_us.json` — a mod cannot rewrite `menu.singleplayer`. To put rubies on vanilla text, ship a
resource pack that holds those keys with `§^` markup and enable it above the mod. The mod's own
language file only covers its options labels.

### Languages ending in `_rubi`

When the selected language ends in `_rubi` (the pack below provides `ja_jp_rubi`), the base
language (`ja_jp`) is inserted into the language stack as well — at index 1, directly above `en_us`
and below every other fallback. On its own the stack is `[en_us, ja_jp, ja_jp_rubi]`, so keys the
rubi pack does not carry fall back to the base language instead of English, and mods that only ship
`ja_jp` keep their translations.

That low position is deliberate: it is the lowest-priority slot that still beats English.
A fallback chain configured through [secondlang](https://github.com/Shiro-Design-16/secondlang) takes
precedence over this automatic insertion, and the two can be used together. The base language is
inserted only when some resource pack (including the mod's own `assets/ruppi/lang/ja_jp.json`) provides
`lang/<base>.json`, and never a second time if it is already in the stack.

## Companion resource pack: Japanese furigana

`resourcepack/` in this repository (also attached to every release as
`Furigana-v4+1-26.3.zip`) is a 26.3 build of
[Furigana](https://github.com/keve1227/furigana) by Kevin Sundqvist Norlén, extended to cover every
key in the 26.3 `en_us` language file, so Japanese text in vanilla screens is annotated too.

| | |
| --- | --- |
| Drop-in install | `resourcepacks/Furigana-v4+1-26.3.zip` → enable above the mod |
| Language entry | `日本語（ふりがな）` (`ja_jp_rubi`) |
| Coverage | 8557 / 8559 `en_us` keys (the two missing ones have empty English values) |
| License | GPL-3.0, credited in `Copyright.txt` / `NOTICE.txt` |

Readings for keys the upstream pack does not cover were written out in the rubi notation for this
build; `Alex's Japanese Furigana Language` (`ja_fw.json`) was consulted as a reference for them.
Regenerate with `python3 tools/merge_furigana_pack.py` (the script lives in the author's workspace,
not in this repository — see `resourcepack/NOTICE.txt`).

## How it works

- `§^…(…)` is parsed inside `StringDecomposer.iterateFormatted`; the whole annotation is emitted as a
  single `U+FFFC` code point carrying a `RubyText` payload on the `Style`. `Style` is immutable in
  26.3, so the payload is threaded through every `withXxx` copy.
- `StringSplitter.WidthProvider` reports the annotation's box width, so wrapping, centering and
  widget fitting behave like a normal glyph.
- `Font.prepareText` / `Font.prepare8xTextOutline` are intercepted: a sequence that contains a ruby
  slot is prepared by the mod (base and reading glyphs, scaled and spread evenly across the box);
  every other sequence falls through to vanilla untouched.

## Building

Needs JDK 25:

    ./gradlew build        # → build/libs/ruppi-<version>.jar

## Documentation

| | |
| --- | --- |
| Porting notes | [English](PORTING.md) · [日本語](PORTING.ja.md) · [简体中文](PORTING.zh-CN.md) |
| Verification screenshots | [`docs/verification/`](docs/verification) |

## License

GPL-3.0, inherited from upstream rubi. See [LICENSE](LICENSE).

## Credits

- Upstream rubi: keve1227 — https://github.com/keve1227/rubi
- Furigana resource pack: Kevin Sundqvist Norlén — https://github.com/keve1227/furigana
- 26.3 port and the extended resource pack: SHIRO
