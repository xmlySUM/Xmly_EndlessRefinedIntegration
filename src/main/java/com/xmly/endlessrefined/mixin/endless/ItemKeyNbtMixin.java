package com.xmly.endlessrefined.mixin.endless;

import com.kwwsyk.endinv.common.util.ItemKey;
import com.xmly.endlessrefined.endless.EndlessBridge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemKey.class)
public abstract class ItemKeyNbtMixin {

    @Inject(method = "encode", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eri$encodeWithNbt(FriendlyByteBuf buf, ItemKey key, CallbackInfo ci) {
        EndlessBridge.traceKey("key.encode", key);

        buf.writeVarInt(BuiltInRegistries.ITEM.getId(key.item()));
        buf.writeNbt(key.tag());

        ci.cancel();
    }

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eri$decodeWithNbt(FriendlyByteBuf buf, CallbackInfoReturnable<ItemKey> cir) {
        Item item = BuiltInRegistries.ITEM.byId(buf.readVarInt());
        ItemKey key = new ItemKey(item == null ? Items.AIR : item, buf.readNbt());

        EndlessBridge.traceKey("key.decode", key);

        cir.setReturnValue(key);
    }
}
