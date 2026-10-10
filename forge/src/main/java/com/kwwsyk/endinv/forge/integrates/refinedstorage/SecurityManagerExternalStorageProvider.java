package com.kwwsyk.endinv.forge.integrates.refinedstorage;

import com.refinedmods.refinedstorage.api.storage.StorageType;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorage;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageContext;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageProvider;
import com.refinedmods.refinedstorage.blockentity.SecurityManagerBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;

/**Makes an External Storage bus pointed at a Security Manager expose the Endless Inventory of the
 * player that manager gives every permission to.<br>
 * This is Refined Storage's own seam for "a block that carries an inventory of its own": the bus
 * asks each registered provider in turn whether it recognises the block it faces, and the first to
 * say yes supplies the storage. Reading the cards off the manager next door means the bus, and not
 * some separate rule about the network as a whole, decides which inventory the network gets.
 */
public final class SecurityManagerExternalStorageProvider implements IExternalStorageProvider<ItemStack> {

    /**Above the handlers Refined Storage registers for item and fluid handlers, which are all 0.<br>
     * The providers are kept in a set ordered by priority alone, so sharing a priority would make
     * this one a duplicate and it would be dropped without a word. Going first costs nothing:
     * {@link #canProvide} says no to every block that is not a Security Manager.
     */
    private static final int PRIORITY = 1;

    @Override
    public boolean canProvide(BlockEntity blockEntity, Direction direction) {
        return blockEntity instanceof SecurityManagerBlockEntity;
    }

    @Override
    @Nonnull
    public IExternalStorage<ItemStack> provide(IExternalStorageContext context, BlockEntity blockEntity, Direction direction) {
        return new SecurityManagerExternalStorage(context, (SecurityManagerBlockEntity) blockEntity);
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    /**Kept next to the provider so the two cannot drift apart. */
    public static StorageType storageType() {
        return StorageType.ITEM;
    }
}
