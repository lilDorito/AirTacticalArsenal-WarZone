package com.synarsis.airtacticalarsenal.util;

public final class MapScale {

    public static final int RADAR_CACHE_SIZE = 1536;
    public static final int ROUTE_CACHE_SIZE = 2048;

    private MapScale() {}

    public static int radarBpp(int radarRange) {
        int half = RADAR_CACHE_SIZE / 2;
        return Math.max(2, (radarRange + half - 1) / half);
    }

    public static int routeBpp(int maxRoute) {
        int half = ROUTE_CACHE_SIZE / 2;
        return Math.max(10, (maxRoute + half - 1) / half);
    }

    public static int serverRadarBpp() {
        return radarBpp(com.synarsis.airtacticalarsenal.config.ShahedConfig.getRadarRange());
    }

    public static int serverRouteBpp() {
        return routeBpp(Math.max(com.synarsis.airtacticalarsenal.config.ShahedConfig.getShahedMaxRouteDistance(),
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getOrlanMaxRouteDistance()));
    }
}
