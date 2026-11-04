package com.shiro.ruppi;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class Utils {
    public static final Logger LOGGER = LoggerFactory.getLogger("Ruppi");

    private static final ThreadLocal<Boolean> GUARD = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private Utils() {}

    public static FormattedCharSequence transformStyle(FormattedCharSequence text, UnaryOperator<Style> transformer) {
        return sink -> text.accept((index, style, codePoint) -> sink.accept(index, transformer.apply(style), codePoint));
    }

    public static <T> T withGuard(Supplier<T> action) {
        if (GUARD.get()) {
            return null;
        }
        GUARD.set(Boolean.TRUE);
        try {
            return action.get();
        } finally {
            GUARD.set(Boolean.FALSE);
        }
    }

    public static boolean isGuarded() {
        return GUARD.get();
    }
}
