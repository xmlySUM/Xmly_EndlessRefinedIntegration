package com.xmly.endlessrefined.mixin;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class ERIMixinPlugin implements IMixinConfigPlugin {

    private static final String MOD_REFINED_STORAGE = "refinedstorage";

    private static final String PKG_REFINED_STORAGE = ".mixin.refinedstorage.";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(PKG_REFINED_STORAGE)) {
            return isModLoaded(MOD_REFINED_STORAGE);
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isModLoaded(String modId) {
        LoadingModList loadingModList = FMLLoader.getLoadingModList();

        if (loadingModList != null && loadingModList.getModFileById(modId) != null) {
            return true;
        }

        ModList modList = ModList.get();

        return modList != null && modList.isLoaded(modId);
    }
}
