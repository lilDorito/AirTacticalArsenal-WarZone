package com.synarsis.airtacticalarsenal.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CameraTrackingGuard {
    private static final double KEEP_RADIUS = 10 * 16.0;
    private static final Map<UUID, Entity> STRIKE_VIEWERS = new ConcurrentHashMap<>();
    private static final List<Pending> PENDING = new ArrayList<>();

    private record Pending(ServerPlayer player, Entity target) {
    }

    private CameraTrackingGuard() {
    }

    public static void setStrikeViewer(UUID viewer, Entity drone) {
        if (drone == null) {
            STRIKE_VIEWERS.remove(viewer);
        } else {
            STRIKE_VIEWERS.put(viewer, drone);
        }
    }

    public static Entity strikeDroneOf(UUID viewer) {
        return STRIKE_VIEWERS.get(viewer);
    }

    public static void clearStrikeViewer(UUID viewer, Entity drone) {
        STRIKE_VIEWERS.remove(viewer, drone);
    }

    public static Entity viewedDrone(ServerPlayer player) {
        Entity strike = STRIKE_VIEWERS.get(player.getUUID());
        if (strike != null && strike.isAlive()) {
            return strike;
        }
        for (OrlanEntity orlan : OrlanEntity.getActiveOrlansWithCamera()) {
            if (orlan.isLinkedToPlayer(player.getUUID())) {
                return orlan;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onStopTracking(PlayerEvent.StopTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Entity target = event.getTarget();
        if (!target.isAlive()) {
            return;
        }
        Entity drone = viewedDrone(player);
        if (drone == null || drone.level() != target.level()) {
            return;
        }
        double dx = target.getX() - drone.getX();
        double dz = target.getZ() - drone.getZ();
        if (target == drone || dx * dx + dz * dz <= KEEP_RADIUS * KEEP_RADIUS) {
            synchronized (PENDING) {
                PENDING.add(new Pending(player, target));
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        com.synarsis.airtacticalarsenal.network.OrlanCameraViewPacket.tickPendingReturns(event.getServer());
        List<Pending> batch;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) {
                return;
            }
            batch = new ArrayList<>(PENDING);
            PENDING.clear();
        }
        for (Pending pending : batch) {
            ServerPlayer player = pending.player();
            Entity target = pending.target();
            if (player.hasDisconnected() || !target.isAlive() || viewedDrone(player) == null) {
                continue;
            }
            Packet<?> add;
            try {
                add = target.getAddEntityPacket();
            } catch (RuntimeException e) {
                continue;
            }
            if (add == null) {
                continue;
            }
            player.connection.send(add);
            var data = target.getEntityData().getNonDefaultValues();
            if (data != null) {
                player.connection.send(new ClientboundSetEntityDataPacket(target.getId(), data));
            }
            player.connection.send(new ClientboundTeleportEntityPacket(target));
            player.connection.send(new ClientboundSetEntityMotionPacket(target));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STRIKE_VIEWERS.clear();
        ShahedEntity.clearRegistry();
        synchronized (PENDING) {
            PENDING.clear();
        }
    }
}
