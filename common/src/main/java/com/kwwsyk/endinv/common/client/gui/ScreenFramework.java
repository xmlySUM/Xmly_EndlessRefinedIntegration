package com.kwwsyk.endinv.common.client.gui;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.SourceInventory;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.ClientModInfo;
import com.kwwsyk.endinv.common.client.KeyMappings;
import com.kwwsyk.endinv.common.client.gui.bg.FromResource;
import com.kwwsyk.endinv.common.client.gui.bg.SFBgRenderer;
import com.kwwsyk.endinv.common.client.gui.bg.ScreenRectangleWidgetParam;
import com.kwwsyk.endinv.common.client.gui.bg.Transparent;
import com.kwwsyk.endinv.common.client.gui.page.DisplayPage;
import com.kwwsyk.endinv.common.client.gui.page.ItemPage;
import com.kwwsyk.endinv.common.client.gui.page.manager.PageManager;
import com.kwwsyk.endinv.common.client.gui.widget.SortTypeSwitchBox;
import com.kwwsyk.endinv.common.client.option.CachedConfig;
import com.kwwsyk.endinv.common.client.option.IClientConfig;
import com.kwwsyk.endinv.common.client.option.TextureMode;
import com.kwwsyk.endinv.common.network.payloads.PageData;
import com.kwwsyk.endinv.common.network.payloads.toServer.CreativeItemModPayload;
import com.kwwsyk.endinv.common.network.payloads.toServer.QuickMoveToPagePayload;
import com.kwwsyk.endinv.common.network.payloads.toServer.StarItemPayload;
import com.kwwsyk.endinv.common.util.SortType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.kwwsyk.endinv.common.client.ClientModInfo.containerScreenHelper;
import static com.kwwsyk.endinv.common.client.ClientModInfo.inputHandler;

public class ScreenFramework implements PageManager{

    private static ScreenFramework INSTANCE;

    /**Where the attached panel puts itself before the player's drag is applied. */
    private static final int ATTACHED_BASE_LEFT = 20;

    private final Minecraft mc;
    public final AbstractContainerScreen<?> screen;
    public final AbstractContainerMenu menu;

    private ScreenRectangleWidgetParam searchBoxParam;
    private ScreenRectangleWidgetParam sortBoxParam;
    private ScreenRectangleWidgetParam configButtonParam;
    private ScreenRectangleWidgetParam pageBarScrollUpButtonParam, pageBarScrollDownButtonParam;
    public SFBgRenderer SFBgRenderer;
    public final int pageBarCount;
    public int firstPageIndex = 0;

    //Always pageBarCount + firstPageIndex <= meta.getPages.size()
    public int leftPos, topPos;
    public int imageWidth, imageHeight;
    private int pageX;
    private int pageY;

    private int pageXSize;
    private int pageYSize;
    private int pageOffsetX;
    private int pageOffsetY;
    private int roughMouseX;
    private int roughMouseY;
    public EditBox searchBox;
    public SortTypeSwitchBox sortTypeSwitchBox;
    private Button reverseSortButton;
    private Button configButton;
    private Button pageBarUpButton;
    private Button pageBarDownButton;

    //Dragging of the panel attached to a container screen. The dedicated EndInv menu screen keeps
    //the vanilla centring and is not movable, so all of this is inert unless attachedMode is set.
    private final boolean attachedMode;
    private int baseLeftPos;
    private int baseTopPos;
    private int panelOffsetX;
    private int panelOffsetY;
    private boolean dragging;
    private int dragGrabX;
    private int dragGrabY;

    private final List<AbstractWidget> widgets = new ArrayList<>();
    //page meta data fields
    private int rows;
    private final int columns;
    private DisplayPage displayingPage;
    public final List<DisplayPage> pages;

