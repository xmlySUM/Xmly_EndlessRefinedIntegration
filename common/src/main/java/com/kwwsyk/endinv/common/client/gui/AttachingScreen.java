package com.kwwsyk.endinv.common.client.gui;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.option.CachedConfig;
import com.kwwsyk.endinv.common.network.payloads.toServer.OpenEndInvPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;


/**The exceeded screen that controls interaction with Endless Inventory in client side.
 * Attached by an {@link AbstractContainerScreen<T>}
 * @param <T>
 *
 * @author Kay Zhang
 * @since 2025-6-4
 * @version 1.0.5
 */
public class AttachingScreen<T extends AbstractContainerMenu>{

    public static AttachingScreen<?> INSTANCE;

    private static final Logger LOGGER = LogUtils.getLogger();

    public final AbstractContainerScreen<T> screen;
    public final T menu;

    private ScreenFramework frameWork;

    //invoked when an ACS is created or initialized
    public AttachingScreen(AbstractContainerScreen<T> screen){
        this.screen = screen;
        this.menu = screen.getMenu();
        INSTANCE = this;
    }

    //invoked when an ACS is initialized
    public void init(IScreenEvent event){
        this.frameWork = ScreenFramework.getInstance()==null ? new ScreenFramework(this) : ScreenFramework.getInstance();
        frameWork.addWidgetToScreen(event::addListener);
        // Ensure server-side manager attaches for the current menu so actions like quick-move are authoritative
        CachedConfig.readAndSyncClientConfigToServer(false);
        ModInfo.getPacketDistributor().sendToServer(new OpenEndInvPayload(false, CachedConfig.currentLayout().rows()));
    }

    /**Lay the panel out again and hand its widgets to a screen that was initialised once more.<br>
     * A screen that is resized rebuilds its widget list and its size changed, so the panel has to be
     * re-anchored against the new one; without this the panel kept the geometry of the old size and
     * its widgets silently disappeared.
     */
    public void refresh(IScreenEvent event){
        if(frameWork==null){
            init(event);
            return;
        }
        frameWork.relayout();
        frameWork.addWidgetToScreen(event::addListener);
    }

    public void renderPre(IScreenEvent event) {
    }

    public void render(IScreenEvent event) {
        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        float partialTick = event.getPartialTick();

        frameWork.renderBg(guiGraphics,mouseX,mouseY,partialTick);
        frameWork.render(guiGraphics,mouseX,mouseY,partialTick);
    }


    public void mouseClicked(IScreenEvent event) {
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        int keyCode = event.getButton();
        boolean isActionOverride = frameWork.mouseClicked(mouseX,mouseY,keyCode);
        event.setCanceled(isActionOverride);
    }


    public void mouseReleased(IScreenEvent event) {
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        int keyCode = event.getButton();
        event.setCanceled(frameWork.mouseReleased(mouseX,mouseY,keyCode));
    }

    public void mouseDragged(IScreenEvent event) {
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        int button = event.getMouseButton();
        double dragX = event.getDragX();
        double dragY = event.getDragY();

        event.setCanceled(frameWork.mouseDragged(mouseX,mouseY,button,dragX,dragY));
    }

    public void mouseScrolled(IScreenEvent event) {
        double scrollY = event.getScrollDeltaY();
        frameWork.mouseScrolled(event.getMouseX(),event.getMouseY(),scrollY);
    }

    public void keyPressed(IScreenEvent event) {
        int keyCode = event.getKeyCode();
        int scanCode = event.getScanCode();
        int modifiers = event.getModifiers();
        event.setCanceled(frameWork.keyPressed(keyCode,scanCode,modifiers));
    }

    public void charTyped(IScreenEvent event) {
        char codePoint = event.getCodePoint();
        int modifiers = event.getModifiers();
        event.setCanceled(frameWork.charTyped(codePoint,modifiers));
    }

    public void closed(IScreenEvent event){
        frameWork.onClose();
        close("On attached screen closing");
    }

    public void close(@Nullable String reason){
        INSTANCE = null;
        LOGGER.info("Attached Screen {} closed with reason: {}", this, reason);
    }

    public AbstractContainerScreen<?> getScreen() {
        return screen;
    }

    public ScreenFramework getFrameWork(){
        return frameWork;
    }

    public List<Rect2i> getArea(){
        return List.of(new Rect2i(frameWork.leftPos, frameWork.topPos, frameWork.imageWidth, frameWork.imageHeight));
    }

}















