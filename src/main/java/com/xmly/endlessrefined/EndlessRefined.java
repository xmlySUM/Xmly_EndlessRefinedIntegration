package com.xmly.endlessrefined;

import com.xmly.endlessrefined.compat.Compat;
import com.xmly.endlessrefined.config.ERIConfig;
import com.xmly.endlessrefined.event.PlayerTickEvents;
import com.xmly.endlessrefined.hotbar.engine.HotbarEngine;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(EndlessRefined.MOD_ID)
public final class EndlessRefined {

    public static final String MOD_ID = "endless_refined";

    @SuppressWarnings("removal")
    public EndlessRefined() {
        ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                ERIConfig.SERVER_SPEC
        );

        MinecraftForge.EVENT_BUS.register(
                new Compat()
        );

        MinecraftForge.EVENT_BUS.register(
                new PlayerTickEvents()
        );

        MinecraftForge.EVENT_BUS.register(HotbarEngine.class);

        HotbarEngine.init();
    }
}
