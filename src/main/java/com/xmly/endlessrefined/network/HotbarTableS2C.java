package com.xmly.endlessrefined.network;

import com.kwwsyk.endinv.common.util.ItemKey;
import com.xmly.endlessrefined.client.HotbarPanelState;
import com.xmly.endlessrefined.hotbar.HotbarTable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record HotbarTableS2C(List<ItemStack> cells) {

    public static void encode(HotbarTableS2C message, FriendlyByteBuf buffer) {
        for (int i = 0; i < HotbarTable.CELLS; i++) {
            ItemStack cell = i < message.cells().size() ? message.cells().get(i) : ItemStack.EMPTY;

            buffer.writeBoolean(!cell.isEmpty());

            if (!cell.isEmpty()) {
                KeyCodec.write(buffer, ItemKey.asKey(cell));
            }
        }
    }

    public static HotbarTableS2C decode(FriendlyByteBuf buffer) {
        List<ItemStack> cells = new ArrayList<>(HotbarTable.CELLS);

        for (int i = 0; i < HotbarTable.CELLS; i++) {
            cells.add(buffer.readBoolean() ? KeyCodec.read(buffer).toStack(1) : ItemStack.EMPTY);
        }

        return new HotbarTableS2C(cells);
    }

    public static void handle(HotbarTableS2C message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> HotbarPanelState.setCells(message.cells())));

        context.setPacketHandled(true);
    }
}
