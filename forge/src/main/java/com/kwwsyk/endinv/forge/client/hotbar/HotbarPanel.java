package com.kwwsyk.endinv.forge.client.hotbar;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.ClientModInfo;
import com.kwwsyk.endinv.common.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.client.option.MenuAttachabilityCache;
import com.kwwsyk.endinv.common.client.gui.bg.ScreenRectangleWidgetParam;
import com.kwwsyk.endinv.common.client.hotbar.HotbarPanelState;
import com.kwwsyk.endinv.common.client.option.IClientConfig;
import com.kwwsyk.endinv.common.hotbar.HotbarTable;
import com.kwwsyk.endinv.forge.client.events.ScreenAttachment;
import com.kwwsyk.endinv.forge.network.payloads.HotbarCellClearPayload;
import com.kwwsyk.endinv.forge.network.payloads.HotbarPanelPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

/**Two small tabs over a container screen, and the whole hotbar table behind one of them.<br>
 * The table tab opens a nine by nine grid of the items the hotbar holds; the other puts the EndInv
 * panel itself away and brings it back. Both sit just right of the EndInv panel's search box, and
 * follow it: the position they are dragged to is stored as a distance from there, not as a place
 * on the screen, so moving the panel moves them too.
 */
@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID, value = Dist.CLIENT)
public final class HotbarPanel {

    private static final int TAB_SIZE = 20;
    private static final int TAB_GAP = 2;
    private static final int CELL_SIZE = 18;
    private static final int GRID_SIZE = HotbarTable.COLUMNS * CELL_SIZE;
    private static final int GAP = 2;

    private static final int PANEL_BACKGROUND = 0xE0101010;
    private static final int SLOT_BORDER = 0xFF373737;
    private static final int SLOT_FACE = 0xFF8B8B8B;

    @Nullable
    private static Geometry dragging;
    private static int dragOffsetX;
    private static int dragOffsetY;

    private HotbarPanel() {
    }

    /**Whether the tabs belong on the screen being drawn.<br>
     * They sit beside the EndInv panel and are meaningless without it, so they are only drawn where
     * that panel is attached. That leaves them off the Refined Storage grid screen, which shows the
     * same contents and suppresses the panel, and off the EndInv screen itself.
     */
    private static boolean shown(Screen screen) {
        if (!HotbarEngine.isActive() || !(screen instanceof AbstractContainerScreen<?> container)) {
            return false;
        }
        if (screen instanceof EndlessInventoryScreen) {
            return false;
        }
        // Asked of the attachability rule rather than of the panel that happens to be attached: the
        // panel can be put away for a moment with the EndInv tab, and the tabs beside it should not
        // vanish and come back when it is.
        return MenuAttachabilityCache.isAttachable(container);
    }

    /**Drawn as early as this event allows.<br>
     * The EndInv overlay draws on the same event, and whichever handler runs last ends up on top.
     * The tabs belong under that overlay and under its tooltips, so they go first.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRender(ScreenEvent.Render.Post event) {
        if (shown(event.getScreen())) {
            draw(event.getGuiGraphics(), geometry((AbstractContainerScreen<?>) event.getScreen()));
        }
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!shown(event.getScreen())) {
            return;
        }
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();

        Geometry geometry = geometry(screen);
        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();

        if (!geometry.contains(mouseX, mouseY)) {
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            dragOffsetX = mouseX - geometry.tabX();
            dragOffsetY = mouseY - geometry.tabY();
            dragging = clamped(screen, geometry.tabX(), geometry.tabY());
            event.setCanceled(true);
            return;
        }

        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }

        if (geometry.containsTableTab(mouseX, mouseY)) {
            HotbarPanelState.toggleExpanded();
            ModInfo.getPacketDistributor().sendToServer(new HotbarPanelPayload(HotbarPanelState.isExpanded()));
            event.setCanceled(true);
            return;
        }

        if (geometry.containsEndInvTab(mouseX, mouseY)) {
            ScreenAttachment.togglePanel();
            event.setCanceled(true);
            return;
        }


        int cell = geometry.cellAt(mouseX, mouseY);

        if (cell < 0) {
            return;
        }

        if (Screen.hasShiftDown()) {
            ModInfo.getPacketDistributor().sendToServer(new HotbarCellClearPayload(cell));
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (dragging == null) {
            return;
        }

        Screen screen = event.getScreen();

        dragging = clamped(screen, (int) event.getMouseX() - dragOffsetX, (int) event.getMouseY() - dragOffsetY);

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (dragging == null || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        setPanelOffset(event.getScreen(), dragging.tabX(), dragging.tabY());
        dragging = null;
        event.setCanceled(true);
    }

    /**Store where the tabs were dragged to, as a distance from where they put themselves. */
    private static void setPanelOffset(Screen screen, int tabX, int tabY) {
        IClientConfig config = ClientModInfo.getClientConfig();
        config.hotbarPanelX().set(tabX - anchorX(screen));
        config.hotbarPanelY().set(tabY - anchorY(screen));
        config.save();
    }

