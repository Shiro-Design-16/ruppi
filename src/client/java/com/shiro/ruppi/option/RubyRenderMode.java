package com.shiro.ruppi.option;

import com.mojang.serialization.Codec;
import com.shiro.ruppi.Utils;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

import java.util.List;

public enum RubyRenderMode {
    HIDDEN("hidden"),
    ABOVE("above"),
    BELOW("below"),
    REPLACE("replace");

    public static final String TRANSLATION_KEY = "options.ruppi.renderMode";
    public static final String SAVE_KEY = "ruppi.renderMode";

    private static final Codec<RubyRenderMode> CODEC =
        Codec.INT.xmap(RubyRenderMode::byId, RubyRenderMode::getId);

    private static final OptionInstance<RubyRenderMode> INSTANCE = new OptionInstance<>(
        TRANSLATION_KEY,
        OptionInstance.noTooltip(),
        (caption, value) -> Options.genericValueLabel(caption, Component.translatable(value.getTranslationKey())),
        new OptionInstance.SliderableEnum<>(List.of(values()), CODEC),
        ABOVE,
        value -> Utils.LOGGER.debug("Ruby display mode changed to {} ({})", value, value.getId())
    );

    private final String translationKey;

    RubyRenderMode(String name) {
        this.translationKey = TRANSLATION_KEY + "." + name;
    }

    public static OptionInstance<RubyRenderMode> option() {
        return INSTANCE;
    }

    public static RubyRenderMode byId(int id) {
        RubyRenderMode[] modes = values();
        return id >= 0 && id < modes.length ? modes[id] : ABOVE;
    }

    // Options.OptionAccess 是 26.3 的包私有嵌套接口，由 ruppi.accesswidener 放开
    public static void accept(Options.OptionAccess access) {
        access.process(SAVE_KEY, INSTANCE);
    }

    public int getId() {
        return this.ordinal();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }
}
