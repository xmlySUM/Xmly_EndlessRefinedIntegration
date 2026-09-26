package com.xmly.endlessrefined.mixin.endless;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerMenu.class)
public interface ContainerMenuAccessor {

    @Accessor("slots")
    NonNullList<Slot> eri$slots();
}
