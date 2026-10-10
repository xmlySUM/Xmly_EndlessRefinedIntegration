package com.kwwsyk.endinv.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class StartupConfig {

    public static final StartupConfig CONFIG;
    public static final ForgeConfigSpec CONFIG_SPEC;

    public final ForgeConfigSpec.BooleanValue ENABLE_JEI_ATTACHED_TRANSFER;

    static {
        Pair<StartupConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(StartupConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    private StartupConfig(ForgeConfigSpec.Builder builder) {
        builder.push("experimental");
        ENABLE_JEI_ATTACHED_TRANSFER = builder
                .comment("Enable experimental JEI recipe transfer integration with attached Endless Inventory.")
                .define("jeiAttachedRecipeTransfer", false);
        builder.pop();
    }

    public static boolean enableJeiAttachedTransfer() {
        return CONFIG.ENABLE_JEI_ATTACHED_TRANSFER.get();
    }
}
