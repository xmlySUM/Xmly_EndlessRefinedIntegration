package com.kwwsyk.endinv.forge;

import com.kwwsyk.endinv.common.AbstractModInitializer;
import com.kwwsyk.endinv.common.IPlatform;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.NbtAttachment;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.network.IPacketDistributor;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.options.IServerConfig;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.forge.client.config.ClientConfig;
import com.kwwsyk.endinv.forge.integrates.clothconfig.ClothConfigIntegration;
import com.kwwsyk.endinv.forge.integrates.curio.CurioInit;
import com.kwwsyk.endinv.forge.integrates.curio.CurioPageType;
import com.kwwsyk.endinv.forge.nbtAttcachment.AttachingCapabilities;
import com.kwwsyk.endinv.forge.nbtAttcachment.IEndInvUuid;
import com.kwwsyk.endinv.forge.nbtAttcachment.ISyncedConfig;
import com.kwwsyk.endinv.forge.hotbar.HotbarServerState;
import com.kwwsyk.endinv.forge.network.ModPacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

@Mod(ModInfo.MOD_ID) @SuppressWarnings("removal")
public class ModInitializer extends AbstractModInitializer {

    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(ForgeRegistries.ITEMS,ModInfo.MOD_ID);

    public static final DeferredRegister<MenuType<?>> MENU = DeferredRegister.create(ForgeRegistries.MENU_TYPES,ModInfo.MOD_ID);

    public ModInitializer() {
        // Forge 要求必须提供的无参构造器
        this(FMLJavaModLoadingContext.get().getModEventBus(), ModLoadingContext.get().getActiveContainer());
    }

    public ModInitializer(IEventBus modEventBus, ModContainer container){
        super.init();

        ITEM.register(modEventBus);
        MENU.register(modEventBus);
        // The file names are given explicitly and keep the ones this mod has always used. Left to
        // itself Forge would name them after the mod id, which changed when the Endless Refined
        // addon was merged in: every player's existing config would be silently abandoned and a
        // fresh set of defaults written beside it.
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                StartupConfig.CONFIG_SPEC, "endless_inventory-common.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                ServerConfig.CONFIG_SPEC, "endless_inventory-server.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                ClientConfig.CONFIG_SPEC, "endless_inventory-client.toml");

        if(FMLEnvironment.dist.isClient()){
            new ClientModInitializer();
            ClientModInitializer.init(modEventBus);

            if(ModList.get().isLoaded("cloth_config")){
                ClothConfigIntegration.register(container);
            }
        }

        if(ModList.get().isLoaded("curios")){
            modEventBus.addListener(CurioInit::registerCapabilities);
            CurioPageType.register();
        }
    }

    @Override
    protected IPlatform loadOtherPlatformSpecific() {
        return new IPlatform() {
            @Override
            public boolean onItemStackedOn(ItemStack clickedItem, ItemStack carriedItem, Slot slot, ClickAction action, Player player, SlotAccess access) {
                //Forge takes the carried item first and the item it is stacked onto second, which is
                //the other way round from this interface.
                return ForgeHooks.onItemStackedOn(carriedItem, clickedItem, slot, action, player, access);
            }

            @Override
            public boolean onPageClicked(ServerPlayer player, ItemKey key, ClickType clickType) {
                HotbarServerState state = HotbarServerState.of(player);
                if (clickType == ClickType.QUICK_MOVE && !key.isEmpty() && state.isPanelExpanded()) {
                    state.placeOnHotbar(player, key);
                    return true;
                }
                //A pick-up can leave the hotbar's own item on the cursor; it goes back.
                if (clickType == ClickType.PICKUP && !player.containerMenu.getCarried().isEmpty()) {
                    state.onReturnedToEndless(player, player.containerMenu.getCarried());
                }
                return false;
            }
        };
    }

    @Override
    protected IPacketDistributor loadPacketDistributor() {
        return new IPacketDistributor() {
            @Override
            public void sendToServer(ModPacketPayload payload) {
                ModPacketHandler.INSTANCE.sendToServer(payload);
            }

            @Override
            public void sendToPlayer(ServerPlayer player, ModPacketPayload payload) {
                ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(()->player),payload);
            }

            @Override
            public void sendToAllPlayer(ModPacketPayload payload) {
                ModPacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),payload);
            }
        };
    }

    @Override
    protected IServerConfig loadServerConfig() {
        return ServerConfig.CONFIG.INSTANCE;
    }

    @Override
    protected RegistryCallback<Item> itemReg() {
        return new RegistryCallback<Item>() {
            @Override
            public <R extends Item> Supplier<R> register(String id, Supplier<R> supplier) {
                return ITEM.register(id,supplier);
            }
        };
    }

    @Override
    protected RegistryCallback<MenuType<?>> menuReg() {
        return new RegistryCallback<>() {
            @Override
            public <R extends MenuType<?>> Supplier<R> register(String id, Supplier<R> supplier) {
                return MENU.register(id, supplier);
            }
        };
    }

    @Override
    protected Supplier<MenuType<EndlessInventoryMenu>> createEndInvMenuType() {
        return () -> new MenuType<>(EndlessInventoryMenu::createClient, FeatureFlags.DEFAULT_FLAGS);
    }

    @Override
    protected NbtAttachment<UUID> createEndInvUUID(String name) {
        return new NbtAttachment<UUID>() {
            @Override
            public UUID getWith(Player player) {
                var opt = player.getCapability(AttachingCapabilities.END_INV_UUID).resolve();
                return opt.map(IEndInvUuid::getUuid).orElse(ModInfo.DEFAULT_UUID);
            }

            @Override
            public void setTo(Player player, UUID uuid) {
                var opt = player.getCapability(AttachingCapabilities.END_INV_UUID).resolve();
                opt.ifPresent(endInvUuid -> endInvUuid.setUuid(uuid));
            }

            @Override
            public UUID computeIfAbsent(Player player) {
                return player.getCapability(AttachingCapabilities.END_INV_UUID).resolve().map(IEndInvUuid::getUuid).orElse(ModInfo.DEFAULT_UUID);
            }
        };
    }

    @Override
    protected NbtAttachment<SyncedConfig> createSyncedConfig(String name) {
        return new NbtAttachment<>() {
            @Override
            public SyncedConfig getWith(Player player) {
                return player.getCapability(AttachingCapabilities.END_INV_CONFIG)
                        .resolve()
                        .map(ISyncedConfig::getSyncedConfig)
                        .orElse(SyncedConfig.DEFAULT);
            }

            @Override
            public void setTo(Player player, SyncedConfig syncedConfig) {
                player.getCapability(AttachingCapabilities.END_INV_CONFIG)
                        .resolve()
                        .ifPresent(config->config.setSyncedConfig(syncedConfig));
            }

            @Override
            public SyncedConfig computeIfAbsent(Player player) {
                return player.getCapability(AttachingCapabilities.END_INV_CONFIG)
                        .resolve()
                        .map(ISyncedConfig::getSyncedConfig)
                        .orElse(SyncedConfig.DEFAULT);
            }
        };
    }
}
