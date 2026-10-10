package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.gui.page.ItemDisplay;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemState;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**What changed in an Endless Inventory since the screen last heard about it.<br>
 * {@link EndInvContent} carries the whole thing, which is right when a screen opens and wrong every
 * time after that: taking an item out would send every other item in the inventory along with it,
 * and a player moving items quickly would have the whole map on the wire several times a second.
 */
public record EndInvContentDelta(Map<ItemKey, ItemState> changed, List<ItemKey> removed) implements ModPacketPayload {

    public static void encode(EndInvContentDelta payload, FriendlyByteBuf o) {
        o.writeVarInt(payload.changed.size());
        payload.changed.forEach((key, state) -> {
            ItemKey.encode(o, key);
            ItemState.encode(o, state);
        });
        o.writeVarInt(payload.removed.size());
        payload.removed.forEach(key -> ItemKey.encode(o, key));
    }

    public static EndInvContentDelta decode(FriendlyByteBuf o) {
        int changedSize = o.readVarInt();
        Map<ItemKey, ItemState> changed = new Object2ObjectLinkedOpenHashMap<>(changedSize);
        for (int i = 0; i < changedSize; i++) {
            ItemKey key = ItemKey.decode(o);
            changed.put(key, ItemState.decode(o));
        }
        int removedSize = o.readVarInt();
        List<ItemKey> removed = new ArrayList<>(removedSize);
        for (int i = 0; i < removedSize; i++) {
            removed.add(ItemKey.decode(o));
        }
        return new EndInvContentDelta(changed, removed);
    }

    @Override
    public String id() {
        return "endinv_content_delta";
    }

    @Override
    public void handle(ModPacketContext context) {
        CachedSrcInv.INSTANCE.applyDelta(changed, removed);
        ModPacketPayload.getClientPageMeta().ifPresent(
                mng -> {
                    if (mng.getDisplayingPage() instanceof ItemDisplay itemPage) {
                        itemPage.readCachedItems();
                    }
                }
        );
    }
}
