package com.xmly.endlessrefined.mixin.endless;

import com.kwwsyk.endinv.common.util.ItemKey;
import com.xmly.endlessrefined.endless.EndlessBridge;
import com.xmly.endlessrefined.network.KeyCodec;
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
    private static void eri$encodeWithNbt(FriendlyByteBuf o, ItemKey key, CallbackInfo ci) {
        EndlessBridge.traceKey("key.encode", key);
        KeyCodec.write(o, key);
        ci.cancel();
    }

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eri$decodeWithNbt(FriendlyByteBuf O, CallbackInfoReturnable<ItemKey> cir) {
        Item item = BuiltInRegistries.ITEM.byId(O.readVarInt());
        ItemKey key = new ItemKey(item == null ? Items.AIR : item, O.readNbt());

        EndlessBridge.traceKey("key.decode", key);

        cir.setReturnValue(key);
    }
}
