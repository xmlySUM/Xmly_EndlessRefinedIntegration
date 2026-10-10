package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.util.ItemStackCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Used when client item modified in ItemDisplay with {@link net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu}
 * @param isAdding true for add item and false for take item.
 */
public record CreativeItemModPayload(ItemStack stack, boolean isAdding) implements ModPacketPayload {

    public static void encode(CreativeItemModPayload payload, FriendlyByteBuf o){
        ItemStackCodec.writeStack(o, payload.stack);
        o.writeBoolean(payload.isAdding);
    }

    public static CreativeItemModPayload decode(FriendlyByteBuf o){
        return new CreativeItemModPayload(ItemStackCodec.readStack(o),o.readBoolean());
    }


    @Override
    public String id() {
        return "item_modify";
    }

    @Override
    public void handle(ModPacketContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        if(player==null)return;
        if(player.containerMenu.getCarried().isEmpty() && player.isCreative()){
            Optional<EndlessInventory> optional = ServerLevelEndInv.getEndInvForPlayer(player);
            if(optional.isPresent()) {
                EndlessInventory ei = optional.get();
                if(isAdding){
                    ei.addItem(stack);
                }else {
                    ItemStack taken = ei.takeItem(stack);
                    if(!taken.isEmpty()) player.containerMenu.setCarried(taken);
                }
            }

        }
    }
}
