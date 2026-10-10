package com.kwwsyk.endinv.forge.mixin.hotbar;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.forge.hotbar.HotbarServerState;
import com.kwwsyk.endinv.forge.hotbar.InventoryTransfer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**Makes Endless Inventory behave like inventory space:<br>
 * items that arrive in slots 9-35 are swept into it, and items that fit nowhere go there rather
 * than onto the ground. Only {@link Inventory#add} is touched, which is how passively gained items
 * arrive; items moved around inside a GUI go through other methods and are deliberately left alone,
 * so a shift-click cannot be undone behind the player's back.
 */
@Mixin(Inventory.class)
public abstract class InventoryOverflowMixin {
    @Shadow
    @Final
    public Player player;
    @Unique
    private ItemStack endinv$offered = ItemStack.EMPTY;

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"))
    private void endinv$captureOffered(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.player instanceof ServerPlayer) {
            endinv$offered = stack.copy();
        }
    }

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void endinv$endlessOverflow(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!(this.player instanceof ServerPlayer serverPlayer) || this.player instanceof FakePlayer) {
            return;
        }
        ItemStack offered = endinv$offered;
        endinv$offered = ItemStack.EMPTY;
        if (offered.isEmpty()) {
            return;
        }
        if (cir.getReturnValueZ()) {
            if (!ModInfo.getServerConfig().enableSweep().get()) {
                return;
            }
            int added = offered.getCount() - stack.getCount();
            if (added > 0) {
                InventoryTransfer.moveExtendedToEndless(serverPlayer, offered, added);
            }
            return;
        }
        if (!ModInfo.getServerConfig().enableOverflow().get() || stack.isEmpty()) {
            return;
        }
        EndlessInventory endInv = HotbarServerState.endInv(serverPlayer);
        if (endInv == null) {
            return;
        }
        ItemStack remainder = endInv.addItem(ItemKey.asKey(stack), stack.getCount());
        int stored = stack.getCount() - remainder.getCount();
        if (stored <= 0) {
            return;
        }
        stack.shrink(stored);
        cir.setReturnValue(true);
    }
}
