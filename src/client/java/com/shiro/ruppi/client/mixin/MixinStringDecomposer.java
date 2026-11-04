package com.shiro.ruppi.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.shiro.ruppi.IRubyStyle;
import com.shiro.ruppi.RubyText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;

@Mixin(StringDecomposer.class)
public class MixinStringDecomposer {
    /**
     * 注入点选在 § 分支里 ChatFormatting.getByCode(char) 的调用处：
     * 此时 i 仍指向 §、code 已经算好（= '^' 才继续），且 § 之后的内容还没被消费。
     * 看得到 source String 与索引，因此能做跨字符的前瞻匹配（单字符判断做不到）。
     */
    @Inject(
        method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/ChatFormatting;getByCode(C)Lnet/minecraft/ChatFormatting;"),
        cancellable = true
    )
    private static void ruppi$parseRuby(String string, int offset, Style currentStyle, Style resetStyle, FormattedCharSink output,
                                        CallbackInfoReturnable<Boolean> cir,
                                        @Local(ordinal = 2) Style style,
                                        @Local(ordinal = 2) LocalIntRef index) {
        int start = index.get();
        if (start + 1 >= string.length() || string.charAt(start + 1) != '^') {
            return;
        }
        Matcher matcher = RubyText.RUBY_PATTERN.matcher(string).region(start, string.length());
        if (!matcher.lookingAt()) {
            return;
        }
        RubyText ruby = RubyText.fromFormatted(matcher.group(1), matcher.group(2), style);
        Style rubyStyle = ((IRubyStyle) (Object) style).ruppi$withRuby(ruby);
        // 0xFFFC = U+FFFC OBJECT REPLACEMENT CHARACTER：渲染层用它占位（B2 负责画）
        if (!output.accept(start, rubyStyle, 0xFFFC)) {
            cir.setReturnValue(false);
            return;
        }
        // 注入点之后还有两处自增：§ 分支自己的 i++ 和 for 循环的 i++。
        // 因此要把 i 设成 end - 2，两次自增后正好落在 ')' 之后的下一个字符。
        index.set(matcher.end() - 2);
    }
}
