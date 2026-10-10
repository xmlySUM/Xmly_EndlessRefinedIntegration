package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Optional;

public record QuickMoveToPagePayload(int slotId) implements ModPacketPayload {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void encode(QuickMoveToPagePayload payload, FriendlyByteBuf o){
        o.writeInt(payload.slotId);
    }

    public static QuickMoveToPagePayload decode(FriendlyByteBuf o){
        return new QuickMoveToPagePayload(o.readInt());
    }

    @Override
    public String id() {
        return "quick_move_page";
    }

    public void handle(ModPacketContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        Optional<EndlessInventory> oendinv = ServerLevelEndInv.getEndInvForPlayer(player);
        {
            Optional<PageMetaDataManager> optional = ServerLevelEndInv.checkAndGetManagerForPlayer(player);
            LOGGER.info("Test: Optional AttachingManager Status: {}", optional);
        }
        if(oendinv.isEmpty()){
            LOGGER.warn("{}: Player who has not an EndInv quick-moved item to EndInv's page.", id()+" payload");
        }else {
            EndlessInventory endinv = oendinv.get();
            AbstractContainerMenu menu = player.containerMenu;
            if (slotId >= 0 && slotId < menu.slots.size()) {
                Slot slot = menu.getSlot(slotId);
                ItemStack itemStack = slot.getItem();
                ItemStack remain = endinv.addItem(itemStack);
                slot.setByPlayer(remain);
                slot.onTake(player, itemStack);
            } else {
                LOGGER.warn("{}: SlotId in payload exceeded menu's slots.", id()+" payload");
            }
        }
    }
}
