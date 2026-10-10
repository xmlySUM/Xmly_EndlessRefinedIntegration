package com.kwwsyk.endinv.forge.nbtAttcachment;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public class NBTCapability<T extends INBTSerializable<CompoundTag>> implements ICapabilitySerializable<CompoundTag> {

    T backend;
    Capability<?> capability;
    LazyOptional<? super T> optional = LazyOptional.of(()->backend);

    public NBTCapability(T attachment, Capability<? super T> capability){
        this.backend = attachment;
        this.capability = capability;
    }

    @Override
    public <U> LazyOptional<U> getCapability(Capability<U> capability, @Nullable Direction direction) {
        return capability==this.capability ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return backend.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag compoundTag) {
        backend.deserializeNBT(compoundTag);
    }
}
