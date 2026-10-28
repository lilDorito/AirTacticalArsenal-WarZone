package com.synarsis.airtacticalarsenal.salvo;

import com.synarsis.airtacticalarsenal.config.ShahedConfig;
import com.synarsis.airtacticalarsenal.entity.UavLauncherEntity;
import com.synarsis.airtacticalarsenal.util.LauncherZoneValidator;
import com.synarsis.airtacticalarsenal.util.RouteLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SalvoScheduler {
    public static final int MAX_LAUNCHERS = 10;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private record Pending(UUID player, BlockPos launcher, boolean strike, BlockPos offset, long dueTick,
                           List<BlockPos> route, boolean inherited) {
    }

    private static final List<Pending> QUEUE = new ArrayList<>();

    private SalvoScheduler() {
    }

    public static void schedule(ServerPlayer player, boolean strike, List<BlockPos> positions, int gapTicks, int spread,
                                boolean timeOnTarget, BlockPos mainPos) {
        ServerLevel level = player.serverLevel();
        List<BlockPos> mainRoute = null;
        UavLauncherEntity main = mainPos != null ? find(level, mainPos) : null;
        if (main != null && main.hasSavedRoute()) {
            mainRoute = new ArrayList<>(main.getSavedRoute());
        }
        if (mainRoute == null) {
            for (BlockPos pos : positions) {
                UavLauncherEntity candidate = find(level, pos);
                if (candidate != null && candidate.hasSavedRoute()) {
                    mainRoute = new ArrayList<>(candidate.getSavedRoute());
                    break;
                }
            }
        }
        List<UavLauncherEntity> launchers = new ArrayList<>();
        List<List<BlockPos>> routes = new ArrayList<>();
        List<Boolean> inherited = new ArrayList<>();
        for (BlockPos pos : positions) {
            if (launchers.size() >= MAX_LAUNCHERS) {
                break;
            }
            UavLauncherEntity launcher = find(level, pos);
            if (launcher == null || launcher.isLaunching()) {
                continue;
            }
            if (strike ? !launcher.hasStrikeDrone() : !launcher.hasReconDrone()) {
                continue;
            }
            if (com.synarsis.airtacticalarsenal.api.DroneVariants.interceptor(launcher.getDroneKind())) {
                continue;
            }
            boolean own = launcher.hasSavedRoute();
            if (!own && mainRoute == null) {
                continue;
            }
            launchers.add(launcher);
            routes.add(own ? new ArrayList<>(launcher.getSavedRoute()) : new ArrayList<>(mainRoute));
            inherited.add(!own);
        }
        if (launchers.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.ata.salvo.none"), true);
            return;
        }
        int n = launchers.size();
        long[] delays = new long[n];
        if (timeOnTarget) {
            double[] eta = new double[n];
            double longest = 0.0;
            for (int i = 0; i < n; i++) {
                UavLauncherEntity launcher = launchers.get(i);
                double length = RouteLimits.pathLength(launcher.blockPosition(), routes.get(i));
                eta[i] = length / Math.max(0.2, launcher.plannedCruiseSpeed(level));
                longest = Math.max(longest, eta[i]);
            }
            for (int i = 0; i < n; i++) {
                delays[i] = Math.round(longest - eta[i]);
            }
        } else {
            for (int i = 0; i < n; i++) {
                delays[i] = (long) i * Math.max(0, gapTicks);
            }
        }
        long now = level.getGameTime();
        for (int i = 0; i < n; i++) {
            BlockPos offset = BlockPos.ZERO;
            if (spread > 0 && n > 1) {
                double r = spread * Math.sqrt((i + 0.5) / n);
                double a = i * GOLDEN_ANGLE;
                offset = new BlockPos((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r));
            }
            QUEUE.add(new Pending(player.getUUID(), launchers.get(i).blockPosition(), strike, offset, now + delays[i],
                    routes.get(i), inherited.get(i)));
        }
        long last = 0;
        for (long d : delays) {
            last = Math.max(last, d);
        }
        player.displayClientMessage(Component.translatable("message.ata.salvo.started", n, (int) Math.ceil(last / 20.0)), true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUE.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Pending> it = QUEUE.iterator();
        List<Pending> due = new ArrayList<>();
        while (it.hasNext()) {
            Pending p = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(p.player());
            if (player == null) {
                it.remove();
                continue;
            }
            if (player.serverLevel().getGameTime() >= p.dueTick()) {
                due.add(p);
                it.remove();
            }
        }
        for (Pending p : due) {
            ServerPlayer player = server.getPlayerList().getPlayer(p.player());
            if (player != null) {
                launch(player, p);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        QUEUE.clear();
    }

    private static void launch(ServerPlayer player, Pending p) {
        ServerLevel level = player.serverLevel();
        UavLauncherEntity launcher = find(level, p.launcher());
        if (launcher == null || launcher.isLaunching() || p.route() == null || p.route().isEmpty()) {
            return;
        }
        if (player.position().distanceTo(launcher.position()) > ShahedConfig.getTabletLauncherRange()) {
            player.displayClientMessage(Component.translatable("message.ata.salvo.out_of_range"), true);
            return;
        }
        List<BlockPos> route = new ArrayList<>(p.route());
        if (p.inherited()) {
            while (route.size() > 1
                    && !LauncherZoneValidator.isTargetInValidZone(launcher.blockPosition(), launcher.getYRot(), route.get(0))) {
                route.remove(0);
            }
        }
        BlockPos last = route.get(route.size() - 1);
        route.set(route.size() - 1, last.offset(p.offset()));
        BlockPos target = route.get(route.size() - 1);
        List<BlockPos> waypoints = route.size() > 1 ? new ArrayList<>(route.subList(0, route.size() - 1)) : new ArrayList<>();
        if (!LauncherZoneValidator.isTargetInValidZone(launcher.blockPosition(), launcher.getYRot(), route.get(0))) {
            player.displayClientMessage(Component.translatable("message.ata.salvo.route_outside"), true);
            return;
        }
        double length = RouteLimits.pathLength(launcher.blockPosition(), route);
        if (p.strike()) {
            if (!launcher.hasStrikeDrone() || length > RouteLimits.shahedMax(launcher.getDroneKind()) + Math.abs(p.offset().getX()) + Math.abs(p.offset().getZ())) {
                return;
            }
            launcher.startLaunchSequenceWithRoute(player, target, "low", waypoints, 0);
        } else {
            if (!launcher.hasReconDrone() || length > RouteLimits.orlanMax(launcher.getDroneKind()) + Math.abs(p.offset().getX()) + Math.abs(p.offset().getZ())) {
                return;
            }
            launcher.startReconLaunch(player, target, waypoints.isEmpty() ? null : waypoints);
        }
    }

    private static UavLauncherEntity find(ServerLevel level, BlockPos pos) {
        List<UavLauncherEntity> launchers = level.getEntitiesOfClass(UavLauncherEntity.class, new AABB(pos).inflate(2.0));
        return launchers.isEmpty() ? null : launchers.get(0);
    }
}
