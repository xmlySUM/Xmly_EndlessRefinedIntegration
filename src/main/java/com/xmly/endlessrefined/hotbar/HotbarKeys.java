package com.xmly.endlessrefined.hotbar;

import com.mojang.blaze3d.platform.InputConstants;
import com.xmly.endlessrefined.EndlessRefined;
import com.xmly.endlessrefined.hotbar.engine.HotbarEngineClientEvents;
import com.xmly.endlessrefined.hotbar.engine.HotbarEngineConfig;
import com.xmly.endlessrefined.network.HotbarStateC2S;
import com.xmly.endlessrefined.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = EndlessRefined.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HotbarKeys {

    private static final KeyMapping[] PAGE_KEYS = new KeyMapping[10];

    private HotbarKeys() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        for (int digit = 0; digit <= 9; digit++) {
            int key = digit == 0 ? GLFW.GLFW_KEY_0 : GLFW.GLFW_KEY_1 + digit - 1;

            PAGE_KEYS[digit] = new KeyMapping("key.endless_refined.row_" + digit, KeyConflictContext.IN_GAME, KeyModifier.CONTROL, InputConstants.Type.KEYSYM.getOrCreate(key), "key.categories.endless_refined");

            event.register(PAGE_KEYS[digit]);
        }
    }

    @Mod.EventBusSubscriber(modid = EndlessRefined.MOD_ID, value = Dist.CLIENT)
    public static final class ClientTick {

        private static int lastReportedGroups = -1;

        private ClientTick() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player == null || minecraft.screen != null) {
                return;
            }

            reportGroupsIfChanged();

            for (int digit = 0; digit <= 9; digit++) {
                if (PAGE_KEYS[digit] != null && PAGE_KEYS[digit].consumeClick()) {
                    apply(minecraft, digit);
                    return;
                }
            }
        }

        /**
         * Keeps the server's idea of the group count current. It needs it for the
         * overflow sweep, which must not touch a slot the hotbar is showing, and the
         * count is a client config the server cannot read.
         */
        private static void reportGroupsIfChanged() {
            int groups = HotbarEngineConfig.hotbarGroups;

            if (groups == lastReportedGroups) {
                return;
            }

            lastReportedGroups = groups;
            NetworkHandler.sendToServer(new HotbarStateC2S(false, 0, groups));
        }

        private static void apply(Minecraft minecraft, int digit) {
            int groups = HotbarEngineConfig.hotbarGroups;

            // Ctrl+0 swaps to the ender chest and back, or ends a mapping. Which of those it
            // did is the server's to decide, and to say.
            if (digit == 0) {
                NetworkHandler.sendToServer(new HotbarStateC2S(true, 0, groups));
                return;
            }

            if (groups < 2) {
                HotbarEngineClientEvents.showNotice("endless_refined.notice.no_groups");
                return;
            }

            if (!HotbarEngineClientEvents.canSwitchGroups()) {
                HotbarEngineClientEvents.showNotice("endless_refined.notice.layout_hides_groups");
                return;
            }

            int engineGroup = HotbarGroups.engineGroupForDigit(groups, digit);

            if (engineGroup <= 0) {
                HotbarEngineClientEvents.showNotice("endless_refined.notice.no_groups");
                return;
            }

            int column = 0;
            if (minecraft.player != null) {
                column = minecraft.player.getInventory().selected % 9;
            }

            NetworkHandler.sendToServer(new HotbarStateC2S(true, digit, groups));
            HotbarEngineClientEvents.moveSelectionToGroup(engineGroup, column);
        }
    }
}
