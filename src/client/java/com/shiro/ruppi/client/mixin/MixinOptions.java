package com.shiro.ruppi.client.mixin;

import com.shiro.ruppi.option.RubyRenderMode;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class MixinOptions {
    /**
     * load / save / dump 三条路径都会经过 processDumpedOptions（processOptions 首句就回调它），
     * 所以只挂这一处即可让 ruppi.renderMode 出现在 options.txt 里。
     */
    @Inject(method = "processDumpedOptions", at = @At("HEAD"))
    private void ruppi$acceptRubyMode(Options.OptionAccess access, CallbackInfo ci) {
        RubyRenderMode.accept(access);
    }
}
