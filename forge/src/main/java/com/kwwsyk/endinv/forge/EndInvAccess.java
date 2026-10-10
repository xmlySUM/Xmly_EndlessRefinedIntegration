package com.kwwsyk.endinv.forge;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**Reaches the Endless Inventory of a player, from server-side code that is not the inventory's
 * own. Returns null whenever there is nothing to use: a client-side player, or a level whose
 * inventories have not been loaded yet.
 */
public final class EndInvAccess {

    private EndInvAccess() {
    }

    @Nullable
    public static EndlessInventory of(@Nullable Player player) {
        if (player == null || player.level().isClientSide || ServerLevelEndInv.levelEndInvData == null) {
            return null;
        }
        return ServerLevelEndInv.getEndInvForPlayer(player).orElse(null);
    }
}
