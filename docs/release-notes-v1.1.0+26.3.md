# Ruppi v1.1.0+26.3

[![](https://img.shields.io/badge/powered_by-dsh-4D6BFE?style=flat-square&logo=deepseek&logoColor=white)](https://github.com/deepseek-ai/deepseek-harness)

Minecraft **26.3** port of [rubi](https://github.com/keve1227/rubi) by keve1227, plus a 26.3 build of the
[Furigana](https://github.com/keve1227/furigana) resource pack.

## Assets

| File | What it is |
| --- | --- |
| `ruppi-1.1.0+26.3.jar` | The mod. Fabric, client-side only, needs MC 26.3 + Fabric Loader 0.20.1+ + Fabric API + Java 25 |
| `Furigana-v4+1-26.3.zip` | Japanese furigana resource pack (8557 / 8559 `en_us` keys), enable above the mod |

## Install

1. Drop `ruppi-1.1.0+26.3.jar` into `mods/`.
2. Drop `Furigana-v4+1-26.3.zip` into `resourcepacks/` and enable it in **Options → Resource Packs**.
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

# Ruppi v1.1.0+26.3（日本語）

[rubi](https://github.com/keve1227/rubi)（keve1227 作）の **Minecraft 26.3** 移植版と、
[Furigana](https://github.com/keve1227/furigana) リソースパックの 26.3 ビルドです。

- `ruppi-1.1.0+26.3.jar` … MOD 本体（Fabric・クライアント専用・MC 26.3 / Loader 0.20.1+ / Fabric API / Java 25）
- `Furigana-v4+1-26.3.zip` … 振り仮名リソースパック（`en_us` 8559 キー中 8557 キーを収録）。MOD より上で有効化

**使い方**：jar を `mods/`、zip を `resourcepacks/` に入れ、**設定 → ユーザー補助設定 → Rubies** で
描画モード（上 / 下 / 置換 / 非表示）を選びます。任意の文字列に `§^漢字(かんじ)` と書けばルビが付きます。

26.3 のクライアントには `GuiGraphics` が無いため、`Font.prepareText` 系の新パイプライン上での
再実装になっています（移植メモは上記リンク）。ライセンスは rubi から引き継いだ GPL-3.0、
リソースパックは Kevin Sundqvist Norlén の著作表示を保持しています。

---

# Ruppi v1.1.0+26.3（简体中文）

[keve1227 的 rubi](https://github.com/keve1227/rubi) 的 **Minecraft 26.3** 移植，附带
[Furigana](https://github.com/keve1227/furigana) 资源包的 26.3 版本。

- `ruppi-1.1.0+26.3.jar`：mod 本体（Fabric，仅客户端，需 MC 26.3 + Fabric Loader 0.20.1+ + Fabric API + Java 25）
- `Furigana-v4+1-26.3.zip`：日文注音资源包（收录 `en_us` 8559 个键中的 8557 个），在 mod 之上启用

**用法**：jar 放进 `mods/`，zip 放进 `resourcepacks/`，在 **选项 → 无障碍设置 → Rubies** 里选择绘制模式
（上方 / 下方 / 替换 / 隐藏）。任意客户端绘制的字符串都支持内联标记 `§^正文(注音)`，例如 `§^汉字(かんじ)`。

26.3 客户端已没有 `GuiGraphics`，因此这是在新 `Font.prepareText` 管线上的行为级重写，而非机械升版本
（移植说明见上面链接）。mod 沿用 rubi 的 GPL-3.0；资源包保留 Kevin Sundqvist Norlén 的署名。
