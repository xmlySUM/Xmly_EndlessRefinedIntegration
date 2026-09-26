/*
 * Derived from QuadHotbar by ArchangelD (mod_id: quadhotbar), LGPL-3.0.
 * This file remains under LGPL-3.0. See THIRD-PARTY.md for the list of changes.
 */
package com.xmly.endlessrefined.mixin.hotbar;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lifts vanilla's nine-slot clamp off the selected slot, so the hotbar engine can
 * point {@code selected} at any of the 36 inventory slots.
 *
 * <p>The original used SRG names with {@code remap = false}, because QuadHotbar ships
 * no refmap and needed a second, SRG-targeted copy of each injection to work in
 * production. This project does have a refmap, so the official names below are
 * remapped for us and one injection each is enough.
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow
    @Final
    public NonNullList<ItemStack> items;

    @Shadow
    public int selected;

    @Inject(method = "getSelected", at = @At("HEAD"), cancellable = true)
    private void eri$extendedGetSelected(CallbackInfoReturnable<ItemStack> cir) {
        if (this.selected >= 0 && this.selected < this.items.size()) {
            cir.setReturnValue(this.items.get(this.selected));
        }
    }

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void eri$extendedGetDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        ItemStack selectedStack = this.selected >= 0 && this.selected < this.items.size()
                ? this.items.get(this.selected)
                : ItemStack.EMPTY;

        cir.setReturnValue(selectedStack.getDestroySpeed(state));
    }
}
