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

/**
 * Draws and drives the multi-group hotbar.
 *
 * <p>Vanilla's hotbar overlay is cancelled and redrawn here, because vanilla draws
 * exactly nine slots and this needs up to four rows of them. The selection itself is
 * entirely vanilla: {@code Inventory#selected} simply ranges past 8, which the two
 * mixins in {@code com.xmly.endlessrefined.mixin.hotbar} allow.
 */
@Mod.EventBusSubscriber(modid = EndlessRefined.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HotbarEngineClientEvents {

    @SuppressWarnings("removal")
    private static final ResourceLocation WIDGETS = new ResourceLocation("minecraft", "textures/gui/widgets.png");
    private static final int HOTBAR_WIDTH = 182;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int SELECTOR_SIZE = 24;
    private static final int VANILLA_HOTBAR_SLOTS = 9;

    /**
     * Where the held item's name would draw with an untouched HUD, and the lower of the two floors
     * the HUD's text is placed against. The action bar message has one nine higher, which is why
     * one measurement serves both.
     */
    private static final int TEXT_FLOOR = 59;

    /**
     * How much the base was raised by at the start of this frame.
     */
    private static int lastRaise = 0;

    /**
     * How tall the HUD would have stacked itself with no raise, as measured last frame.
     *
     * <p>Measured a frame behind because the accumulator is reset to 39 just before the frame's
     * first event and only reaches its final value as the elements draw. It is stable from frame to
     * frame, so a one-frame-old reading is the same number.
     */
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

    /**
     * The group of nine the selection is currently in.
     */
    public static int activeGroup() {
        return activeGroup;
    }

    public static HotbarDisplayMode displayMode() {
        return displayMode;
    }

    /**
     * Whether more than one group can be selected, i.e. page keys can do anything.
     */
    public static boolean canSwitchGroups() {
        return selectionGroups() > 1;
    }

    /**
     * Moves the selection to {@code column} of {@code engineGroup}, which is how a page
     * key keeps the player's chosen column while changing which group it is in.
     *
     * @return false when the current layout does not allow reaching that group
     */
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

    /**
     * Shows a message above the hotbar.
     */
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

        // Measured here because this is late enough to see the whole stack: everything that adds to
        // the HUD's heights has drawn by now, and the text that reads them draws after this. The
        // raise it is measured net of is the one applied at the start of this frame.
        if (event.getOverlay().id().equals(VanillaGuiOverlay.ITEM_NAME.id())
                && minecraft.gui instanceof ForgeGui forgeGui) {
            lastNatural = Math.max(forgeGui.leftHeight, forgeGui.rightHeight) - lastRaise;
        }

        // The experience bar is drawn at a height of its own rather than from the HUD's running
        // heights, so it is the one element that has to be moved by hand.
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

    /**
     * Other mods get the scroll wheel first for item-specific interactions.
     * This only handles normal hotbar scrolling, as a fallback.
     */
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
        // Ctrl+1..0 belong to the page keys. Without this, holding Ctrl would also
        // cycle a hotbar row here, because KeyMapping#matches ignores modifiers.
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

        if (minecraft.player != null
                && minecraft.screen == null
                && boundSlot >= 0
                && boundSlot < selectionGroups() * VANILLA_HOTBAR_SLOTS) {
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

        // Tell the server how many groups the hotbar shows, so its overflow sweep knows
        // which extended slots to leave alone. Digit 0: nothing is mirrored yet.
        NetworkHandler.sendToServer(new HotbarStateC2S(false, 0, configuredGroups()));
    }

    /**
     * Keeps the server's idea of whether a container screen is open current.
     *
     * <p>The server cannot tell on its own, and it needs to know: a hotbar slot that refills
     * itself while the player is moving stacks around by hand cannot be emptied, and each
     * attempt costs them a stack from Endless.
     */
    private static void reportScreenState(Minecraft minecraft) {
        boolean open = minecraft.screen instanceof AbstractContainerScreen<?>;

        if (open == lastScreenOpen) {
            return;
        }

        lastScreenOpen = open;
        NetworkHandler.sendToServer(new ScreenOpenC2S(open));
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        setServerSupportsExtendedSlots(false);
    }

    public static void setServerSupportsExtendedSlots(boolean supported) {
        serverSupportsExtendedSlots = supported;

        if (!supported) {
            pendingServerSelectedSlot = -1;
        }
    }

    /**
     * Vanilla's login packets do not synchronize the selected inventory slot.
     * Use the server's saved value before processing client hotbar input.
     */
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
            // ONE_ROW_WINDOWS：单行横向渲染全部组
            for (int groupIdx = 0; groupIdx < groups; groupIdx++) {
                int[] coords = getHotbarCoords(groupIdx, rows, screenWidth, screenHeight);
                graphics.blit(WIDGETS, coords[0], coords[1], 0, 0, HOTBAR_WIDTH, HOTBAR_HEIGHT);
                renderRowItems(graphics, minecraft, groupIdx, coords[0], coords[1]);
            }
            // 选中框位置：activeGroup作为索引
            int[] selectedCoords = getHotbarCoords(activeGroup, rows, screenWidth, screenHeight);
            int selectorX = selectedCoords[0] - 1 + getSelectedColumn(minecraft.player) * 20;
            int selectorY = selectedCoords[1] - 1;
            graphics.blit(WIDGETS, selectorX, selectorY, 0, HOTBAR_HEIGHT, SELECTOR_SIZE, SELECTOR_SIZE);
        } else {
            // 原有滑动窗口渲染逻辑
            int selectedRow = mode.rowForActiveGroup(activeGroup, groups);
            for (int row = 0; row < rows; row++) {
                int[] coords = getHotbarCoords(row, rows, screenWidth, screenHeight);
                graphics.blit(WIDGETS, coords[0], coords[1], 0, 0, HOTBAR_WIDTH, HOTBAR_HEIGHT);
                renderRowItems(graphics, minecraft, windowStart + row, coords[0], coords[1]);
            }
            // 选中框位置：activeGroup作为索引
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

    /**
     * Lifts the HUD stack clear of the extra hotbar rows.
     *
     * <p>{@code leftHeight} and {@code rightHeight} are reset to 39 at the start of every HUD frame
     * and then accumulated by each element as it draws. {@link RenderGuiEvent.Pre} fires after that
     * reset and before anything draws, so raising the base here lifts everything that comes after
     * it — the health, armour and food bars, and both pieces of text.
     *
     * <p>The text needs more than the bars do. The action bar message and the held item's name are
     * placed from the accumulator, but each has a floor below which it will not go — 68 and 59, the
     * spots vanilla draws them at. Raising the accumulator therefore moves them by the raise
     * <em>minus</em> whatever the floor swallows, and how much that is depends on how tall the rest
     * of the HUD already was. The two floors are exactly nine apart, so both swallow the same
     * amount, and it is measurable: {@code max(0, 59 - natural)}.
     *
     * <p>That is why this is not a per-game-mode constant. Measured against a full survival HUD the
     * floors swallow nothing and the plain raise is already right; in creative, where no health or
     * food bars are drawn, they swallow twenty pixels; in a pack that puts its own bar there, some
     * other number again. Reading the height the last frame actually reached covers all of them,
     * without asking what game mode the player is in.
     *
     * <p>Added rather than clamped to a fixed height, which is what this used to do: clamping moved
     * the accumulator to an absolute value, so the distance things moved was the difference between
     * that value and however tall the stack already was.
     */
    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
//        if (!shouldLiftExtraHud() || !(Minecraft.getInstance().gui instanceof ForgeGui forgeGui)) {
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
                // TWO_ROWS_COLUMNS：原有逻辑
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
                // ONE_ROW_WINDOWS: 单行横向排布
                x = screenWidth / 2 - (HOTBAR_WIDTH * groups) / 2 + row * HOTBAR_WIDTH;
                y = screenHeight - HOTBAR_HEIGHT;
                yield new int[]{x, y};
            }
            case ONE_COL_WINDOWS -> {
                // ONE_COL_WINDOWS：单列纵向堆叠
                x = screenWidth / 2 - HOTBAR_WIDTH / 2;
                y = screenHeight - HOTBAR_HEIGHT * (row + 1);
                yield new int[]{x, y};
            }
            case ROW_COLUMNS_WINDOW -> {
                // ROW_COLUMNS_WINDOW：原2行2列逻辑保留
                if (rows == 2) {
                    x = screenWidth / 2 - HOTBAR_WIDTH * (row % 2 == 0 ? 1 : 0);
                    y = screenHeight - HOTBAR_HEIGHT * (row < 2 ? 1 : 2);
                } else {
                    x = screenWidth / 2 - HOTBAR_WIDTH / 2;
                    y = screenHeight - HOTBAR_HEIGHT;
                }
                yield new int[]{x, y};
            }
            case COLUMN_ROWS_WINDOW -> {
                // COLUMN_ROWS_WINDOW: 2行1列
                x = screenWidth / 2 - HOTBAR_WIDTH / 2;
                y = screenHeight - HOTBAR_HEIGHT * (row + 1);
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
        // 正向 / 逆向选择下一个模式
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
