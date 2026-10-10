/*
 * Derived from QuadHotbar by ArchangelD (mod_id: quadhotbar), LGPL-3.0.
 * This file remains under LGPL-3.0. See THIRD-PARTY.md for the list of changes.
 */
package com.kwwsyk.endinv.forge.client.hotbar;

import com.kwwsyk.endinv.common.ModInfo;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HotbarEngineKeyMappings {

    private static final int FIRST_CUSTOM_SLOT = 9;
    public static final String CATEGORY = "key.categories.xmly_endless_refined";
    public static final KeyMapping CYCLE_DISPLAY = new KeyMapping("key.xmly_endless_refined.cycle_display",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH, CATEGORY);
    public static final KeyMapping[] HOTBAR_SLOTS = createSlotMappings();

    private HotbarEngineKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
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
            mappings[slot] = create("key.xmly_endless_refined.slot." + (FIRST_CUSTOM_SLOT + slot + 1));
        }
        return mappings;
    }

    private static KeyMapping create(String translationKey) {
        return new KeyMapping(translationKey, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    }
}
