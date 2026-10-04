package com.xmly.endlessrefined.mixin.endless;

import com.kwwsyk.endinv.common.network.payloads.toClient.SetItemDisplayContentPayload;
import com.xmly.endlessrefined.endless.EndlessBridge;
import com.xmly.endlessrefined.network.KeyCodec;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SetItemDisplayContentPayload.class)
public abstract class ItemDisplayPayloadNbtMixin {

    @Inject(method = "encode", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eri$encodeWithNbt(SetItemDisplayContentPayload payload, FriendlyByteBuf o, CallbackInfo ci) {
        EndlessBridge.traceStacks("payload.encode", payload.stacks());
        o.writeCollection(payload.stacks(), KeyCodec::writeStack);
        ci.cancel();
    }

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eri$decodeWithNbt(FriendlyByteBuf o, CallbackInfoReturnable<SetItemDisplayContentPayload> cir) {
        java.util.List<net.minecraft.world.item.ItemStack> stacks = o.readList(KeyCodec::readStack);
        EndlessBridge.traceStacks("payload.decode", stacks);
        cir.setReturnValue(new SetItemDisplayContentPayload(stacks));
    }
}