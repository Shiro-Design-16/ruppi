package com.shiro.ruppi.client.mixin;

import com.shiro.ruppi.IRubyStyle;
import com.shiro.ruppi.RubyText;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Style.class)
public abstract class MixinStyle implements IRubyStyle {
    @Shadow @Final private TextColor color;
    @Shadow @Final private Integer shadowColor;
    @Shadow @Final private Boolean bold;
    @Shadow @Final private Boolean italic;
    @Shadow @Final private Boolean underlined;
    @Shadow @Final private Boolean strikethrough;
    @Shadow @Final private Boolean obfuscated;
    @Shadow @Final private ClickEvent clickEvent;
    @Shadow @Final private HoverEvent hoverEvent;
    @Shadow @Final private String insertion;
    @Shadow @Final private FontDescription font;

    @Unique private RubyText ruppi$ruby;

    @Override
    public RubyText ruppi$getRuby() {
        return this.ruppi$ruby;
    }

    @Override
    public Style ruppi$withRuby(RubyText ruby) {
        // 永远复制出一个新对象：Style.EMPTY 是全局单例，往里写字段会污染整个游戏。
        Style copy = new Style(
            this.color, this.shadowColor, this.bold, this.italic, this.underlined, this.strikethrough,
            this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font
        );
        if (ruby != null) {
            ((MixinStyle) (Object) copy).ruppi$setRuby(ruby);
        }
        return copy;
    }

    @Unique
    private void ruppi$setRuby(RubyText ruby) {
        this.ruppi$ruby = ruby;
    }

    /**
     * 把 ruby 挂到 target 上并返回"应该被调用方看到"的 Style。
     * target == Style.EMPTY 时新建一个 11 个字段全 null 的 Style 再挂，绝不写 EMPTY 单例。
     */
    @Unique
    private static Style ruppi$attach(Style target, RubyText ruby) {
        if (target == null || ruby == null) {
            return target;
        }
        if (((IRubyStyle) (Object) target).ruppi$getRuby() != null) {
            return target;
        }
        Style attached = target == Style.EMPTY
            ? new Style(null, null, null, null, null, null, null, null, null, null, null)
            : target;
        ((MixinStyle) (Object) attached).ruppi$setRuby(ruby);
        return attached;
    }

    @Unique
    private Style ruppi$carryInto(Style result) {
        if (this.ruppi$ruby == null || result == (Object) this) {
            return result;
        }
        return ruppi$attach(result, this.ruppi$ruby);
    }

    // 11 个 withXxx / 3 个 apply* 的复制路径：全部 new Style，ruby 会丢。
    // 处理器只带 CallbackInfoReturnable（不带任何目标参数），这样同一个 handler 能同时匹配签名互不相同的多个目标。
    @Inject(
        method = {
            "withColor(Lnet/minecraft/network/chat/TextColor;)Lnet/minecraft/network/chat/Style;",
            "withColor(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/Style;",
            "withColor(I)Lnet/minecraft/network/chat/Style;",
            "withShadowColor(I)Lnet/minecraft/network/chat/Style;",
            "withoutShadow()Lnet/minecraft/network/chat/Style;",
            "withBold(Ljava/lang/Boolean;)Lnet/minecraft/network/chat/Style;",
            "withItalic(Ljava/lang/Boolean;)Lnet/minecraft/network/chat/Style;",
            "withUnderlined(Ljava/lang/Boolean;)Lnet/minecraft/network/chat/Style;",
            "withStrikethrough(Ljava/lang/Boolean;)Lnet/minecraft/network/chat/Style;",
            "withObfuscated(Ljava/lang/Boolean;)Lnet/minecraft/network/chat/Style;",
            "withClickEvent(Lnet/minecraft/network/chat/ClickEvent;)Lnet/minecraft/network/chat/Style;",
            "withHoverEvent(Lnet/minecraft/network/chat/HoverEvent;)Lnet/minecraft/network/chat/Style;",
            "withInsertion(Ljava/lang/String;)Lnet/minecraft/network/chat/Style;",
            "withFont(Lnet/minecraft/network/chat/FontDescription;)Lnet/minecraft/network/chat/Style;",
            "applyFormat(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/Style;",
            "applyLegacyFormat(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/Style;",
            "applyFormats([Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/Style;"
        },
        at = @At("RETURN"),
        cancellable = true
    )
    private void ruppi$keepRuby(CallbackInfoReturnable<Style> cir) {
        Style result = cir.getReturnValue();
        Style carried = this.ruppi$carryInto(result);
        if (carried != result) {
            cir.setReturnValue(carried);
        }
    }

    // applyTo 的语义是 this 覆盖 other，因此 this 没有 ruby 时还要把 other 的 ruby 带过来。
    @Inject(
        method = "applyTo(Lnet/minecraft/network/chat/Style;)Lnet/minecraft/network/chat/Style;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void ruppi$keepRubyOnApplyTo(Style other, CallbackInfoReturnable<Style> cir) {
        Style result = cir.getReturnValue();
        if (result == (Object) this || result == other) {
            return;
        }
        RubyText ruby = this.ruppi$ruby != null ? this.ruppi$ruby : ((IRubyStyle) (Object) other).ruppi$getRuby();
        if (ruby == null) {
            return;
        }
        Style carried = ruppi$attach(result, ruby);
        if (carried != result) {
            cir.setReturnValue(carried);
        }
    }

    // equals 必须纳入 ruby：否则 checkEmptyAfterChange 里 newStyle.equals(EMPTY) 会把带 ruby 的 style 折叠成 EMPTY 单例。
    @Inject(method = "equals(Ljava/lang/Object;)Z", at = @At("RETURN"), cancellable = true)
    private void ruppi$equals(Object other, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !(other instanceof Style style)) {
            return;
        }
        cir.setReturnValue(Objects.equals(this.ruppi$ruby, ((IRubyStyle) (Object) style).ruppi$getRuby()));
    }

    @Inject(method = "hashCode()I", at = @At("RETURN"), cancellable = true)
    private void ruppi$hashCode(CallbackInfoReturnable<Integer> cir) {
        if (this.ruppi$ruby == null) {
            return;
        }
        cir.setReturnValue(31 * cir.getReturnValue() + this.ruppi$ruby.hashCode());
    }
}
