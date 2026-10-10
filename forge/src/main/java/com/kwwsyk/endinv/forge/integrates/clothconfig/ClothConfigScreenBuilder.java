package com.kwwsyk.endinv.forge.integrates.clothconfig;

import com.kwwsyk.endinv.common.client.ClientModInfo;
import com.kwwsyk.endinv.common.client.option.IClientConfig;
import com.kwwsyk.endinv.common.client.option.TextureMode;
import com.kwwsyk.endinv.common.menu.page.PageTypeRegistry;
import com.kwwsyk.endinv.forge.client.config.ClientConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

final class ClothConfigScreenBuilder {

    private ClothConfigScreenBuilder() {
    }

    static Screen create(Screen parent) {
        IClientConfig config = ClientModInfo.getClientConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.endinv.settings"));
        builder.setSavingRunnable(config::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("endinv.setting.category.general"));
        addGeneralEntries(entryBuilder, general, config);

        addHotbarEntries(entryBuilder, builder, config);

        addPageEntries(entryBuilder, builder, config);

        return builder.build();
    }

    private static void addGeneralEntries(ConfigEntryBuilder entryBuilder, ConfigCategory category, IClientConfig config) {
        category.addEntry(entryBuilder.startBooleanToggle(Component.translatable("endinv.setting.attaching"), config.attaching().get())
                .setDefaultValue(ClientConfig.CONFIG.ATTACHING.getDefault())
                .setSaveConsumer(value -> config.attaching().set(value))
                .build());

        category.addEntry(entryBuilder.startIntField(Component.translatable("endinv.setting.rows"), config.rows().get())
                .setDefaultValue(((Number) ClientConfig.CONFIG.ROWS.getDefault()).intValue())
                .setMin(0)
                .setTooltip(Component.translatable("config.endinv.comment.row1"))
                .setSaveConsumer(value -> config.rows().set(value))
                .build());

        category.addEntry(entryBuilder.startIntField(Component.translatable("endinv.setting.columns"), config.columns().get())
                .setDefaultValue(((Number) ClientConfig.CONFIG.COLUMNS.getDefault()).intValue())
                .setMin(0)
                .setSaveConsumer(value -> config.columns().set(value))
                .build());

        category.addEntry(entryBuilder.startBooleanToggle(Component.translatable("endinv.setting.auto_suit"), config.autoSuitColumn().get())
                .setDefaultValue(ClientConfig.CONFIG.AUTO_SUIT_COLUMN.getDefault())
                .setSaveConsumer(value -> config.autoSuitColumn().set(value))
                .build());

        category.addEntry(entryBuilder.startEnumSelector(Component.translatable("endinv.setting.texture"), TextureMode.class, config.textureMode().get())
                .setDefaultValue(ClientConfig.CONFIG.TEXTURE.getDefault())
                .setEnumNameProvider(mode -> Component.translatable("endinv.setting.entry." + mode.name()))
                .setSaveConsumer(value -> config.textureMode().set(value))
                .build());

        category.addEntry(entryBuilder.startIntField(Component.translatable("endinv.setting.max_page_bar"), config.maxPageBarCount().get())
                .setDefaultValue(((Number) ClientConfig.CONFIG.MAX_PAGE_BARS.getDefault()).intValue())
                .setMin(1)
                .setSaveConsumer(value -> config.maxPageBarCount().set(value))
                .build());

        category.addEntry(entryBuilder.startBooleanToggle(Component.translatable("endinv.setting.screen_debug"), config.screenDebugging().get())
                .setDefaultValue(ClientConfig.CONFIG.ENABLE_DEBUG.getDefault())
                .setSaveConsumer(value -> config.screenDebugging().set(value))
                .build());
    }

    /**The settings this mod gained when the Endless Refined addon was merged in. They belong on the
     * same screen as everything else: a player should not have to know that half of the settings
     * came from somewhere else.
     */
    private static void addHotbarEntries(ConfigEntryBuilder entryBuilder, ConfigBuilder builder, IClientConfig config) {
        ConfigCategory hotbar = builder.getOrCreateCategory(Component.translatable("endinv.setting.category.hotbar"));

        hotbar.addEntry(entryBuilder.startIntSlider(Component.translatable("endinv.setting.hotbar_groups"), config.hotbarGroups().get(), 1, 4)
                .setDefaultValue(2)
                .setSaveConsumer(value -> config.hotbarGroups().set(value))
                .build());

        hotbar.addEntry(entryBuilder.startBooleanToggle(Component.translatable("endinv.setting.hotbar_wrap_scroll"), config.hotbarWrapScroll().get())
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.hotbarWrapScroll().set(value))
                .build());

        hotbar.addEntry(entryBuilder.startBooleanToggle(Component.translatable("endinv.setting.hotbar_panel_expanded"), config.hotbarPanelExpanded().get())
                .setDefaultValue(false)
                .setSaveConsumer(value -> config.hotbarPanelExpanded().set(value))
                .build());

        hotbar.addEntry(entryBuilder.startIntField(Component.translatable("endinv.setting.hotbar_panel_x"), config.hotbarPanelX().get())
                .setDefaultValue(0)
                .setSaveConsumer(value -> config.hotbarPanelX().set(value))
                .build());

        hotbar.addEntry(entryBuilder.startIntField(Component.translatable("endinv.setting.hotbar_panel_y"), config.hotbarPanelY().get())
                .setDefaultValue(0)
                .setSaveConsumer(value -> config.hotbarPanelY().set(value))
                .build());

        hotbar.addEntry(entryBuilder.startIntField(Component.translatable("endinv.setting.attached_panel_x"), config.attachedPanelOffsetX().get())
                .setDefaultValue(0)
                .setSaveConsumer(value -> config.attachedPanelOffsetX().set(value))
                .build());

        hotbar.addEntry(entryBuilder.startIntField(Component.translatable("endinv.setting.attached_panel_y"), config.attachedPanelOffsetY().get())
                .setDefaultValue(0)
                .setSaveConsumer(value -> config.attachedPanelOffsetY().set(value))
                .build());
    }

    private static void addPageEntries(ConfigEntryBuilder entryBuilder, ConfigBuilder builder, IClientConfig config) {
        List<String> pageIds = PageTypeRegistry.getIdList();
        if (pageIds.isEmpty()) {
            return;
        }

        ConfigCategory pages = builder.getOrCreateCategory(Component.translatable("endinv.setting.category.pages"));
        for (String id : pageIds) {
            boolean hidden = config.isPageHidden(id);
            boolean defaultHidden = Optional.ofNullable(ClientConfig.CONFIG.PAGE2HIDING.get(id))
                    .map(value -> value.getDefault())
                    .orElse(false);

            pages.addEntry(entryBuilder.startBooleanToggle(Component.translatable("endinv.setting.hide_page", id), hidden)
                    .setDefaultValue(defaultHidden)
                    .setSaveConsumer(value -> config.setPageHiding(id, value))
                    .build());
        }
    }
}
