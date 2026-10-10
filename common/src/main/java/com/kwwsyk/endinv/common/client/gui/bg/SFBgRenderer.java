package com.kwwsyk.endinv.common.client.gui.bg;

import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface SFBgRenderer {

    ScreenFramework getScreenFrameWork();

    void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY);

    ScreenRectangleWidgetParam pageSwitchBarParam();

    /**How many rows the background is drawn for. Built in, because a renderer takes its size when
     * it is made; reported so that a page drawn shorter than its own background can be seen.
     */
    default int rows() {
        return -1;
    }

    default Optional<PageBgRender> getDefaultPageBgRenderer(){
        return Optional.empty();
    }

    @FunctionalInterface
    interface PageBgRender {
        void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY);
    }
}
