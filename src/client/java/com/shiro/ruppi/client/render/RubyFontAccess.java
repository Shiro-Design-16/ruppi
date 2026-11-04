package com.shiro.ruppi.client.render;

import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.network.chat.Style;

/**
 * 由 MixinFont 实现：把 Font 的两个私有成员带出 mixin —— getGlyph(int, Style)
 * （Font.java:86-95，含 obfuscated 的随机字形逻辑）与 provider.effect()（Font.java:370-374）。
 * Mixin 会把本接口合入 Font，所以渲染侧拿到的 Font 实例本身就是 RubyFontAccess。
 */
public interface RubyFontAccess {
    BakedGlyph ruppi$getGlyph(int codepoint, Style style);

    EffectGlyph ruppi$effect();
}
