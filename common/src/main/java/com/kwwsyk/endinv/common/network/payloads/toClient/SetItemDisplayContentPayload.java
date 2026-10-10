package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.gui.page.ItemDisplay;
import com.kwwsyk.endinv.common.client.gui.page.ItemPage;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemStackCodec;
import com.kwwsyk.endinv.common.util.ItemState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**Packet that contains required {@link ItemDisplay}'s content view
 *  when {@code TransferMode==PART}
 * @param stacks
 */
public record SetItemDisplayContentPayload(List<ItemStack> stacks) implements ModPacketPayload {

    public static void encode(SetItemDisplayContentPayload payload, FriendlyByteBuf o){
        o.writeCollection(payload.stacks, ItemStackCodec::writeStack);
    }

    public static SetItemDisplayContentPayload decode(FriendlyByteBuf o){
        return new SetItemDisplayContentPayload(o.readList(ItemStackCodec::readStack));
    }

    @Override
    public String id() {
        return "itemdisplay_content";
    }

    public void handle(ModPacketContext context) {//todo handle such cache
        var stackStream = stacks.stream();
        Map<ItemKey, ItemState> partlyMap = new HashMap<>();
        stacks.forEach(stack -> partlyMap.put(ItemKey.asKey(stack), new ItemState(stack.getCount(), -1)));
        //The snapshot accessor hands out a copy, so this has to go through the cache itself.
        CachedSrcInv.INSTANCE.applyPartialSnapshot(partlyMap);
        ModPacketPayload.getClientPageMeta().ifPresent(mng->{
            if(mng.getDisplayingPage() instanceof ItemDisplay itemDisplay){
                itemDisplay.buildContentsWith(stackStream.map(ItemKey::asKey).map(ItemPage.ItemPointer::new).toList());
            }
        });
    }
}
