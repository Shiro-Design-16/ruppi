# 26.3 移植说明

[English](PORTING.md) · [日本語](PORTING.ja.md) · **简体中文**

把上游 [rubi](https://github.com/keve1227/rubi)（Minecraft 1.20、Yarn 映射、Java 17）移植到
Minecraft 26.3（Mojang 官方映射、Loom 1.19、Java 25）。上游源码在 `../references/rubi`（只读参考）。

- modid `ruppi`，包 `com.shiro.ruppi`，版本 `1.1.0+26.3`，仅客户端。
- 产物：`build/libs/ruppi-1.1.0+26.3.jar`。

## 结构

| 上游（1.20 / Yarn） | 本工程（26.3 / mojmap） |
| --- | --- |
| RubyText、IRubyStyle、Utils、option/RubyRenderMode | 同名保留（全部按 26.3 类型重写，宽度/几何常量与上游逐字一致：TEXT_SCALE 0.8、RUBY_SCALE 0.5、RUBY_OVERLAP 0.1） |
| MixinStringVisitable（`StringVisitable.getString`） | MixinFormattedText（`FormattedText.getString`） |
| MixinTextVisitFactory（解析 `§^`） | MixinStringDecomposer（`StringDecomposer.iterateFormatted`） |
| MixinStyle | MixinStyle |
| MixinTextHandler（宽度） | MixinStringSplitter（`StringSplitter.WidthProvider`） |
| MixinGameOptions + MixinAccessibilityOptionsScreen | MixinOptions + MixinAccessibilityOptionsScreen |
| MixinTextRenderer（绘制） | MixinFont（`Font.prepareText` / `prepare8xTextOutline`）+ `client/render/*` 自建 `PreparedText` |
| MixinFormatting | 删除：只 `@Shadow` 了常量，没有任何行为改动 |
| MixinTextWidget | 删除：26.3 的 `StringWidget.getWidth` 已非 final 且走 `Font.width` |

上行 mixin 共 9 个，本工程 7 个。

## 26.3 的关键差异与处理

1. **没有 `GuiGraphics`**。GUI 文本提交点是 `GuiGraphicsExtractor.text(...)` → 缓存
   `GuiTextRenderState` → `GuiRenderer.prepareText()` 消费 `Font.PreparedText`。唯一稳定的挂钩点是
   `Font.prepareText(FormattedCharSequence,float,float,int,boolean,boolean,int)` 与
   `Font.prepare8xTextOutline(...)`：HEAD 注入，命中 ruby 时返回自建 `PreparedText`，否则返回 null 让原版继续。
   自建实现照抄 `Font` 的四段 visit 顺序（背景 → glyph → 效果线 → empty area），因为 GUI 平面模式下
   `flat=true` 会把所有深度压到 z=0，层叠完全由 visit 顺序决定。
2. **`Style` 是不可变类**，没有 `withParent`，11 个 `withXxx` 全部返回新对象，且
   `checkEmptyAfterChange` 会用 `equals(EMPTY)` 把新对象折叠回 `EMPTY` 单例。因此：
   ruby 载荷必须被复制到每一个返回值（单个多目标 `@Inject(at = RETURN)` 覆盖 17 个方法），
   `equals`/`hashCode` 必须把 ruby 纳入（否则带 ruby 的 Style 会被误折叠成 EMPTY），
   往 `Style.EMPTY` 上挂载时必须新建对象而不是污染单例。复制构造器通过 access widener 放开。
3. **access widener 用 `official` 命名空间**：模板工程没有 mappings 依赖，jar 本身就是 mojmap 命名。
   条目写 `named` 会在配置期硬失败（`Expected official namespace for access widener entry, found: named`）。
   条目只有两条：`Options$OptionAccess`（选项接入 load/save/dump）和 `Style` 的 11 参构造器。
4. **`Options.Visitor` 已删除**，改成 `Options.processDumpedOptions(Options.OptionAccess)` 一个私有方法承载
   load / save / dump 三条路径，注入它即可同时覆盖三者。选项控件用
   `OptionInstance.SliderableEnum<T>(List<T>, Codec<T>)`，因此 `RubyRenderMode` 自带 `Codec.INT.xmap(byId, getId)`；
   枚举的显示名用 caption 回调 + `Options.genericValueLabel`（`TranslatableOption` 已不存在）。
5. **`AccessibilityOptionsScreen` 没有 `getOptions()`**，只有 `private static OptionInstance<?>[] options(Options)`，
   在其 RETURN 处复制数组并追加一项。追加到末尾是因为 `addOptions()` 只对 `options[0]` 做特殊排版。
6. **屏幕归属变了**：`Minecraft.setScreen` 与 `Minecraft.screen` 都不再存在，改用
   `Minecraft.setScreenAndShow(Screen)` / `Minecraft.gui.screen()` / `Minecraft.gui.setScreen(Screen)`。
7. **宽度是 float**：`StringSplitter.WidthProvider` 是 `float getWidth(int, Style)`，`Font.width` 最终 `Mth.ceil`。
   ruby 槽在 `StringSplitter` 构造器 **RETURN** 处被包一层：码点 `U+FFFC` 且带 ruby 时返回盒宽，其余原样委托。
   注意：`@ModifyVariable(argsOnly = true, at = @At(value = "FIELD", opcode = PUTFIELD))` 打在
   `this.widthProvider = widthProvider` 上**不会真正改到写入的值**（javac 在这之前已经 `aload_1`，
   插入点落在 load 与 putfield 之间，handler 改的是局部变量，存进去的仍是原值），症状是宽度计算静默失效，
   见「修复记录」第 1 条。

## 验收记录

构建与启动：

    JAVA_HOME=<JDK 25> ./gradlew build          # BUILD SUCCESSFUL
    JAVA_HOME=<JDK 25> ./gradlew runClient      # 标题画面，日志无任何 mixin / 注入异常

人工验证证据（`docs/verification/`，均来自 26.3 dev 客户端实机截图）：

- `full-above-cjk.png`：无障碍设置页，"Rubies" 一行渲染 `§^設定(せってい)`，注音在正文上方。
- `row-1-above-cjk.png` … `row-4-hidden-cjk.png`：同一行在 Above / Below / Replace / Hidden 四模式下各一张。
- `row-1-above-latin.png`：英文 demo（`§^Above (Above) Text`）。

另外三项运行时事实：

- `run/options.txt` 出现 `ruppi.renderMode:0`：选项确实接入了 Options 的 load/save 路径
  （access widener + `Options.OptionAccess` + `Codec` 三处串联生效）。
- 资源包重载日志顺序为 `vanilla, …, ruppi`，说明 mod 资源包在栈中位于原版之上。
- 启动日志中 `Mixin` 0.8.7 + MixinExtras 0.5.5 正常应用 7 个 mixin，`defaultRequire: 1` 未触发任何 `InjectionError`。

验证时用过临时脚手架（一个轮播四模式并调用 `Screenshot.grab` 的 entrypoint、把选项临时挪到列表首位、
语言切到 en_us、语言文件里放 demo 键），交付前已全部移除：`fabric.mod.json` 的 `client` 入口只剩
`com.shiro.ruppi.RuppiClient`，jar 内不含任何验收类，语言文件只保留选项自己的 5 个键。

## 修复记录（实机验证后）

1. **带注音的文字整体向右偏移（宽度少算）**。
   `StringSplitter` 的宽度提供者包装最初用 `@ModifyVariable(argsOnly = true)` + `@At(value = "FIELD", opcode = PUTFIELD)`，
   编译与启动都不报错，但运行时根本没换掉字段（反射读到的仍是 `Font$$Lambda`）。后果：`Font.width` 把一个
   `U+FFFC` 槽当成单个缺字方块（9 px）来算，而渲染按盒宽前进，于是居中文本被往右推半个差额。
   实测（1708×960，界面缩放 4）：标题界面 `設定...` 按钮文字中心 665.5 vs 按钮中心 645.5 = **右偏 20 设备像素（5 GUI px）**；
   改成构造器 RETURN 用 `@Shadow @Final @Mutable` 直接赋值后，同一测量为 646.0 vs 645.5（**+0.5 设备像素**）。
2. **注音段吞掉紧随其后的字符**。`§^正文(注音)` 的解析注入点在 `ChatFormatting.getByCode` 调用处，之后还有
   **两处**自增（`§` 分支自己的 `i++` 与 `for` 的 `i++`），旧实现 `index.set(matcher.end() - 1)` 只算了一处，
   于是每个 ruby 段后紧跟的一个字符被跳过（`設定...` 实际只画成 `設定`）。改为 `matcher.end() - 2`。
   序列级证据（`Component.literal(...).getVisualOrderText()`）：修复前 `A§^設(せつ)B` → `[41][fffc]`，
   修复后 → `[41][fffc][42]`，宽度同步从 15 变为 21。

## 语言叠加：`*_rubi` 与基语言

`MixinLanguageManager` 用 `@WrapOperation` 包住 `ClientLanguage.loadFrom`（不用 `@Redirect`，是为了与同样包装该调用的其它 mod 共存）。选中 `ja_jp_rubi` 这类以 `_rubi` 结尾的语言时，基语言 `ja_jp` 被插到语言栈的**索引 1**——紧贴 `en_us` 之上、其它所有回退之下，独立运行时栈为 `[en_us, ja_jp, ja_jp_rubi]`。rubi 包没有提供的键因此回落到基语言而不是英语，只提供 `ja_jp` 的其它 mod 也保住自己的翻译。

这个低位置是刻意选的：ruppi 只保证「至少别再退回英语」，不与其它 mod 配置的更高优先级链争夺。[secondlang](https://github.com/Shiro-Design-16/secondlang) 的回退链（例如 `ja_jp_rubi → zh_cn → ja_jp`）因此压过这条自动规则，两者可以共存。只有某个资源包（含本 mod 自带的 `assets/ruppi/lang/ja_jp.json`）提供了 `lang/<base>.json` 时才插入；栈中已有该语言时不重复插入。实机验证：栈为 `[en_us, ja_jp, ja_jp_rubi]`，ruppi 自己的 `options.ruppi.renderMode` 取到日文「ルビ表示」。

## 已知限制

- **不能覆盖原版文案**：原版资源包在语言合并时优先于 mod 资源包，mod 提供的 `en_us.json` 改不动
  `menu.singleplayer` 这类键。要在原版界面显示注音，需要一个包含 `§^` 标记的资源包在 mod 之上启用
  （这也是上游 rubi 的用法）。
- **调试 gizmo 文本未覆盖**：`Font.prepareText(String, …)` 重载（`GizmoFeatureRenderer` 使用）没有挂钩；
  文本展示实体走的是 `FormattedCharSequence` 重载，已覆盖。
- **CJK 下注音与正文贴得很近**：几何完全沿用上游公式（注音基线 = 正文基线 - 0.5×行高 + 0.1×行高），
  CJK 字形几乎填满 em 框，所以视觉上会压到正文顶部；这是上游既有表现，不是移植引入的。
- **空格的 hover 命中框**：自建的 empty area 上沿用了行高相关的常量，缩放下可能有几像素偏差（只影响空格区域的交互判定）。

## 复现

    ./gradlew build     # 需要 JDK 25
    ./gradlew runClient # dev 客户端，run/ 为工作目录

## 附：Furigana 资源包（Furigana-v4+1）的 26.3 适配

26.3 起资源包版本号从单个整数改为 `major.minor`（当前 `97.1`），并且把「支持范围」写进新字段。
老的 `"pack_format": 34` + `"supported_formats": {"min_inclusive": 33, "max_inclusive": 38}` 在 26.3 里
会被判成「旧版本包」：加载器仍允许强制启用，但资源包列表会标红不兼容。

判据在 `net/minecraft/server/packs/metadata/pack/PackFormat.java`：`lastPreMinorVersion(CLIENT_RESOURCES) = 64`，
只有 `major ≤ 64` 的声明才走旧字段路径；超过 64 且缺少 `min_format`/`max_format` 会直接报
"declares support for version newer than 64, but is missing mandatory fields min_format and max_format"。
`min_format`/`max_format` 由 `ExtraCodecs.compactListCodec` 解析，可以写整数（只写 major）或 `[major, minor]`；
只写整数时 `min` 的 minor 补 0、`max` 的 minor 补 `Integer.MAX_VALUE`，等价于「整个 97.x」。

适配后的 `pack.mcmeta`（其余内容与原件一致）：

    {
      "pack": {
        "description": "Provides Japanese furigana for use with the Rubi mod. (26.3 port)",
        "min_format": 97,
        "max_format": 97
      },
      "language": { "ja_jp_rubi": { … } }
    }

语言部分不需要改动：`LanguageMetadataSection` 仍是 `Map<语言代码, LanguageInfo>`，代码长度限制 1–16 字符，
`ja_jp_rubi`（10 字符）合法；语言文件路径仍是 `assets/minecraft/lang/ja_jp_rubi.json`。
实测 `PackCompatibility.forVersion` 返回 `COMPATIBLE`，语言列表里出现 `ja_jp_rubi`，选中后注音正常渲染。

键集演进：适配 26.3 时包内 6874 个键全部仍在，但缺 1685 个 26.3 新增键（语言栈是 `en_us + ja_jp_rubi`，
缺失键回退英文、没有注音）。这 1685 个键后来用方案 A 从第三方包补齐，见下一节；补齐后包内 **8557 键**，
对照 26.3 `en_us`（8559 键）只缺 `potion.potency.0`、`selectWorld.gameMode.spectator.line2` 两个英文原值本就为空的键。

交付：`Furigana-v4+1-26.3/`（目录）与 `Furigana-v4+1-26.3.zip`（直接丢进 `resourcepacks/`）。
验收截图：`docs/verification/furigana-pack-title-above.png`、`furigana-pack-language-list.png`。

## 附二：合并第三方 `furigana-pack` 的读法（方案 A）

用户提供了另一个人做的包 `ruppi/furigana-pack`（"Alex's Japanese Furigana Language"，声明 `pack_format 121`），
它的记号与 rubi 不兼容：

| 包 | 记号 | 例 |
| --- | --- | --- |
| rubi / Furigana-v4+1 | `§^base(读法)`，送假名留在括号外 | `§^押(お)す`、`§^音声読(おんせいよ)み§^上(あ)げ`、`§^設定(せってい)` |
| furigana-pack `ja_fw` | `词(读法)`，括号紧跟在词后 | `設定(せってい)`、`音声読み上げ(おんせいよみあげ)` |
| furigana-pack `ja_fe` | 读法整串挂在句末 | `… (おす\|おんせいよみあげ\|ゆうこう)` — 无法反推每个词的读法，**不可解析** |

评估结论：**选 A（把读法转换成 rubi 记号补进 Furigana-v4+1），不改 mod 去解析括号**。理由：

1. 两包是同一译文谱系（8557 个重合键的纯文本一致率 98.1%，128 处差异只是措辞微调），合并不产生术语冲突。
2. 改 mod 要在渲染热路径做同样的日语分词启发式，且必须对全部 8557 键用粗粒度方案，反而把已有 6874 键的精确注音改差。
3. `ja_fe` 的记号无论如何不可解析。
4. A 是纯数据产物，可逐条审查、零运行时风险；渲染路径（已像素级验收）完全不动。

转换脚本：`tools/merge_furigana_pack.py`（`--check` 只校验不写盘）。算法：

- 读法必须是全平假名（可含 `ー`）；从前一个 `(` 向前跳过假名得到送假名段、再跳过汉字得到 base 候选，base 必须含汉字。
- 长音便（送假名）剥离：base 末尾与读法末尾相同的假名从两侧一起剥掉（`押す`/`おす` → base `押`、读法 `お`、外面的 `す` 留下）。
- 置信门限：`0.4 ≤ len(读法)/汉字数 ≤ 3.0`；剥离过送假名时额外要求 `≤ 2.2`（挡住 `手に入れ(てにいれ)る` 这类"读法跨到前面汉字"的误配）。
  不置信的括号原样留在文中（保住读法信息，不产出错误注音）。
- 只处理 base 包没有的键，base 的 6874 条逐字节不动。

结果：新增 1683 键（其中 1333 键带注音、24 键部分括号保留、326 键纯文本、无法解析 0 键），合并后 8557 键、14898 个注音 span。
校验：base 6874 条逐字节相同；新增键"可见文本"与源文本去括号后完全一致（0 处文本丢失）。
抽样：`ブロック§^輪郭(りんかく)のコントラスト§^強化(きょうか)`、`§^模様入(もようい)りの§^辰砂(しんしゃ)`、`メインメニューの§^黄色(きいろ)いスプラッシュテキストを§^非表示(ひひょうじ)にします。`

验证：`docs/verification/pack-merge-accessibility-bottom.png`（无障碍设置页滚到底部，26.3 新增键
`options.hideSplashTexts`、`options.hideLightningFlashes`、`options.panoramaSpeed` 等已显示日文并带注音）、
`pack-merge-new-keys.png`（自绘屏逐行渲染 8 个新增键）。实测截图见 `docs/verification/`。

许可与署名：本包不是把 `furigana-pack` 的文件搬过来，而是**参照它的读音、用 rubi 记号自己写的一份语言文件**；
因此整包继续按 **GPL-3.0** 分发，署名保留上游 + 本次移植：

- `Copyright.txt`：`© Kevin Sundqvist Norlén 2024 - Furigana v4 base keys` 与 `© SHIRO 2026 - 26.3 port: pack metadata and the added keys`；
- `NOTICE.txt`：说明 base 键来源、新增键的写法与参照来源（含 `ja_fe` 不可用）、GPL-3.0 覆盖整包、上游仓库地址；
- `pack.mcmeta` 的 description 末尾标注 `GPL-3.0`。

重新生成只需 `python3 tools/merge_furigana_pack.py`。

