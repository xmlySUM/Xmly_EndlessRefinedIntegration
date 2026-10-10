package com.kwwsyk.endinv.common.options;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.util.Accessibility;

public interface IServerConfig {

    IConfigValue<Integer> getMaxAllowedStackSize();

    IConfigValue<Boolean> allowInfinityMode();

    IConfigValue<Boolean> enableAttaching();

    IConfigValue<Boolean> enableAutoPick();

    default void onAttachingOrAutopickConfigChanged(){
        ModInfo.getPacketDistributor().sendToAllPlayer(new SyncedConfig(enableAttaching().get(),enableAutoPick().get()));
    }

    /**
     * Notify all players that the server's specified menu attachability has changed.
     * Broadcasts an effective attachability snapshot for client-side checks.
     */
    default void onSpecifiedMenuAttachabilityChanged(){
        var config = specifiedMenuAttachability().get();
        boolean defaultAttach = enableAttaching().get();
        var payload = new com.kwwsyk.endinv.common.network.payloads.toClient.MenuAttachabilityPayload(
                defaultAttach,
                config.isInventoryAttachable(),
                config.getConfigs()
        );
        ModInfo.getPacketDistributor().sendToAllPlayer(payload);
    }

    IConfigValue<ContentTransferMode> transferMode();

    IConfigValue<Accessibility> defaultAccessibility();

    IConfigValue<MissingEndInvPolicy> policyHandlingMissing();

    IConfigValue<Boolean> doConvertEmptyTag();

    IConfigValue<SpecifiedMenuAttachingConfig> specifiedMenuAttachability();

    /**Move passively gained items that land in inventory slots 9-35 into Endless Inventory. */
    IConfigValue<Boolean> enableSweep();

    /**Send items that fit nowhere to Endless Inventory instead of dropping them. */
    IConfigValue<Boolean> enableOverflow();

    /**Master switch for the multi-group hotbar. */
    IConfigValue<Boolean> enableHotbar();

    /**Let Ctrl+0 swap the hotbar between the player's inventory and their ender chest. */
    IConfigValue<Boolean> enableEnderChestHotbar();

    /**A Refined Storage portable grid with no storage disk shows the player's Endless Inventory
     * instead. Only used when Refined Storage is installed.
     */
    IConfigValue<Boolean> enableRefinedStorage();

    /**Also open a disk-less portable grid when it has run out of energy. */
    IConfigValue<Boolean> refinedStorageIgnoreEnergy();

    /**Let a Refined Storage network whose security manager gives somebody every permission draw on
     * that player's Endless Inventory as one of its storages.
     */
    IConfigValue<Boolean> enableRefinedStorageNetwork();
}
