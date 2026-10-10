package com.kwwsyk.endinv.forge.nbtAttcachment;

import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;

public interface ISyncedConfig {

    SyncedConfig getSyncedConfig();
    void setSyncedConfig(SyncedConfig syncedConfig);
}
