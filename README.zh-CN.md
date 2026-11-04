[English](README.md) · [日本語](README.ja.md) · **简体中文**

# Ruppi

把 keve1227 的 [rubi](https://github.com/keve1227/rubi)（GPL-3.0）移植到 Minecraft **26.3**。
Ruppi 为客户端渲染的任意文本绘制注音——振假名 / 读音提示。

Minecraft 1.20 → 26.3 换掉了整条渲染管线（26.3 客户端已没有 `GuiGraphics`），因此这里是按 rubi 的行为重写，而不是机械升版。移植细节见 [PORTING.zh-CN.md](PORTING.zh-CN.md)。

## 环境要求

- Minecraft 26.3
- Fabric Loader 0.20.1+
- Fabric API（任意 26.3 构建）
- Java 25

仅客户端。

## 用法

在客户端绘制的任意字符串里内联书写注音（语言文件、聊天、物品名、NBT 文本）：

    §^text(ruby)

`§^` 开启注音，正文是括号之前的全部内容，读法是括号里的内容：

    §^漢字(かんじ) と §^Tokyo(とうきょう)

注音占用一个文本槽位，其宽度随当前模式变化，因此切换模式时布局保持不变。

四个模式位于 **选项 → 无障碍设置 → Rubies**（持久化为 `options.txt` 中的 `ruppi.renderMode`）：

| 模式 | 绘制 |
| --- | --- |
| 上方 | 正文 80 %，读法 50 %，位于正文上方 |
| 下方 | 正文 80 %，读法 50 %，位于正文下方 |
| 替换 | 仅读法，全尺寸 |
| 隐藏 | 仅正文，全尺寸 |

### 为原版文案添加注音

资源包堆叠在 mod 资源之上，原版包会压过 mod 提供的 `en_us.json`——mod 无法改写 `menu.singleplayer`。要给原版文案加注音，需要发布一个资源包，在其中以 `§^` 标记这些键，并在 mod 之上启用它。mod 自带的语言文件只覆盖它自己的选项标签。

## 配套资源包：日文注音

本仓库的 `resourcepack/`（每次发布也会附上 `Furigana-v4+1-26.3.zip`）是 Kevin Sundqvist Norlén 的 [Furigana](https://github.com/keve1227/furigana) 的 26.3 构建，并扩展到覆盖 26.3 `en_us` 语言文件中的每一个键，因此原版界面里的日文也会带注音。

| | |
| --- | --- |
| 直接安装 | `resourcepacks/Furigana-v4+1-26.3.zip` → 在 mod 之上启用 |
| 语言条目 | `日本語（ふりがな）`（`ja_jp_rubi`） |
| 覆盖 | 8557 / 8559 个 `en_us` 键（缺失的两个键英文原值为空） |
| 许可 | GPL-3.0，署名见 `Copyright.txt` / `NOTICE.txt` |

上游包未覆盖的键，其读法按 rubi 记号为本构建写出；这些读法参照了 `Alex's Japanese Furigana Language`（`ja_fw.json`）。重新生成用 `python3 tools/merge_furigana_pack.py`（该脚本位于作者工作区，不在本仓库内——见 `resourcepack/NOTICE.txt`）。

## 工作原理

- `§^…(…)` 在 `StringDecomposer.iterateFormatted` 内解析；整段注音输出为单个 `U+FFFC` 码点，并在 `Style` 上携带 `RubyText` 载荷。26.3 的 `Style` 是不可变类，因此载荷要穿过每一个 `withXxx` 拷贝。
- `StringSplitter.WidthProvider` 上报注音的盒宽，因此换行、居中和控件适配都与普通字形一致。
- `Font.prepareText` / `Font.prepare8xTextOutline` 被拦截：含注音槽的序列由 mod 自行准备（正文与读法的字形按比例缩放，并在盒内均匀分布）；其余序列原样交回原版。

## 构建

需要 JDK 25：

    ./gradlew build        # → build/libs/ruppi-<version>.jar

## 文档

| | |
| --- | --- |
| 移植笔记 | [English](PORTING.md) · [日本語](PORTING.ja.md) · [简体中文](PORTING.zh-CN.md) |
| 验证截图 | [`docs/verification/`](docs/verification) |

## 许可

GPL-3.0，继承自上游 rubi。见 [LICENSE](LICENSE)。

## 致谢

- 上游 rubi：keve1227 — https://github.com/keve1227/rubi
- Furigana 资源包：Kevin Sundqvist Norlén — https://github.com/keve1227/furigana
- 26.3 移植与扩展资源包：SHIRO

[![](https://img.shields.io/badge/powered_by-dsh-4D6BFE?style=flat-square&logo=deepseek&logoColor=white)](https://github.com/deepseek-ai/deepseek-harness)
