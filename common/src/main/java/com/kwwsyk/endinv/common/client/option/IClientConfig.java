package com.kwwsyk.endinv.common.client.option;

import com.kwwsyk.endinv.common.options.IConfigValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.Set;

public interface IClientConfig {

    IConfigValue<Boolean> attaching();

    IConfigValue<Integer> rows();

    IConfigValue<Integer> columns();

    IConfigValue<Boolean> autoSuitColumn();

    IConfigValue<TextureMode> textureMode();

    IConfigValue<Boolean> screenDebugging();

    IConfigValue<Integer> maxPageBarCount();

    /**Where the player dragged the EndInv panel that is attached to a container screen, in pixels
     * from the place it puts itself. Persisted so the panel comes back where it was left.
     */
    IConfigValue<Integer> attachedPanelOffsetX();

    IConfigValue<Integer> attachedPanelOffsetY();

    /**How many groups of nine the hotbar can select across, 1 to 4. */
    IConfigValue<Integer> hotbarGroups();

    /**Whether scrolling past the last slot wraps round to the first. */
    IConfigValue<Boolean> hotbarWrapScroll();

    /**Where the player put the hotbar panel, or -1 to follow the container GUI. */
    IConfigValue<Integer> hotbarPanelX();

    IConfigValue<Integer> hotbarPanelY();

    /**Whether the hotbar panel was left open. */
    IConfigValue<Boolean> hotbarPanelExpanded();

    Set<String> hidingPageIds();

    void setPageHiding(String id, boolean hiding);

    default void save(){}

    default boolean isPageHidden(String id){
        return hidingPageIds().contains(id);
    }

    default int calculateDefaultRowCount(boolean ofMenu){
        Minecraft mc = Minecraft.getInstance();
        int height = mc.getWindow().getGuiScaledHeight();
        return Math.max(Math.floorDiv(height-60,18)-(ofMenu?4:0),0);
    }

    default int calculateSuitInColumnCount(AbstractContainerScreen<?> screen){
        int leftPos = (screen.width - 176)/2;
        int width = leftPos - 20 - 6 -6;
        return Math.max(0,Math.floorDiv(width,18));
    }
}
