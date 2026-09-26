package com.xmly.endlessrefined.hotbar;

public enum HotbarDisplayMode {

    VANILLA_ONLY("endless_refined.display.vanilla_only"),

    ONE_WINDOWS("endless_refined.display.one_windows"),

    TWO_ROWS_COLUMNS("endless_refined.display.two_rows_columns"),

    ONE_ROW_WINDOWS("endless_refined.display.one_row_windows"),

    ONE_COL_WINDOWS("endless_refined.display.one_col_windows"),

    ROW_COLUMNS_WINDOW("endless_refined.display.row_columns_window"),

    COLUMN_ROWS_WINDOW("endless_refined.display.column_rows_window");

    private final String translationKey;

    HotbarDisplayMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public HotbarDisplayMode next() {
        HotbarDisplayMode[] modes = values();

        return modes[(ordinal() + 1) % modes.length];
    }

    public int selectionGroups(int configuredGroups) {
        return this == VANILLA_ONLY ? 1 : configuredGroups;
    }

    public int windowCounts(int configuredGroups) {
        return switch (this) {
            case VANILLA_ONLY, ONE_WINDOWS, ONE_ROW_WINDOWS -> 1;
            case TWO_ROWS_COLUMNS, ONE_COL_WINDOWS -> configuredGroups;
            case ROW_COLUMNS_WINDOW, COLUMN_ROWS_WINDOW -> Math.min(2, configuredGroups);
        };
    }

    public int windowHeight(int configuredGroups) {
        return switch (this) {
            case VANILLA_ONLY, ONE_WINDOWS, ONE_ROW_WINDOWS, ROW_COLUMNS_WINDOW -> 1;
            case TWO_ROWS_COLUMNS -> Math.min(2, (configuredGroups + 1) / 2);
            case ONE_COL_WINDOWS -> Math.min(4, configuredGroups);
            case COLUMN_ROWS_WINDOW -> Math.min(2, configuredGroups);
        };
    }

    public int windowStart(int activeGroup, int configuredGroups) {
        int height = windowCounts(configuredGroups);
        int lowestStart = Math.max(0, configuredGroups - height);

        return Math.max(0, Math.min(activeGroup - (height - 1), lowestStart));
    }

    public int groupForRow(int row, int activeGroup, int configuredGroups) {
        return windowStart(activeGroup, configuredGroups) + row;
    }

    public int rowForActiveGroup(int activeGroup, int configuredGroups) {
        return activeGroup - windowStart(activeGroup, configuredGroups);
    }
}
