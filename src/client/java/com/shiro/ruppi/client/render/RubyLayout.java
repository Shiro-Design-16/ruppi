package com.shiro.ruppi.client.render;

import com.shiro.ruppi.RubyText;
import com.shiro.ruppi.option.RubyRenderMode;

/**
 * 一个 ruby slot 的几何：盒宽、正文/注音各自的笔位 y 与缩放、以及背景带。
 * 公式照抄上游 RubyText#drawAbove / #drawBelow（references/rubi/.../RubyText.java:102-123）。
 * 26.3 的 y 是行盒顶部：下划线在 y+9、删除线在 y+4.5（Font.java:290、296），行高 9（Font.java:33）。
 */
record RubyLayout(
        RubyRenderMode mode,
        float boxWidth,
        float bodyY,
        float rubyY,
        float bodyScale,
        float rubyScale,
        float backgroundTop,
        float backgroundBottom) {

    static RubyLayout of(final RubyRenderMode mode, final float y, final float lineHeight,
                         final float baseWidth, final float rubyWidth) {
        final float textHeight = lineHeight * RubyText.TEXT_SCALE;
        final float rubyHeight = lineHeight * RubyText.RUBY_SCALE;
        final float overlap = lineHeight * RubyText.RUBY_OVERLAP;
        final float pairWidth = Math.max(baseWidth * RubyText.TEXT_SCALE, rubyWidth * RubyText.RUBY_SCALE);

        return switch (mode) {
            case ABOVE -> {
                final float bodyY = y + (lineHeight - textHeight);
                final float rubyY = bodyY - rubyHeight + overlap;
                yield new RubyLayout(mode, pairWidth, bodyY, rubyY, RubyText.TEXT_SCALE, RubyText.RUBY_SCALE,
                    rubyY - 1.0F, Math.max(rubyY + rubyHeight, y + lineHeight));
            }
            case BELOW -> {
                final float rubyY = y + textHeight - overlap;
                yield new RubyLayout(mode, pairWidth, y, rubyY, RubyText.TEXT_SCALE, RubyText.RUBY_SCALE,
                    y - 1.0F, Math.max(rubyY + rubyHeight, y + lineHeight));
            }
            case REPLACE -> new RubyLayout(mode, rubyWidth, y, y, 1.0F, 1.0F, y - 1.0F, y + lineHeight);
            case HIDDEN -> new RubyLayout(mode, baseWidth, y, y, 1.0F, 1.0F, y - 1.0F, y + lineHeight);
        };
    }

    boolean hasBody() {
        return this.mode != RubyRenderMode.REPLACE;
    }

    boolean hasRuby() {
        return this.mode != RubyRenderMode.HIDDEN;
    }
}
