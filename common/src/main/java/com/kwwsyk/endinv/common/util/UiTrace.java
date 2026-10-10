package com.kwwsyk.endinv.common.util;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.ClientModInfo;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**Logs the state of the EndInv screen, for when something about its layout is wrong.<br>
 * The screen's geometry is decided by several pieces - the menu's row count, the crafter being on
 * or off, the page framework and a background renderer that takes its size when it is built - and
 * when they disagree the result is a mess of overlapping layers that says nothing about which one
 * is out of step. These lines name every value at every point it changes.
 * <p>Off by default. Turn it on with <b>Screen debug</b> in the mod's settings screen, or by
 * starting the game with {@code -Dendinv.uiTrace=true}.
 */
public final class UiTrace {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String FORCE_PROPERTY = "endinv.uiTrace";

    private UiTrace() {
    }

    public static boolean on() {
        if (Boolean.getBoolean(FORCE_PROPERTY)) {
            return true;
        }
        if (!ModInfo.isClientLoaded()) {
            return false;
        }
        var config = ClientModInfo.getClientConfig();
        return config != null && config.screenDebugging().get();
    }

    public static void log(String format, Object... args) {
        if (on()) {
            LOGGER.info("[endinv ui] " + format, args);
        }
    }

    /**For values that are looked at every frame: says something only when they change. */
    public static final class Watch {
        private final String name;
        private String last;

        public Watch(String name) {
            this.name = name;
        }

        public void log(String format, Object... args) {
            if (!UiTrace.on()) {
                return;
            }
            String now = String.format(format, args);
            if (now.equals(last)) {
                return;
            }
            last = now;
            LOGGER.info("[endinv ui] {} {}", name, now);
        }
    }
}
