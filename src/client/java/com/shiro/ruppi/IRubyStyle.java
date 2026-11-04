package com.shiro.ruppi;

import net.minecraft.network.chat.Style;

import java.util.Optional;

public interface IRubyStyle {
    static Optional<RubyText> getRuby(Style style) {
        return Optional.ofNullable(((IRubyStyle) (Object) style).ruppi$getRuby());
    }

    Style ruppi$withRuby(RubyText ruby);

    RubyText ruppi$getRuby();
}
