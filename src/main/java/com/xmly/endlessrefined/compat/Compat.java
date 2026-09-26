package com.xmly.endlessrefined.compat;

import net.minecraftforge.fml.ModList;

public final class Compat {

    public static boolean hasEndlessInventory() {
        return ModList.get().isLoaded("endless_inventory");
    }

    public static boolean hasRefinedStorage() {
        return ModList.get().isLoaded("refinedstorage");
    }

    public static boolean hasJei() {
        return ModList.get().isLoaded("jei");
    }

    public static boolean hasQuadHotbar() {
        return ModList.get().isLoaded("quadhotbar");
    }
}