package com.kwwsyk.endinv.forge;

import com.kwwsyk.endinv.common.options.*;
import com.kwwsyk.endinv.common.util.Accessibility;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class ServerConfig {

    public static final ServerConfig CONFIG;
    public static final ForgeConfigSpec CONFIG_SPEC;
    public final ForgeConfigSpec.IntValue MAX_STACK_SIZE;
    public final ForgeConfigSpec.BooleanValue ENABLE_INFINITE;
    public final ForgeConfigSpec.BooleanValue ENABLE_ATTACHING;
    public final ForgeConfigSpec.BooleanValue ENABLE_AUTO_PICK;
    public final ForgeConfigSpec.EnumValue<ContentTransferMode> TRANSFER_MODE;
    public final ForgeConfigSpec.EnumValue<Accessibility> DEFAULT_ACCESSIBILITY;
    public final ForgeConfigSpec.EnumValue<MissingEndInvPolicy> CREATION_MODE;
    public final ForgeConfigSpec.BooleanValue CONVERT_EMPTY_TAG;

    public final ForgeConfigSpec.ConfigValue<List<?>> SPECIFIED_MENU_ATTACHABLE;
    public final ForgeConfigSpec.BooleanValue ENABLE_SWEEP;
    public final ForgeConfigSpec.BooleanValue ENABLE_OVERFLOW;
    public final ForgeConfigSpec.BooleanValue ENABLE_HOTBAR;
    public final ForgeConfigSpec.BooleanValue ENABLE_ENDER_CHEST_HOTBAR;
    public final ForgeConfigSpec.BooleanValue ENABLE_RS_INTEGRATION;
    public final ForgeConfigSpec.BooleanValue RS_IGNORE_ENERGY;
    public final ForgeConfigSpec.BooleanValue ENABLE_RS_NETWORK;

    private ServerConfig(ForgeConfigSpec.Builder builder){
        MAX_STACK_SIZE = builder
                .translation("config.endinv.comment.max_stack_size")
                .defineInRange("ItemCapacity.maxStackSize",Integer.MAX_VALUE,0,Integer.MAX_VALUE);
        ENABLE_INFINITE = builder
                .translation("config.endinv.comment.enable_infinite1")
                .define("ItemCapacity.enableInfinite",false);
        ENABLE_ATTACHING = builder
                .comment("Allow players have attached Inventory by menu.")
                .define("allowAttaching", true);
        ENABLE_AUTO_PICK = builder
                .comment("Will enable player to auto pick item and exp")
                .define("autoPickUtility",false);
        TRANSFER_MODE = builder
                .defineEnum("TransferMode",ContentTransferMode.ALL);
        DEFAULT_ACCESSIBILITY = builder
                .defineEnum("defaultAccessibility",Accessibility.PUBLIC);
        CREATION_MODE = builder
                .defineEnum("creationMode",MissingEndInvPolicy.CREATE_PER_PLAYER);
        CONVERT_EMPTY_TAG = builder
                .comment("Convert itemstack with empty tag {} to null tag, for the bugs that item cannot be taken/stacked.")
                .define("convert_empty_tag",true);

        SPECIFIED_MENU_ATTACHABLE = builder.comment(SpecifiedMenuAttachingConfig.Parser.configComments)
                .defineListAllowEmpty(
                        "Specified menu attach abilities",
                        List.of(),
                        (s)-> SpecifiedMenuAttachingConfig.Parser.tryParseString((String) s)!=null
                );

        ENABLE_OVERFLOW = builder
                .comment("Send items that fit nowhere to Endless Inventory instead of dropping them.")
                .define("inventory.overflow",true);
        ENABLE_SWEEP = builder
                .comment("Move passively gained items that land in inventory slots 9-35 into Endless Inventory.",
                        "Only items that arrived through Inventory#add count: pickups, /give and the like.",
                        "Items moved around inside a GUI are never touched, so shift-clicking is safe.",
                        "Slots the hotbar is showing are never touched either.")
                .define("inventory.sweep",true);

        ENABLE_HOTBAR = builder
                .comment("Turn the nine slot hotbar into up to four groups of nine, selectable across slots 9-35.")
                .define("hotbar.enabled",true);
        ENABLE_ENDER_CHEST_HOTBAR = builder
                .comment("Let Ctrl+0 swap the hotbar between the player's own inventory and their ender chest.")
                .define("hotbar.enderChest",true);

        ENABLE_RS_INTEGRATION = builder
                .comment("A Refined Storage portable grid with no storage disk shows the player's Endless Inventory instead.")
                .define("refined_storage.enabled",true);
        RS_IGNORE_ENERGY = builder
                .comment("Also open a disk-less portable grid when it has run out of energy.")
                .define("refined_storage.ignoreEnergy",true);
        ENABLE_RS_NETWORK = builder
                .comment("An External Storage bus pointed at a Security Manager offers the Endless Inventory of",
                        "the first player that manager gives every permission to, as that bus's storage.",
                        "It is merged with whatever else the network has, and works whether or not that",
                        "player is online.")
                .define("refined_storage.network",true);
    }

    static {
        Pair<ServerConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    public final IServerConfig INSTANCE = new IServerConfig() {

        private static IConfigValue<Integer> convert(ForgeConfigSpec.IntValue value){
            return IConfigValue.of(value,value::set, CONFIG_SPEC::save);
        }

        private static IConfigValue<Boolean> convert(ForgeConfigSpec.BooleanValue value){
            return IConfigValue.of(value,value::set, CONFIG_SPEC::save);
        }

        private static IConfigValue<Boolean> convert(ForgeConfigSpec.BooleanValue value, Runnable onSet){
            return IConfigValue.of(value,value::set, onSet);
        }

        @Override
        public IConfigValue<Integer> getMaxAllowedStackSize() {
            return convert(MAX_STACK_SIZE);
        }

        @Override
        public IConfigValue<Boolean> allowInfinityMode() {
            return convert(ENABLE_INFINITE);
        }

        @Override
        public IConfigValue<Boolean> enableAttaching() {
            return convert(ENABLE_ATTACHING, this::onAttachingOrAutopickConfigChanged);
        }

        @Override
        public IConfigValue<Boolean> enableAutoPick() {
            return convert(ENABLE_AUTO_PICK, this::onAttachingOrAutopickConfigChanged);
        }

        @Override
        public IConfigValue<ContentTransferMode> transferMode() {
            return IConfigValue.of(TRANSFER_MODE,TRANSFER_MODE::set);
        }

        @Override
        public IConfigValue<Accessibility> defaultAccessibility() {
            return IConfigValue.of(DEFAULT_ACCESSIBILITY,DEFAULT_ACCESSIBILITY::set);
        }

        @Override
        public IConfigValue<MissingEndInvPolicy> policyHandlingMissing() {
            return IConfigValue.of(CREATION_MODE,CREATION_MODE::set);
        }

        @Override
        public IConfigValue<Boolean> doConvertEmptyTag() {
            return convert(CONVERT_EMPTY_TAG);
        }

        @Override
        public IConfigValue<Boolean> enableSweep() {
            return convert(ENABLE_SWEEP);
        }

        @Override
        public IConfigValue<Boolean> enableOverflow() {
            return convert(ENABLE_OVERFLOW);
        }

        @Override
        public IConfigValue<Boolean> enableHotbar() {
            return convert(ENABLE_HOTBAR);
        }

        @Override
        public IConfigValue<Boolean> enableEnderChestHotbar() {
            return convert(ENABLE_ENDER_CHEST_HOTBAR);
        }

        @Override
        public IConfigValue<Boolean> enableRefinedStorage() {
            return convert(ENABLE_RS_INTEGRATION);
        }

        @Override
        public IConfigValue<Boolean> refinedStorageIgnoreEnergy() {
            return convert(RS_IGNORE_ENERGY);
        }

        @Override
        public IConfigValue<Boolean> enableRefinedStorageNetwork() {
            return convert(ENABLE_RS_NETWORK);
        }

        @Override
        public IConfigValue<SpecifiedMenuAttachingConfig> specifiedMenuAttachability() {
            return new IConfigValue<SpecifiedMenuAttachingConfig>() {
                @Override @SuppressWarnings("unchecked")
                public SpecifiedMenuAttachingConfig get() {
                    return SpecifiedMenuAttachingConfig.Parser.readList((List<String>) SPECIFIED_MENU_ATTACHABLE.get());
                }

                @Override
                public void set(SpecifiedMenuAttachingConfig config) {
                    SPECIFIED_MENU_ATTACHABLE.set(SpecifiedMenuAttachingConfig.Parser.fromMap(config.getConfigs()));
                    onSpecifiedMenuAttachabilityChanged();
                }
            };
        }
    };
}
