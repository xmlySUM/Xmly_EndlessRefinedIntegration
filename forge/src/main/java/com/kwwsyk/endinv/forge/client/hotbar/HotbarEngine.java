package com.kwwsyk.endinv.forge.client.hotbar;

import com.kwwsyk.endinv.common.ModInfo;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**Whether the multi-group hotbar runs at all, on this client.<br>
 * The hotbar is switched off in two cases. The standalone {@code quadhotbar} cannot coexist with
 * it - both redirect the same instruction - and a server may have turned the whole feature off, in
 * which case it says so over {@link com.kwwsyk.endinv.forge.network.payloads.ServerSupportPayload}.
 * Either way the hotbar falls back to the vanilla nine slots instead of failing.
 */
public final class HotbarEngine {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**The mod this one forked its hotbar from and cannot run beside. */
    private static final String CONFLICTING_MOD = "quadhotbar";

    private static boolean conflictingModPresent = false;
    private static boolean serverAllowsHotbar = false;

    private HotbarEngine() {
    }

    public static void init() {
        if (ModList.get().isLoaded(CONFLICTING_MOD)) {
            conflictingModPresent = true;
            LOGGER.error("The standalone '{}' mod is installed. Both it and Endless Inventory redirect "
                            + "Inventory#getSelectionSize inside ServerGamePacketListenerImpl#handleSetCarriedItem, "
                            + "so they cannot both apply: the multi-group hotbar is disabled to leave '{}' working.",
                    CONFLICTING_MOD, CONFLICTING_MOD);
        }
    }

    /**True when the hotbar may be drawn and driven. */
    public static boolean isActive() {
        return !conflictingModPresent && serverAllowsHotbar;
    }

    static void setServerAllowsHotbar(boolean allowed) {
        serverAllowsHotbar = allowed;
    }

    static boolean isConflictingModPresent() {
        return conflictingModPresent;
    }

    /**Kept so the mixin plugin and the message above agree on what counts as a conflict. */
    public static String conflictingModId() {
        return CONFLICTING_MOD;
    }
}
