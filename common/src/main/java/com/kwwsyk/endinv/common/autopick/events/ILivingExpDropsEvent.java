package com.kwwsyk.endinv.common.autopick.events;

import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public interface ILivingExpDropsEvent extends ICancelable{

    int getDroppedExperience();

    void setDroppedExperience(int droppedExperience);

    @Nullable
    Player getAttackingPlayer();
}
