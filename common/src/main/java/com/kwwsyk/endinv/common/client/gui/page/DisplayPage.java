package com.kwwsyk.endinv.common.client.gui.page;


import com.kwwsyk.endinv.common.SourceInventory;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.KeyMappings;
import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.client.gui.bg.SFBgRenderer;
import com.kwwsyk.endinv.common.client.gui.page.manager.PageManager;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

import static com.kwwsyk.endinv.common.client.ClientModInfo.inputHandler;
import static net.minecraft.client.gui.screens.Screen.hasShiftDown;

/**<p>A widget of {@link ScreenFramework} , serving render and input events.</p>
 * <p>
 *     For the main page type, or in version 1.1.0-snapshots(2025 sept) the all pages shown in Menu/AttachedScreen, see {@link ItemPage}.
 *     The specified work of showing {@code EndlessInventory (EndInv)}'s content and handle item interactions are mainly handled by it</p>
 * </p>
 *
 * <em>Server-sync duties</em>: only interactions inner page are handled by page itself.<br>
 * <ul>e.g.
 *     <li>Inner page ops: {@link #mouseClicked},{@link #keyPressed}(when mouse is hovering on it): call packet sending itself.</li>
 *     <li>Outer page ops: {@link #tryInsertItem},{@link #tryExtractItem}: call packet sending by invoker.</li>
 * </ul>
 *
 * @since 1.0.0
 * @author K.Z
 * */
public abstract class DisplayPage{


    protected final PageType pageType;
    //the registry name of this page type.
    public final String id;

    @Nullable
    protected final Predicate<ItemStack> itemClassify;

    public ResourceLocation icon = null;
    //displayed when hovering on Page switch bar.
    public Component name;

    public PageManager meta;

    public final SourceInventory srcInv;

    protected final AbstractContainerMenu menu;

    protected final Minecraft mc;

    //leftPos and topPos are used as Renderer param
    protected ScreenFramework framework;

    protected int leftPos;
    protected int topPos;
    protected int renderOffsetX;
    protected int renderOffsetY;
    private boolean rendererInitialized;
    private int lastFrameworkPageX;
    private int lastFrameworkPageY;
    //if holding on the page view shall not change temporarily.
    protected boolean holdOn = false;

    //constructor and initialization methods
    /**
     * Pages are constructed when {@link PageManager#buildPages()} is invoked.
     *
     * @param pageType defines type that is synced, including the item-classify.
     * @param manager
     */
    public DisplayPage(PageType pageType, PageManager manager){
        this.mc = Minecraft.getInstance();
        this.meta = manager;
        this.menu = manager.getMenu();
        this.srcInv = CachedSrcInv.INSTANCE;
        this.pageType = pageType;
        this.id = pageType.registerName;
        this.itemClassify = pageType.itemClassify;
        this.name = Component.translatableWithFallback("page.endinv."+pageType.registerName, pageType.registerName);
    }

    //abstract methods
    /**Build or reload page contents
     */
    public abstract void initializeContents();

    /**Controls page's scroll behavior.
     * @param pos influent new row index: {@link #getRowIndexForScroll(float)},{@link #getScrollForRowIndex(int)}
     */
    public abstract void scrollTo(float pos);

    /**Controls whether page will change view when mouse scrolled up and down
     * @return true if scroll can be performed
     */
    public abstract boolean canScroll();

    /**The change usually means pageMetaData changes and called by framework in sort,search,... changes.
     */
    public abstract void sendChangesToServer();

    public abstract boolean hasSearchbox();

    public abstract boolean hasSortTypeSwitchBar();

    /**Render page icon with page's {@link #icon}
     * icon can be an item location or sprite location with 16*16 size.
     */
    public ResourceLocation getIcon(){
        return icon;
    }

    public int getPageLeft() {
        return leftPos;
    }

    public int getPageTop() {
        return topPos;
    }

    @Nullable
    public Predicate<ItemStack> getClassify(){
        return itemClassify;
    }

    public PageType getPageType() {
        return pageType;
    }

    public int getRowIndexForScroll(float scrollOffs) {
        return Math.max((int)((double)(scrollOffs * (float)calculateRowCount()) + 0.5), 0);
    }

    public float getScrollForRowIndex(int rowIndex) {
        return Mth.clamp((float)rowIndex / (float)calculateRowCount(), 0.0F, 1.0F);
    }

    public int calculateRowCount(){
        return Math.max(meta.getItemSize()/ meta.columns(), CachedSrcInv.INSTANCE.getItemSize()/ meta.columns());
    }

    protected float subtractInputFromScroll(float scrollOffs, double input) {
        return Mth.clamp(scrollOffs - (float)(input / (double)meta.rows()), 0.0F, 1.0F);
    }

    public void setChanged() {}