    /**Where the tabs put themselves, worked out once per screen.<br>
     * The place is taken from the EndInv panel's search box while the panel is there, and kept for
     * the screen it was taken on. Recomputing it whenever the panel is missing would drop the tabs
     * to the fallback corner every time the panel was put away, and they would jump back when it
     * came out again.
     */
    @Nullable
    private static Screen anchorScreen;
    private static int anchorCacheX = Integer.MIN_VALUE;
    private static int anchorCacheY = Integer.MIN_VALUE;

    private static void refreshAnchor(Screen screen) {
        if (anchorScreen != screen) {
            anchorScreen = screen;
            anchorCacheX = Integer.MIN_VALUE;
            anchorCacheY = Integer.MIN_VALUE;
        }
        ScreenFramework framework = ScreenFramework.getInstance();
        if (framework == null) {
            return;
        }
        ScreenRectangleWidgetParam search = framework.searchBoxParam();
        if (search == null) {
            return;
        }
        anchorCacheX = search.XPos() + search.XSize() + 4;
        anchorCacheY = search.YPos();
    }

    private static int anchorX(Screen screen) {
        refreshAnchor(screen);
        if (anchorCacheX != Integer.MIN_VALUE) {
            return anchorCacheX;
        }
        return screen instanceof AbstractContainerScreen<?> container
                ? container.getGuiLeft() - TAB_SIZE - 4
                : 4;
    }

    private static int anchorY(Screen screen) {
        refreshAnchor(screen);
        if (anchorCacheY != Integer.MIN_VALUE) {
            return anchorCacheY;
        }
        return screen instanceof AbstractContainerScreen<?> container
                ? container.getGuiTop() + container.getYSize() - TAB_SIZE
                : 4;
    }

    private static Geometry clamped(Screen screen, int tabX, int tabY) {
        int lowestY = Math.min(GRID_SIZE + GAP, Math.max(0, screen.height - TAB_SIZE));

        return new Geometry(Math.max(0, Math.min(tabX, Math.max(0, screen.width - TAB_SIZE))), Math.max(lowestY, Math.min(tabY, Math.max(lowestY, screen.height - TAB_SIZE))));
    }

