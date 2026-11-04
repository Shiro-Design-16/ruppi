package com.shiro.ruppi.client.mixin;

import com.shiro.ruppi.IRubyStyle;
import com.shiro.ruppi.RubyText;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StringSplitter.class)
public abstract class MixinStringSplitter {
    @Shadow
    @Final
    @Mutable
    private StringSplitter.WidthProvider widthProvider;

    /**
     * Font 自己 new StringSplitter（lambda 捕获 this），构造期拿不到 Font，也没有别的替换入口，
     * 所以只能在构造器末尾把 widthProvider 换成包装版。宽度一律用未包装的原始 provider 逐字量，
     * 保持 float 精度（不走 Font.width 的 ceil）。
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void ruppi$wrapWidthProvider(final StringSplitter.WidthProvider provider, final CallbackInfo ci) {
        if (!(provider instanceof RubyWidthProvider)) {
            this.widthProvider = new RubyWidthProvider(provider);
        }
    }

    private record RubyWidthProvider(StringSplitter.WidthProvider delegate) implements StringSplitter.WidthProvider {
        @Override
        public float getWidth(final int codePoint, final Style style) {
            if (codePoint == 0xFFFC) {
                final RubyText ruby = IRubyStyle.getRuby(style).orElse(null);
                if (ruby != null) {
                    // 必须传未包装的 delegate，否则 ruby.getWidth 会回调自己
                    return ruby.getWidth(this.delegate);
                }
            }

            return this.delegate.getWidth(codePoint, style);
        }
    }
}
