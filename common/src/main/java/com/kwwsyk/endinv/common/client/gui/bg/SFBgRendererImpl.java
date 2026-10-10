package com.kwwsyk.endinv.common.client.gui.bg;

import com.kwwsyk.endinv.common.client.ClientModInfo;
import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public abstract class SFBgRendererImpl implements SFBgRenderer {

    protected final ScreenFramework frameWork;

    protected final int menuLeft;
    protected final int menuTop;
    protected final int pageLeft;
    protected final int pageTop;
    protected final AbstractContainerScreen<?> screen;
    public ScreenRectangleWidgetParam pageSwitchTabParam;
    protected final int imageWidth;

    protected final int rows;
    protected final int columns;

    public SFBgRendererImpl(ScreenFramework frameWork){
        this.frameWork = frameWork;
        this.screen = frameWork.screen;
        this.imageWidth = 256;
        this.rows = this.frameWork.rows();
        this.columns = this.frameWork.columns();
        this.menuLeft = ClientModInfo.containerScreenHelper.getGuiLeft(screen);
        this.menuTop = ClientModInfo.containerScreenHelper.getGuiTop(screen);
        this.pageLeft = frameWork.leftPos;
        this.pageTop = frameWork.topPos;
    }

    protected void renderPageBarContent(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY){
        int pageX = pageSwitchTabParam.XPos();
        int pageY = pageSwitchTabParam.YPos();
        int selectedPageIndex = frameWork.getDisplayingPageIndex();
        for (int i = frameWork.firstPageIndex; i < frameWork.firstPageIndex+ frameWork.pageBarCount; ++i) {
            frameWork.getPages().get(i).renderPageIcon(guiGraphics, pageX + 15, pageY + 5, partialTick);
            if (mouseX > pageX && mouseX < pageX + 32 && mouseY > pageY && mouseY < pageY + 28) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 550.0f);
                guiGraphics.renderTooltip(Minecraft.getInstance().font, frameWork.getPages().get(i).name, mouseX, mouseY);
                guiGraphics.pose().popPose();
            }
            pageY += 28;
        }
    }

    @Override
    public int rows() {
        return rows;
    }

    @Override
    public ScreenRectangleWidgetParam pageSwitchBarParam() {
        return pageSwitchTabParam;
    }

    @Override
    public ScreenFramework getScreenFrameWork(){
        return frameWork;
    }
}
