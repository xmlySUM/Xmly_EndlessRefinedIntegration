package com.kwwsyk.endinv.forge.nbtAttcachment;

import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraftforge.common.util.INBTSerializable;
import org.slf4j.Logger;


public class ISyncedConfigImpl implements ISyncedConfig, INBTSerializable<CompoundTag> {

    private SyncedConfig syncedConfig = SyncedConfig.DEFAULT;
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public SyncedConfig getSyncedConfig() {
        return syncedConfig;
    }

    @Override
    public void setSyncedConfig(SyncedConfig syncedConfig) {
        this.syncedConfig = syncedConfig;
    }

    @Override
    public CompoundTag serializeNBT() {
        return (CompoundTag) SyncedConfig.CODEC
                .encodeStart(NbtOps.INSTANCE, this.syncedConfig) // this.config 是 SyncedConfig 实例
                .getOrThrow(false, error -> LOGGER.error("Failed to encode config to NBT: {}", error));
    }

    @Override
    public void deserializeNBT(CompoundTag compoundTag) {
        SyncedConfig.CODEC
                .parse(NbtOps.INSTANCE, compoundTag)
                .resultOrPartial(error -> LOGGER.error("Failed to decode config from NBT: {}", error))
                .ifPresent(cfg -> this.syncedConfig = cfg);
    }
}
