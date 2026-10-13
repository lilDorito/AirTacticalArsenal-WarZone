package com.synarsis.airtacticalarsenal.radar.vehicle;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

public final class VehicleRadarProfile {

    public enum Kind { MCA, AESA }

    public enum Shape { ROUND, CONE }

    public final Kind kind;
    public final Shape shape;
    public final int range;
    public final int halfAngle;
    public final int sweepTicks;
    public final int refreshTicks;
    public final int minAltitude;

    public VehicleRadarProfile(Kind kind, Shape shape, int range, int halfAngle, int sweepTicks, int refreshTicks,
                               int minAltitude) {
        this.kind = kind;
        this.shape = shape;
        this.range = range;
        this.halfAngle = halfAngle;
        this.sweepTicks = sweepTicks;
        this.refreshTicks = refreshTicks;
        this.minAltitude = minAltitude;
    }

    public static VehicleRadarProfile fromJson(JsonObject json) {
        Kind kind = "aesa".equalsIgnoreCase(GsonHelper.getAsString(json, "type", "mca")) ? Kind.AESA : Kind.MCA;
        Shape shape = "round".equalsIgnoreCase(GsonHelper.getAsString(json, "shape", "cone")) ? Shape.ROUND : Shape.CONE;
        int range = Mth.clamp(GsonHelper.getAsInt(json, "range", 1000), 32, 8000);
        int halfAngle = shape == Shape.ROUND ? 180 : Mth.clamp(GsonHelper.getAsInt(json, "half_angle", 60), 5, 90);
        int sweepTicks = Mth.clamp(Math.round(GsonHelper.getAsFloat(json, "sweep_seconds", 2.0f) * 20), 4, 1200);
        int refreshTicks = Mth.clamp(Math.round(20.0f / Math.max(0.05f, GsonHelper.getAsFloat(json, "refresh_per_second", 4.0f))), 1, 400);
        int minAltitude = GsonHelper.getAsInt(json, "min_altitude", kind == Kind.MCA ? 30 : 0);
        return new VehicleRadarProfile(kind, shape, range, halfAngle, sweepTicks, refreshTicks, Math.max(0, minAltitude));
    }
}
