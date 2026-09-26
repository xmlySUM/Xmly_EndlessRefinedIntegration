package com.xmly.endlessrefined.mixin.endless;

import com.kwwsyk.endinv.common.util.ItemStackLike;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStackLike.class)
public abstract class ItemStackLikeNbtMixin {

    @Inject(method = "encode", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eri$encodeWithNbt(FriendlyByteBuf o, ItemStackLike item, CallbackInfo ci) {
        o.writeVarInt(BuiltInRegistries.ITEM.getId(item.item()));
        o.writeInt(item.count());
        o.writeNbt(item.tag());

        ci.cancel();
    }

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eri$decodeWithNbt(FriendlyByteBuf o, CallbackInfoReturnable<ItemStackLike> cir) {
        Item item = BuiltInRegistries.ITEM.byId(o.readVarInt());
        int count = o.readInt();

        cir.setReturnValue(new ItemStackLike(item == null ? Items.AIR : item, count, o.readNbt()));
    }
}
