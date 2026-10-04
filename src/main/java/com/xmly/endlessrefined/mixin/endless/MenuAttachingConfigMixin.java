package com.xmly.endlessrefined.mixin.endless;

import com.kwwsyk.endinv.common.options.SpecifiedMenuAttachingConfig;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;


@Mixin(SpecifiedMenuAttachingConfig.class)
public abstract class MenuAttachingConfigMixin {

    @Shadow(remap = false)
    public abstract boolean isInventoryAttachable();

    @Shadow(remap = false)
    public abstract boolean isMenuAttachable(@Nullable MenuType<?> type);

    @Inject(method = "isMenuAttachable(Lnet/minecraft/world/inventory/AbstractContainerMenu;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void eri$withoutClientOnlyMenu(AbstractContainerMenu menu, CallbackInfoReturnable<Boolean> cir) {
        if (menu instanceof InventoryMenu || (FMLEnvironment.dist.isClient() && ClientMenus.isCreativePicker(menu))) {
            cir.setReturnValue(isInventoryAttachable());
            return;
        }
        cir.setReturnValue(isMenuAttachable(menu.getType()));
    }
}
