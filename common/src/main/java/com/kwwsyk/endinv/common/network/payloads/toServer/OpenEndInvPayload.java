package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.menu.page.pageManager.AttachingMonitor;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;


/**Send to server,
 * to open {@link EndlessInventoryMenu} when player is not opening a menu,
 * or to mention server to attach an {@link AttachingMonitor} if player is opening a menu.
 * @param openNew true for opening a new EIM, false -> ignore rows
 * @param rows The row data to adjust position of EIM widgets
 * @param columns reserved data, default to 9
 */
public record OpenEndInvPayload(boolean openNew, int rows, int columns) implements ModPacketPayload {

    private static final Logger LOGGER = LogUtils.getLogger();

    public OpenEndInvPayload(boolean ofMenu, int rows){
        this(ofMenu,rows,9);
    }

    public OpenEndInvPayload(){
        this(false,6);
    }

    public static void encode(OpenEndInvPayload payload, FriendlyByteBuf o){
        o.writeBoolean(payload.openNew);o.writeInt(payload.rows);o.writeInt(payload.columns);
    }

    public static OpenEndInvPayload decode(FriendlyByteBuf o){
        return new OpenEndInvPayload(o.readBoolean(),o.readInt(),o.readInt());
    }

    @Override
    public String id() {
        return "open_endinv";
    }

    @Override
    public void handle(ModPacketContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        if(player==null) return;
        if(player.containerMenu == player.inventoryMenu && openNew()){
            player.openMenu(EndlessInventoryMenu.provide(rows));
        }else if(!openNew()){
            if(!checkAttachingStatus(player)) {
                LOGGER.warn("Player unable to open AttachingScreen sent open AS.");
                return;
            }
            ServerLevelEndInv.getEndInvForPlayer(player).ifPresent(endInv->{
                AttachingMonitor manager = new AttachingMonitor(player.containerMenu, endInv ,player);
                ServerLevelEndInv.PAGE_META_DATA_MANAGER.put(player,manager);
                manager.sendEndInvData();
            });

        }

    }

    /**
     */
    private boolean checkAttachingStatus(ServerPlayer player){
        SyncedConfig syncedConfig = ModRegistries.NbtAttachments.getSyncedConfig().getWith(player);
        boolean nbtAttaching = syncedConfig.attaching();//presents player's client attaching config
        //boolean configAttaching = ModInfo.getServerConfig().enableAttaching().get();
        boolean menuAttachable = ModInfo.getServerConfig().specifiedMenuAttachability().get().attachable(player);

        if(!nbtAttaching){
            LOGGER.warn("Player with attaching=false nbt sent openAttaching payload.");
            //assume player's client attaching config is true.
            ModRegistries.NbtAttachments.getSyncedConfig().setTo(player, new SyncedConfig(true, syncedConfig.autoPicking()));
        }

        return menuAttachable;
    }

}
