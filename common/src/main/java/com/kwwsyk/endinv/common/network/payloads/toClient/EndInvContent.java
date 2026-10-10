package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.gui.page.ItemDisplay;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemState;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;

import java.util.Map;

public record EndInvContent(Map<ItemKey, ItemState> itemMap) implements ModPacketPayload {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void encodeItemMap(Map<ItemKey,ItemState> map, FriendlyByteBuf o){
        o.writeMap(map,ItemKey::encode,ItemState::encode);
    }

    public static Map<ItemKey,ItemState> decodeItemMap(FriendlyByteBuf o){
        return o.readMap(Object2ObjectLinkedOpenHashMap::new,ItemKey::decode,ItemState::decode);
    }

    public static void encode(EndInvContent content,FriendlyByteBuf o){
        encodeItemMap(content.itemMap,o);
    }

    public static EndInvContent decode(FriendlyByteBuf o){
        return new EndInvContent(decodeItemMap(o));
    }

    /**Say how much item data survived the trip, when the client's debug switch is on.<br>
     * Whether an item's NBT reached the client is otherwise only visible by eye, on an icon whose
     * enchantment glow or tooltip gives it away.
     */
    private void traceTags(){
        if(!com.kwwsyk.endinv.common.client.ClientModInfo.getClientConfig().screenDebugging().get()) return;
        long tagged = itemMap.keySet().stream()
                .filter(key -> key.tag()!=null && !key.tag().isEmpty())
                .count();
        LOGGER.info("[endinv nbt] content received: {} items, {} carrying a tag", itemMap.size(), tagged);
    }

    @Override
    public String id() {
        return "endinv_content";
    }

    public void handle(ModPacketContext context){
        traceTags();
        CachedSrcInv.INSTANCE.initializeContents(this.itemMap());

        ModPacketPayload.getClientPageMeta().ifPresent(
                mng->{
                    if(mng.getDisplayingPage() instanceof ItemDisplay itemPage){
                        itemPage.readCachedItems();//9.25 update: packet invoke page item refresh, item refresh should not send packet (ItemPageContext)
                    }
                }
        );
    }
}