    public ScreenFramework(EndlessInventoryScreen screen) {//when opening EIS
        //------MOST BASE DATA-------
        this.screen = screen;
        this.mc = Minecraft.getInstance();
        this.menu = screen.getMenu();
        this.attachedMode = false;

        //---STRUCTURE AND RENDER DATA---
        this.leftPos = screen.getGuiLeft();
        this.topPos = screen.getGuiTop();
        this.imageWidth = screen.getXSize();
        this.imageHeight = screen.getYSize();
        //row and columns affects the structure
        PageData layout = CachedConfig.resolveLayout(screen, true);
        this.columns = Math.max(1, layout.columns());
        // The row count comes from the menu, not from the configuration: the menu is what takes
        // rows away for the crafter, so a page built to the configured count is drawn over the
        // crafter whenever the screen is built while it is showing. Opening the screen with the
        // crafter already on - which is what a JEI recipe transfer does - was exactly that.
        this.rows = Math.max(1, screen.getMenu().getVisibleRows());
        //renderer may need structure and widget data --here YES: needs row/col/left/top...
        this.SFBgRenderer = new FromResource.MenuMode(this, new ScreenRectangleWidgetParam(leftPos - 32, topPos + 1, 32, 28));

        //------WIDGET DATA-------
        this.pages = buildPages();//is page a widget? but init page bar count indeed needs it.
        //page switch bar's pos < leftPos in EIS
        this.pageBarCount = Math.min(ClientModInfo.getClientConfig().maxPageBarCount().get(), getPages().size());
        this.pageBarScrollUpButtonParam = new ScreenRectangleWidgetParam(leftPos - 32, topPos - 16, 30, 14);
        this.pageBarScrollDownButtonParam = new ScreenRectangleWidgetParam(leftPos - 32, topPos + 2 + 28 * pageBarCount, 30, 14);
        //page switch bar --end--
        this.configButtonParam = new ScreenRectangleWidgetParam(this.leftPos + this.imageWidth, Math.min(this.topPos + this.imageHeight, screen.height - 20), 18, 18);
        this.searchBoxParam = new ScreenRectangleWidgetParam(this.leftPos + 89, this.topPos + 5, 80, 12);
        this.sortBoxParam = new ScreenRectangleWidgetParam(this.leftPos + 8, topPos + 5, 60, 12);

        //------PAGES-----------
        //------prepare page data---------
        this.pageX = leftPos + 8;
        this.pageY = topPos + 17;
        this.pageXSize = columns * 18;
        this.pageYSize = rows * 18;
        //--base info should be all initialized--

        //---construct and switch displaying pages---
        switchPageWithId(layout.pageRegKey());
        //add widgets when base info are all prepared including displayingPage
        addWidgets();

        INSTANCE = this;
    }

    public ScreenFramework(AttachingScreen<?> attachingScreen) {
        //------MOST BASE DATA-------
        this.screen = attachingScreen.screen;
        this.mc = Minecraft.getInstance();
        this.menu = attachingScreen.menu;
        this.attachedMode = true;

        //---STRUCTURE AND RENDER DATA---
        PageData layout = CachedConfig.resolveLayout(screen, false);
        this.rows = layout.rows();//row and columns affects the structure
        this.columns = layout.columns();
        this.imageWidth = 13 + 18 * columns;
        this.imageHeight = screen.height;
        //Where the panel puts itself on its own; relayout() adds the player's drag on top of this.
        this.baseLeftPos = ATTACHED_BASE_LEFT;
        this.baseTopPos = Math.max((screen.height - rows * 18 - 17 - 10) / 2, 20);

        IClientConfig clientConfig = ClientModInfo.getClientConfig();

        //---WIDGET DATA---
        this.pages = buildPages();//is page a widget? but init page bar count indeed needs it.
        //page switch bar
        this.pageBarCount = Math.min(ClientModInfo.getClientConfig().maxPageBarCount().get(), getPages().size());

        //---LAYOUT---
        this.panelOffsetX = 0;
        this.panelOffsetY = 0;
        relayout();
        //Apply the saved position through the clamping path, so a stale or hand-edited value cannot
        //leave the panel somewhere the player cannot reach it.
        setPanelOffset(clientConfig.attachedPanelOffsetX().get(), clientConfig.attachedPanelOffsetY().get());

        //---construct and switch displaying pages---
        switchPageWithId(layout.pageRegKey());

        //add widgets when base info are all prepared including displayingPage
        addWidgets();

        INSTANCE = this;
    }

