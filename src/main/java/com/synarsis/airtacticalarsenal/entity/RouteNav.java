package com.synarsis.airtacticalarsenal.entity;

import net.minecraft.world.phys.Vec3;

public final class RouteNav {

    private RouteNav() {}

    public static boolean reached(Vec3 pos, Vec3 heading, Vec3 from, Vec3 point, double capture) {
        double dx = point.x - pos.x;
        double dz = point.z - pos.z;
        if (dx * dx + dz * dz < capture * capture) return true;
        if (from == null) return false;
        double sx = point.x - from.x;
        double sz = point.z - from.z;
        double len = Math.sqrt(sx * sx + sz * sz);
        if (len < 1.0) return true;
        double along = ((pos.x - from.x) * sx + (pos.z - from.z) * sz) / len;
        boolean behind = heading.x * dx + heading.z * dz < 0;
        return behind && along >= len - capture;
    }

    public static boolean insideTurn(Vec3 pos, Vec3 heading, Vec3 point, double speed, double turnDegPerTick) {
        double dx = point.x - pos.x;
        double dz = point.z - pos.z;
        double hl = Math.sqrt(heading.x * heading.x + heading.z * heading.z);
        if (hl < 1.0E-4 || turnDegPerTick <= 0) return false;
        double lateral = Math.abs(heading.x * dz - heading.z * dx) / hl;
        double radius = speed / Math.toRadians(turnDegPerTick);
        return dx * dx + dz * dz < 2.0 * radius * lateral;
    }
}
