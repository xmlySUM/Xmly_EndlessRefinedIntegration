package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.network.payloads.toClient.SetStarredPagePayload;
import com.kwwsyk.endinv.common.util.ItemStackCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record StarItemPayload(ItemStack stack,boolean isAdding) implements ModPacketPayload {

    public static void encode(StarItemPayload payload, FriendlyByteBuf o){
        ItemStackCodec.writeStack(o, payload.stack);
        o.writeBoolean(payload.isAdding);
    }

    public static StarItemPayload decode(FriendlyByteBuf o){
        return new StarItemPayload(ItemStackCodec.readStack(o),o.readBoolean());
    }

    @Override
    public String id() {
        return "star_item";
    }

    public void handle(ModPacketContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        if(player==null) return;
        ServerLevelEndInv.getEndInvForPlayer(player).ifPresent(endInv->{
            if(isAdding()) {
                endInv.affinities.addStarredItem(stack);
            }else {
                endInv.affinities.removeStarredItem(stack);
            }
            ModInfo.getPacketDistributor().sendToPlayer(player, new SetStarredPagePayload(endInv.getStarredItems()));
            endInv.setChanged();
        });
    }
}