    /**Every position of the attached panel, derived from where the panel puts itself plus the
     * player's drag.<br>
     * The panel normally computes its layout once, when it opens, and offers no way to recompute it,
     * which is why it could not be moved as a whole from outside. Recomputing is also what a drag
     * and a window resize need.
     */
    void relayout(){
        if(!attachedMode) return;

        this.leftPos = baseLeftPos + panelOffsetX;
        this.topPos = baseTopPos + panelOffsetY;

        //The background renderer captures the geometry it is built with, so it has to be rebuilt.
        ScreenRectangleWidgetParam tabParam = new ScreenRectangleWidgetParam(leftPos - 32, topPos + 20, 32, 28);
        this.SFBgRenderer = ClientModInfo.getClientConfig().textureMode().get() != TextureMode.TRANSPARENT ?
                new FromResource.LeftLayout(this, tabParam) :
                new Transparent(this, tabParam);

        //The page bar and the config button live in the gutter immediately left of the panel, so
        //they follow it rather than staying pinned to the edge of the screen.
        int gutterX = leftPos - 20;
        this.pageBarScrollUpButtonParam = new ScreenRectangleWidgetParam(gutterX, topPos, 20, 14);
        this.pageBarScrollDownButtonParam = new ScreenRectangleWidgetParam(gutterX, topPos + 22 + 28 * pageBarCount, 20, 14);

        int searchBoxY = this.topPos + 17 + 18 * rows + 12;
        this.searchBoxParam = new ScreenRectangleWidgetParam(this.leftPos + 1, searchBoxY, Math.min(200, imageWidth), Math.min(20, screen.height - searchBoxY));
        this.configButtonParam = new ScreenRectangleWidgetParam(gutterX, Math.min(searchBoxY, screen.height - 20), 20, 20);
        this.sortBoxParam = new ScreenRectangleWidgetParam(this.leftPos + 6, topPos + 5, 77, 12);

        this.pageX = leftPos + 8;
        this.pageY = topPos + 17;
        this.pageXSize = columns * 18;
        this.pageYSize = rows * 18;

        repositionWidgets();
    }

    /**Move the widgets that were handed to the screen to where the current layout puts them.<br>
     * They are installed once, when the screen is initialised, so a relayout has to move them
     * rather than build them again.
     */
    private void repositionWidgets(){
        if(configButton!=null) configButton.setPosition(configButtonParam.XPos(),configButtonParam.YPos());
        if(reverseSortButton!=null) reverseSortButton.setPosition(sortBoxParam.XPos()+sortBoxParam.XSize()+2,sortBoxParam.YPos());
        if(searchBox!=null){
            searchBox.setX(searchBoxParam.XPos());
            searchBox.setY(searchBoxParam.YPos());
        }
        if(sortTypeSwitchBox!=null){
            sortTypeSwitchBox.setX(sortBoxParam.XPos());
            sortTypeSwitchBox.setY(sortBoxParam.YPos());
        }
        if(pageBarUpButton!=null) pageBarUpButton.setPosition(pageBarScrollUpButtonParam.XPos(),pageBarScrollUpButtonParam.YPos());
        if(pageBarDownButton!=null) pageBarDownButton.setPosition(pageBarScrollDownButtonParam.XPos(),pageBarScrollDownButtonParam.YPos());
    }


    private void addWidgets() {
        this.configButton = Button.builder(Component.literal("⚙"),
                        btn -> {
                            mc.setScreen(ClientModInfo.createConfigScreen(screen));
                        })
                .pos(this.configButtonParam.XPos(), this.configButtonParam.YPos())
                .size(this.configButtonParam.XSize(), this.configButtonParam.YSize())
                .build();
        this.reverseSortButton = Button.builder(Component.literal("⇅"),
                        btn -> {
                            CachedConfig.setReverseSort(!CachedConfig.reverseSort());
                            if(getDisplayingPage() instanceof ItemPage page){
                                page.refreshItems();
                            }
                        }
                )
                .pos(sortBoxParam.XPos() + sortBoxParam.XSize() + 2, sortBoxParam.YPos())
                .size(sortBoxParam.YSize(), sortBoxParam.YSize())
                .build();
        this.searchBox = new EditBox(mc.font,
                this.searchBoxParam.XPos(), this.searchBoxParam.YPos(), this.searchBoxParam.XSize(), this.searchBoxParam.YSize(),
                Component.translatable("itemGroup.search"));
        this.sortTypeSwitchBox = new SortTypeSwitchBox(this, this, sortBoxParam);

        this.searchBox.setValue(searching());

        if (pageBarCount < getPages().size()) {
            this.pageBarUpButton = Button.builder(Component.literal("^"), btn -> {
                        if (firstPageIndex > 0) firstPageIndex--;
                    })
                    .pos(pageBarScrollUpButtonParam.XPos(), pageBarScrollUpButtonParam.YPos())
                    .size(pageBarScrollUpButtonParam.XSize(), pageBarScrollUpButtonParam.YSize())
                    .build();
            this.pageBarDownButton = Button.builder(Component.literal("v"), btn -> {
                        if (firstPageIndex + pageBarCount < getPages().size())
                            firstPageIndex++;
                    })
                    .pos(pageBarScrollDownButtonParam.XPos(), pageBarScrollDownButtonParam.YPos())
                    .size(pageBarScrollDownButtonParam.XSize(), pageBarScrollDownButtonParam.YSize())
                    .build();
            widgets.add(pageBarUpButton);
            widgets.add(pageBarDownButton);
        }

        widgets.add(configButton);
        widgets.add(reverseSortButton);
        widgets.add(searchBox);
        widgets.add(sortTypeSwitchBox);
    }

