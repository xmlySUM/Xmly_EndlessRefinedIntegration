package com.kwwsyk.endinv.forge.client.config;

import com.kwwsyk.endinv.common.client.option.IClientConfig;
import com.kwwsyk.endinv.common.client.option.TextureMode;
import com.kwwsyk.endinv.common.menu.page.PageTypeRegistry;
import com.kwwsyk.endinv.common.options.IConfigValue;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kwwsyk.endinv.common.client.gui.bg.FromResource.*;

public class ClientConfig {

    public static final ClientConfig CONFIG;
    public static final ForgeConfigSpec CONFIG_SPEC;
    public final ForgeConfigSpec.IntValue ROWS;
    public final ForgeConfigSpec.IntValue COLUMNS;
    public final ForgeConfigSpec.BooleanValue AUTO_SUIT_COLUMN;
    public final ForgeConfigSpec.EnumValue<TextureMode> TEXTURE;
    public final Map<String,ForgeConfigSpec.BooleanValue> PAGE2HIDING = new LinkedHashMap<>();
    public final ForgeConfigSpec.BooleanValue ATTACHING;
    public final ForgeConfigSpec.BooleanValue ENABLE_DEBUG;
    public final ForgeConfigSpec.IntValue MAX_PAGE_BARS;
    public final ForgeConfigSpec.IntValue ATTACHED_PANEL_OFFSET_X;
    public final ForgeConfigSpec.IntValue ATTACHED_PANEL_OFFSET_Y;
    public final ForgeConfigSpec.IntValue HOTBAR_GROUPS;
    public final ForgeConfigSpec.BooleanValue HOTBAR_WRAP_SCROLL;
    public final ForgeConfigSpec.IntValue HOTBAR_PANEL_X;
    public final ForgeConfigSpec.IntValue HOTBAR_PANEL_Y;
    public final ForgeConfigSpec.BooleanValue HOTBAR_PANEL_EXPANDED;

    private ClientConfig(ForgeConfigSpec.Builder builder){
        ATTACHING = builder.comment("show endless inventory view when opening a menu.")
                .define("attachingMenuScreen",true);

        ROWS = builder.comment("Default rows of EndInv view, 0 for auto.")
                .translation("config.endinv.comment.row1")
                .defineInRange("rows",0,0,Integer.MAX_VALUE);
        COLUMNS = builder.comment("Default columns of EndInv view, 0 for auto.")
                .defineInRange("columns",9,0,Integer.MAX_VALUE);

        AUTO_SUIT_COLUMN = builder.comment("auto suit in columns if GUI Size is too big.")
                .define("auto_suit_column",true);

        TEXTURE = builder
                .comment("Texture mode of EndInv view, transparent or vanilla menu style")
                .comment("FROM_RESOURCE uses vanilla textures, grid is "+CONTAINER_TEXTURE_RESOURCE+", tab is "+TABS_RESOURCE+".")
                .comment("DEDICATED_LOCATION allows using custom texture in resource packs, to use refer such locations: ")
                .comment("grid: "+DEDICATED_CONTAINER_TEXTURE+", tab: "+DEDICATED_TABS+", item_entry: "+ITEM_ENTRY_DISPLAY_RESOURCE)
                .defineEnum("texture_mode",TextureMode.FROM_RESOURCE);

        ENABLE_DEBUG = builder.comment("Press F3 in screen can show some information of menu screen")
                .define("enable_debug",false);

        MAX_PAGE_BARS = builder
                .defineInRange("max_page_bars",10,1,255);

        ATTACHED_PANEL_OFFSET_X = builder
                .comment("How far the EndInv panel attached to a container screen is dragged from where it puts itself.")
                .comment("Written when the panel is dragged; change it to move the panel back.")
                .defineInRange("attached_panel_offset_x",0,-10000,10000);
        ATTACHED_PANEL_OFFSET_Y = builder
                .comment("See attached_panel_offset_x.")
                .defineInRange("attached_panel_offset_y",0,-10000,10000);

        HOTBAR_GROUPS = builder
                .comment("How many groups of nine slots the hotbar can select across. Valid range: 1-4.")
                .defineInRange("hotbar_groups",2,1,4);
        HOTBAR_WRAP_SCROLL = builder
                .comment("When true, scrolling past the last slot wraps to the first slot and back.")
                .define("hotbar_wrap_scroll",true);
        HOTBAR_PANEL_X = builder
                .comment("How far the hotbar panel is dragged from where it puts itself, which is just")
                .comment("right of the EndInv panel's search box. 0 keeps it there.")
                .defineInRange("hotbar_panel_x",0,-10000,10000);
        HOTBAR_PANEL_Y = builder
                .comment("See hotbar_panel_x.")
                .defineInRange("hotbar_panel_y",0,-10000,10000);
        HOTBAR_PANEL_EXPANDED = builder
                .comment("Whether the hotbar panel was left open.")
                .define("hotbar_panel_expanded",false);

        for (String id : PageTypeRegistry.getIdList()) {
            ForgeConfigSpec.BooleanValue pageEntry = builder
                    .comment("Hide page: " + id)
                    .define("hide_pages." + id, false);
            PAGE2HIDING.put(id,pageEntry);
        }
    }

