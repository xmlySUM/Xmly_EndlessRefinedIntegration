package com.kwwsyk.endinv.common.client.gui;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.gui.page.ItemPage;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.network.payloads.toServer.ToggleCraftingPayload;
import com.kwwsyk.endinv.common.util.UiTrace;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@SuppressWarnings("removal")
public class EndlessInventoryScreen extends AbstractContainerScreen<EndlessInventoryMenu> {
    private static final ResourceLocation CRAFTING_TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/crafting_table.png");
    private ScreenFramework frameWork;

    /**Says something only when the screen's geometry changes, so that a render loop does not
     * drown everything else out.
     */
    private static final UiTrace.Watch GEOMETRY = new UiTrace.Watch("render:");
    private CycleButton<Boolean> craftingToggleButton;
    private boolean craftingVisible;

    public EndlessInventoryScreen(EndlessInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        recalcDimensions();
    }

    private void recalcDimensions() {
        int baseRows = menu.getBaseRows();
        this.imageHeight = 114 + baseRows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    public void init() {
        super.init();
        craftingVisible = menu.isCraftingVisible();
        recalcDimensions();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        UiTrace.log("screen init: baseRows={} visibleRows={} craftingVisible={} imageHeight={}",
                menu.getBaseRows(), menu.getVisibleRows(), menu.isCraftingVisible(), imageHeight);
        var existing = ScreenFramework.getInstance();
        if (existing != null) {
            existing.onClose();
        }
        this.frameWork = new ScreenFramework(this);
        UiTrace.log("screen framework: pageRows={} columns={} backgroundRows={} menuVisibleRows={}",
                frameWork.rows(), frameWork.columns(), frameWork.SFBgRenderer.rows(), menu.getVisibleRows());

        frameWork.addWidgetToScreen(this::addRenderableWidget);
        addCraftingToggleButton();
    }

    private void addCraftingToggleButton() {
        int width = 70;
        this.craftingToggleButton = CycleButton.onOffBuilder()
                .withInitialValue(false)
                .create(0, 0, width, 20, Component.literal("Crafter"), (it, on) -> {
                    toggleCrafting();
                    if (it.getValue() != craftingVisible) it.setValue(craftingVisible);
                });
        updateCraftingToggleButtonPosition();
        addRenderableWidget(this.craftingToggleButton);
    }

    /**
     * Keeps the crafting toggle anchored to the screen chrome after layout changes.
     */
    private void updateCraftingToggleButtonPosition() {
        if (this.craftingToggleButton == null) {
            return;
        }
        int width = this.craftingToggleButton.getWidth();
        int x = this.leftPos + this.imageWidth - width - 8;
        int y = this.topPos - 20;
        this.craftingToggleButton.setX(x);
        this.craftingToggleButton.setY(y);
    }

    /**
     * Toggle crafter visibility and realign the surrounding widgets without rebuilding the screen.
     */
    private void toggleCrafting() {
        craftingVisible = !craftingVisible;
        menu.setCraftingVisible(craftingVisible);
        ModInfo.getPacketDistributor().sendToServer(new ToggleCraftingPayload(craftingVisible));
        UiTrace.log("crafter button -> {}", craftingVisible);
        applyCraftingVisibility();
    }

    /**
     * Take the menu's crafter state as the screen's own.<br>
     * The page is drawn for as many rows as the menu has to show, so when the crafter takes two of
     * them away the page has to be resized to match - otherwise the page and the crafter are drawn
     * on top of one another. The button does this itself, but a JEI recipe transfer turns the
     * crafter on through the menu without the screen ever hearing about it.
     */
    private void applyCraftingVisibility() {
        int previousTop = this.topPos;
        recalcDimensions();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        updateCraftingToggleButtonPosition();
        if (craftingToggleButton != null) {
            craftingToggleButton.setValue(craftingVisible);
        }
        if (frameWork != null) {
            UiTrace.log("applying crafter {}: imageHeight={} frameworkRows={} -> {}",
                    craftingVisible, imageHeight, frameWork.rows(), menu.getVisibleRows());
            frameWork.resizePageRows(menu.getVisibleRows());
            frameWork.move(0, this.topPos - previousTop);
        }
    }

    /**
     * Notice the crafter being turned on or off by something other than the button.
     */
    private void syncCraftingVisibility() {
        if (menu.isCraftingVisible() == craftingVisible) {
            return;
        }
        UiTrace.log("crafter followed the menu: {} -> {}", craftingVisible, menu.isCraftingVisible());
        craftingVisible = menu.isCraftingVisible();
        applyCraftingVisibility();
    }

    private void drawCraftingBackground(GuiGraphics guiGraphics) {
        int craftX = this.leftPos;
        int craftY = this.topPos + 18 * menu.getVisibleRows() + 18;
        UiTrace.log("crafter background at x={} y={} (topPos={} visibleRows={})", craftX, craftY, topPos, menu.getVisibleRows());
        guiGraphics.blit(CRAFTING_TEXTURE, craftX, craftY, 0, 12, 176, 58);
    }

    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        syncCraftingVisibility();
        int active = 0;
        for (Slot slot : menu.slots) {
            if (slot.isActive()) {
                active++;
            }
        }
        GEOMETRY.log("leftPos={} topPos={} imageH={} menuRows={} craft={} pageRows={} pageColumns={} bgRows={} slots={}/{} firstSlotActive={}",
                leftPos, topPos, imageHeight, menu.getVisibleRows(), menu.isCraftingVisible(),
                frameWork == null ? -1 : frameWork.rows(), frameWork == null ? -1 : frameWork.columns(),
                frameWork == null || frameWork.SFBgRenderer == null ? -1 : frameWork.SFBgRenderer.rows(),
                active, menu.slots.size(), !menu.slots.isEmpty() && menu.slots.get(0).isActive());
        this.renderBackground(gui);
        frameWork.renderPre(gui, mouseX, mouseY, partialTick);

        super.render(gui, mouseX, mouseY, partialTick);

        frameWork.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int keyCode) {
        for (GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener.mouseClicked(mouseX, mouseY, keyCode)) {
                this.setFocused(guieventlistener);
                if (keyCode == 0) {
                    this.setDragging(true);
                }
                return true;
            }
        }
        return frameWork.mouseClicked(mouseX, mouseY, keyCode) || super.mouseClicked(mouseX, mouseY, keyCode);
    }


    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return frameWork.mouseDragged(mouseX, mouseY, button, dragX, dragY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int keyCode) {
        return frameWork.mouseReleased(mouseX, mouseY, keyCode) || super.mouseReleased(mouseX, mouseY, keyCode);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return super.mouseScrolled(mouseX, mouseY, scrollY) || frameWork.mouseScrolled(mouseX, mouseY, scrollY);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return frameWork.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return frameWork.charTyped(codePoint, modifiers);
    }

    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, @NotNull ClickType type) {
        super.slotClicked(slot, slotId, mouseButton, type);
        this.menu.broadcastChanges();
    }

    public void onClose() {
        super.onClose();
        frameWork.onClose();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        this.frameWork.renderBg(guiGraphics, mouseX, mouseY, partialTick);
        if (menu.isCraftingVisible()) {
            drawCraftingBackground(guiGraphics);
        }
    }

    public com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager getPageManager() {
        return menu;
    }

    public AbstractContainerScreen<?> getScreen() {
        return this;
    }

    public ScreenFramework getFrameWork() {
        return frameWork;
    }

    public int getGuiLeft() {
        return leftPos;
    }

    public int getGuiTop() {
        return topPos;
    }

    public int getXSize() {
        return imageWidth;
    }

    public int getYSize() {
        return imageHeight;
    }
}