    public void addWidgetToScreen(Consumer<AbstractWidget> installer) {
        widgets.forEach(installer);
    }

    public void renderPre(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    public void renderBg(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        SFBgRenderer.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        getDisplayingPage().initRenderer(this, getPageX(), getPageY());
        getDisplayingPage().renderBg(SFBgRenderer, guiGraphics, partialTick, mouseX, mouseY);
    }

    private boolean isHoveringOnPage;

    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        roughMouseX = mouseX;
        roughMouseY = mouseY;

        isHoveringOnPage = hasClickedOnPage(mouseX, mouseY);

        getDisplayingPage().initRenderer(this, getPageX(), getPageY());
        getDisplayingPage().render(guiGraphics, mouseX, mouseY, partialTick);

        if (searchBox.isHovered() && !searchBox.isFocused()) guiGraphics.renderTooltip(mc.font, List.of(
                Component.translatable("search.endinv.prefix.sharp"),
                Component.translatable("search.endinv.prefix.at"),
                Component.translatable("search.endinv.prefix.xor"),
                Component.translatable("search.endinv.prefix.star")
        ), Optional.empty(), mouseX, mouseY);
        if (reverseSortButton.isHovered())
            guiGraphics.renderTooltip(mc.font, Component.translatable("button.endinv.reverse"), mouseX, mouseY);

    }

    protected boolean hasClickedOnPage(double mouseX, double mouseY) {
        return mouseX >= (double) getPageX() && mouseX <= (double) getPageX() + pageXSize
                && mouseY >= (double) getPageY() && mouseY <= (double) getPageY() + pageYSize
                && !sortTypeSwitchBox.isHovered();
    }

    protected int hasClickedOnPageSwitchBar(double mouseX, double mouseY) {
        double XOffset = mouseX - SFBgRenderer.pageSwitchBarParam().XPos();
        double YOffset = mouseY - SFBgRenderer.pageSwitchBarParam().YPos();
        if (XOffset < 0 || XOffset > SFBgRenderer.pageSwitchBarParam().XSize() || YOffset < 0) return -1;
        int index = (int) YOffset / SFBgRenderer.pageSwitchBarParam().YSize();
        if (index < 0 || index >= pageBarCount) return -1;
        return index;
    }

    protected void pageSwitched(int index) {
        switchPageWithIndex(index + firstPageIndex);
        //getDisplayingPage().syncContentToServer();
        this.searchBox.setVisible(getDisplayingPage().hasSearchbox());
        this.sortTypeSwitchBox.visible = getDisplayingPage().hasSortTypeSwitchBar();
        CachedConfig.setDisplayingPageKey(getDisplayingPageId());
        CachedConfig.updateLayoutWith(getPageData());
    }

    public void switchSortTypeTo(SortType type) {
        CachedConfig.setSortType(type);
        if(getDisplayingPage() instanceof ItemPage page){
            page.refreshItems();
        }
    }

