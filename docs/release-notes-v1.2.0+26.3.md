# Ruppi v1.2.0+26.3

[![](https://img.shields.io/badge/powered_by-dsh-4D6BFE?style=flat-square&logo=deepseek&logoColor=white)](https://github.com/deepseek-ai/deepseek-harness)

Minecraft **26.3** port of [rubi](https://github.com/keve1227/rubi) by keve1227, plus a 26.3 build of the
[Furigana](https://github.com/keve1227/furigana) resource pack.

## What's new

### `_rubi` languages fall back to the base language

When the selected language ends in `_rubi` (the bundled pack provides `ja_jp_rubi`), ruppi inserts
the base language (`ja_jp`) into the language stack at **index 1** — directly above `en_us` and
below every other fallback, so a standalone install loads `[en_us, ja_jp, ja_jp_rubi]`. Keys the
rubi pack does not carry now resolve against the base language instead of English, and mods that
only ship `ja_jp` keep their translations.

The low position is deliberate: the automatic entry only guarantees "do not fall back to English"
and does not compete with a higher-priority chain. A
[secondlang](https://github.com/Shiro-Design-16/secondlang) fallback chain (for example
`ja_jp_rubi → zh_cn → ja_jp`) wins over it, so the two can be used together.

## Assets

| File | What it is |
| --- | --- |
| `ruppi-1.2.0+26.3.jar` | The mod. Fabric, client-side only, needs MC 26.3 + Fabric Loader 0.20.1+ + Fabric API + Java 25 |
| `Furigana-v4+1-26.3.zip` | Japanese furigana resource pack (8557 / 8559 `en_us` keys). Identical to the 1.1.0 asset — enable above the mod |

## Install

1. Replace any older `ruppi-*.jar` in `mods/` with `ruppi-1.2.0+26.3.jar`.
2. Drop `Furigana-v4+1-26.3.zip` into `resourcepacks/` and enable it in **Options → Resource Packs**.
   The file is unchanged from 1.1.0, so an existing copy keeps working as is.
3. Pick a drawing mode in **Options → Accessibility → Rubies**.

Any client-drawn string supports the inline markup `§^base(reading)` — e.g. `§^漢字(かんじ)`.

## Notes

- The 26.3 client no longer has `GuiGraphics`, so this is a behavioural re-implementation on the new
  `Font.prepareText` pipeline, not a mechanical version bump. Porting notes:
  [English](https://github.com/Shiro-Design-16/ruppi/blob/main/PORTING.md) ·
  [日本語](https://github.com/Shiro-Design-16/ruppi/blob/main/PORTING.ja.md) ·
  [简体中文](https://github.com/Shiro-Design-16/ruppi/blob/main/PORTING.zh-CN.md)
- A mod's own `en_us.json` cannot override vanilla keys, which is why vanilla text needs the resource pack.
- License: GPL-3.0, inherited from rubi. Resource pack keeps Kevin Sundqvist Norlén's attribution
  (`Copyright.txt`, `NOTICE.txt`).

---

# Ruppi v1.2.0+26.3（日本語）

[rubi](https://github.com/keve1227/rubi)（keve1227 作）の **Minecraft 26.3** 移植版と、
[Furigana](https://github.com/keve1227/furigana) リソースパックの 26.3 ビルドです。

## 新機能

### `_rubi` で終わる言語は基底言語へフォールバック

選択中の言語が `_rubi` で終わるとき（同梱パックでは `ja_jp_rubi`）、ruppi は基底言語 `ja_jp` を
言語スタックの**インデックス 1**（`en_us` の直上、他のすべてのフォールバックより下）に挿入します。
単独構成では `[en_us, ja_jp, ja_jp_rubi]` として読み込まれます。ルビパックが持たないキーは英語ではなく
基底言語で解決され、`ja_jp` しか同梱しない mod の翻訳も維持されます。

この低い位置は意図的です。自動挿入が保証するのは「少なくとも英語には戻さない」ところまでで、
より高優先度の連鎖とは競合しません。[secondlang](https://github.com/Shiro-Design-16/secondlang) の
フォールバック連鎖（例：`ja_jp_rubi → zh_cn → ja_jp`）はこれより優先されるため、両者は併用できます。

## アセット

| ファイル | 内容 |
| --- | --- |
| `ruppi-1.2.0+26.3.jar` | MOD 本体（Fabric・クライアント専用・MC 26.3 / Loader 0.20.1+ / Fabric API / Java 25） |
| `Furigana-v4+1-26.3.zip` | 振り仮名リソースパック（`en_us` 8559 キー中 8557 キーを収録）。1.1.0 のアセットと同一です。MOD より上で有効化 |

## 導入

1. `mods/` にある古い `ruppi-*.jar` を `ruppi-1.2.0+26.3.jar` に置き換えます。
2. `Furigana-v4+1-26.3.zip` を `resourcepacks/` に入れ、**設定 → リソースパック** で有効化します
   （1.1.0 と同じファイルなので、既存のコピーのままで構いません）。
3. **設定 → ユーザー補助設定 → Rubies** で描画モード（上 / 下 / 置換 / 非表示）を選びます。

任意の文字列に `§^漢字(かんじ)` と書けばルビが付きます。

## 備考

- 26.3 のクライアントには `GuiGraphics` が無いため、`Font.prepareText` 系の新パイプライン上での
  再実装になっています（移植メモは上記リンク）。
- mod 自身の `en_us.json` はバニラのキーを上書きできないため、バニラの文言にはリソースパックが必要です。
- ライセンスは rubi から引き継いだ GPL-3.0、リソースパックは Kevin Sundqvist Norlén の著作表示を
  保持しています。

---

# Ruppi v1.2.0+26.3（简体中文）

[keve1227 的 rubi](https://github.com/keve1227/rubi) 的 **Minecraft 26.3** 移植，附带
[Furigana](https://github.com/keve1227/furigana) 资源包的 26.3 版本。

## 新功能

### 以 `_rubi` 结尾的语言回退到基底语言

选中语言以 `_rubi` 结尾时（同捆资源包提供的 `ja_jp_rubi` 即是），ruppi 会把基底语言 `ja_jp` 插到语言栈的
**索引 1**——紧贴 `en_us` 之上、其它所有回退之下，独立安装时加载的栈为 `[en_us, ja_jp, ja_jp_rubi]`。
rubi 包没有提供的键因此回落到基底语言而不是英语，只提供 `ja_jp` 的 mod 也能保住自己的翻译。

这个低位置是刻意选的：自动插入只保证「至少别再退回英语」，不与更高优先级的链争夺。用户通过
[secondlang](https://github.com/Shiro-Design-16/secondlang) 配置的回退链（例如
`ja_jp_rubi → zh_cn → ja_jp`）优先级高于它，两者可以共存。

## 资产

| 文件 | 内容 |
| --- | --- |
| `ruppi-1.2.0+26.3.jar` | mod 本体（Fabric，仅客户端，需 MC 26.3 + Fabric Loader 0.20.1+ + Fabric API + Java 25） |
| `Furigana-v4+1-26.3.zip` | 日文注音资源包（收录 `en_us` 8559 个键中的 8557 个）。与 1.1.0 的资产完全相同，在 mod 之上启用 |

## 安装

1. 用 `ruppi-1.2.0+26.3.jar` 替换 `mods/` 中旧的 `ruppi-*.jar`。
2. 把 `Furigana-v4+1-26.3.zip` 放进 `resourcepacks/`，在 **选项 → 资源包** 中启用
   （与 1.1.0 是同一个文件，沿用现有副本即可）。
3. 在 **选项 → 无障碍设置 → Rubies** 里选择绘制模式（上方 / 下方 / 替换 / 隐藏）。

任意客户端绘制的字符串都支持内联标记 `§^正文(注音)`，例如 `§^汉字(かんじ)`。

## 备注

- 26.3 客户端已没有 `GuiGraphics`，因此这是在新 `Font.prepareText` 管线上的行为级重写，而非机械升版本
  （移植说明见上面链接）。
- mod 自身的 `en_us.json` 无法覆盖原版键，因此原版文案需要资源包配合。
- mod 沿用 rubi 的 GPL-3.0；资源包保留 Kevin Sundqvist Norlén 的署名（`Copyright.txt`、`NOTICE.txt`）。
