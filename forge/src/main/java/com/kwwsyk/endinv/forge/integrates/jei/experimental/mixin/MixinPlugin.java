package com.kwwsyk.endinv.forge.integrates.jei.experimental.mixin;

import com.kwwsyk.endinv.forge.StartupConfig;
import com.mojang.logging.LogUtils;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();


    /**
     * Called after the plugin is instantiated, do any setup here.
     *
     * @param mixinPackage The mixin root package from the config
     */
    @Override
    public void onLoad(String mixinPackage) {

    }

    /**
     * Called only if the "referenceMap" key in the config is <b>not</b> set.
     * This allows the refmap file name to be supplied by the plugin
     * programatically if desired. Returning <code>null</code> will revert to
     * the default behaviour of using the default refmap json file.
     *
     * @return Path to the refmap resource or null to revert to the default
     */
    @Override
    public String getRefMapperConfig() {
        return null;
    }

    /**
     * Called during mixin intialisation, allows this plugin to control whether
     * a specific will be applied to the specified target. Returning false will
     * remove the target from the mixin's target set, and if all targets are
     * removed then the mixin will not be applied at all.
     *
     * @param targetClassName Fully qualified class name of the target class
     * @param mixinClassName  Fully qualified class name of the mixin
     * @return True to allow the mixin to be applied, or false to remove it from
     * target's mixin set
     */
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            return StartupConfig.enableJeiAttachedTransfer();
        } catch (IllegalStateException e) {
            LOGGER.warn("JEI mixin config accessed before load; reading file directly.");
            // Fallback to reading the COMMON config directly before Forge loads it
            // Try typical dev and runtime locations
            String fileName = "endless_inventory-common.toml";
            java.nio.file.Path[] candidates = new java.nio.file.Path[]{
                    java.nio.file.Paths.get("run", "config", fileName),
                    java.nio.file.Paths.get("config", fileName),
                    java.nio.file.Paths.get(fileName)
            };
            for (java.nio.file.Path p : candidates) {
                try {
                    if (java.nio.file.Files.isRegularFile(p)) {
                        String content = java.nio.file.Files.readString(p);
                        Boolean val = parseEnableFromToml(content);
                        if (val != null) {
                            LOGGER.info("JEI mixin config read from {}: experimental.jeiAttachedRecipeTransfer={}", p, val);
                            return val;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            LOGGER.warn("JEI mixin config not found or unreadable; defaulting to disabled.");
            return false;
        }
    }

    private static Boolean parseEnableFromToml(String content) {
        // Minimal TOML section/key parser for: [experimental] jeiAttachedRecipeTransfer = true/false
        boolean inExperimental = false;
        for (String rawLine : content.split("\r?\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("[")) {
                // new section
                int r = line.indexOf(']');
                if (r > 1) {
                    String sect = line.substring(1, r).trim();
                    inExperimental = "experimental".equals(sect);
                } else {
                    inExperimental = false;
                }
                continue;
            }
            if (!inExperimental) continue;
            // Strip inline comments
            int hash = line.indexOf('#');
            if (hash >= 0) line = line.substring(0, hash).trim();
            if (line.startsWith("jeiAttachedRecipeTransfer")) {
                int eq = line.indexOf('=');
                if (eq >= 0) {
                    String rhs = line.substring(eq + 1).trim();
                    if (rhs.equalsIgnoreCase("true")) return Boolean.TRUE;
                    if (rhs.equalsIgnoreCase("false")) return Boolean.FALSE;
                }
            }
        }
        return null;
    }

    /**
     * Called after all configurations are initialised, this allows this plugin
     * to observe classes targetted by other mixin configs and optionally remove
     * targets from its own set. The set myTargets is a direct view of the
     * targets collection in this companion config and keys may be removed from
     * this set to suppress mixins in this config which target the specified
     * class. Adding keys to the set will have no effect.
     *
     * @param myTargets    Target class set from the companion config
     * @param otherTargets Target class set incorporating targets from all other
     *                     configs, read-only
     */
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    /**
     * After mixins specified in the configuration have been processed, this
     * method is called to allow the plugin to add any additional mixins to
     * load. It should return a list of mixin class names or return null if the
     * plugin does not wish to append any mixins of its own.
     *
     * @return additional mixins to apply
     */
    @Override
    public List<String> getMixins() {
        return null;
    }

    /**
     * Called immediately <b>before</b> a mixin is applied to a target class,
     * allows any pre-application transformations to be applied.
     *
     * @param targetClassName Transformed name of the target class
     * @param targetClass     Target class tree
     * @param mixinClassName  Name of the mixin class
     * @param mixinInfo       Information about this mixin
     */
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    /**
     * Called immediately <b>after</b> a mixin is applied to a target class,
     * allows any post-application transformations to be applied.
     *
     * @param targetClassName Transformed name of the target class
     * @param targetClass     Target class tree
     * @param mixinClassName  Name of the mixin class
     * @param mixinInfo       Information about this mixin
     */
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