    private boolean isHovering(Slot slot, double mouseX, double mouseY) {
        return this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }

    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        int i = containerScreenHelper.getGuiLeft(screen);
        int j = containerScreenHelper.getGuiTop(screen);
        mouseX -= i;
        mouseY -= j;
        return mouseX >= (double) (x - 1)
                && mouseX < (double) (x + width + 1)
                && mouseY >= (double) (y - 1)
                && mouseY < (double) (y + height + 1);
    }

    public boolean hoveringOnPage() {
        return !sortTypeSwitchBox.isHovered();
    }

    @Nullable
    private Slot findSlot(double mouseX, double mouseY) {
        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            if (this.isHovering(slot, mouseX, mouseY) && slot.isActive()) {
                return slot;
            }
        }

        return null;
    }


    private ItemStack creativeQuickInsertedItem = ItemStack.EMPTY;
    private void slotQuickMoved(Slot clicked) {
        ItemStack itemStack = clicked.getItem().copy();
        if (menu instanceof CreativeModeInventoryScreen.ItemPickerMenu && clicked.index < 45 && menu.slots.size() >= 54) {
            if (ItemStack.isSameItemSameTags(itemStack, creativeQuickInsertedItem)) {
                return;
            } else creativeQuickInsertedItem = itemStack;
            itemStack.setCount(itemStack.getMaxStackSize());
            getDisplayingPage().tryInsertItem(itemStack);
            ModInfo.getPacketDistributor().sendToServer(new CreativeItemModPayload(itemStack, true));
        } else {
            boolean canAttach = (screen instanceof EndlessInventoryScreen) || com.kwwsyk.endinv.common.client.option.MenuAttachabilityCache.isAttachable(screen);
            if (!canAttach) return;
            ItemStack remain = getDisplayingPage().tryInsertItem(itemStack);
            clicked.setByPlayer(remain);
            clicked.onTake(getPlayer(), itemStack);
            int payloadId = menu instanceof CreativeModeInventoryScreen.ItemPickerMenu
                    ? getItemPickerMenuSlotOffset(clicked)
                    : menu.slots.indexOf(clicked);
            if (payloadId >= 0) {
                ModInfo.getPacketDistributor().sendToServer(new QuickMoveToPagePayload(payloadId));
            }
        }// should use slot.getContainerSlot() instead of getSlotIndex()
        if (getDisplayingPage() instanceof ItemPage itemPage) {
            itemPage.requestRemoteContents();//send such payloads will not let server send contents
        }//another aspect is to check whether contents are synced across server and client
    }

    /**<p>Get correspond slot index between client creative menu and server player's inventory menu</p>
     * When client player is in {@link CreativeModeInventoryScreen.ItemPickerMenu} player on server only holds {@link net.minecraft.world.inventory.InventoryMenu}<br>
     * <p>
     * In {@code ItemPickerMenu} there are two situations:<br>
     *     1.When player is picking items in tab, there are 9*5+9 slots, slot in hotbar starts with index 45 ends with 53.<br>
     *     2.When player is in "Survival Inventory", the {@code slot.index} is always 0, only {@link Slot#getContainerSlot()} is valid.<br>
     *     To be noticed, {@link Slot#getSlotIndex()} returns same value {@code Slot.slot} but it only exists in Forge's lib. This means use this in Fabric running will throw {@link NoSuchMethodError}</p>
     * @param clicked slot clicked in Inventory by creative player on client.
     * @return slot index that can locate correspond inventory slot used in {@link QuickMoveToPagePayload}
     */
    private int getItemPickerMenuSlotOffset(Slot clicked){
        int originalIndex = clicked.index;
        if(originalIndex==0 && clicked.getContainerSlot() >0) return clicked.getContainerSlot();
        if(originalIndex<45) return originalIndex;
        return originalIndex - 9;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int keyCode) {
        if (!searchBoxParam.hasClickedOn((int) mouseX, (int) mouseY)) {
            searchBox.setFocused(false);
        } else {
            searchBox.setFocused(true);//this is what JEI behaves
            if (keyCode == 1) {
                searchBox.setValue("");
                refreshSearchResults();
                return true;
            }
        }
        //handle menu item quick move
        boolean flg = inputHandler.isActiveAndMatches(KeyMappings.QUICK_MOVE, InputConstants.Type.MOUSE.getOrCreate(keyCode));
        if (flg) {
            Slot clicked = findSlot(mouseX, mouseY);
            if (clicked != null && clicked.hasItem()) {
                slotQuickMoved(clicked);
                return true;
            }
        }
        //handle clicked on the page switch bar
        int pageIndex = hasClickedOnPageSwitchBar(mouseX, mouseY);
        if (pageIndex >= 0) {
            pageSwitched(pageIndex);
            return true;
        }
        //
        if (hasClickedOnPage(mouseX, mouseY)) {
            sortTypeSwitchBox.setOpen(false);
            return getDisplayingPage().mouseClicked(mouseX - getPageX(), mouseY - getPageY(), keyCode);
        }
        //Last, so that a click on the grid, a widget or the page bar is never taken as a drag.
        if (attachedMode && keyCode == 0 && isOnDragHandle(mouseX, mouseY)) {
            this.dragging = true;
            this.dragGrabX = (int) mouseX - leftPos;
            this.dragGrabY = (int) mouseY - topPos;
            return true;
        }
        return false;
    }


    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if(dragging){
            setPanelOffset((int) mouseX - dragGrabX - baseLeftPos, (int) mouseY - dragGrabY - baseTopPos);
            return true;
        }
        ItemStack itemstack = this.menu.getCarried();
        //ignore QUICK_CRAFT and touchscreen
        if (!itemstack.isEmpty() || mc.options.touchscreen().get())
            return false;
        //CTRL-click(default) to quick move items as behavior as Mouse Tweaks
        if (inputHandler.isActiveAndMatches(KeyMappings.QUICK_MOVE, InputConstants.Type.MOUSE.getOrCreate(button))) {
            Slot clicked = findSlot(mouseX, mouseY);
            if (clicked != null && clicked.hasItem()) {
                slotQuickMoved(clicked);
                return true;
            }
        }

        if (hasClickedOnPage(mouseX, mouseY)) {
            return getDisplayingPage().mouseDragged(mouseX - getPageX(), mouseY - getPageY(), button, dragX, dragY);
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int keyCode) {
        if(dragging){
            this.dragging = false;
            persistPanelOffset();
            return true;
        }
        creativeQuickInsertedItem = ItemStack.EMPTY;

        DisplayPage displayingPage = getDisplayingPage();
        displayingPage.release();
        if (hasClickedOnPage(mouseX, mouseY)) {
            return displayingPage.mouseReleased(mouseX - getPageX(), mouseY - getPageY(), keyCode);
        }
        return false;
    }


    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (hasClickedOnPage(mouseX, mouseY)) {
            return getDisplayingPage().mouseScrolled(mouseX - getPageX(), mouseY - getPageY(), scrollY);
        }
        return false;
    }

    private boolean ignoreTextInput;

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.ignoreTextInput = false;

        if (inputHandler.isActiveAndMatches(KeyMappings.STAR_ITEM, InputConstants.getKey(keyCode, scanCode))) {
            Slot clicked = findSlot(roughMouseX, roughMouseY);
            if (clicked != null && clicked.hasItem()) {
                ItemStack itemStack = clicked.getItem();
                ModInfo.getPacketDistributor().sendToServer(new StarItemPayload(itemStack, true));
                getDisplayingPage().sendChangesToServer();
                return true;
            }
        }

        boolean flag = false;
        if (isHoveringOnPage) {
            flag = getDisplayingPage().keyPressed(keyCode, scanCode, modifiers, roughMouseX - getPageX(), roughMouseY - getPageY());
        }
        if (flag) {
            this.ignoreTextInput = true;
            return true;
        }

        if (getDisplayingPage().hasSearchbox() && this.searchBox.isFocused()) {
            String s = this.searchBox.getValue();
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                if (!Objects.equals(s, this.searchBox.getValue())) {
                    this.refreshSearchResults();
                }
                return true;
            } else {
                return this.searchBox.isFocused() && this.searchBox.isVisible() && keyCode != 256;
            }
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (this.ignoreTextInput || !getDisplayingPage().hasSearchbox()) {
            return false;
        } else {
            String s = this.searchBox.getValue();
            if (this.searchBox.charTyped(codePoint, modifiers)) {
                if (!Objects.equals(s, this.searchBox.getValue())) {
                    this.refreshSearchResults();
                }

                return true;
            } else {
                return false;
            }
        }
    }

    public void onClose() {
        INSTANCE = null;
    }

    public void refreshSearchResults() {
        String searching = searchBox.getValue();
        CachedConfig.setSearching(searching);
        if(getDisplayingPage() instanceof ItemPage page){
            page.refreshItems();
        }
    }

    public static @Nullable ScreenFramework getInstance() {
        return INSTANCE;
    }

    /**Where the search box ended up, so that anything that wants to sit beside the panel can ask
     * rather than guess.
     */
    public ScreenRectangleWidgetParam searchBoxParam() {
        return searchBoxParam;
    }

    public int getPageX() {
        // Combine the static anchor and the debug offset for consistent hit tests.
        return pageX + pageOffsetX;
    }

    public int getPageY() {
        // Combine the static anchor and the debug offset for consistent hit tests.
        return pageY + pageOffsetY;
    }

    public void move(int deltaX, int deltaY) {
        if(attachedMode){
            //Move the whole panel. The page grid follows by itself: it re-reads getPageX()/getPageY()
            //every frame, so shifting the page here as well would move it twice.
            setPanelOffset(panelOffsetX + deltaX, panelOffsetY + deltaY);
            return;
        }
        // Support debug nudging without rebuilding the widget tree.
        this.pageOffsetX += deltaX;
        this.pageOffsetY += deltaY;
        DisplayPage current = getDisplayingPage();
        if (current != null) {
            current.move(deltaX, deltaY);
        }
    }

    /**Put the attached panel at an offset from where it places itself, kept on screen. */
    void setPanelOffset(int offsetX, int offsetY){
        int minX = 12 - baseLeftPos;
        int maxX = screen.width - imageWidth - 12 - baseLeftPos;
        //The panel is as tall as the screen, so what has to stay in view is the page grid itself.
        int pageHeight = 17 + rows * 18 + 12;
        int minY = 12 - baseTopPos;
        int maxY = screen.height - pageHeight - baseTopPos;
        this.panelOffsetX = Math.max(minX, Math.min(offsetX, Math.max(minX, maxX)));
        this.panelOffsetY = Math.max(minY, Math.min(offsetY, Math.max(minY, maxY)));
        relayout();
    }

    private void persistPanelOffset(){
        IClientConfig config = ClientModInfo.getClientConfig();
        config.attachedPanelOffsetX().set(panelOffsetX);
        config.attachedPanelOffsetY().set(panelOffsetY);
        config.save();
    }

    /**True when the mouse is on the panel's own frame rather than on anything interactive, which
     * is where a drag may start. Container slots underneath stay clickable everywhere else.
     */
    private boolean isOnDragHandle(double mouseX, double mouseY){
        boolean insideFrame = mouseX >= leftPos - 7 && mouseX <= leftPos + pageXSize + 8
                && mouseY >= topPos && mouseY <= topPos + 17 + pageYSize + 12;
        if(!insideFrame) return false;
        if(hasClickedOnPage(mouseX,mouseY)) return false;
        if(isOnWidget(mouseX,mouseY)) return false;
        return true;
    }

    /**Whether any widget the screen was handed is under the mouse.<br>
     * The drag is offered the click before the screen is, so a drag that started on top of a widget
     * would take the click from it and the widget would never fire - which is what stopped the
     * sort box and the reverse sort button from working on the attached panel.
     */
    private boolean isOnWidget(double mouseX, double mouseY){
        for(AbstractWidget widget : widgets){
            if(widget.visible && widget.isMouseOver(mouseX, mouseY)) return true;
        }
        return false;
    }

    public void resizePageRows(int rows) {
        // Mirror menu row changes so the client page layout stays aligned with the server menu.
        this.rows = Math.max(1, rows);
        this.pageYSize = this.rows * 18;
        com.kwwsyk.endinv.common.util.UiTrace.log("page rows -> {} (columns={} backgroundRows={} attached={})",
                this.rows, columns, SFBgRenderer == null ? -1 : SFBgRenderer.rows(), attachedMode);
        //The background renderer takes the row count when it is built, so without this the page
        //would show fewer items but still have its background drawn at the old height.
        if (attachedMode) {
            relayout();
        } else {
            this.SFBgRenderer = new FromResource.MenuMode(this, new ScreenRectangleWidgetParam(leftPos - 32, topPos + 1, 32, 28));
        }
        DisplayPage current = getDisplayingPage();
        if (current != null) {
            current.resize(this.rows);
        }
    }

    @Override
    public AbstractContainerMenu getMenu() {
        return menu;
    }

    @Override
    public SourceInventory getSourceInventory() {
        return CachedSrcInv.INSTANCE;
    }

    @Override
    public Player getPlayer() {
        return mc.player;
    }

    @Override
    public void switchPageWithIndex(int index) {
        this.displayingPage = pages.get(index);
        displayingPage.initializeContents();
    }

    @Override
    public int rows() {
        return rows;
    }

    @Override
    public int columns() {
        return columns;
    }



    @Override
    public List<DisplayPage> getPages() {
        return pages;
    }

    @Override
    public DisplayPage getDisplayingPage() {
        return displayingPage;
    }
}
