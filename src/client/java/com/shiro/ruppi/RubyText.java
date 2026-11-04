package com.shiro.ruppi;

import com.shiro.ruppi.option.RubyRenderMode;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record RubyText(FormattedCharSequence text, FormattedCharSequence ruby) {
    public static final Pattern RUBY_PATTERN = Pattern.compile("§\\^\\s*(.+?)\\s*\\(\\s*(.+?)\\s*\\)");
    public static final float TEXT_SCALE = 0.8f;
    public static final float RUBY_SCALE = 0.5f;
    public static final float RUBY_OVERLAP = 0.1f;

    public static String strip(String value) {
        StringBuilder sb = new StringBuilder(value.length());

        Matcher matcher = RUBY_PATTERN.matcher(value);
        while (matcher.find()) {
            String replacement = RubyRenderMode.option().get() == RubyRenderMode.REPLACE
                ? matcher.group(2)
                : matcher.group(1);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static RubyText fromFormatted(String word, String ruby, Style style) {
        FormattedCharSequence formattedWord = sink -> StringDecomposer.iterateFormatted(word, style, sink);
        FormattedCharSequence formattedRuby = sink -> StringDecomposer.iterateFormatted(ruby, style, sink);
        formattedRuby = Utils.transformStyle(formattedRuby, s -> s.withUnderlined(false).withStrikethrough(false));
        return new RubyText(formattedWord, formattedRuby);
    }

    public static float widthOf(FormattedCharSequence text, StringSplitter.WidthProvider provider) {
        float[] width = {0f};
        text.accept((index, style, codePoint) -> {
            width[0] += provider.getWidth(codePoint, style);
            return true;
        });
        return width[0];
    }

    public float baseWidth(Font font) {
        return font.getSplitter().stringWidth(this.text);
    }

    public float rubyWidth(Font font) {
        return font.getSplitter().stringWidth(this.ruby);
    }

    public float getWidth(StringSplitter.WidthProvider provider) {
        RubyRenderMode mode = RubyRenderMode.option().get();
        float baseWidth = mode != RubyRenderMode.REPLACE ? widthOf(this.text, provider) : 0f;
        float rubyWidth = mode != RubyRenderMode.HIDDEN ? widthOf(this.ruby, provider) : 0f;

        return switch (mode) {
            case ABOVE, BELOW -> Math.max(baseWidth * TEXT_SCALE, rubyWidth * RUBY_SCALE);
            case HIDDEN -> baseWidth;
            case REPLACE -> rubyWidth;
        };
    }

    public float getWidth(Font font) {
        RubyRenderMode mode = RubyRenderMode.option().get();
        float baseWidth = mode != RubyRenderMode.REPLACE ? baseWidth(font) : 0f;
        float rubyWidth = mode != RubyRenderMode.HIDDEN ? rubyWidth(font) : 0f;

        return switch (mode) {
            case ABOVE, BELOW -> Math.max(baseWidth * TEXT_SCALE, rubyWidth * RUBY_SCALE);
            case HIDDEN -> baseWidth;
            case REPLACE -> rubyWidth;
        };
    }
}
