package com.shiro.ruppi.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(LanguageManager.class)
public class MixinLanguageManager {
    /**
     * 选中 xxx_rubi 时，把基语言 xxx 插到选中语言之前，让只提供 xxx 的其它 mod 也参与叠加。
     * 用 @WrapOperation 而不是 @Redirect：同一个 loadFrom 调用可能被别的 mod 包装，WrapOperation 可以共存。
     */
    @WrapOperation(
        method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/language/ClientLanguage;loadFrom(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resources/language/ClientLanguage;"
        )
    )
    private static ClientLanguage ruppi$addBaseLanguage(
        final ResourceManager resourceManager,
        final List<String> languageStack,
        final boolean defaultRightToLeft,
        final Operation<ClientLanguage> original
    ) {
        List<String> stack = languageStack;
        if (!languageStack.isEmpty()) {
            String selected = languageStack.get(languageStack.size() - 1);
            if (selected.endsWith("_rubi") && selected.length() > 5) {
                String base = selected.substring(0, selected.length() - 5);
                if (!base.equals("en_us") && !languageStack.contains(base) && hasTranslations(resourceManager, base)) {
                    // 插到 en_us 之上、所有其它回退之下：ruppi 只保证「至少别再退回英语」，
                    // 不与其它 mod（如 secondlang 的用户链）争夺更高优先级。
                    stack = new ArrayList<>(languageStack);
                    stack.add(1, base);
                }
            }
        }

        return original.call(resourceManager, stack, defaultRightToLeft);
    }

    private static boolean hasTranslations(final ResourceManager resourceManager, final String languageCode) {
        String path = String.format(Locale.ROOT, "lang/%s.json", languageCode);
        for (String namespace : resourceManager.getNamespaces()) {
            Identifier location = Identifier.fromNamespaceAndPath(namespace, path);
            if (!resourceManager.getResourceStack(location).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
