package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.config.ShahedConfig;

public final class ClientRouteLimits {

    private static volatile int shahedMax = -1;
    private static volatile int orlanMax = -1;
    private static volatile int radarRange = -1;
    private static volatile int iskanderMin = -1;
    private static volatile int iskanderMax = -1;
    private static volatile int tabletRange = -1;

    private ClientRouteLimits() {}

    public static void set(int shahed, int orlan, int iskMin, int iskMax, int radar, int tablet) {
        tabletRange = tablet;
        radarRange = radar;
        shahedMax = shahed;
        orlanMax = orlan;
        iskanderMin = iskMin;
        iskanderMax = iskMax;
    }

    public static int shahed() {
        int v = shahedMax;
        return v > 0 ? v : ShahedConfig.getShahedMaxRouteDistance();
    }

    public static int orlan() {
        int v = orlanMax;
        return v > 0 ? v : ShahedConfig.getOrlanMaxRouteDistance();
    }

    public static int radarRange() {
        int v = radarRange;
        return v > 0 ? v : ShahedConfig.getRadarRange();
    }

    public static int radarBpp() {
        return com.synarsis.airtacticalarsenal.util.MapScale.radarBpp(radarRange());
    }

    public static int routeBpp() {
        return com.synarsis.airtacticalarsenal.util.MapScale.routeBpp(Math.max(shahed(), orlan()));
    }

    public static int iskanderMin() {
        int v = iskanderMin;
        return v > 0 ? v : ShahedConfig.getIskanderMinDistance();
    }

    public static int tabletRange() {
        int v = tabletRange;
        return v > 0 ? v : ShahedConfig.getTabletLauncherRange();
    }

    public static int iskanderMax() {
        int v = iskanderMax;
        return v > 0 ? v : ShahedConfig.getIskanderMaxRange();
    }
}
