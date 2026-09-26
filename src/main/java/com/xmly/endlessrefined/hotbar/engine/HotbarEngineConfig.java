/*
 * Derived from QuadHotbar by ArchangelD (mod_id: quadhotbar), LGPL-3.0.
 * This file remains under LGPL-3.0. See THIRD-PARTY.md for the list of changes.
 */
package com.xmly.endlessrefined.hotbar.engine;

import com.xmly.endlessrefined.EndlessRefined;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Client settings of the hotbar engine.
 *
 * <p>{@link #hotbarGroups} is the number of groups of nine slots the engine can move
 * the selection across. It replaces QuadHotbar's {@code hotbarRows}, which meant both
 * this and "how many rows to draw" — see {@link HotbarDisplayMode} for the split.
 */
@Mod.EventBusSubscriber(modid = EndlessRefined.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HotbarEngineConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue HOTBAR_GROUPS = BUILDER
            .comment("How many groups of nine slots the hotbar can select across. Valid range: 1-4.")
            .defineInRange("hotbarGroups", 2, 1, 4);

    private static final ForgeConfigSpec.BooleanValue WRAP_SCROLL = BUILDER
            .comment("When true, scrolling past the last slot wraps to the first slot and back.")
            .define("wrapScroll", true);

    private static final ForgeConfigSpec.IntValue PANEL_X = BUILDER
            .comment("Where the player put the hotbar panel, or -1 to follow the container GUI.")
            .defineInRange("panelX", -1, -1, 100000);

    private static final ForgeConfigSpec.IntValue PANEL_Y = BUILDER
            .comment("Where the player put the hotbar panel, or -1 to follow the container GUI.")
            .defineInRange("panelY", -1, -1, 100000);

    private static final ForgeConfigSpec.BooleanValue PANEL_EXPANDED = BUILDER
            .comment("Whether the hotbar panel was left open.")
            .define("panelExpanded", false);

    private static final ForgeConfigSpec.IntValue ENDLESS_OFFSET_X = BUILDER
            .comment("How far the Endless Inventory overlay is shifted from where it puts itself.")
            .defineInRange("endlessOffsetX", 0, -10000, 10000);

    private static final ForgeConfigSpec.IntValue ENDLESS_OFFSET_Y = BUILDER
            .comment("How far the Endless Inventory overlay is shifted from where it puts itself.")
            .defineInRange("endlessOffsetY", 0, -10000, 10000);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int hotbarGroups = 2;
    public static boolean wrapScroll = true;
    public static int panelX = -1;
    public static int panelY = -1;
    public static boolean panelExpanded = false;
    public static int endlessOffsetX = 0;
    public static int endlessOffsetY = 0;

    private HotbarEngineConfig() {
    }

    public static void setHotbarGroups(int groups) {
        HOTBAR_GROUPS.set(Math.max(1, Math.min(4, groups)));
        hotbarGroups = HOTBAR_GROUPS.get();
        SPEC.save();
    }

    public static void setWrapScroll(boolean value) {
        WRAP_SCROLL.set(value);
        wrapScroll = WRAP_SCROLL.get();
        SPEC.save();
    }

    /** Remembers where the panel was dragged to. */
    public static void setPanelPosition(int x, int y) {
        PANEL_X.set(x);
        PANEL_Y.set(y);
        panelX = x;
        panelY = y;
        SPEC.save();
    }

    public static void setPanelExpanded(boolean expanded) {
        PANEL_EXPANDED.set(expanded);
        panelExpanded = expanded;
        SPEC.save();
    }

    public static void setEndlessOffset(int x, int y) {
        ENDLESS_OFFSET_X.set(x);
        ENDLESS_OFFSET_Y.set(y);
        endlessOffsetX = x;
        endlessOffsetY = y;
        SPEC.save();
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        hotbarGroups = HOTBAR_GROUPS.get();
        wrapScroll = WRAP_SCROLL.get();
        panelX = PANEL_X.get();
        panelY = PANEL_Y.get();
        panelExpanded = PANEL_EXPANDED.get();
        endlessOffsetX = ENDLESS_OFFSET_X.get();
        endlessOffsetY = ENDLESS_OFFSET_Y.get();
    }
}
