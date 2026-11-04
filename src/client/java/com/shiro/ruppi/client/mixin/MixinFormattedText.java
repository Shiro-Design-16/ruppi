package com.shiro.ruppi.client.mixin;

import com.shiro.ruppi.RubyText;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FormattedText.class)
public interface MixinFormattedText {
    @Inject(method = "getString", at = @At("RETURN"), cancellable = true)
    default void ruppi$stripRuby(CallbackInfoReturnable<String> cir) {
        String value = cir.getReturnValue();
        // 只有真的含 §^ 标记时才做正则替换，避免所有 getString 都要跑一遍 strip
        if (value == null || !value.contains("\u00a7^")) {
            return;
        }
        cir.setReturnValue(RubyText.strip(value));
    }
}
