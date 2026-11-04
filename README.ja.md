[English](README.md) · **日本語** · [简体中文](README.zh-CN.md)

# Ruppi

[rubi](https://github.com/keve1227/rubi)（作者 keve1227、GPL-3.0）の Minecraft **26.3** 移植版です。
Ruppi は、クライアントが描画する任意のテキストにルビ（振り仮名・読み仮名）を付けます。

Minecraft 1.20 → 26.3 では描画パイプラインが新しくなり（26.3 のクライアントには
`GuiGraphics` がありません）、単純なバージョン更新ではなく rubi の挙動を再実装しています。
移植メモは [PORTING.ja.md](PORTING.ja.md) を参照してください。

## 動作環境

- Minecraft 26.3
- Fabric Loader 0.20.1+
- Fabric API（26.3 向けのビルドならどれでも）
- Java 25

クライアント専用です。

## 使い方

クライアントが描画する任意の文字列（言語ファイル、チャット、アイテム名、NBT テキスト）に、
インラインで注釈を書けます：

    §^text(ruby)

`§^` が注釈の開始、基底テキストは括弧の手前まで、読みは括弧の中身です：

    §^漢字(かんじ) と §^Tokyo(とうきょう)

注釈は 1 つのテキストスロットを占有し、その幅は現在のモードに従います。そのため、モードを
切り替えてもレイアウトは安定したままです。

4 つのモードは **Options → Accessibility → Rubies** にあります（`options.txt` に
`ruppi.renderMode` として保存されます）：

| モード | 描画 |
| --- | --- |
| 上 | 基底を 80 %、読みを 50 % でその上に |
| 下 | 基底を 80 %、読みを 50 % でその下に |
| 置換 | 読みのみ、等倍 |
| 非表示 | 基底のみ、等倍 |

### バニラの文字列に注釈を付ける

リソースパックは mod のリソースより上に積まれ、バニラのパックは mod が提供する `en_us.json` より
優先されます。つまり mod は `menu.singleplayer` を書き換えられません。バニラのテキストにルビを
付けるには、該当するキーを `§^` 記法で持つリソースパックを用意し、mod より上で有効化してください。
mod 自身の言語ファイルは、そのオプションのラベルのみを対象にしています。

## 同梱リソースパック：日本語の振り仮名

このリポジトリの `resourcepack/`（各リリースには `Furigana-v4+1-26.3.zip` として添付）は、
Kevin Sundqvist Norlén による [Furigana](https://github.com/keve1227/furigana) の 26.3 向けビルドで、
26.3 の `en_us` 言語ファイルのすべてのキーをカバーするよう拡張されており、バニラの画面の
日本語テキストにもルビが付きます。

| | |
| --- | --- |
| そのまま導入 | `resourcepacks/Furigana-v4+1-26.3.zip` → mod より上で有効化 |
| 言語エントリ | `日本語（ふりがな）` (`ja_jp_rubi`) |
| カバレッジ | `en_us` の 8557 / 8559 キー（不足する 2 キーは英語の値が空） |
| ライセンス | GPL-3.0、`Copyright.txt` / `NOTICE.txt` にクレジットを記載 |

上流のパックがカバーしていないキーの読みは、このビルドのために rubi 記法で書き起こしました。
その際 `Alex's Japanese Furigana Language`（`ja_fw.json`）を参考にしています。再生成は
`python3 tools/merge_furigana_pack.py` で行えます（スクリプトは作者のワークスペースにあり、
このリポジトリには含まれていません。`resourcepack/NOTICE.txt` を参照）。

## 仕組み

- `§^…(…)` は `StringDecomposer.iterateFormatted` の中で解析されます。注釈全体は 1 つの `U+FFFC`
  コードポイントとして出力され、`Style` 上に `RubyText` ペイロードを載せます。26.3 の `Style` は
  不変なので、ペイロードはすべての `withXxx` のコピーへ引き継がれます。
- `StringSplitter.WidthProvider` が注釈のボックス幅を返すため、折り返し・中央揃え・ウィジェットへの
  収まりは通常のグリフと同じように動作します。
- `Font.prepareText` / `Font.prepare8xTextOutline` をフックします。ルビのスロットを含むシーケンスは
  mod 側で準備し（基底と読みのグリフを拡縮し、ボックス全体に均等に配置）、それ以外のシーケンスは
  そのままバニラに委ねます。

## ビルド

JDK 25 が必要です：

    ./gradlew build        # → build/libs/ruppi-<version>.jar

## ドキュメント

| | |
| --- | --- |
| 移植メモ | [English](PORTING.md) · [日本語](PORTING.ja.md) · [简体中文](PORTING.zh-CN.md) |
| 検証スクリーンショット | [`docs/verification/`](docs/verification) |

## ライセンス

GPL-3.0（上流の rubi から継承）。[LICENSE](LICENSE) を参照してください。

## クレジット

- 上流 rubi: keve1227 — https://github.com/keve1227/rubi
- Furigana リソースパック: Kevin Sundqvist Norlén — https://github.com/keve1227/furigana
- 26.3 移植と拡張リソースパック: SHIRO

[![](https://img.shields.io/badge/powered_by-dsh-4D6BFE?style=flat-square&logo=deepseek&logoColor=white)](https://github.com/deepseek-ai/deepseek-harness)