    /**
     * Used by outer page item operations that move items in, e.g. quick_move (shift/ctrl-click).<br>
     * Server-sync tasks like sending packet should be completed by caller.
     * @param stack the itemstack quick moved TO page
     * @return <em>REMAIN item in case the EndInv cannot contain the item.Mostly it's {@link ItemStack#EMPTY}</em>
     */
    public ItemStack tryInsertItem(ItemStack stack){
        return srcInv.addItem(stack);
    }

    /**
     * Used by outer page item operations that move items out, e.g. pickup-all (double-click in menu).<br>
     * @param item itemstack to take
     * @param count count to take
     * @return the taken itemstack
     */
    public ItemStack tryExtractItem(ItemStack item, int count){
        return srcInv.takeItem(item,count);
    }

    /**{@inheritDoc}
     * Set freeze the page view like disable sort/arrangement of items
     */
    public void setHoldOn(){
        if(!holdOn){
            holdOn = true;
        }
    }

    /**{@inheritDoc}
     * Set unfreeze page view and
     *  may call rearrangement like sort of items
     */
    public void release(){
        if(holdOn){
            holdOn = false;
        }
    }


    //page renderer
    public void renderBg(SFBgRenderer SFBgRenderer, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        SFBgRenderer.getDefaultPageBgRenderer().ifPresent(bgRenderer -> bgRenderer.renderBg(guiGraphics, partialTick, mouseX, mouseY));
    }

    public void initRenderer(ScreenFramework framework, int pageXPos, int pageYPos){
        this.framework = framework;
        if (!rendererInitialized) {
            this.leftPos = pageXPos;
            this.topPos = pageYPos;
            this.renderOffsetX = 0;
            this.renderOffsetY = 0;
            this.rendererInitialized = true;
        } else {
            this.leftPos = pageXPos + renderOffsetX;
            this.topPos = pageYPos + renderOffsetY;
        }

        this.lastFrameworkPageX = pageXPos;
        this.lastFrameworkPageY = pageYPos;
    }

    /**
     * Apply a relative offset so debug adjustments survive screen refreshes.
     */
    public void move(int deltaX, int deltaY) {
        this.renderOffsetX += deltaX;
        this.renderOffsetY += deltaY;
        this.leftPos = lastFrameworkPageX + renderOffsetX;
        this.topPos = lastFrameworkPageY + renderOffsetY;
    }

    /**
     * Hook for page implementations that need to respond to a change in visible rows.
     */
    public void resize(int rows) {
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick){
        renderPage(graphics);
        if(framework.hoveringOnPage()){
            renderHovering(graphics, mouseX, mouseY, partialTick);
        }
    }

    public abstract void renderPage(GuiGraphics graphics);

    public void renderHovering(GuiGraphics graphics, int mouseX, int mouseY, float partialTick){}

    /**
     * Render Page's icon, will first try render {@link #icon} as {@code Item} location, then it will try render a {@code 16x16} icon.
     * <p>
     *Invoked when page s not initialized (items) yet.
     */
    public void renderPageIcon(GuiGraphics graphics, int x, int y, float partialTick) {
        if(getIcon()==null) return;
        Optional<Item> optionalItem = BuiltInRegistries.ITEM.getOptional(getIcon());
        if (optionalItem.isPresent()) {
            ItemStack stack = new ItemStack(optionalItem.get());
            graphics.renderItem(stack,x,y);
            return;
        }
        try {
            graphics.blit(getIcon(),x,y,0,0,16,16,16,16);
        }catch (Exception ignored){}
    }

    //page click handler
    //click handler

    /**Check if it has double-clicked on one object: item slot or other widgets.
     * @param clickInterval time interval of two clicks
     * @return true if it should be seen as double-clicked
     */
    public abstract boolean doubleClickedOnOne(double XOffset, double YOffset, double lastX, double lastY, long clickInterval);

    public abstract void pageClicked(double XOffset, double YOffset, int keyCode, ClickType clickType);

    /**
     * Get an area of one independent interactable area, mainly one item slot.
     * Can be used to mark one clickable area with other mods, such as Jei's register {@code getClickableIngredientUnderMouse}
     * @param XOffset mouseX-pageX
     * @param YOffset mouseY-pageY
     * @return one interactable area
     */
    @Nullable
    public abstract Rect2i getOneInteractableArea(double XOffset, double YOffset);

    /**Get mouse hovered or clicked item by mouse offset.
     * @param XOffset mouseX-pageX
     * @param YOffset mouseY-pageY
     * @return hovered or clicked item
     */
    public abstract ItemStack getItemByMouseOffset(double XOffset, double YOffset);

    /**
     * Get mouse hovered or clicked item with absolute mouse position in screen.
     * Easily call it out of DisplayPage class
     * @param mouseX screen mouseX
     * @param mouseY screen mouseY
     * @return hovered or clicked item
     */
    public ItemStack getHoveredOrClickedItem(double mouseX, double mouseY){
        return getItemByMouseOffset(mouseX - leftPos, mouseY - topPos);
    }

    public abstract void handleStarItem(double XOffset, double YOffset);

