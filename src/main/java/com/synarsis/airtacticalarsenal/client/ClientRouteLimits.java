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

    private static volatile java.util.Map<String, Double> scales = java.util.Map.of();

    public static void setScales(java.util.Map<String, Double> newScales) {
        scales = java.util.Map.copyOf(newScales);
    }

    public static double scale(String kind) {
        if (kind == null || kind.isEmpty()) {
            return 1.0;
        }
        Double value = scales.get(kind);
        if (value == null) {
            return com.synarsis.airtacticalarsenal.api.DroneVariants.routeScale(kind);
        }
        return value > 0.0 ? value : 1.0;
    }

    public static int shahed(String kind) {
        return (int) Math.round(shahed() * scale(kind));
    }

    public static int orlan(String kind) {
        return (int) Math.round(orlan() * scale(kind));
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

    public static double maxScale() {
        double max = 1.0;
        for (double value : scales.values()) {
            max = Math.max(max, value);
        }
        for (double value : com.synarsis.airtacticalarsenal.api.DroneVariants.routeScales().values()) {
            max = Math.max(max, value);
        }
        return max;
    }

    public static int maxReach() {
        return (int) Math.ceil(Math.max(shahed(), orlan()) * maxScale());
    }

    public static int borderReach(double x, double z) {
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        return com.synarsis.airtacticalarsenal.util.MapScale.borderReach(level != null ? level.getWorldBorder() : null, x, z);
    }

    public static int routeBpp(int reach, double x, double z) {
        return com.synarsis.airtacticalarsenal.util.MapScale.routeBpp(reach, borderReach(x, z));
    }

    public static int radarBpp(double x, double z) {
        return com.synarsis.airtacticalarsenal.util.MapScale.radarBpp(radarRange(), borderReach(x, z));
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
