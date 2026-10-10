package com.kwwsyk.endinv.forge.integrates.refinedstorage;

import com.kwwsyk.endinv.common.ModInfo;
import com.refinedmods.refinedstorage.apiimpl.API;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**Tells Refined Storage that Security Managers carry an inventory as far as an External Storage bus
 * is concerned. Only registered when Refined Storage is there to hear it.
 */
@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RefinedStorageInit {

    private RefinedStorageInit() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded("refinedstorage")) {
            return;
        }
        event.enqueueWork(() -> API.instance().addExternalStorageProvider(
                SecurityManagerExternalStorageProvider.storageType(),
                new SecurityManagerExternalStorageProvider()));
    }
}
