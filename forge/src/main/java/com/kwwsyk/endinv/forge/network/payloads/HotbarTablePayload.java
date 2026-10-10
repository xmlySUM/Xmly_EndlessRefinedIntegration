package com.kwwsyk.endinv.forge.network.payloads;

import com.kwwsyk.endinv.common.hotbar.HotbarTable;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.client.hotbar.HotbarPanelState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**The whole eighty-one cell table, sent to the client so the panel can draw it.<br>
 * The amounts shown are the stacks sitting in the hotbar slots themselves, so only the item
 * identity travels here.
 */
public record HotbarTablePayload(List<ItemStack> cells) implements ModPacketPayload {

    public static void encode(HotbarTablePayload message, FriendlyByteBuf buffer) {
        for (int i = 0; i < HotbarTable.CELLS; i++) {
            ItemStack cell = i < message.cells().size() ? message.cells().get(i) : ItemStack.EMPTY;
            buffer.writeBoolean(!cell.isEmpty());
            if (!cell.isEmpty()) {
                ItemKey.encode(buffer, ItemKey.asKey(cell));
            }
        }
    }

    public static HotbarTablePayload decode(FriendlyByteBuf buffer) {
        List<ItemStack> cells = new ArrayList<>(HotbarTable.CELLS);
        for (int i = 0; i < HotbarTable.CELLS; i++) {
            cells.add(buffer.readBoolean() ? ItemKey.decode(buffer).toStack(1) : ItemStack.EMPTY);
        }
        return new HotbarTablePayload(cells);
    }

    @Override
    public String id() {
        return "hotbar_table";
    }

    @Override
    public void handle(ModPacketContext context) {
        HotbarPanelState.setCells(cells);
    }
}
