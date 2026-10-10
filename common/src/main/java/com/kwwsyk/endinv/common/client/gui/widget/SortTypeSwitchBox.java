package com.kwwsyk.endinv.common.client.gui.widget;

import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.client.gui.bg.ScreenRectangleWidgetParam;
import com.kwwsyk.endinv.common.client.gui.page.manager.PageManager;
import com.kwwsyk.endinv.common.client.option.CachedConfig;
import com.kwwsyk.endinv.common.util.SortType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class SortTypeSwitchBox extends AbstractWidget {

    public ScreenFramework screen;
    private final PageManager pageManager;
    private final int singleBoxHeight;
    private boolean isOpen;


    public SortTypeSwitchBox(ScreenFramework screen, PageManager pageManager, int x, int y, int width, int height){
        super(x,y,width,height, Component.empty());
        this.screen = screen;
        this.pageManager = pageManager;
        this.singleBoxHeight = height;
    }

    public SortTypeSwitchBox(ScreenFramework screen, PageManager pageManager, ScreenRectangleWidgetParam sortTypeSwitchBoxParam){
        this(screen,
                pageManager,
                sortTypeSwitchBoxParam.XPos(),
                sortTypeSwitchBoxParam.YPos(),
                sortTypeSwitchBoxParam.XSize(),
                sortTypeSwitchBoxParam.YSize()
        );
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
        this.height = open ? singleBoxHeight*(1+ SortType.values().length) : singleBoxHeight;
    }

    public void onClick(double mouseX,double mouseY){
        if(!isOpen){
            setOpen(true);
        }else {
            int y1 = getY()+singleBoxHeight;
            for(SortType type : SortType.values()){
                if(isHoveringOnSingleBox((int)mouseY,y1)){
                    screen.switchSortTypeTo(type);
                    return;
                }
                y1+= singleBoxHeight;
            }
            setOpen(false);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button){
        if(active && visible && !this.clicked(mouseX,mouseY) && isOpen){
            setOpen(false);
            return true;
        }else return super.mouseClicked(mouseX,mouseY,button);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F,0.0F,500.0F);
        guiGraphics.fill(getX(),getY(),getX()+width,getY()+singleBoxHeight,0xff888888);
        guiGraphics.fill(getX()+1, getY() +1,getX()+width-1, getY() +singleBoxHeight-1,0xff000000);
        if(isHoveringOnSingleBox(mouseY, getY()))
            guiGraphics.fillGradient(RenderType.guiOverlay(),getX(), getY(),getX()+width, getY() +singleBoxHeight,0x80ffffff,0x80ffffff,0);
        SortType sortType = CachedConfig.sortType();
        String s = sortType.toString();
        guiGraphics.drawString(Minecraft.getInstance().font, s,getX()+2, getY() +2,0xffffffff);
        if(isOpen){
            int y1 = getY() +singleBoxHeight;
            for(SortType type : SortType.values()){
                guiGraphics.fill(RenderType.gui(),getX(),y1,getX()+width,y1+singleBoxHeight,0,0xff888888);
                guiGraphics.fill(RenderType.gui(),getX()+1,y1+1,getX()+width-1,y1+singleBoxHeight-1,0,0xff000000);
                if(isHoveringOnSingleBox(mouseY,y1)) {
                    guiGraphics.fillGradient(RenderType.guiOverlay(), getX(), y1, getX() + width, y1 + singleBoxHeight, 0x80ffffff, 0x80ffffff, 0);
                    guiGraphics.renderTooltip(Minecraft.getInstance().font,Component.translatable(type.translationKey),mouseX,mouseY);
                }
                s = type.toString();
                guiGraphics.drawString(Minecraft.getInstance().font, s,getX()+2,y1+2,0xffffffff);
                y1+=singleBoxHeight;
            }
        }
        guiGraphics.pose().popPose();
    }
    private boolean isHoveringOnSingleBox(int mouseY,int minY){
        return mouseY>=minY && mouseY<=minY+singleBoxHeight && isHovered;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
