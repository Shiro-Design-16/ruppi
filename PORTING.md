**English** · [日本語](PORTING.ja.md) · [简体中文](PORTING.zh-CN.md)

# 26.3 Porting Notes

Porting upstream [rubi](https://github.com/keve1227/rubi) (Minecraft 1.20, Yarn mappings, Java 17) to Minecraft 26.3 (official Mojang mappings, Loom 1.19, Java 25). The upstream sources live in `../references/rubi` (read-only reference).

- modid `ruppi`, package `com.shiro.ruppi`, version `1.1.0+26.3`, client-side only.
- Artifact: `build/libs/ruppi-1.1.0+26.3.jar`.

## Structure

| Upstream (1.20 / Yarn) | This project (26.3 / mojmap) |
| --- | --- |
| RubyText, IRubyStyle, Utils, option/RubyRenderMode | Names kept (all rewritten against 26.3 types; the width/geometry constants match upstream character for character: TEXT_SCALE 0.8, RUBY_SCALE 0.5, RUBY_OVERLAP 0.1) |
| MixinStringVisitable (`StringVisitable.getString`) | MixinFormattedText (`FormattedText.getString`) |
| MixinTextVisitFactory (parses `§^`) | MixinStringDecomposer (`StringDecomposer.iterateFormatted`) |
| MixinStyle | MixinStyle |
| MixinTextHandler (width) | MixinStringSplitter (`StringSplitter.WidthProvider`) |
| MixinGameOptions + MixinAccessibilityOptionsScreen | MixinOptions + MixinAccessibilityOptionsScreen |
| MixinTextRenderer (drawing) | MixinFont (`Font.prepareText` / `prepare8xTextOutline`) + a self-built `PreparedText` under `client/render/*` |
| MixinFormatting | Removed: it only used `@Shadow` for constants, with no behavioural change |
| MixinTextWidget | Removed: in 26.3 `StringWidget.getWidth` is no longer final and goes through `Font.width` |

Upstream has 9 mixins; this project has 7.

## Key 26.3 differences and how they are handled

1. **There is no `GuiGraphics`.** The GUI text submission point is `GuiGraphicsExtractor.text(...)` → a cached `GuiTextRenderState` → `GuiRenderer.prepareText()` consuming `Font.PreparedText`. The only stable hook points are `Font.prepareText(FormattedCharSequence,float,float,int,boolean,boolean,int)` and `Font.prepare8xTextOutline(...)`: inject at HEAD, return the self-built `PreparedText` when a ruby slot is hit, and otherwise return null so vanilla continues. The self-built implementation copies `Font`'s four-phase visit order (background → glyph → effect line → empty area), because in GUI flat mode `flat=true` flattens every depth to z=0, so layering is decided entirely by visit order.
2. **`Style` is an immutable class** with no `withParent`: all 11 `withXxx` methods return a new object, and `checkEmptyAfterChange` folds the new object back into the `EMPTY` singleton with `equals(EMPTY)`. Therefore: the ruby payload must be copied into every return value (a single multi-target `@Inject(at = RETURN)` covers 17 methods); `equals`/`hashCode` must include ruby (otherwise a Style carrying ruby is folded into EMPTY by mistake); and attaching to `Style.EMPTY` must allocate a new object instead of polluting the singleton. The copy constructor is opened up through the access widener.
3. **The access widener uses the `official` namespace**: the template project has no mappings dependency, and the jar itself is mojmap-named. Writing `named` in an entry fails hard at configuration time (`Expected official namespace for access widener entry, found: named`). There are only two entries: `Options$OptionAccess` (wiring the option into load/save/dump) and `Style`'s 11-argument constructor.
4. **`Options.Visitor` is gone**, replaced by `Options.processDumpedOptions(Options.OptionAccess)`, a single private method carrying the load / save / dump paths; injecting into it covers all three at once. The option widget uses `OptionInstance.SliderableEnum<T>(List<T>, Codec<T>)`, so `RubyRenderMode` carries its own `Codec.INT.xmap(byId, getId)`; the enum's display name uses a caption callback plus `Options.genericValueLabel` (`TranslatableOption` no longer exists).
5. **`AccessibilityOptionsScreen` has no `getOptions()`**, only `private static OptionInstance<?>[] options(Options)`; at its RETURN the array is copied and one entry appended. Appending at the end is because `addOptions()` only gives `options[0]` special layout.
6. **Screen ownership changed**: neither `Minecraft.setScreen` nor `Minecraft.screen` exists any more; use `Minecraft.setScreenAndShow(Screen)` / `Minecraft.gui.screen()` / `Minecraft.gui.setScreen(Screen)`.
7. **Width is a float**: `StringSplitter.WidthProvider` is `float getWidth(int, Style)`, and `Font.width` finally applies `Mth.ceil`. The ruby slot is wrapped at the `StringSplitter` constructor's **RETURN**: for code point `U+FFFC` carrying ruby it returns the box width, everything else is delegated unchanged. Note: `@ModifyVariable(argsOnly = true, at = @At(value = "FIELD", opcode = PUTFIELD))` on `this.widthProvider = widthProvider` **does not actually change the stored value** (javac has already emitted `aload_1` before that point, so the insertion lands between the load and the putfield: the handler mutates the local, while the original value is what gets stored). The symptom is silently broken width computation; see "Fix Log" item 1.

## Acceptance record

Build and launch:

    JAVA_HOME=<JDK 25> ./gradlew build          # BUILD SUCCESSFUL
    JAVA_HOME=<JDK 25> ./gradlew runClient      # title screen, no mixin / injection errors in the log

Manual verification evidence (`docs/verification/`, all real screenshots from the 26.3 dev client):

- `full-above-cjk.png`: the Accessibility Settings screen, where the "Rubies" row renders `§^設定(せってい)` with the reading above the base text.
- `row-1-above-cjk.png` … `row-4-hidden-cjk.png`: the same row in each of the four modes, Above / Below / Replace / Hidden.
- `row-1-above-latin.png`: the English demo (`§^Above (Above) Text`).

Three further runtime facts:

- `ruppi.renderMode:0` appears in `run/options.txt`: the option really is wired into Options' load/save path (the access widener + `Options.OptionAccess` + `Codec` chain all take effect).
- The resource-pack reload log order is `vanilla, …, ruppi`, showing that the mod's resource pack sits above vanilla in the stack.
- At startup `Mixin` 0.8.7 + MixinExtras 0.5.5 apply the 7 mixins normally, and `defaultRequire: 1` triggers no `InjectionError`.

Temporary scaffolding was used during verification (an entrypoint that cycles the four modes and calls `Screenshot.grab`, the option temporarily moved to the top of the list, the language switched to en_us, demo keys placed in the language file); all of it was removed before delivery: the `client` entrypoint in `fabric.mod.json` is only `com.shiro.ruppi.RuppiClient`, the jar contains no acceptance classes, and the language file keeps only the option's own 5 keys.

## Fix Log (after on-device verification)

1. **Annotated text was shifted right as a whole (width under-counted).** The `StringSplitter` width-provider wrap initially used `@ModifyVariable(argsOnly = true)` + `@At(value = "FIELD", opcode = PUTFIELD)`; it compiled and launched without errors, but at runtime the field was never replaced at all (reflection still read `Font$$Lambda`). Consequence: `Font.width` measured a `U+FFFC` slot as a single missing-glyph box (9 px) while rendering advanced by the box width, so centred text was pushed right by half the difference. Measured (1708×960, GUI scale 4): on the title screen the `設定...` button's text centre was 665.5 vs the button centre 645.5 = **20 device pixels too far right (5 GUI px)**; after switching to a direct assignment at the constructor's RETURN using `@Shadow @Final @Mutable`, the same measurement was 646.0 vs 645.5 (**+0.5 device pixels**).
2. **A ruby span swallowed the character immediately after it.** The parse injection point for `§^base(reading)` is at the `ChatFormatting.getByCode` call, after which there are **two** increments (the `§` branch's own `i++` and the `for`'s `i++`); the old implementation's `index.set(matcher.end() - 1)` accounted for only one, so one character after every ruby span was skipped (`設定...` was actually drawn as `設定`). Changed to `matcher.end() - 2`. Sequence-level evidence (`Component.literal(...).getVisualOrderText()`): before the fix `A§^設(せつ)B` → `[41][fffc]`, after → `[41][fffc][42]`, and the width went from 15 to 21 accordingly.

## Known limitations

- **Vanilla strings cannot be overridden**: during language merging the vanilla pack takes priority over mod resource packs, so a mod-provided `en_us.json` cannot change keys such as `menu.singleplayer`. Showing rubies on vanilla screens needs a resource pack carrying `§^` markup enabled above the mod (this is also how upstream rubi is used).
- **Debug gizmo text is not covered**: the `Font.prepareText(String, …)` overload (used by `GizmoFeatureRenderer`) is not hooked; text display entities go through the `FormattedCharSequence` overload, which is covered.
- **With CJK the reading sits very close to the base text**: the geometry follows the upstream formula exactly (ruby baseline = base baseline - 0.5×line height + 0.1×line height), and CJK glyphs nearly fill the em box, so visually it presses against the top of the base text; this is upstream behaviour, not something the port introduced.
- **Hover hitbox for spaces**: the self-built empty area reuses the line-height-related constants, so a few pixels of drift are possible at some scales (it only affects interaction hit-testing over space regions).

## Reproduction

    ./gradlew build     # requires JDK 25
    ./gradlew runClient # dev client, run/ is the working directory

## Appendix: 26.3 adaptation of the Furigana resource pack (Furigana-v4+1)

From 26.3 the resource pack version number changed from a single integer to `major.minor` (currently `97.1`), with the supported range moved into new fields. The old `"pack_format": 34` + `"supported_formats": {"min_inclusive": 33, "max_inclusive": 38}` is treated as an "old-version pack" in 26.3: the loader still allows forcing it on, but the pack list marks it red as incompatible.

The predicate lives in `net/minecraft/server/packs/metadata/pack/PackFormat.java`: `lastPreMinorVersion(CLIENT_RESOURCES) = 64`, and only declarations with `major ≤ 64` take the legacy field path; anything above 64 that lacks `min_format`/`max_format` fails outright with "declares support for version newer than 64, but is missing mandatory fields min_format and max_format". `min_format`/`max_format` are parsed by `ExtraCodecs.compactListCodec` and may be an integer (major only) or `[major, minor]`; with a plain integer, `min`'s minor defaults to 0 and `max`'s to `Integer.MAX_VALUE`, equivalent to "all of 97.x".

The adapted `pack.mcmeta` (everything else identical to the original):

    {
      "pack": {
        "description": "Provides Japanese furigana for use with the Rubi mod. (26.3 port)",
        "min_format": 97,
        "max_format": 97
      },
      "language": { "ja_jp_rubi": { … } }
    }

The language section needs no changes: `LanguageMetadataSection` is still `Map<language code, LanguageInfo>`, codes are limited to 1–16 characters, and `ja_jp_rubi` (10 characters) is valid; the language file path is still `assets/minecraft/lang/ja_jp_rubi.json`. Measured: `PackCompatibility.forVersion` returns `COMPATIBLE`, `ja_jp_rubi` appears in the language list, and rubies render normally once it is selected.

Key-set evolution: when adapting to 26.3 all 6874 keys in the pack were still present, but 1685 keys added in 26.3 were missing (the language stack is `en_us + ja_jp_rubi`, so missing keys fall back to English without rubies). Those 1685 keys were later filled in from a third-party pack using option A, see the next section; after that the pack holds **8557 keys**, and against the 26.3 `en_us` (8559 keys) only `potion.potency.0` and `selectWorld.gameMode.spectator.line2` are missing — two keys whose English values are empty to begin with.

Delivery: `Furigana-v4+1-26.3/` (directory) and `Furigana-v4+1-26.3.zip` (drop straight into `resourcepacks/`). Acceptance screenshots: `docs/verification/furigana-pack-title-above.png`, `furigana-pack-language-list.png`.

## Appendix II: merging readings from the third-party `furigana-pack` (option A)

The user supplied a pack made by someone else, `ruppi/furigana-pack` ("Alex's Japanese Furigana Language", declaring `pack_format 121`), whose notation is incompatible with rubi:

| Pack | Notation | Example |
| --- | --- | --- |
| rubi / Furigana-v4+1 | `§^base(reading)`, okurigana left outside the parentheses | `§^押(お)す`、`§^音声読(おんせいよ)み§^上(あ)げ`、`§^設定(せってい)` |
| furigana-pack `ja_fw` | `word(reading)`, parentheses directly after the word | `設定(せってい)`、`音声読み上げ(おんせいよみあげ)` |
| furigana-pack `ja_fe` | the whole reading string hangs at the end of the sentence | `… (おす\|おんせいよみあげ\|ゆうこう)` — each word's reading cannot be inferred, **unparseable** |

Assessment: **choose A (convert the readings into rubi notation and fill them into Furigana-v4+1) rather than changing the mod to parse parentheses**. Reasons:

1. The two packs share one translation lineage (for the 8557 overlapping keys the plain-text agreement rate is 98.1 %, and the 128 differences are only wording tweaks), so merging produces no terminology conflicts.
2. Changing the mod would mean running the same Japanese segmentation heuristics on the rendering hot path, and it would have to use a coarse scheme for all 8557 keys, which would instead make the precise annotation of the existing 6874 keys worse.
3. `ja_fe`'s notation is unparseable in any case.
4. A is a pure data artifact: reviewable entry by entry, zero runtime risk, and the rendering path (already accepted at pixel level) stays completely untouched.

Conversion script: `tools/merge_furigana_pack.py` (`--check` validates only, writes nothing). Algorithm:

- The reading must be all hiragana (may contain `ー`); walk back from the preceding `(` past the kana to get the okurigana segment, then past the kanji to get the base candidate, and the base must contain kanji.
- Okurigana stripping: trailing kana that are identical on the base and on the reading are peeled off from both sides together (`押す`/`おす` → base `押`, reading `お`, with the outer `す` left in place).
- Confidence gate: `0.4 ≤ len(reading)/kanji count ≤ 3.0`; when okurigana was stripped an extra `≤ 2.2` is required (this blocks mismatches such as `手に入れ(てにいれ)る` where the reading reaches into preceding kanji). Parentheses that fail the gate are left in the text unchanged (keeping the reading information rather than producing wrong rubies).
- Only keys missing from the base pack are processed; the base's 6874 entries stay byte-for-byte unchanged.

Result: 1683 keys added (1333 with rubies, 24 with some parentheses kept, 326 plain text, 0 unparseable), giving 8557 keys and 14898 ruby spans after the merge. Validation: the base's 6874 entries are byte-identical; for the added keys the "visible text" matches the source text with parentheses removed, exactly (0 cases of text loss). Samples: `ブロック§^輪郭(りんかく)のコントラスト§^強化(きょうか)`、`§^模様入(もよう)りの§^辰砂(しんしゃ)`、`メインメニューの§^黄色(きいろ)いスプラッシュテキストを§^非表示(ひひょうじ)にします。`

Verification: `docs/verification/pack-merge-accessibility-bottom.png` (the Accessibility Settings screen scrolled to the bottom, where 26.3's new keys `options.hideSplashTexts`, `options.hideLightningFlashes`, `options.panoramaSpeed` and others now show Japanese with rubies) and `pack-merge-new-keys.png` (a self-drawn screen rendering 8 of the added keys line by line). The measured screenshots are in `docs/verification/`.

Licensing and attribution: this pack does not copy `furigana-pack`'s files over; it is **a language file written by hand in rubi notation with that pack's readings as reference**, so the whole pack keeps being distributed under **GPL-3.0**, with attribution kept for both upstream and this port:

- `Copyright.txt`: `© Kevin Sundqvist Norlén 2024 - Furigana v4 base keys` and `© SHIRO 2026 - 26.3 port: pack metadata and the added keys`;
- `NOTICE.txt`: states where the base keys came from, how the added keys were written and what they reference (including that `ja_fe` is unusable), that GPL-3.0 covers the whole pack, and the upstream repository URL;
- the `pack.mcmeta` description ends with `GPL-3.0`.

Regenerating only needs `python3 tools/merge_furigana_pack.py`.
