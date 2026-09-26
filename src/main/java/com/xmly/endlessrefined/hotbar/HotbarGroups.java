package com.xmly.endlessrefined.hotbar;

public final class HotbarGroups {

    public static final int MAX_GROUPS = 4;
    public static final int DIGITS = 9;
    private static final int BAND_SIZE = 3;

    private HotbarGroups() {
    }

    public static int clampConfigured(int groups) {
        return Math.max(1, Math.min(MAX_GROUPS, groups));
    }

    public static int[] visibleRows(int configuredGroups, int digit) {
        int groups = clampConfigured(configuredGroups);

        if (digit < 1 || digit > DIGITS || groups < 2) {
            return new int[0];
        }

        int extra = groups - 1;
        int[] visible = new int[extra];

        if (extra == 1) {
            visible[0] = digit;
        } else if (extra == 2) {
            visible[0] = digit;
            visible[1] = digit == DIGITS ? 1 : digit + 1;
        } else {
            int bandStart = ((digit - 1) / BAND_SIZE) * BAND_SIZE + 1;

            for (int i = 0; i < extra; i++) {
                visible[i] = bandStart + i;
            }
        }

        return visible;
    }

    public static int engineGroupForDigit(int configuredGroups, int digit) {
        int[] visible = visibleRows(configuredGroups, digit);

        for (int i = 0; i < visible.length; i++) {
            if (visible[i] == digit) {
                return i + 1;
            }
        }

        return 0;
    }
}
