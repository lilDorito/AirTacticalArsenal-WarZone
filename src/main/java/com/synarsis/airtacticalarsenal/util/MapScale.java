package com.synarsis.airtacticalarsenal.util;

import net.minecraft.world.level.border.WorldBorder;

public final class MapScale {

    public static final int RADAR_CACHE_SIZE = 1536;
    public static final int ROUTE_CACHE_SIZE = 2048;

    private MapScale() {}

    public static int radarBpp(int radarRange) {
        int half = RADAR_CACHE_SIZE / 2;
        return Math.max(2, (radarRange + half - 1) / half);
    }

    public static int radarBpp(int radarRange, int borderReach) {
        int half = RADAR_CACHE_SIZE / 2;
        return Math.min(radarBpp(radarRange), Math.max(1, (borderReach + half - 1) / half));
    }

    public static int routeBpp(int maxRoute) {
        int half = ROUTE_CACHE_SIZE / 2;
        return Math.max(10, (maxRoute + half - 1) / half);
    }

    public static int routeBpp(int maxRoute, int borderReach) {
        int half = ROUTE_CACHE_SIZE / 2;
        return Math.min(routeBpp(maxRoute), Math.max(1, (borderReach + half - 1) / half));
    }

    public static int borderReach(WorldBorder border, double x, double z) {
        if (border == null) {
            return Integer.MAX_VALUE / 4;
        }
        double reach = Math.max(Math.max(x - border.getMinX(), border.getMaxX() - x),
                Math.max(z - border.getMinZ(), border.getMaxZ() - z));
        return (int) Math.min(Integer.MAX_VALUE / 4, Math.ceil(Math.max(64.0, reach)));
    }

    public static int reuseRadius(int bpp) {
        return Math.min(500, bpp * 256);
    }

    public static double maxRouteScale() {
        double max = 1.0;
        for (double value : com.synarsis.airtacticalarsenal.api.DroneVariants.routeScales().values()) {
            max = Math.max(max, value);
        }
        return max;
    }

    public static int serverRadarBpp(WorldBorder border, double x, double z) {
        return radarBpp(com.synarsis.airtacticalarsenal.config.ShahedConfig.getRadarRange(), borderReach(border, x, z));
    }

    public static int serverMaxRoute() {
        return Math.max(com.synarsis.airtacticalarsenal.config.ShahedConfig.getShahedMaxRouteDistance(),
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getOrlanMaxRouteDistance());
    }

    public static int serverRouteBppLimit() {
        return routeBpp((int) Math.ceil(serverMaxRoute() * maxRouteScale()));
    }
}