    /**Used to handle mouse clicked/dragged on page and page has slots
     * @param XOffset the relative X coordinate to page left pos
     * @param YOffset the relative Y coordinate to page top pos
     * @return the slot id in page or -1 with no slot.
     */
    public int getSlotByMouseOffset(double XOffset, double YOffset){
        return -1;
    }

    private boolean doubleClick;
    private int lastClickedButton;
    private double lastCLickedX;
    private double lastClickedY;
    private long lastClickedTime;
    private boolean skipNextRelease;

    public boolean mouseClicked(double XOffset, double YOffset, int keyCode){
        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(keyCode);
        boolean isKeyPicking = inputHandler.isActiveAndMatches(mc.options.keyPickItem,mouseKey);//is mouse middle button and enabled for pickup or clone
        long clickTime = Util.getMillis();
        this.doubleClick = keyCode==lastClickedButton && doubleClickedOnOne(XOffset,YOffset,lastCLickedX,lastClickedY,clickTime-lastClickedTime);
        this.skipNextRelease = false;
        if(keyCode!=0&&keyCode!=1&&!isKeyPicking){
            checkHotBarClicked:
            if (this.menu.getCarried().isEmpty()) {
                if (this.mc.options.keySwapOffhand.matchesMouse(keyCode)) {
                    pageClicked(XOffset,YOffset,40, ClickType.SWAP);
                    break checkHotBarClicked;
                }

                for (int i = 0; i < 9; i++) {
                    if (this.mc.options.keyHotbarSlots[i].matchesMouse(keyCode)) {
                        pageClicked(XOffset,YOffset, i, ClickType.SWAP);
                    }
                }
            }
        }else {
            if(menu.getCarried().isEmpty()){
                if (inputHandler.isActiveAndMatches(mc.options.keyPickItem,mouseKey)) {
                    pageClicked(XOffset, YOffset, keyCode, ClickType.CLONE);
                } else {
                    ClickType clicktype = ClickType.PICKUP;
                    if (hasShiftDown()) {
                        setHoldOn();
                        //this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                        clicktype = ClickType.QUICK_MOVE;
                    }
                    pageClicked(XOffset, YOffset, keyCode, clicktype);
                }
                this.skipNextRelease = true;
            }else {//deference to vanilla
                pageClicked(XOffset, YOffset, keyCode, ClickType.PICKUP);
            }
        }
        this.lastClickedTime = clickTime;
        this.lastClickedButton = keyCode;
        this.lastCLickedX = XOffset;
        this.lastClickedY = YOffset;
        return true;
    }


    private int lastDraggedPageSlot = -1;

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY){
        if(hasShiftDown()){
            int slotId = getSlotByMouseOffset(mouseX,mouseY);
            if(slotId>=0 && lastDraggedPageSlot>=0 && slotId!=lastDraggedPageSlot){
                pageClicked(mouseX,mouseY,button,ClickType.QUICK_MOVE);
            }
            lastDraggedPageSlot = slotId;
            return true;
        }else return false;
    }

    public boolean mouseReleased(double XOffset, double YOffset, int keyCode){
        lastDraggedPageSlot = -1;
        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(keyCode);
        if (this.doubleClick) {
            this.pageClicked(XOffset,YOffset,keyCode,ClickType.PICKUP_ALL);
            this.doubleClick = false;
            this.lastClickedTime = 0L;
            return true;
        }else {
            //ignore quick craft
            if (this.skipNextRelease) {
                this.skipNextRelease = false;
                return true;
            }
            if(!menu.getCarried().isEmpty()){
                if (inputHandler.isActiveAndMatches(mc.options.keyPickItem,mouseKey)) {
                    this.pageClicked(XOffset,YOffset,keyCode,ClickType.CLONE);
                    return true;
                }
            }
        }
        return false;
    }

    protected float scrollOffs;

    public boolean mouseScrolled(double mouseX,double mouseY,double scrollY){
        if(!canScroll()) return false;
        this.scrollOffs = subtractInputFromScroll(this.scrollOffs,scrollY);
        scrollTo(scrollOffs);
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers, int mouseX, int mouseY){
        boolean isNumericKey = InputConstants.getKey(keyCode, scanCode).getNumericKeyValue().isPresent();

        if (isNumericKey && this.menu.getCarried().isEmpty()) {
            if (inputHandler.isActiveAndMatches(mc.options.keySwapOffhand,InputConstants.getKey(keyCode, scanCode))) {
                pageClicked(mouseX, mouseY, 40, ClickType.SWAP);
                return true;
            }

            for(int i = 0; i < 9; ++i) {
                if (inputHandler.isActiveAndMatches(mc.options.keyHotbarSlots[i],InputConstants.getKey(keyCode, scanCode))) {
                    pageClicked(mouseX, mouseY, i, ClickType.SWAP);
                    return true;
                }
            }
        }

        if(inputHandler.isActiveAndMatches(KeyMappings.STAR_ITEM,InputConstants.getKey(keyCode,scanCode))){
            handleStarItem(mouseX,mouseY);
            return true;
        }
        return false;
    }
}
