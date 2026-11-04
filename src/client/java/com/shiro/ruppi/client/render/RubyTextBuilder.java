package com.shiro.ruppi.client.render;

import com.mojang.blaze3d.font.GlyphInfo;
import com.shiro.ruppi.IRubyStyle;
import com.shiro.ruppi.RubyText;
import com.shiro.ruppi.option.RubyRenderMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import org.jspecify.annotations.Nullable;

/**
 * Font.PreparedTextBuilder（Font.java:193-368）的等价实现：普通字形的笔位推进、颜色推导、
 * 下划线/删除线 effect、EmptyArea 与背景带的记账逐行照抄原版；唯一的差别是携带 ruby 的
 * U+FFFC slot 换成「正文 + 注音两行、各自缩放、在盒宽内均匀拉开」，右边界仍是 x + boxWidth，
 * 所以后面的文字位置与原版一致。
 */
final class RubyTextBuilder implements FormattedCharSink {
    private final RubyFontAccess access;
    private final RubyRenderMode mode;
    private final float lineHeight;
    private final boolean drawShadow;
    private final int color;
    private final int backgroundColor;
    private final boolean includeEmpty;
    private final float startX;
    private final float y;

    private float x;
    private boolean outline;
    private int outlineDx;
    private int outlineDy;
    private float left = Float.MAX_VALUE;
    private float top = Float.MAX_VALUE;
    private float right = -Float.MAX_VALUE;
    private float bottom = -Float.MAX_VALUE;
    private float backgroundLeft = Float.MAX_VALUE;
    private float backgroundTop = Float.MAX_VALUE;
    private float backgroundRight = -Float.MAX_VALUE;
    private float backgroundBottom = -Float.MAX_VALUE;
    private final List<TextRenderable.Styled> glyphs = new ArrayList<>();
    private @Nullable List<TextRenderable> effects;
    private @Nullable List<EmptyArea> emptyAreas;

    RubyTextBuilder(final Font font, final RubyFontAccess access, final float x, final float y,
                    final int color, final int backgroundColor, final boolean drawShadow, final boolean includeEmpty) {
        this.access = access;
        this.mode = RubyRenderMode.option().get();
        this.lineHeight = font.lineHeight;
        this.x = x;
        this.startX = x;
        this.y = y;
        this.drawShadow = drawShadow;
        this.color = color;
        this.backgroundColor = backgroundColor;
        this.includeEmpty = includeEmpty;
        this.markBackground(x, y, 0.0F, 1.0F);
    }

    /** 描边路径：每个 pass 从 x 重新起笔，字形位置额外偏移 (dx, dy) * shadowOffset。 */
    void beginOutlinePass(final int dx, final int dy) {
        this.outline = true;
        this.outlineDx = dx;
        this.outlineDy = dy;
        this.x = this.startX;
    }

    @Override
    public boolean accept(final int position, final Style style, final int codepoint) {
        final RubyText ruby = IRubyStyle.getRuby(style).orElse(null);
        if (ruby != null) {
            this.emitRuby(ruby);
            return true;
        }

        final BakedGlyph glyph = this.access.ruppi$getGlyph(codepoint, style);
        final float advance = advanceOf(glyph, style);
        this.emit(position, style, glyph, advance, this.x, this.y, 1.0F);
        this.x += advance;
        return true;
    }

    private void emitRuby(final RubyText ruby) {
        final float baseWidth = this.measure(ruby.text());
        final float rubyWidth = this.measure(ruby.ruby());
        final RubyLayout layout = RubyLayout.of(this.mode, this.y, this.lineHeight, baseWidth, rubyWidth);

        if (layout.hasBody()) {
            this.drawSpaced(ruby.text(), baseWidth, layout.bodyY(), layout.bodyScale(), layout.boxWidth());
        }

        if (layout.hasRuby()) {
            this.drawSpaced(ruby.ruby(), rubyWidth, layout.rubyY(), layout.rubyScale(), layout.boxWidth());
        }

        this.markSlotBackground(this.x - 1.0F, layout.backgroundTop(), this.x + layout.boxWidth(), layout.backgroundBottom());
        this.x += layout.boxWidth();
    }

    /** 上游 TextDrawer#drawSpacedApart（references/rubi/.../TextDrawer.java:35-50）的等价实现。 */
    private void drawSpaced(final FormattedCharSequence seq, final float lineWidth, final float penY,
                           final float scale, final float boxWidth) {
        final float textWidth = lineWidth * scale;
        final float[] cursor = {this.x};

        seq.accept((position, style, codepoint) -> {
            final BakedGlyph glyph = this.access.ruppi$getGlyph(codepoint, style);
            final float advance = advanceOf(glyph, style);
            final float charWidth = advance * scale;
            final float spaceAround = textWidth == 0.0F ? 0.0F : (boxWidth - textWidth) * charWidth / textWidth;
            cursor[0] += spaceAround / 2.0F;
            this.emit(position, style, glyph, advance, cursor[0], penY, scale);
            cursor[0] += charWidth + spaceAround / 2.0F;
            return true;
        });
    }

    private float measure(final FormattedCharSequence seq) {
        return RubyText.widthOf(seq, this::widthOfGlyph);
    }

    private float widthOfGlyph(final int codepoint, final Style style) {
        return advanceOf(this.access.ruppi$getGlyph(codepoint, style), style);
    }

    private static float advanceOf(final BakedGlyph glyph, final Style style) {
        return glyph.info().getAdvance(style.isBold());
    }

