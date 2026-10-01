package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.config.ShahedConfig;

public final class ClientRouteLimits {

    private static volatile int shahedMax = 10000;
    private static volatile int orlanMax = 10000;
    private static volatile int iskanderMin = -1;
    private static volatile int iskanderMax = -1;

    private ClientRouteLimits() {}

    public static void set(int shahed, int orlan, int iskMin, int iskMax) {
        shahedMax = shahed;
        orlanMax = orlan;
        iskanderMin = iskMin;
        iskanderMax = iskMax;
    }

    public static int shahed() {
        return shahedMax;
    }

    public static int orlan() {
        return orlanMax;
    }

    public static int iskanderMin() {
        int v = iskanderMin;
        return v > 0 ? v : ShahedConfig.getIskanderMinDistance();
    }

    public static int iskanderMax() {
        int v = iskanderMax;
        return v > 0 ? v : ShahedConfig.getIskanderMaxRange();
    }
}