    static {
        Pair<ClientConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    public final IClientConfig INSTANCE = new IClientConfig() {

        private static IConfigValue<Boolean> convert(ForgeConfigSpec.BooleanValue value){
            return IConfigValue.of(value,value::set);
        }

        private static IConfigValue<Integer> convert(ForgeConfigSpec.IntValue value){
            return IConfigValue.of(value,value::set);
        }

        @Override
        public IConfigValue<Boolean> attaching() {
            return convert(ATTACHING);
        }

        @Override
        public IConfigValue<Integer> rows() {
            return convert(ROWS);
        }

        @Override
        public IConfigValue<Integer> columns() {
            return convert(COLUMNS);
        }

        @Override
        public IConfigValue<Boolean> autoSuitColumn() {
            return convert(AUTO_SUIT_COLUMN);
        }

        @Override
        public IConfigValue<TextureMode> textureMode() {
            return IConfigValue.of(TEXTURE,TEXTURE::set);
        }

        @Override
        public IConfigValue<Boolean> screenDebugging() {
            return convert(ENABLE_DEBUG);
        }

        @Override
        public IConfigValue<Integer> maxPageBarCount(){
            return convert(MAX_PAGE_BARS);
        }

        @Override
        public IConfigValue<Integer> attachedPanelOffsetX(){
            return convert(ATTACHED_PANEL_OFFSET_X);
        }

        @Override
        public IConfigValue<Integer> attachedPanelOffsetY(){
            return convert(ATTACHED_PANEL_OFFSET_Y);
        }

        @Override
        public IConfigValue<Integer> hotbarGroups(){
            return convert(HOTBAR_GROUPS);
        }

        @Override
        public IConfigValue<Boolean> hotbarWrapScroll(){
            return convert(HOTBAR_WRAP_SCROLL);
        }

        @Override
        public IConfigValue<Integer> hotbarPanelX(){
            return convert(HOTBAR_PANEL_X);
        }

        @Override
        public IConfigValue<Integer> hotbarPanelY(){
            return convert(HOTBAR_PANEL_Y);
        }

        @Override
        public IConfigValue<Boolean> hotbarPanelExpanded(){
            return convert(HOTBAR_PANEL_EXPANDED);
        }

        @Override
        public Set<String> hidingPageIds() {
            return PAGE2HIDING.entrySet().stream()
                    .filter(entry->entry.getValue().get())
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
        }

        @Override
        public void setPageHiding(String id, boolean hiding) {
            Optional.ofNullable(PAGE2HIDING.get(id)).ifPresent(v->v.set(hiding));
            CONFIG_SPEC.save();
        }

        @Override
        public void save() {
            CONFIG_SPEC.save();
        }
    };
}
