package com.kwwsyk.endinv.forge.mixin;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**Decides which mixins may apply.<br>
 * Two groups need gating. The multi-group hotbar here is a fork of the standalone
 * {@code quadhotbar} mod, and the two cannot coexist: both redirect the same
 * {@code Inventory#getSelectionSize()} call inside
 * {@code ServerGamePacketListenerImpl#handleSetCarriedItem}, and two redirects on one instruction
 * are a hard failure. Rather than let the game die on it, the two mixins that come from QuadHotbar
 * are skipped when that mod is installed, and {@code HotbarEngine} notices the same thing at
 * startup and leaves the hotbar alone. Separately, the Refined Storage mixins target that mod's
 * classes, which are simply absent when it is not installed.
 */
public final class EndInvMixinPlugin implements IMixinConfigPlugin {

    private static final String CONFLICTING_MOD = "quadhotbar";
    private static final String MOD_REFINED_STORAGE = "refinedstorage";

    private static final String QUADHOTBAR_MIXIN_PACKAGE = ".mixin.hotbar.engine.";
    private static final String REFINED_STORAGE_MIXIN_PACKAGE = ".mixin.refinedstorage.";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(QUADHOTBAR_MIXIN_PACKAGE)) {
            return !isModLoaded(CONFLICTING_MOD);
        }
        if (mixinClassName.contains(REFINED_STORAGE_MIXIN_PACKAGE)) {
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

    /**The mod list is not built yet when mixins are applied, so both states are checked. */
    private static boolean isModLoaded(String modId) {
        LoadingModList loadingModList = FMLLoader.getLoadingModList();

        if (loadingModList != null && loadingModList.getModFileById(modId) != null) {
            return true;
        }
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(modId);
    }
}
