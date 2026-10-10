package com.kwwsyk.endinv.forge.nbtAttcachment;

import com.kwwsyk.endinv.common.ModInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.kwwsyk.endinv.common.AbstractModInitializer.withModLocation;

@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AttachingCapabilities {

    public static final Capability<IEndInvUuid> END_INV_UUID = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<ISyncedConfig> END_INV_CONFIG = CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public static void reg(RegisterCapabilitiesEvent event){
        event.register(IEndInvUuid.class);
        event.register(ISyncedConfig.class);
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event){
        if (!(event.getObject() instanceof Player)) return;

        event.addCapability(withModLocation("uuid"), new NBTCapability<>(new EndInvUuid(),END_INV_UUID));
        event.addCapability(withModLocation("synced_config"),new NBTCapability<>(new ISyncedConfigImpl(),END_INV_CONFIG));
    }
}
