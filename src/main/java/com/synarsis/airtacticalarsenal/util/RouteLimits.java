package com.synarsis.airtacticalarsenal.util;

import com.synarsis.airtacticalarsenal.config.ShahedConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class RouteLimits {

    private RouteLimits() {}

    public static double pathLength(BlockPos start, List<BlockPos> points) {
        double total = 0;
        double px = start.getX();
        double pz = start.getZ();
        for (BlockPos p : points) {
            double dx = p.getX() - px;
            double dz = p.getZ() - pz;
            total += Math.sqrt(dx * dx + dz * dz);
            px = p.getX();
            pz = p.getZ();
        }
        return total;
    }

    public static double pathLength(BlockPos start, List<BlockPos> waypoints, BlockPos target) {
        double total = pathLength(start, waypoints);
        BlockPos last = waypoints.isEmpty() ? start : waypoints.get(waypoints.size() - 1);
        double dx = target.getX() - last.getX();
        double dz = target.getZ() - last.getZ();
        return total + Math.sqrt(dx * dx + dz * dz);
    }

    public static int shahedMax() {
        return ShahedConfig.getShahedMaxRouteDistance();
    }

    public static int orlanMax() {
        return ShahedConfig.getOrlanMaxRouteDistance();
    }

    public static int shahedMax(String kind) {
        return scaled(shahedMax(), kind);
    }

    public static int orlanMax(String kind) {
        return scaled(orlanMax(), kind);
    }

    public static int scaled(int base, String kind) {
        return (int) Math.round(base * com.synarsis.airtacticalarsenal.api.DroneVariants.routeScale(kind));
    }

    public static boolean check(ServerPlayer player, double length, int max) {
        if (length <= max) return true;
        player.displayClientMessage(Component.translatable("message.ata.route.too_long",
                (int) Math.round(length), max), true);
        return false;
    }
}
