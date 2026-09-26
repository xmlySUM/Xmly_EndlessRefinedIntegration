/*
 * Derived from QuadHotbar by ArchangelD (mod_id: quadhotbar), LGPL-3.0.
 * This file remains under LGPL-3.0. See THIRD-PARTY.md for the list of changes.
 */
package com.xmly.endlessrefined.hotbar.engine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HotbarEngineConfigScreen extends Screen {

    private final Screen parent;

    public HotbarEngineConfigScreen(Screen parent) {
        super(Component.translatable("endless_refined.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 220;
        int buttonHeight = 20;
        int x = (this.width - buttonWidth) / 2;
        int y = this.height / 2 - 34;

        addRenderableWidget(Button.builder(groupsMessage(), button -> {
            int next = HotbarEngineConfig.hotbarGroups >= 4 ? 1 : HotbarEngineConfig.hotbarGroups + 1;
            HotbarEngineConfig.setHotbarGroups(next);
            button.setMessage(groupsMessage());
        }).bounds(x, y, buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(wrapMessage(), button -> {
            HotbarEngineConfig.setWrapScroll(!HotbarEngineConfig.wrapScroll);
            button.setMessage(wrapMessage());
        }).bounds(x, y + 24, buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(x, y + 62, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 24, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static Component groupsMessage() {
        return Component.translatable("endless_refined.config.hotbar_groups", HotbarEngineConfig.hotbarGroups);
    }

    private static Component wrapMessage() {
        return Component.translatable(
                "endless_refined.config.wrap_scroll",
                Component.translatable(HotbarEngineConfig.wrapScroll
                        ? "endless_refined.config.on"
                        : "endless_refined.config.off"));
    }
}
