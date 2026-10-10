package com.kwwsyk.endinv.forge.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.autopick.AutoPickHelper;
import com.kwwsyk.endinv.common.autopick.events.IBlockBreakEvent;
import com.kwwsyk.endinv.common.autopick.events.ILivingDropsEvent;
import com.kwwsyk.endinv.common.autopick.events.ILivingExpDropsEvent;
import com.kwwsyk.endinv.common.autopick.events.IPlayerPickupItemEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**Handle auto-pickup features, triggered when
 * 1. Entity drop item and exp: automatically transfer items into EndInv, and exp picked up, meaning jumping item and exp entity spawn.
 * 2. Block drop item and exp: Similar to item behavior. But beds or chests may still drop item entities as different drop logic.
 * 3. Picked-up items (by player touch item entities) will be automatically transferred into EndInv. But items satisfied with some conditions will stay in playerInv.
 */
@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LootEvent {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        AutoPickHelper.onLivingDrops(
                new ILivingDropsEvent() {
                    @Override
                    public DamageSource getSource() {
                        return event.getSource();
                    }

                    @Override
                    public Collection<ItemEntity> getDrops() {
                        return event.getDrops();
                    }

                    @Override
                    public void setCanceled(boolean canceled) {
                        event.setCanceled(canceled);
                    }
                }
        );
    }

    /**
     *
     * @param event
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        AutoPickHelper.onBlockBreak(
                new IBlockBreakEvent() {
                    @Override
                    public LevelAccessor getLevel() {
                        return event.getLevel();
                    }

                    @Override
                    public BlockPos getPos() {
                        return event.getPos();
                    }

                    @Override
                    public BlockState getState() {
                        return event.getState();
                    }

                    @Override
                    public Player getPlayer() {
                        return event.getPlayer();
                    }

                    @Override
                    public int getExpToDrop() {
                        return event.getExpToDrop();
                    }

                    @Override
                    public void setExpToDrop(int exp) {
                        event.setExpToDrop(exp);
                    }

                    @Override
                    public void setCanceled(boolean canceled) {
                        event.setCanceled(canceled);
                    }
                }
        );
    }

    @SubscribeEvent
    public static void onExpDrops(LivingExperienceDropEvent event){
        AutoPickHelper.onExpDrops(
                new ILivingExpDropsEvent() {
                    @Override
                    public int getDroppedExperience() {
                        return event.getDroppedExperience();
                    }

                    @Override
                    public void setDroppedExperience(int droppedExperience) {
                        event.setDroppedExperience(droppedExperience);
                    }

                    @Override
                    public @Nullable Player getAttackingPlayer() {
                        return event.getAttackingPlayer();
                    }

                    @Override
                    public void setCanceled(boolean canceled) {
                        event.setCanceled(canceled);
                    }
                }
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)//mostly, the itemEntity should vanish.
    public static void onPickupItem(EntityItemPickupEvent event){
        AutoPickHelper.onPickupItem(
                new IPlayerPickupItemEvent() {
                    @Override
                    public ItemEntity getItem() {
                        return event.getItem();
                    }

                    @Override
                    public Player getPlayer() {
                        return event.getEntity();
                    }
                }
        );
    }








}
