package com.xmly.endlessrefined.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ERIConfig {

    public static final ForgeConfigSpec SERVER_SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_OVERFLOW;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SWEEP;
    public static final ForgeConfigSpec.BooleanValue ENDLESS_DEATH_PROTECTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_JEI_INTEGRATION;

    public static final ForgeConfigSpec.BooleanValue ENABLE_RS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue RS_IGNORE_ENERGY;

    public static final ForgeConfigSpec.BooleanValue ENABLE_ENDLESS_HOTBAR;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ENDER_CHEST_HOTBAR;

    public static final ForgeConfigSpec.BooleanValue TRACE_NBT;

    static {

        ForgeConfigSpec.Builder builder =
                new ForgeConfigSpec.Builder();

        builder.push("inventory");

        ENABLE_OVERFLOW =
                builder
                        .comment(
                                "Move overflow items from vanilla inventory to Endless Inventory."
                        )
                        .define("overflow", true);

        ENABLE_SWEEP =
                builder
                        .comment(
                                "Move passively gained items that land in inventory slots 9-35 into Endless Inventory.",
                                "Only items that arrived through Inventory#add count: pickups, /give and the like.",
                                "Items you move around inside a GUI are never touched, so shift-clicking is safe.",
                                "Slots 0-8 are never touched, and neither are slots a hotbar page is showing."
                        )
                        .define("sweep", true);

        ENDLESS_DEATH_PROTECTION =
                builder
                        .comment(
                                "Endless Inventory items never drop on death."
                        )
                        .define("deathProtection", true);

        builder.pop();

        builder.push("jei");

        ENABLE_JEI_INTEGRATION =
                builder
                        .define("enabled", true);

        builder.pop();

        builder.push("refined_storage");

        ENABLE_RS_INTEGRATION =
                builder
                        .comment(
                                "A portable grid with no storage disk shows the player's Endless Inventory instead."
                        )
                        .define("enabled", true);

        RS_IGNORE_ENERGY =
                builder
                        .comment(
                                "Also open a disk-less portable grid when it has run out of energy."
                        )
                        .define("ignoreEnergy", true);

        builder.pop();

        builder.push("hotbar");

        ENABLE_ENDLESS_HOTBAR =
                builder
                        .define("enabled", true);

        ENABLE_ENDER_CHEST_HOTBAR =
                builder
                        .comment(
                                "Let Ctrl+0 swap the hotbar between the player's own inventory and their ender chest.",
                                "With this off, Ctrl+0 only ends an Endless Inventory mapping."
                        )
                        .define("enderChest", true);

        builder.pop();

        builder.push("debug");

        TRACE_NBT =
                builder
                        .comment(
                                "Diagnostic logging for the integration points that are hard to see otherwise.",
                                "Off by default. Currently covers item NBT crossing to the client, and the",
                                "Endless Inventory panel's drag handling."
                        )
                        .define("traceNbt", false);

        builder.pop();

        SERVER_SPEC = builder.build();
    }

    private ERIConfig() {
    }
}