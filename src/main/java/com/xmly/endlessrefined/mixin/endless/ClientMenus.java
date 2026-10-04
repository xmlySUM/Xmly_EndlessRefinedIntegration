package com.xmly.endlessrefined.mixin.endless;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

final class ClientMenus {

    private ClientMenus() {
    }

    static boolean isCreativePicker(AbstractContainerMenu menu) {
        return menu instanceof CreativeModeInventoryScreen.ItemPickerMenu;
    }
}
