package com.shiro.ruppi.client.render;

import com.shiro.ruppi.IRubyStyle;
import com.shiro.ruppi.Utils;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Font.PreparedText 的自建实现，只有带 ruby 的序列才会走到这里。visit 顺序与 Font.java:304-329 逐段一致：
 * 背景 effect → 全部 glyph → 全部 effect（下划线/删除线）→ 全部 EmptyArea。
 * 两个静态入口刻意放在普通类而不是 MixinFont 里：Mixin 注解处理器不接受捕获 this 的 lambda，
 * 而 Utils.withGuard 的入参是 Supplier。
 */
public final class RubyPreparedText implements Font.PreparedText {
    /** ruby slot 的占位码点：B1 把它写进序列，只有它携带 ruby Style。 */
    private static final int RUBY_SLOT = 0xFFFC;

    private final @Nullable TextRenderable background;
    private final List<TextRenderable.Styled> glyphs;
    private final @Nullable List<TextRenderable> effects;
    private final @Nullable List<EmptyArea> emptyAreas;
    private final @Nullable ScreenRectangle bounds;

    private RubyPreparedText(final @Nullable TextRenderable background, final List<TextRenderable.Styled> glyphs,
                             final @Nullable List<TextRenderable> effects, final @Nullable List<EmptyArea> emptyAreas,
                             final float left, final float top, final float right, final float bottom) {
        this.background = background;
        this.glyphs = glyphs;
        this.effects = effects;
        this.emptyAreas = emptyAreas;
        this.bounds = left >= right || top >= bottom ? null
            : new ScreenRectangle(Mth.floor(left), Mth.floor(top), Mth.ceil(right) - Mth.floor(left), Mth.ceil(bottom) - Mth.floor(top));
    }

    static RubyPreparedText of(final @Nullable TextRenderable background, final List<TextRenderable.Styled> glyphs,
                               final @Nullable List<TextRenderable> effects, final @Nullable List<EmptyArea> emptyAreas,
                               final float left, final float top, final float right, final float bottom) {
        return new RubyPreparedText(background, glyphs, effects, emptyAreas, left, top, right, bottom);
    }

    /**
     * prepareText 的替身。返回 null 表示「照常走原版」——无 ruby 时这里只做一次 accept 扫描，
     * 不分配任何对象，因此普通文本的代价是一次扫描加一次提前 return。
     */
    public static @Nullable RubyPreparedText tryBuild(final Font font, final FormattedCharSequence text,
                                                      final float x, final float y, final int color,
                                                      final boolean drawShadow, final boolean includeEmpty,
                                                      final int backgroundColor) {
        if (Utils.isGuarded() || !containsRuby(text)) {
            return null;
        }

        return Utils.withGuard(() -> {
            final RubyTextBuilder builder = new RubyTextBuilder(font, (RubyFontAccess) font, x, y, color, backgroundColor, drawShadow, includeEmpty);
            text.accept(builder);
            return builder.build();
        });
    }

    /**
     * prepare8xTextOutline 的替身：原版的一圈 xo/yo ∈ [-1,1]（去掉 (0,0)）在这里重放，
     * 每个 pass 把笔位重置回 x、按 dx/dy 乘 shadowOffset 偏移，最后与原版一样丢弃效果。
     */
    public static @Nullable RubyPreparedText tryBuildOutline(final Font font, final FormattedCharSequence text,
                                                             final float x, final float y, final int outlineColor) {
        if (Utils.isGuarded() || !containsRuby(text)) {
            return null;
        }

        return Utils.withGuard(() -> {
            final RubyTextBuilder builder = new RubyTextBuilder(font, (RubyFontAccess) font, x, y, outlineColor, 0, false, false);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx != 0 || dy != 0) {
                        builder.beginOutlinePass(dx, dy);
                        text.accept(builder);
                    }
                }
            }
            builder.discardEffects();
            return builder.build();
        });
    }

    private static boolean containsRuby(final FormattedCharSequence text) {
        return !text.accept((position, style, codepoint) -> codepoint != RUBY_SLOT || IRubyStyle.getRuby(style).isEmpty());
    }

    @Override
    public void visit(final Font.GlyphVisitor visitor) {
        if (this.background != null) {
            visitor.acceptEffect(this.background);
        }

        for (final TextRenderable.Styled glyph : this.glyphs) {
            visitor.acceptGlyph(glyph);
        }

        if (this.effects != null) {
            for (final TextRenderable effect : this.effects) {
                visitor.acceptEffect(effect);
            }
        }

        if (this.emptyAreas != null) {
            for (final EmptyArea emptyArea : this.emptyAreas) {
                visitor.acceptEmptyArea(emptyArea);
            }
        }
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return this.bounds;
    }
}
