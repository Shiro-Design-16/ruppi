package com.shiro.ruppi.client.mixin;

import com.shiro.ruppi.option.RubyRenderMode;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(AccessibilityOptionsScreen.class)
public class MixinAccessibilityOptionsScreen {
    /**
     * 追加在末尾：addOptions() 把 options[0] 与 narrator/controls 塞进同一行做特殊排版，
     * 追加到末尾才能走批量 addSmall 的常规两列布局。
     */
    @Inject(
        method = "options(Lnet/minecraft/client/Options;)[Lnet/minecraft/client/OptionInstance;",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void ruppi$appendRubyOption(Options options, CallbackInfoReturnable<OptionInstance<?>[]> cir) {
        OptionInstance<?>[] original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        OptionInstance<?>[] extended = Arrays.copyOf(original, original.length + 1);
        extended[original.length] = RubyRenderMode.option();
        cir.setReturnValue(extended);
    }
}
