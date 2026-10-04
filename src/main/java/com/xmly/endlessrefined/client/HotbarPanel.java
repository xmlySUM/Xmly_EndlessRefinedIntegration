package com.xmly.endlessrefined.client;

import com.xmly.endlessrefined.EndlessRefined;
import com.xmly.endlessrefined.hotbar.HotbarTable;
import com.xmly.endlessrefined.hotbar.engine.HotbarEngineConfig;
import com.xmly.endlessrefined.network.HotbarCellClearC2S;
import com.xmly.endlessrefined.network.HotbarPanelC2S;
import com.xmly.endlessrefined.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = EndlessRefined.MOD_ID, value = Dist.CLIENT)
public final class HotbarPanel {

    private static final int TAB_SIZE = 20;
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

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            draw(event.getGuiGraphics(), geometry(screen));
        }
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }

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

        if (geometry.containsTab(mouseX, mouseY)) {
            HotbarPanelState.toggleExpanded();
            NetworkHandler.sendToServer(new HotbarPanelC2S(HotbarPanelState.isExpanded()));
            event.setCanceled(true);
            return;
        }

        int cell = geometry.cellAt(mouseX, mouseY);

        if (cell < 0) {
            return;
        }

        if (Screen.hasShiftDown()) {
            NetworkHandler.sendToServer(new HotbarCellClearC2S(cell));
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

        HotbarEngineConfig.setPanelPosition(dragging.tabX(), dragging.tabY());
        dragging = null;
        event.setCanceled(true);
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

        int tabX = geometry.tabX();
        int tabY = geometry.tabY();

        graphics.fill(tabX, tabY, tabX + TAB_SIZE, tabY + TAB_SIZE, SLOT_BORDER);
        graphics.fill(tabX + 1, tabY + 1, tabX + TAB_SIZE - 1, tabY + TAB_SIZE - 1, HotbarPanelState.isExpanded() ? 0xFF4060C0 : SLOT_FACE);

        // A nine-square hint, so the tab reads as a grid rather than a plain button.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                graphics.fill(tabX + 5 + column * 4, tabY + 5 + row * 4, tabX + 7 + column * 4, tabY + 7 + row * 4, 0xFFFFFFFF);
            }
        }
    }

    private static Geometry geometry(AbstractContainerScreen<?> screen) {
        if (dragging != null) {
            return dragging;
        }

        int tabX = HotbarEngineConfig.panelX;
        int tabY = HotbarEngineConfig.panelY;

        if (tabX < 0 || tabY < 0) {
            // Just outside the container's bottom-left corner: where the eye goes for it,
            // and where it covers nothing.
            tabX = screen.getGuiLeft() - TAB_SIZE - 4;
            tabY = screen.getGuiTop() + screen.getYSize() - TAB_SIZE;
        }

        return clamped(screen, tabX, tabY);
    }

    private record Geometry(int tabX, int tabY) {

        int gridX() {
            return tabX;
        }

        int gridY() {
            return tabY - GRID_SIZE - GAP;
        }

        boolean containsTab(int mouseX, int mouseY) {
            return mouseX >= tabX && mouseX < tabX + TAB_SIZE && mouseY >= tabY && mouseY < tabY + TAB_SIZE;
        }

        boolean contains(int mouseX, int mouseY) {
            if (containsTab(mouseX, mouseY)) {
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
