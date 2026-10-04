/*
 * Derived from QuadHotbar by ArchangelD (mod_id: quadhotbar), LGPL-3.0.
 * This file remains under LGPL-3.0. See THIRD-PARTY.md for the list of changes.
 *
 * Changes from the original, beyond the package and mod id:
 *  - getRows() split into selectionGroups() and windowHeight(), with the drawn rows
 *    acting as a sliding window over the groups (HotbarDisplayMode).
 *  - the grave accent key cycles four display modes instead of toggling one flag.
 *  - onKey() ignores key presses with Ctrl held, so that this mod's Ctrl+1..0-page
 *    keys do not also trigger a row cycle. The original had no modifier check.
 */
package com.xmly.endlessrefined.hotbar.engine;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xmly.endlessrefined.EndlessRefined;
import com.xmly.endlessrefined.config.ERIConfig;
import com.xmly.endlessrefined.hotbar.HotbarDisplayMode;
import com.xmly.endlessrefined.network.HotbarStateC2S;
import com.xmly.endlessrefined.network.ScreenOpenC2S;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.xmly.endlessrefined.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = EndlessRefined.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HotbarEngineClientEvents {
    @SuppressWarnings("removal")
    private static final ResourceLocation WIDGETS = new ResourceLocation("minecraft", "textures/gui/widgets.png");
    private static final int HOTBAR_WIDTH = 182;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int SELECTOR_SIZE = 24;
    private static final int VANILLA_HOTBAR_SLOTS = 9;

    private static final int TEXT_FLOOR = 59;

    private static int lastRaise = 0;

    private static int lastNatural = 39;
    private static HotbarDisplayMode displayMode = HotbarDisplayMode.TWO_ROWS_COLUMNS;
    private static int activeGroup = 0;
    private static int pendingVisualSlot = -1;
    private static int pendingServerSelectedSlot = -1;
    private static int savedSelectedSlotBeforeVanilla = -1;
    private static boolean movingExperienceBar = false;
    private static boolean serverSupportsExtendedSlots = false;
    private static boolean lastScreenOpen = false;

    private HotbarEngineClientEvents() {
    }

    public static int activeGroup() {
        return activeGroup;
    }

    public static HotbarDisplayMode displayMode() {
        return displayMode;
    }

    public static boolean canSwitchGroups() {
        return selectionGroups() > 1;
    }

    public static boolean moveSelectionToGroup(int engineGroup, int column) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        int target = engineGroup * VANILLA_HOTBAR_SLOTS + column;
        if (target > selectionGroups() * VANILLA_HOTBAR_SLOTS - 1) {
            return false;
        }
        setSelectedSlot(minecraft.player, target);
        return true;
    }

    public static void showNotice(String translationKey) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(translationKey), true);
        }
    }

    private static int configuredGroups() {
        return Mth.clamp(HotbarEngineConfig.hotbarGroups, 1, 4);
    }

    private static int windowCounts() {
        return displayMode.windowCounts(configuredGroups());
    }

    private static int windowHeight() {
        return displayMode.windowHeight(configuredGroups());
    }

    private static int selectionGroups() {
        return displayMode.selectionGroups(configuredGroups());
    }

    @SubscribeEvent
    public static void onRenderHotbar(RenderGuiOverlayEvent.Pre event) {
        if (!ERIConfig.ENABLE_ENDLESS_HOTBAR.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        event.setCanceled(true);
        updateSelectionMode(minecraft);
        clampSelectedSlot(minecraft.player);
        renderHotbars(event.getGuiGraphics(), minecraft, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
    }

    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        if (event.getOverlay().id().equals(VanillaGuiOverlay.ITEM_NAME.id()) && minecraft.gui instanceof ForgeGui forgeGui) {
            lastNatural = Math.max(forgeGui.leftHeight, forgeGui.rightHeight) - lastRaise;
        }

        if (event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id()) && shouldLiftExtraHud()) {
            movingExperienceBar = true;
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0.0F, -HOTBAR_HEIGHT * (windowHeight() - 1), 0.0F);
        }
    }

    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (movingExperienceBar && event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            event.getGuiGraphics().pose().popPose();
            movingExperienceBar = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (event.isCanceled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || selectionGroups() <= 1) {
            return;
        }
        updateSelectionMode(minecraft);
        int selected = getSelectedSlot(minecraft.player);
        if (event.getScrollDelta() < 0) {
            selected++;
        } else if (event.getScrollDelta() > 0) {
            selected--;
        } else {
            return;
        }
        activateVisualSlot(minecraft, selected);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        updateSelectionMode(minecraft);
        if (displayMode == HotbarDisplayMode.VANILLA_ONLY || selectionGroups() <= 1) {
            return;
        }
        if (Screen.hasControlDown()) {
            return;
        }
        for (int slot = 0; slot < VANILLA_HOTBAR_SLOTS; slot++) {
            if (!minecraft.options.keyHotbarSlots[slot].matches(event.getKey(), event.getScanCode())) {
                continue;
            }
            int currentColumn = getSelectedColumn(minecraft.player);
            int group = currentColumn == slot ? activeGroup + 1 : 0;
            if (group >= selectionGroups()) {
                group = 0;
            }
            pendingVisualSlot = group * VANILLA_HOTBAR_SLOTS + slot;
            activateVisualSlot(minecraft, pendingVisualSlot);
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
            return;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        reportScreenState(minecraft);
        applyPendingServerSelection(minecraft);
        if (minecraft.player != null && minecraft.screen == null && HotbarEngineKeyMappings.OPEN_CONFIG.consumeClick()) {
            minecraft.setScreen(new HotbarEngineConfigScreen(null));
        }
        if (minecraft.player != null && minecraft.screen == null && HotbarEngineKeyMappings.CYCLE_DISPLAY.consumeClick()) {
            boolean shiftDown = Screen.hasShiftDown();
            cycleDisplayMode(minecraft, shiftDown);
        }
        int boundSlot = HotbarEngineKeyMappings.consumeHotbarSlot();
        if (minecraft.player != null && minecraft.screen == null && boundSlot >= 0 && boundSlot < selectionGroups() * VANILLA_HOTBAR_SLOTS) {
            pendingVisualSlot = -1;
            activateVisualSlot(minecraft, boundSlot);
        }
        if (pendingVisualSlot < 0) {
            return;
        }
        if (minecraft.player != null) {
            updateSelectionMode(minecraft);
            activateVisualSlot(minecraft, pendingVisualSlot);
        }
        pendingVisualSlot = -1;
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        setServerSupportsExtendedSlots(false);
        lastScreenOpen = false;

        NetworkHandler.sendToServer(new HotbarStateC2S(false, 0, configuredGroups()));
    }

    private static void reportScreenState(Minecraft minecraft) {
        boolean open = minecraft.screen instanceof AbstractContainerScreen<?>;
        if (open == lastScreenOpen) {
            return;
        }
        lastScreenOpen = open;

        if (minecraft.getConnection() == null) {
            return;
        }
        NetworkHandler.sendToServer(new ScreenOpenC2S(open));
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        setServerSupportsExtendedSlots(false);

        lastScreenOpen = false;
    }

    public static void setServerSupportsExtendedSlots(boolean supported) {
        serverSupportsExtendedSlots = supported;
        if (!supported) {
            pendingServerSelectedSlot = -1;
        }
    }

    public static void syncServerSelectedSlot(int selectedSlot) {
        serverSupportsExtendedSlots = true;
        pendingServerSelectedSlot = Mth.clamp(selectedSlot, 0, Inventory.INVENTORY_SIZE - 1);
        applyPendingServerSelection(Minecraft.getInstance());
    }

    private static void renderHotbars(GuiGraphics graphics, Minecraft minecraft, int screenWidth, int screenHeight) {
        syncActiveGroupFromSelected(minecraft.player);
        int groups = configuredGroups();
        HotbarDisplayMode mode = displayMode();
        int rows = windowCounts();
        int windowStart = mode.windowStart(activeGroup, groups);
        RenderSystem.enableBlend();
        if (mode == HotbarDisplayMode.ONE_ROW_WINDOWS) {
            for (int groupIdx = 0; groupIdx < groups; groupIdx++) {
                int[] coords = getHotbarCoords(groupIdx, rows, screenWidth, screenHeight);
                graphics.blit(WIDGETS, coords[0], coords[1], 0, 0, HOTBAR_WIDTH, HOTBAR_HEIGHT);
                renderRowItems(graphics, minecraft, groupIdx, coords[0], coords[1]);
            }
            int[] selectedCoords = getHotbarCoords(activeGroup, rows, screenWidth, screenHeight);
            int selectorX = selectedCoords[0] - 1 + getSelectedColumn(minecraft.player) * 20;
            int selectorY = selectedCoords[1] - 1;
            graphics.blit(WIDGETS, selectorX, selectorY, 0, HOTBAR_HEIGHT, SELECTOR_SIZE, SELECTOR_SIZE);
        } else {
            int selectedRow = mode.rowForActiveGroup(activeGroup, groups);
            for (int row = 0; row < rows; row++) {
                int[] coords = getHotbarCoords(row, rows, screenWidth, screenHeight);
                graphics.blit(WIDGETS, coords[0], coords[1], 0, 0, HOTBAR_WIDTH, HOTBAR_HEIGHT);
                renderRowItems(graphics, minecraft, windowStart + row, coords[0], coords[1]);
            }
            int[] selectedCoords = getHotbarCoords(selectedRow, rows, screenWidth, screenHeight);
            int selectorY = selectedCoords[1] - 1;
            int selectorX = selectedCoords[0] - 1 + getSelectedColumn(minecraft.player) * 20;
            graphics.blit(WIDGETS, selectorX, selectorY, 0, HOTBAR_HEIGHT, SELECTOR_SIZE, SELECTOR_SIZE);
        }
        renderOffhandSlot(graphics, minecraft, rows, screenWidth, screenHeight);
        RenderSystem.disableBlend();
    }

    private static void applyPendingServerSelection(Minecraft minecraft) {
        if (minecraft.player == null || pendingServerSelectedSlot < 0) {
            return;
        }
        minecraft.player.getInventory().selected = pendingServerSelectedSlot;
        pendingServerSelectedSlot = -1;
        syncActiveGroupFromSelected(minecraft.player);
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        if (!(Minecraft.getInstance().gui instanceof ForgeGui forgeGui)) {
            lastRaise = 0;
            return;
        }
        int extra = HOTBAR_HEIGHT * (windowHeight() - 1);
        int swallowed = Math.max(0, TEXT_FLOOR - lastNatural);
        lastRaise = extra + swallowed;
        forgeGui.leftHeight += lastRaise;
        forgeGui.rightHeight += lastRaise;
    }

    private static int[] getHotbarCoords(int row, int rows, int screenWidth, int screenHeight) {
        int x;
        int y;
        HotbarDisplayMode mode = displayMode();
        int groups = configuredGroups();
        return switch (mode) {
            case VANILLA_ONLY, ONE_WINDOWS -> {
                x = screenWidth / 2 - HOTBAR_WIDTH / 2;
                y = screenHeight - HOTBAR_HEIGHT;
                yield new int[]{x, y};
            }
            case TWO_ROWS_COLUMNS -> {
                if (rows == 2 || rows == 4) {
                    x = screenWidth / 2 - HOTBAR_WIDTH * (row % 2 == 0 ? 1 : 0);
                    y = screenHeight - HOTBAR_HEIGHT * (row < 2 ? 1 : 2);
                } else {
                    x = (int) ((float) screenWidth / 2 - HOTBAR_WIDTH * (row == 2 ? 0.5F : (row == 1 ? 0 : 1)));
                    y = screenHeight - HOTBAR_HEIGHT * (row == 2 ? 2 : 1);
                }
                yield new int[]{x, y};
            }
            case ONE_ROW_WINDOWS -> {
                x = screenWidth / 2 - (HOTBAR_WIDTH * groups) / 2 + row * HOTBAR_WIDTH;
                y = screenHeight - HOTBAR_HEIGHT;
                yield new int[]{x, y};
            }
            case ONE_COL_WINDOWS, COLUMN_ROWS_WINDOW -> {
                x = screenWidth / 2 - HOTBAR_WIDTH / 2;
                y = screenHeight - HOTBAR_HEIGHT * (row + 1);
                yield new int[]{x, y};
            }
            case ROW_COLUMNS_WINDOW -> {
                if (rows == 2) {
                    x = screenWidth / 2 - HOTBAR_WIDTH * (row % 2 == 0 ? 1 : 0);
                    y = screenHeight - HOTBAR_HEIGHT * (row < 2 ? 1 : 2);
                } else {
                    x = screenWidth / 2 - HOTBAR_WIDTH / 2;
                    y = screenHeight - HOTBAR_HEIGHT;
                }
                yield new int[]{x, y};
            }
        };
    }

    private static void renderOffhandSlot(GuiGraphics graphics, Minecraft minecraft, int rows, int screenWidth, int screenHeight) {
        ItemStack offhand = null;
        if (minecraft.player != null) {
            offhand = minecraft.player.getOffhandItem();
        }
        if (offhand != null && offhand.isEmpty()) {
            return;
        }
        int leftX = screenWidth;
        int rightX = 0;
        int slotY = screenHeight - HOTBAR_HEIGHT;
        for (int row = 0; row < rows; row++) {
            int[] coords = getHotbarCoords(row, rows, screenWidth, screenHeight);
            if (coords[1] != slotY) {
                continue;
            }
            leftX = Math.min(leftX, coords[0]);
            rightX = Math.max(rightX, coords[0] + HOTBAR_WIDTH);
        }
        boolean offhandOnLeft = false;
        if (minecraft.player != null) {
            offhandOnLeft = minecraft.player.getMainArm() == HumanoidArm.RIGHT;
        }
        int slotX = offhandOnLeft ? leftX - HOTBAR_HEIGHT - 5 : rightX + 5;
        int textureX = offhandOnLeft ? SELECTOR_SIZE : SELECTOR_SIZE + HOTBAR_HEIGHT;
        graphics.blit(WIDGETS, slotX, slotY, textureX, HOTBAR_HEIGHT + 1, (HOTBAR_HEIGHT), HOTBAR_HEIGHT);
        if (offhand != null) {
            graphics.renderItem(offhand, slotX + 3, slotY + 3);
            graphics.renderItemDecorations(minecraft.font, offhand, slotX + 3, slotY + 3);
        }
    }

    private static void renderRowItems(GuiGraphics graphics, Minecraft minecraft, int group, int hotbarX, int hotbarY) {
        for (int column = 0; column < VANILLA_HOTBAR_SLOTS; column++) {
            int inventorySlot = group * VANILLA_HOTBAR_SLOTS + column;
            ItemStack stack = null;
            if (minecraft.player != null) {
                stack = minecraft.player.getInventory().getItem(inventorySlot);
            }
            if (stack != null && stack.isEmpty()) {
                continue;
            }
            int x = hotbarX + 3 + column * 20;
            int y = hotbarY + 3;
            if (stack != null) {
                graphics.renderItem(stack, x, y);
                graphics.renderItemDecorations(minecraft.font, stack, x, y);
            }
        }
    }

    private static void activateVisualSlot(Minecraft minecraft, int visualSlot) {
        if (minecraft.player == null) {
            return;
        }
        int max = selectionGroups() * VANILLA_HOTBAR_SLOTS - 1;
        if (HotbarEngineConfig.wrapScroll) {
            if (visualSlot < 0) {
                visualSlot = max;
            } else if (visualSlot > max) {
                visualSlot = 0;
            }
        } else {
            visualSlot = Mth.clamp(visualSlot, 0, max);
        }
        int targetGroup = visualSlot / VANILLA_HOTBAR_SLOTS;
        int column = visualSlot % VANILLA_HOTBAR_SLOTS;
        setSelectedSlot(minecraft.player, targetGroup * VANILLA_HOTBAR_SLOTS + column);
    }

    private static void cycleDisplayMode(Minecraft minecraft, boolean reverse) {
        if (minecraft.player == null) {
            return;
        }
        boolean wasVanillaOnly = displayMode == HotbarDisplayMode.VANILLA_ONLY;
        HotbarDisplayMode[] modes = HotbarDisplayMode.values();
        int nextOrdinal;
        if (reverse) {
            nextOrdinal = displayMode.ordinal() - 1;
            if (nextOrdinal < 0) nextOrdinal = modes.length - 1;
        } else {
            nextOrdinal = displayMode.ordinal() + 1;
            if (nextOrdinal >= modes.length) nextOrdinal = 0;
        }
        displayMode = modes[nextOrdinal];
        boolean isVanillaOnly = displayMode == HotbarDisplayMode.VANILLA_ONLY;
        if (!wasVanillaOnly && isVanillaOnly) {
            savedSelectedSlotBeforeVanilla = getSelectedSlot(minecraft.player);
            setSelectedSlot(minecraft.player, getSelectedColumn(minecraft.player));
        } else if (wasVanillaOnly && !isVanillaOnly && savedSelectedSlotBeforeVanilla >= 0) {
            setSelectedSlot(minecraft.player, savedSelectedSlotBeforeVanilla);
            savedSelectedSlotBeforeVanilla = -1;
        }
        pendingVisualSlot = -1;
        showNotice(displayMode.translationKey());
    }

    private static void clampSelectedSlot(LocalPlayer player) {
        setSelectedSlot(player, getSelectedSlot(player));
    }

    private static int getSelectedColumn(LocalPlayer player) {
        return Mth.clamp(getSelectedSlot(player) % VANILLA_HOTBAR_SLOTS, 0, VANILLA_HOTBAR_SLOTS - 1);
    }

    private static int getSelectedSlot(LocalPlayer player) {
        int max = selectionGroups() * VANILLA_HOTBAR_SLOTS - 1;
        return Mth.clamp(player.getInventory().selected, 0, max);
    }

    private static void setSelectedSlot(LocalPlayer player, int slot) {
        int max = selectionGroups() * VANILLA_HOTBAR_SLOTS - 1;
        int clampedSlot = Mth.clamp(slot, 0, max);
        if (player.getInventory().selected != clampedSlot) {
            player.getInventory().selected = clampedSlot;
            player.connection.send(new ServerboundSetCarriedItemPacket(clampedSlot));
        }
        syncActiveGroupFromSelected(player);
    }

    private static void syncActiveGroupFromSelected(LocalPlayer player) {
        int groups = selectionGroups();
        if (activeGroup >= groups) {
            activeGroup = 0;
        }
        activeGroup = Mth.clamp(getSelectedSlot(player) / VANILLA_HOTBAR_SLOTS, 0, groups - 1);
    }

    private static void updateSelectionMode(Minecraft minecraft) {
        if (minecraft.player == null || supportsExtendedSlots(minecraft)) {
            return;
        }
        activeGroup = 0;
        int selectedColumn = getSelectedColumn(minecraft.player);
        if (minecraft.player.getInventory().selected != selectedColumn) {
            setSelectedSlot(minecraft.player, selectedColumn);
        }
    }

    private static boolean supportsExtendedSlots(Minecraft minecraft) {
        return minecraft.getSingleplayerServer() != null || serverSupportsExtendedSlots;
    }

    private static boolean shouldLiftExtraHud() {
        return windowHeight() > 1;
    }
}