    private void emit(final int position, final Style style, final BakedGlyph glyph, final float advance,
                      final float penX, final float penY, final float scale) {
        final GlyphInfo info = glyph.info();
        final float shadowOffset = info.getShadowOffset();
        final float glyphX = penX + this.outlineDx * shadowOffset;
        final float glyphY = penY + this.outlineDy * shadowOffset;
        final Style drawnStyle = this.outline ? style.withColor(this.color) : style;
        final boolean bold = drawnStyle.isBold();
        final int textColor = this.textColor(drawnStyle);
        final int shadowColor = this.shadowColor(drawnStyle, textColor);
        final float effectX0 = position == 0 ? glyphX - 1.0F : glyphX;
        final float boldOffset = bold ? info.getBoldOffset() : 0.0F;

        final TextRenderable.Styled instance = glyph.createGlyph(glyphX, glyphY, textColor, shadowColor, drawnStyle, boldOffset, shadowOffset);
        if (instance != null) {
            this.addGlyph(scale == 1.0F ? instance : new RubyScaledRenderable(instance, scale, glyphX, glyphY));
        } else if (this.includeEmpty) {
            this.addEmptyArea(new EmptyArea(glyphX, glyphY, advance * scale, 7.0F * scale, 9.0F * scale, drawnStyle));
        }

        this.markBackground(glyphX, glyphY, advance * scale, scale);

        if (drawnStyle.isStrikethrough()) {
            this.addEffect(this.effect(effectX0, glyphY + 4.5F - 1.0F, glyphX + advance, glyphY + 4.5F,
                glyphX, glyphY, scale, textColor, shadowColor, shadowOffset));
        }

        if (drawnStyle.isUnderlined()) {
            this.addEffect(this.effect(effectX0, glyphY + 9.0F - 1.0F, glyphX + advance, glyphY + 9.0F,
                glyphX, glyphY, scale, textColor, shadowColor, shadowOffset));
        }
    }

    private TextRenderable effect(final float x0, final float y0, final float x1, final float y1,
                                  final float anchorX, final float anchorY, final float scale,
                                  final int color, final int shadowColor, final float shadowOffset) {
        final EffectGlyph glyph = this.access.ruppi$effect();
        if (scale == 1.0F) {
            return glyph.createEffect(x0, y0, x1, y1, 0.01F, color, shadowColor, shadowOffset);
        }

        return glyph.createEffect(anchorX + (x0 - anchorX) * scale, anchorY + (y0 - anchorY) * scale,
            anchorX + (x1 - anchorX) * scale, anchorY + (y1 - anchorY) * scale, 0.01F, color, shadowColor, shadowOffset);
    }

    private int textColor(final Style style) {
        final TextColor styleColor = style.getColor();
        if (styleColor == null) {
            return this.color;
        }

        return ARGB.color(ARGB.alpha(this.color), styleColor.getValue());
    }

    private int shadowColor(final Style style, final int textColor) {
        final Integer shadow = style.getShadowColor();
        if (shadow != null) {
            final float textAlpha = ARGB.alphaFloat(textColor);
            final float shadowAlpha = ARGB.alphaFloat(shadow);
            return textAlpha != 1.0F ? ARGB.color(ARGB.as8BitChannel(textAlpha * shadowAlpha), shadow) : shadow;
        }

        return this.drawShadow ? ARGB.scaleRGB(textColor, 0.25F) : 0;
    }

    private void markSize(final float left, final float top, final float right, final float bottom) {
        this.left = Math.min(this.left, left);
        this.top = Math.min(this.top, top);
        this.right = Math.max(this.right, right);
        this.bottom = Math.max(this.bottom, bottom);
    }

    private void markBackground(final float x, final float y, final float advance, final float scale) {
        if (ARGB.alpha(this.backgroundColor) != 0) {
            this.markSlotBackground(x - 1.0F, y - scale, x + advance, y + 9.0F * scale);
        }
    }

    private void markSlotBackground(final float left, final float top, final float right, final float bottom) {
        this.backgroundLeft = Math.min(this.backgroundLeft, left);
        this.backgroundTop = Math.min(this.backgroundTop, top);
        this.backgroundRight = Math.max(this.backgroundRight, right);
        this.backgroundBottom = Math.max(this.backgroundBottom, bottom);
        this.markSize(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom);
    }

    private void addGlyph(final TextRenderable.Styled instance) {
        this.glyphs.add(instance);
        this.markSize(instance.left(), instance.top(), instance.right(), instance.bottom());
    }

    private void addEffect(final TextRenderable effect) {
        if (this.effects == null) {
            this.effects = new ArrayList<>();
        }

        this.effects.add(effect);
        this.markSize(effect.left(), effect.top(), effect.right(), effect.bottom());
    }

    private void addEmptyArea(final EmptyArea empty) {
        if (this.emptyAreas == null) {
            this.emptyAreas = new ArrayList<>();
        }

        this.emptyAreas.add(empty);
    }

    void discardEffects() {
        this.effects = null;
    }

    RubyPreparedText build() {
        final TextRenderable background = ARGB.alpha(this.backgroundColor) == 0 ? null : this.access.ruppi$effect()
            .createEffect(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom, -0.01F, this.backgroundColor, 0, 0.0F);
        return RubyPreparedText.of(background, this.glyphs, this.effects, this.emptyAreas,
            this.left, this.top, this.right, this.bottom);
    }
}
