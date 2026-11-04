package com.shiro.ruppi.client.mixin;

import com.shiro.ruppi.client.render.RubyFontAccess;
import com.shiro.ruppi.client.render.RubyPreparedText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rušpi 的渲染挂钩。两个注入点都在 HEAD、cancellable，返回 null 时原版方法体照常执行。
 * 方法体里不出现捕获 this 的 lambda（Mixin AP 不接受），守卫逻辑全部在 RubyPreparedText 的静态方法里。
 */
@Mixin(Font.class)
public abstract class MixinFont implements RubyFontAccess {
    @Invoker("getGlyph")
    protected abstract BakedGlyph ruppi$invokeGetGlyph(int codepoint, Style style);

    @Accessor("provider")
    protected abstract Font.Provider ruppi$provider();

    @Override
    public BakedGlyph ruppi$getGlyph(final int codepoint, final Style style) {
        return this.ruppi$invokeGetGlyph(codepoint, style);
    }

    @Override
    public EffectGlyph ruppi$effect() {
        return this.ruppi$provider().effect();
    }

    @Inject(
        method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void ruppi$prepareRubyText(final FormattedCharSequence text, final float x, final float y, final int color,
                                       final boolean drawShadow, final boolean includeEmpty, final int backgroundColor,
                                       final CallbackInfoReturnable<Font.PreparedText> cir) {
        final RubyPreparedText prepared = RubyPreparedText.tryBuild(
            (Font) (Object) this, text, x, y, color, drawShadow, includeEmpty, backgroundColor);
        if (prepared != null) {
            cir.setReturnValue(prepared);
        }
    }

    @Inject(
        method = "prepare8xTextOutline(Lnet/minecraft/util/FormattedCharSequence;FFI)Lnet/minecraft/client/gui/Font$PreparedText;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void ruppi$prepareRubyOutline(final FormattedCharSequence text, final float x, final float y, final int outlineColor,
                                          final CallbackInfoReturnable<Font.PreparedText> cir) {
        final RubyPreparedText prepared = RubyPreparedText.tryBuildOutline((Font) (Object) this, text, x, y, outlineColor);
        if (prepared != null) {
            cir.setReturnValue(prepared);
        }
    }
}
