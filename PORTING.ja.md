[English](PORTING.md) · **日本語** · [简体中文](PORTING.zh-CN.md)

# 26.3 移植メモ

上流の [rubi](https://github.com/keve1227/rubi)（Minecraft 1.20、Yarn マッピング、Java 17）を
Minecraft 26.3（Mojang 公式マッピング、Loom 1.19、Java 25）へ移植しました。上流のソースは
`../references/rubi`（読み取り専用の参照）にあります。

- modid `ruppi`、パッケージ `com.shiro.ruppi`、バージョン `1.1.0+26.3`、クライアント専用です。
- 成果物：`build/libs/ruppi-1.1.0+26.3.jar`。

## 構成

| 上流（1.20 / Yarn） | 本プロジェクト（26.3 / mojmap） |
| --- | --- |
| RubyText、IRubyStyle、Utils、option/RubyRenderMode | 同名で保持（すべて 26.3 の型で書き直し。幅・ジオメトリの定数は上流と一字一句同じ：TEXT_SCALE 0.8、RUBY_SCALE 0.5、RUBY_OVERLAP 0.1） |
| MixinStringVisitable（`StringVisitable.getString`） | MixinFormattedText（`FormattedText.getString`） |
| MixinTextVisitFactory（`§^` を解析） | MixinStringDecomposer（`StringDecomposer.iterateFormatted`） |
| MixinStyle | MixinStyle |
| MixinTextHandler（幅） | MixinStringSplitter（`StringSplitter.WidthProvider`） |
| MixinGameOptions + MixinAccessibilityOptionsScreen | MixinOptions + MixinAccessibilityOptionsScreen |
| MixinTextRenderer（描画） | MixinFont（`Font.prepareText` / `prepare8xTextOutline`）+ `client/render/*` の自前 `PreparedText` |
| MixinFormatting | 削除：定数を `@Shadow` していただけで、挙動の変更はない |
| MixinTextWidget | 削除：26.3 の `StringWidget.getWidth` は final ではなくなり、`Font.width` を通るようになった |

上流の mixin は計 9 個、本プロジェクトは 7 個です。

## 26.3 の主な差分と対処

1. **`GuiGraphics` がない**。GUI テキストの投入点は `GuiGraphicsExtractor.text(...)` → キャッシュ
   `GuiTextRenderState` → `GuiRenderer.prepareText()` が `Font.PreparedText` を消費する経路です。
   唯一安定したフック点は `Font.prepareText(FormattedCharSequence,float,float,int,boolean,boolean,int)` と
   `Font.prepare8xTextOutline(...)` で、HEAD に注入し、ルビに該当する場合は自前の `PreparedText` を返し、
   それ以外は null を返してバニラに処理を続けさせます。
   自前実装は `Font` の 4 段の visit 順（背景 → グリフ → エフェクト線 → empty area）をそのまま踏襲しています。
   GUI 平面モードでは `flat=true` によりすべての深度が z=0 に潰され、重なりは visit 順だけで決まるためです。
2. **`Style` は不変クラス**で、`withParent` はなく、11 個の `withXxx` はすべて新しいオブジェクトを返し、
   さらに `checkEmptyAfterChange` が `equals(EMPTY)` で新しいオブジェクトを `EMPTY` シングルトンへ畳み込みます。
   そのため、ルビのペイロードはすべての戻り値へ複製する必要があり（単一の複数ターゲット `@Inject(at = RETURN)` で
   17 個のメソッドをカバー）、`equals`/`hashCode` にルビを含める必要があり（そうしないとルビ付きの `Style` が
   誤って EMPTY に畳み込まれます）、`Style.EMPTY` に載せるときはシングルトンを汚さず新しいオブジェクトを
   作る必要があります。コピーコンストラクタは access widener で開放します。
3. **access widener は `official` 名前空間を使う**：テンプレートプロジェクトには mappings 依存がなく、
   jar 自体が mojmap 命名です。エントリを `named` で書くと構成時にハード失敗します
   （`Expected official namespace for access widener entry, found: named`）。
   エントリは 2 つだけです：`Options$OptionAccess`（オプションを load/save/dump に接続）と
   `Style` の 11 引数コンストラクタ。
4. **`Options.Visitor` は削除され**、load / save / dump の 3 経路を private メソッド
   `Options.processDumpedOptions(Options.OptionAccess)` 1 つが担う形になりました。ここに注入すれば
   3 つすべてを同時にカバーできます。オプションのコントロールは
   `OptionInstance.SliderableEnum<T>(List<T>, Codec<T>)` を使うため、`RubyRenderMode` は
   `Codec.INT.xmap(byId, getId)` を自前で持ちます。enum の表示名は caption コールバック +
   `Options.genericValueLabel` で付けます（`TranslatableOption` はもう存在しません）。
5. **`AccessibilityOptionsScreen` には `getOptions()` がなく**、
   `private static OptionInstance<?>[] options(Options)` だけがあります。その RETURN で配列をコピーし、
   1 項目を追加します。末尾に追加するのは、`addOptions()` が `options[0]` にだけ特別なレイアウトを
   行うためです。
6. **画面の持ち主が変わった**：`Minecraft.setScreen` も `Minecraft.screen` も存在しなくなり、
   `Minecraft.setScreenAndShow(Screen)` / `Minecraft.gui.screen()` / `Minecraft.gui.setScreen(Screen)` を
   使うようになりました。
7. **幅は float**：`StringSplitter.WidthProvider` は `float getWidth(int, Style)` で、`Font.width` は
   最終的に `Mth.ceil` します。ルビのスロットは `StringSplitter` のコンストラクタ **RETURN** で 1 層
   ラップし、コードポイントが `U+FFFC` でルビを持つ場合はボックス幅を返し、それ以外はそのまま委譲します。
   注意：`@ModifyVariable(argsOnly = true, at = @At(value = "FIELD", opcode = PUTFIELD))` を
   `this.widthProvider = widthProvider` に当てても**書き込まれる値は実際には変わりません**（javac は
   この時点ですでに `aload_1` を済ませており、挿入点が load と putfield の間に落ちるため、ハンドラが
   変更するのはローカル変数で、格納されるのは元の値のままです）。症状は幅の計算が静かに失敗することで、
   「修正記録」の 1 番目を参照してください。

## 検収記録

ビルドと起動：

    JAVA_HOME=<JDK 25> ./gradlew build          # BUILD SUCCESSFUL
    JAVA_HOME=<JDK 25> ./gradlew runClient      # タイトル画面。ログに mixin / 注入の例外は一切なし

人手による検証の証跡（`docs/verification/`、いずれも 26.3 の dev クライアントでの実機スクリーンショット）：

- `full-above-cjk.png`：アクセシビリティ設定画面で、「Rubies」の行に `§^設定(せってい)` が描画され、
  ルビが本文の上に付いている。
- `row-1-above-cjk.png` … `row-4-hidden-cjk.png`：同じ行を Above / Below / Replace / Hidden の
  4 モードで 1 枚ずつ撮ったもの。
- `row-1-above-latin.png`：英語のデモ（`§^Above (Above) Text`）。

さらに実行時の事実が 3 つあります：

- `run/options.txt` に `ruppi.renderMode:0` が現れる：オプションが実際に Options の load/save 経路へ
  接続されている（access widener + `Options.OptionAccess` + `Codec` の 3 か所が連鎖して効いている）。
- リソースパック再読み込みのログ順が `vanilla, …, ruppi` で、mod のリソースパックがスタック上で
  バニラより上にあることを示す。
- 起動ログでは `Mixin` 0.8.7 + MixinExtras 0.5.5 が 7 個の mixin を正常に適用し、`defaultRequire: 1` で
  `InjectionError` は 1 つも発生していない。

検証時には一時的な足場（4 モードを切り替えて `Screenshot.grab` を呼ぶ entrypoint、オプションを一時的に
リスト先頭へ移動、言語を en_us に切り替え、言語ファイルにデモ用キーを追加）を使いましたが、納品前に
すべて削除しました：`fabric.mod.json` の `client` エントリは `com.shiro.ruppi.RuppiClient` のみ、jar に
検収用クラスは含まれず、言語ファイルにはオプション自身の 5 キーだけが残っています。

## 修正記録（実機検証後）

1. **ルビ付きのテキスト全体が右へずれる（幅の計算不足）**。
   `StringSplitter` の幅プロバイダのラップは当初 `@ModifyVariable(argsOnly = true)` +
   `@At(value = "FIELD", opcode = PUTFIELD)` を使っていましたが、コンパイルも起動もエラーにならないのに、
   実行時にはフィールドが置き換わっていませんでした（リフレクションで読んでも `Font$$Lambda` のまま）。
   結果として `Font.width` は `U+FFFC` のスロットを欠字の四角 1 個（9 px）として計算し、描画はボックス幅で
   進むため、中央揃えのテキストが差分の半分だけ右へ押し出されます。
   実測（1708×960、GUI スケール 4）：タイトル画面の `設定...` ボタンはテキスト中心 665.5 対 ボタン中心 645.5
   = **右へ 20 デバイスピクセル（5 GUI px）**。コンストラクタ RETURN で `@Shadow @Final @Mutable` を使って
   直接代入する方式に変えた後、同じ測定が 646.0 対 645.5（**+0.5 デバイスピクセル**）になりました。
2. **ルビ区間が直後の文字を飲み込む**。`§^本文(ルビ)` の解析の注入点は `ChatFormatting.getByCode` の
   呼び出し箇所で、その後に**2 か所**のインクリメント（`§` 分岐自身の `i++` と `for` の `i++`）があります。
   旧実装の `index.set(matcher.end() - 1)` は 1 か所分しか数えていなかったため、各ルビ区間の直後の 1 文字が
   飛ばされていました（`設定...` が実際には `設定` としか描かれない）。`matcher.end() - 2` に変更しました。
   シーケンスレベルの証拠（`Component.literal(...).getVisualOrderText()`）：修正前は `A§^設(せつ)B` →
   `[41][fffc]`、修正後は → `[41][fffc][42]`、幅も 15 から 21 に変わりました。

## 既知の制限

- **バニラの文言は上書きできない**：バニラのリソースパックは言語のマージ時に mod のリソースパックより
  優先されるため、mod が提供する `en_us.json` では `menu.singleplayer` のようなキーを変更できません。
  バニラの画面にルビを表示するには、`§^` 記法を含むリソースパックを mod より上で有効化する必要が
  あります（これが上流 rubi の使い方でもあります）。
- **デバッグ gizmo のテキストは未対応**：`Font.prepareText(String, …)` のオーバーロード
  （`GizmoFeatureRenderer` が使用）にはフックを入れていません。テキスト表示エンティティは
  `FormattedCharSequence` のオーバーロードを通るため、対応済みです。
- **CJK ではルビが本文に非常に近い**：ジオメトリは上流の式をそのまま使っており（ルビのベースライン =
  本文のベースライン - 0.5×行の高さ + 0.1×行の高さ）、CJK のグリフは em ボックスをほぼ埋めるため、
  見た目上は本文の上端に重なります。これは上流からの既存の挙動で、移植によって生じたものではありません。
- **空白の hover 判定矩形**：自前の empty area は行の高さに関する定数をそのまま流用しているため、
  スケーリングによって数ピクセルのずれが生じる可能性があります（空白領域のインタラクション判定にのみ影響）。

## 再現

    ./gradlew build     # JDK 25 が必要
    ./gradlew runClient # dev クライアント、run/ が作業ディレクトリ

## 付録：Furigana リソースパック（Furigana-v4+1）の 26.3 対応

26.3 からリソースパックのバージョン番号は単一の整数から `major.minor`（現在は `97.1`）に変わり、
対応範囲を新しいフィールドに書くようになりました。古い `"pack_format": 34` +
`"supported_formats": {"min_inclusive": 33, "max_inclusive": 38}` は 26.3 では「旧バージョンのパック」と
判定されます：ローダーは強制有効化を依然として許しますが、リソースパック一覧では赤く不適合と表示されます。

判定は `net/minecraft/server/packs/metadata/pack/PackFormat.java` にあり、
`lastPreMinorVersion(CLIENT_RESOURCES) = 64` です。`major ≤ 64` の宣言だけが旧フィールドの経路を
通ります。64 を超えていて `min_format`/`max_format` が欠けている場合は、そのまま
"declares support for version newer than 64, but is missing mandatory fields min_format and max_format"
を報告します。`min_format`/`max_format` は `ExtraCodecs.compactListCodec` が解析し、整数（major のみ）
または `[major, minor]` を書けます。整数のみのときは `min` の minor が 0、`max` の minor が
`Integer.MAX_VALUE` で補われ、「97.x 全体」と等価になります。

対応後の `pack.mcmeta`（それ以外の内容は元ファイルと同じ）：

    {
      "pack": {
        "description": "Provides Japanese furigana for use with the Rubi mod. (26.3 port)",
        "min_format": 97,
        "max_format": 97
      },
      "language": { "ja_jp_rubi": { … } }
    }

言語の部分は変更不要です：`LanguageMetadataSection` は依然として `Map<言語コード, LanguageInfo>` で、
コードの長さ制限は 1–16 文字、`ja_jp_rubi`（10 文字）は有効です。言語ファイルのパスも
`assets/minecraft/lang/ja_jp_rubi.json` のままです。実測では `PackCompatibility.forVersion` が
`COMPATIBLE` を返し、言語一覧に `ja_jp_rubi` が現れ、選択するとルビが正常に描画されました。

キー集合の推移：26.3 へ対応した時点でパック内の 6874 キーはすべて残っていましたが、26.3 で新規追加された
1685 キーが欠けていました（言語スタックは `en_us + ja_jp_rubi` で、欠けたキーは英語にフォールバックし、
ルビは付きません）。この 1685 キーは後に案 A でサードパーティのパックから補完しました（次の節を参照）。
補完後、パック内は **8557 キー**となり、26.3 の `en_us`（8559 キー）と比べて欠けているのは
`potion.potency.0`、`selectWorld.gameMode.spectator.line2` の 2 キーだけで、いずれも英語の元値が空の
キーです。

配布物：`Furigana-v4+1-26.3/`（ディレクトリ）と `Furigana-v4+1-26.3.zip`（`resourcepacks/` にそのまま
入れるだけ）。検収スクリーンショット：`docs/verification/furigana-pack-title-above.png`、
`furigana-pack-language-list.png`。

## 付録 2：サードパーティの `furigana-pack` を統合する方針（案 A）

ユーザーから、別の作者によるパック `ruppi/furigana-pack`（"Alex's Japanese Furigana Language"、
`pack_format 121` を宣言）が提供されましたが、その記法は rubi と互換性がありません：

| パック | 記法 | 例 |
| --- | --- | --- |
| rubi / Furigana-v4+1 | `§^base(読み)`、送り仮名は括弧の外に残る | `§^押(お)す`、`§^音声読(おんせいよ)み§^上(あ)げ`、`§^設定(せってい)` |
| furigana-pack `ja_fw` | `語(読み)`、括弧は語の直後 | `設定(せってい)`、`音声読み上げ(おんせいよみあげ)` |
| furigana-pack `ja_fe` | 読みを丸ごと文末にまとめる | `… (おす\|おんせいよみあげ\|ゆうこう)` — 各語の読みを逆算できず、**解析不可** |

評価の結論：**案 A を採用（読みを rubi 記法に変換して Furigana-v4+1 へ補い、mod 側で括弧を解析する
変更は行わない）**。理由：

1. 両パックは同じ翻訳の系統で（重複する 8557 キーのプレーンテキスト一致率は 98.1 %、128 か所の差分は
   文言の微調整にすぎない）、統合しても用語の衝突は起きない。
2. mod を変更すると、描画のホットパスで同じ日本語の分かち書きヒューリスティックを実行することになり、
   しかも 8557 キー全体に粗い方式を使う必要があるため、既存 6874 キーの精密なルビをかえって悪化させる。
3. `ja_fe` の記法はどのみち解析できない。
4. 案 A は純粋なデータ成果物で、1 件ずつレビューでき、実行時リスクはゼロ。描画経路（ピクセル単位で
   検収済み）には一切手を加えない。

変換スクリプト：`tools/merge_furigana_pack.py`（`--check` は検証のみで書き込みなし）。アルゴリズム：

- 読みはすべて平仮名（`ー` を含んでよい）であること。直前の `(` から後ろ向きに仮名を読み飛ばして
  送り仮名の区間を得て、さらに漢字を読み飛ばして base 候補を得る。base は漢字を含む必要がある。
- 送り仮名の剥離：base の末尾と読みの末尾が同じ仮名なら、両側から同時に剥がす（`押す`/`おす` →
  base `押`、読み `お`、外に残る `す` はそのまま）。
- 信頼度のしきい値：`0.4 ≤ len(読み)/漢字数 ≤ 3.0`。送り仮名を剥がした場合はさらに `≤ 2.2` を要求する
  （`手に入れ(てにいれ)る` のように「読みが前の漢字までまたぐ」誤対応を防ぐ）。信頼できない括弧は
  そのまま本文に残す（読みの情報を保ち、誤ったルビを出さない）。
- base パックにないキーだけを処理し、base の 6874 件はバイト単位で変更しない。

結果：1683 キーを新規追加（うち 1333 キーはルビ付き、24 キーは括弧の一部を保留、326 キーは
プレーンテキスト、解析不能 0 キー）。統合後は 8557 キー、ルビ span は 14898 個。
検証：base の 6874 件はバイト単位で同一。追加キーの「可視テキスト」は、ソーステキストから括弧を除いた
ものと完全に一致（テキスト欠落 0 か所）。
サンプル：`ブロック§^輪郭(りんかく)のコントラスト§^強化(きょうか)`、`§^模様入(もようい)りの§^辰砂(しんしゃ)`、
`メインメニューの§^黄色(きいろ)いスプラッシュテキストを§^非表示(ひひょうじ)にします。`

検証：`docs/verification/pack-merge-accessibility-bottom.png`（アクセシビリティ設定画面を最下部まで
スクロールし、26.3 の新規キー `options.hideSplashTexts`、`options.hideLightningFlashes`、
`options.panoramaSpeed` などが日本語でルビ付きで表示されている）、`pack-merge-new-keys.png`（自前の画面で
追加キー 8 個を 1 行ずつ描画）。実測スクリーンショットは `docs/verification/` を参照。

ライセンスとクレジット：本パックは `furigana-pack` のファイルをそのまま持ってきたものではなく、
**その読みを参照して rubi 記法で自ら書き起こした言語ファイル**です。したがってパック全体は引き続き
**GPL-3.0** で配布し、クレジットは上流 + 今回の移植を残します：

- `Copyright.txt`：`© Kevin Sundqvist Norlén 2024 - Furigana v4 base keys` と
  `© SHIRO 2026 - 26.3 port: pack metadata and the added keys`；
- `NOTICE.txt`：base キーの出所、追加キーの書き方と参照元（`ja_fe` が使えないことを含む）、GPL-3.0 が
  パック全体を覆うこと、上流リポジトリの URL を記述；
- `pack.mcmeta` の description の末尾に `GPL-3.0` を明記。

再生成は `python3 tools/merge_furigana_pack.py` だけで済みます。
