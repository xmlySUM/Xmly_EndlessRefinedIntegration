/*
 * Derived from QuadHotbar by ArchangelD (mod_id: quadhotbar), LGPL-3.0.
 * This file remains under LGPL-3.0. See THIRD-PARTY.md for the list of changes.
 */
package com.xmly.endlessrefined.hotbar.engine;

import com.mojang.blaze3d.platform.InputConstants;
import com.xmly.endlessrefined.EndlessRefined;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * The engine's own keys.
 *
 * <p>The per-slot bindings exist because vanilla only offers nine hotbar keys: with
 * more than nine selectable slots there is otherwise no way to reach slot 10 and up
 * by keyboard. They ship unbound; cycling rows with the vanilla 1-9 keys is the
 * default way to move between groups.
 */
@Mod.EventBusSubscriber(modid = EndlessRefined.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HotbarEngineKeyMappings {

    private static final int FIRST_CUSTOM_SLOT = 9;
    public static final String CATEGORY = "key.categories.endless_refined";

    public static final KeyMapping OPEN_CONFIG = create("key.endless_refined.open_config");

    public static final KeyMapping CYCLE_DISPLAY = new KeyMapping(
            "key.endless_refined.cycle_display",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSLASH,
            CATEGORY);

    public static final KeyMapping[] HOTBAR_SLOTS = createSlotMappings();

    private HotbarEngineKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
        event.register(CYCLE_DISPLAY);

        for (KeyMapping keyMapping : HOTBAR_SLOTS) {
            event.register(keyMapping);
        }
    }

    public static int consumeHotbarSlot() {
        for (int slot = 0; slot < HOTBAR_SLOTS.length; slot++) {
            if (HOTBAR_SLOTS[slot].consumeClick()) {
                return FIRST_CUSTOM_SLOT + slot;
            }
        }

        return -1;
    }

    private static KeyMapping[] createSlotMappings() {
        KeyMapping[] mappings = new KeyMapping[36 - FIRST_CUSTOM_SLOT];

        for (int slot = 0; slot < mappings.length; slot++) {
            mappings[slot] = create("key.endless_refined.slot." + (FIRST_CUSTOM_SLOT + slot + 1));
        }

        return mappings;
    }

    private static KeyMapping create(String translationKey) {
        return new KeyMapping(translationKey, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    }
}
