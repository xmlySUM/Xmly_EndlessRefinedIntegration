package com.kwwsyk.endinv.common;

import net.minecraft.world.entity.player.Player;

public interface NbtAttachment<T> {

    T getWith(Player player);

    void setTo(Player player, T t);

    @Deprecated
    T computeIfAbsent(Player player);

}
