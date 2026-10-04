package com.xmly.endlessrefined.endless;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemState;
import com.mojang.logging.LogUtils;
import com.xmly.endlessrefined.compat.Compat;
import com.xmly.endlessrefined.config.ERIConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public final class EndlessBridge {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final int TRACE_FIRST = 20;
    private static final int TRACE_EVERY = 200;
    private static int tracedKeys = 0;

    private EndlessBridge() {
    }

    @Nullable
    public static EndlessInventory forPlayer(@Nullable Player player) {
        if (player == null || player.level().isClientSide) {
            return null;
        }
        if (!Compat.hasEndlessInventory()) {
            return null;
        }
        if (ServerLevelEndInv.levelEndInvData == null) {
            return null;
        }
        return ServerLevelEndInv.getEndInvForPlayer(player).orElse(null);
    }

    public static int availableCount(EndlessInventory endInv, ItemKey key) {
        return countIn(endInv.snapshotItemMap(), key);
    }

    public static int countIn(Map<ItemKey, ItemState> snapshot, ItemKey key) {
        var state = snapshot.get(key);
        if (state != null) {
            return state.count();
        }
        ItemKey normalised = normaliseKey(key);
        if (normalised == null) {
            return 0;
        }
        state = snapshot.get(normalised);
        return state == null ? 0 : state.count();
    }

    public static int acceptedCount(EndlessInventory endInv, ItemKey key, int size) {
        if (size <= 0) {
            return 0;
        }
        int original = availableCount(endInv, key);
        int max = endInv.getMaxItemStackSize();
        if (original >= max) {
            return endInv.isInfinityMode() ? size : 0;
        }
        long increased = (long) original + size;
        long accepted = Math.min(increased, max) - original;
        return (int) Math.max(0L, Math.min((long) size, accepted));
    }

    public static int storedTotal(EndlessInventory endInv) {
        long total = 0L;
        for (var state : endInv.snapshotItemMap().values()) {
            total += state.count();
            if (total >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }

    public static ItemStack insert(@Nullable Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        EndlessInventory endInv = forPlayer(player);
        if (endInv == null) {
            return stack.copy();
        }
        return endInv.addItem(ItemKey.asKey(stack), stack.getCount());
    }

    public static ItemStack toStack(ItemKey key, int count) {
        if (count <= 0 || key.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return key.toStack(count);
    }

    @Nullable
    private static ItemKey normaliseKey(ItemKey key) {
        CompoundTag tag = key.tag();
        if (tag == null) {
            return new ItemKey(key.item(), new CompoundTag());
        }
        if (tag.isEmpty()) {
            return new ItemKey(key.item(), null);
        }
        return null;
    }

    public static List<ItemStack> stockItems(@Nullable Player player) {
        EndlessInventory endInv = forPlayer(player);
        return endInv == null ? List.of() : endInv.getItemsAsList();
    }

    public static void traceKey(String where, ItemKey key) {
        if (!ERIConfig.TRACE_NBT.get()) {
            return;
        }
        int call = ++tracedKeys;
        if (key.tag() != null && !key.tag().isEmpty()) {
            LOGGER.info("[ERI nbt] {} #{} TAGGED item={} tag={}", where, call, key.item(), key.tag());
        } else if (call <= TRACE_FIRST || call % TRACE_EVERY == 0) {
            LOGGER.info("[ERI nbt] {} #{} item={} tag=null", where, call, key.item());
        }
    }

    public static void trace(String message, Object... args) {
        if (ERIConfig.TRACE_NBT.get()) {
            LOGGER.info("[ERI trace] " + message, args);
        }
    }

    public static void traceStacks(String where, List<ItemStack> stacks) {
        if (!ERIConfig.TRACE_NBT.get()) {
            return;
        }
        int withTag = 0;
        for (ItemStack stack : stacks) {
            if (stack.getTag() != null) {
                withTag++;
            }
        }
        LOGGER.info("[ERI nbt] {} {} stacks, {} carrying a tag", where, stacks.size(), withTag);
    }
}
