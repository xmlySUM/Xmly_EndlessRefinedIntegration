package com.kwwsyk.endinv.common.client.option;

import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.client.ClientModInfo;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.network.payloads.PageData;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.util.SortType;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**Caches that keep pages' options, including:<br>
 * <ul>
 *     <li>options toggled in page (sort, search...)</li>
 *     <li>data synced from server (attaching)</li>
 * </ul>
 */
public final class CachedConfig {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static String pageRegKey = PageType.DEFAULT_KEY;
    private static int rows = 6;
    private static int columns = 9;
    private static SortType sortType = SortType.DEFAULT;
    private static boolean reverseSort = false;
    private static String searching = "";

    private static boolean attaching = true;
    private static boolean autoPicking = false;

    private static SyncedConfig cachedFlags = SyncedConfig.DEFAULT;
    private static PageData cachedLayout = PageData.DEFAULT;
    private static boolean layoutInitialized;

    private CachedConfig() {
    }

    public static void readAndSyncClientConfigToServer(boolean refreshLayout) {
        if (refreshLayout || !layoutInitialized) {
            cachedLayout = resolveLayout(null, true);
        }
        synchronizeFlags();
    }

    public static void synchronizeFlags() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        //client attaching data
        IClientConfig config = ClientModInfo.getClientConfig();
        attaching = config.attaching().get();
        //server attaching data is prior
        SyncedConfig serverFlags = ModRegistries.NbtAttachments.getSyncedConfig().getWith(player);
        if(serverFlags==null) {
            LOGGER.warn("Player {} has no synced config.", player.getName().getString());
            return;
        }
        if (!serverFlags.equals(cachedFlags)) {
            cachedFlags = serverFlags;
        }
    }

    public static void acceptServerFlags(SyncedConfig flags) {
        cachedFlags = flags;
    }

    /**
     * Compute and update suit row and column count to let AttachingScreen View can fit the {@code screen}
     * @param ofMenu to specially suit {@code EndlessInventoryMenu}'s view.
     * @return {@link PageData} recode with updated row, column data.
     */
    public static PageData resolveLayout(@Nullable AbstractContainerScreen<?> screen, boolean ofMenu) {
        IClientConfig config = ClientModInfo.getClientConfig();

        int rows = config.rows().get();
        if (rows <= 0) {
            rows = config.calculateDefaultRowCount(ofMenu);
        }
        if (rows <= 0) {
            rows = cachedLayout.rows() > 0 ? cachedLayout.rows() : PageData.DEFAULT.rows();
        }

        int columns = config.columns().get();
        if (columns <= 0) {
            columns = PageData.DEFAULT.columns();
        }
        if (screen != null && config.autoSuitColumn().get()) {
            int fitted = config.calculateSuitInColumnCount(screen);
            columns = Math.min(columns, fitted);
        }
        if (columns <= 0) {
            columns = cachedLayout.columns() > 0 ? cachedLayout.columns() : PageData.DEFAULT.columns();
        }

        // Build layout using cached sort/search/page to keep state across screen reopen
        CachedConfig.rows = rows;
        CachedConfig.columns = columns;
        cachedLayout = new PageData(pageRegKey, rows, columns, sortType, reverseSort, searching);
        layoutInitialized = true;
        return cachedLayout;
    }

    public static PageData currentLayout() {
        if (!layoutInitialized) {
            cachedLayout = resolveLayout(null, true);
        }
        return cachedLayout;
    }

    public static void updateLayoutWith(PageData newLayout) {
        // Keep local cached knobs in sync as the UI changes
        pageRegKey = newLayout.pageRegKey();
        rows = newLayout.rows();
        columns = newLayout.columns();
        sortType = newLayout.sortType();
        reverseSort = newLayout.reverseSort();
        searching = newLayout.search();

        cachedLayout = new PageData(pageRegKey, rows, columns, sortType, reverseSort, searching);
        layoutInitialized = true;
    }

    // Lightweight in-memory anchors for UI state
    public static SortType sortType() { return sortType; }
    public static void setSortType(SortType value) {
        sortType = value;
        cachedLayout = new PageData(pageRegKey, rows, columns, sortType, reverseSort, searching);
    }

    public static boolean reverseSort() { return reverseSort; }
    public static void setReverseSort(boolean value) {
        reverseSort = value;
        cachedLayout = new PageData(pageRegKey, rows, columns, sortType, reverseSort, searching);
    }

    public static String searching() { return searching; }
    public static void setSearching(String value) {
        searching = value;
        cachedLayout = new PageData(pageRegKey, rows, columns, sortType, reverseSort, searching);
    }

    public static String pageKey() { return pageRegKey; }
    public static void setDisplayingPageKey(String key) {
        pageRegKey = key;
        cachedLayout = new PageData(pageRegKey, rows, columns, sortType, reverseSort, searching);
    }
}
