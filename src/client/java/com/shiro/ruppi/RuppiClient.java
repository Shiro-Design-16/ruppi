package com.shiro.ruppi;

import com.shiro.ruppi.option.RubyRenderMode;
import net.fabricmc.api.ClientModInitializer;

public class RuppiClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Utils.LOGGER.debug("Ruppi loaded, ruby render mode: {}", RubyRenderMode.option().get());
    }
}