    private static void draw(GuiGraphics graphics, Geometry geometry) {
        if (HotbarPanelState.isExpanded()) {
            graphics.fill(geometry.gridX() - 2, geometry.gridY() - 2, geometry.gridX() + GRID_SIZE + 2, geometry.gridY() + GRID_SIZE + 2, PANEL_BACKGROUND);

            for (int index = 0; index < HotbarTable.CELLS; index++) {
                int x = geometry.gridX() + index % HotbarTable.COLUMNS * CELL_SIZE;
                int y = geometry.gridY() + index / HotbarTable.COLUMNS * CELL_SIZE;

                // Drawn as a vanilla slot: dark border, lighter face. A single translucent
                // fill left nothing to separate one cell from the next, so the grid read as
                // one blank square.
                graphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, SLOT_BORDER);
                graphics.fill(x + 1, y + 1, x + CELL_SIZE - 1, y + CELL_SIZE - 1, SLOT_FACE);

                ItemStack stack = HotbarPanelState.cellAt(index);

                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, x + 1, y + 1);
                    graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x + 1, y + 1);
                }
            }
        }

        // The table tab: a nine-square hint, so it reads as a grid rather than a plain button.
        int tabX = geometry.tabX();
        int tabY = geometry.tabY();
        drawTab(graphics, tabX, tabY, HotbarPanelState.isExpanded());
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                graphics.fill(tabX + 5 + column * 4, tabY + 5 + row * 4, tabX + 7 + column * 4, tabY + 7 + row * 4, 0xFFFFFFFF);
            }
        }

        // The EndInv tab: the outline of a window.
        int endInvX = geometry.endInvTabX();
        drawTab(graphics, endInvX, tabY, false);
        graphics.fill(endInvX + 5, tabY + 5, endInvX + 15, tabY + 6, 0xFFFFFFFF);
        graphics.fill(endInvX + 5, tabY + 9, endInvX + 15, tabY + 10, 0xFFFFFFFF);
        graphics.fill(endInvX + 5, tabY + 13, endInvX + 15, tabY + 14, 0xFFFFFFFF);
        graphics.fill(endInvX + 5, tabY + 5, endInvX + 6, tabY + 15, 0xFFFFFFFF);
        graphics.fill(endInvX + 14, tabY + 5, endInvX + 15, tabY + 15, 0xFFFFFFFF);
    }

    private static void drawTab(GuiGraphics graphics, int x, int y, boolean highlighted) {
        graphics.fill(x, y, x + TAB_SIZE, y + TAB_SIZE, SLOT_BORDER);
        graphics.fill(x + 1, y + 1, x + TAB_SIZE - 1, y + TAB_SIZE - 1, highlighted ? 0xFF4060C0 : SLOT_FACE);
    }

    private static Geometry geometry(AbstractContainerScreen<?> screen) {
        if (dragging != null) {
            return dragging;
        }

        IClientConfig config = ClientModInfo.getClientConfig();
        int tabX = anchorX(screen) + config.hotbarPanelX().get();
        int tabY = anchorY(screen) + config.hotbarPanelY().get();

        return clamped(screen, tabX, tabY);
    }

    private record Geometry(int tabX, int tabY) {

        int gridX() {
            return tabX;
        }

        int gridY() {
            return tabY - GRID_SIZE - GAP;
        }

        int endInvTabX() {
            return tabX + TAB_SIZE + TAB_GAP;
        }

        boolean containsTableTab(int mouseX, int mouseY) {
            return mouseX >= tabX && mouseX < tabX + TAB_SIZE && mouseY >= tabY && mouseY < tabY + TAB_SIZE;
        }

        boolean containsEndInvTab(int mouseX, int mouseY) {
            int x = endInvTabX();
            return mouseX >= x && mouseX < x + TAB_SIZE && mouseY >= tabY && mouseY < tabY + TAB_SIZE;
        }

        boolean contains(int mouseX, int mouseY) {
            if (containsTableTab(mouseX, mouseY) || containsEndInvTab(mouseX, mouseY)) {
                return true;
            }

            return HotbarPanelState.isExpanded() && mouseX >= gridX() && mouseX < gridX() + GRID_SIZE && mouseY >= gridY() && mouseY < gridY() + GRID_SIZE;
        }

        int cellAt(int mouseX, int mouseY) {
            if (!HotbarPanelState.isExpanded()) {
                return -1;
            }

            int column = (mouseX - gridX()) / CELL_SIZE;
            int row = (mouseY - gridY()) / CELL_SIZE;

            if (column < 0 || column >= HotbarTable.COLUMNS || row < 0 || row >= HotbarTable.ROWS) {
                return -1;
            }

            return row * HotbarTable.COLUMNS + column;
        }
    }
}
