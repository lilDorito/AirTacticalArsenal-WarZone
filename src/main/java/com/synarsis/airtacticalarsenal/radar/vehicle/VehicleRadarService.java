package com.synarsis.airtacticalarsenal.radar.vehicle;

import com.synarsis.airtacticalarsenal.config.ShahedConfig;
import com.synarsis.airtacticalarsenal.network.NetworkHandler;
import com.synarsis.airtacticalarsenal.network.VehicleRadarPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "ata")
public final class VehicleRadarService {

    private static final int MCA_SEND_INTERVAL = 2;

    private static final Map<UUID, Beam> BEAMS = new HashMap<>();
    private static Set<UUID> activePlayers = new HashSet<>();

    private VehicleRadarService() {}

    private static final class Beam {
        float angle;
        int dir = 1;
        int ticks;
        final List<VehicleRadarPacket.Contact> pending = new ArrayList<>();
    }

    private record Air(Entity entity, byte type) {}

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(VehicleRadarProfiles.INSTANCE);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();

        Set<UUID> stillActive = new HashSet<>();
        Set<UUID> liveVehicles = new HashSet<>();
        boolean enabled = ShahedConfig.areVehicleRadarsEnabled() && VehicleRadarProfiles.INSTANCE.hasAnyRadar();

        if (enabled) {
            for (ServerLevel level : server.getAllLevels()) {
                List<ServerPlayer> players = level.players();
                if (players.isEmpty()) continue;
                Map<Entity, List<ServerPlayer>> crews = new IdentityHashMap<>();
                Map<Entity, VehicleRadarProfile> profiles = new IdentityHashMap<>();
                for (ServerPlayer player : players) {
                    if (player.isSpectator()) continue;
                    Entity root = player.getRootVehicle();
                    if (root == player) continue;
                    VehicleRadarProfile profile = profiles.containsKey(root) ? profiles.get(root)
                            : VehicleRadarProfiles.INSTANCE.radarFor(root.getType());
                    profiles.put(root, profile);
                    if (profile == null) continue;
                    crews.computeIfAbsent(root, r -> new ArrayList<>()).add(player);
                }
                if (crews.isEmpty()) continue;

                List<Air> air = collect(level);
                for (Map.Entry<Entity, List<ServerPlayer>> crew : crews.entrySet()) {
                    Entity vehicle = crew.getKey();
                    VehicleRadarProfile profile = profiles.get(vehicle);
                    Beam beam = BEAMS.computeIfAbsent(vehicle.getUUID(), u -> new Beam());
                    liveVehicles.add(vehicle.getUUID());
                    for (ServerPlayer player : crew.getValue()) {
                        stillActive.add(player.getUUID());
                    }
                    VehicleRadarPacket packet = scan(level, vehicle, profile, beam, air, crew.getValue().get(0));
                    if (packet == null) continue;
                    for (ServerPlayer player : crew.getValue()) {
                        NetworkHandler.sendToPlayer(player, packet);
                    }
                }
            }
        }

        for (UUID id : activePlayers) {
            if (stillActive.contains(id)) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) NetworkHandler.sendToPlayer(player, VehicleRadarPacket.off());
        }
        activePlayers = stillActive;
        BEAMS.keySet().retainAll(liveVehicles);
    }

    private static List<Air> collect(ServerLevel level) {
        List<Air> list = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e == null || e.isRemoved()) continue;
            byte type = com.synarsis.airtacticalarsenal.radar.RadarContacts.classify(e);
            if (type >= 0) list.add(new Air(e, type));
        }
        return list;
    }

    private static VehicleRadarPacket scan(ServerLevel level, Entity vehicle, VehicleRadarProfile p, Beam beam,
                                           List<Air> air, ServerPlayer viewer) {
        boolean mca = p.kind == VehicleRadarProfile.Kind.MCA;
        boolean cone = p.shape == VehicleRadarProfile.Shape.CONE;

        float yaw = vehicle.getYRot();
        double fx = -Mth.sin(yaw * Mth.DEG_TO_RAD);
        double fz = Mth.cos(yaw * Mth.DEG_TO_RAD);
        float nose = (float) Math.toDegrees(Math.atan2(fx, -fz));

        float prev = beam.angle;
        float sweptLo = 0;
        float sweptHi = 0;
        if (mca) {
            if (cone) {
                float step = 2.0f * p.halfAngle / p.sweepTicks;
                float raw = prev + beam.dir * step;
                if (raw > p.halfAngle) {
                    beam.angle = 2 * p.halfAngle - raw;
                    beam.dir = -1;
                    sweptLo = Math.min(prev, beam.angle);
                    sweptHi = p.halfAngle;
                } else if (raw < -p.halfAngle) {
                    beam.angle = -2 * p.halfAngle - raw;
                    beam.dir = 1;
                    sweptLo = -p.halfAngle;
                    sweptHi = Math.max(prev, beam.angle);
                } else {
                    beam.angle = raw;
                    sweptLo = Math.min(prev, raw);
                    sweptHi = Math.max(prev, raw);
                }
            } else {
                float step = 360.0f / p.sweepTicks;
                beam.angle = (prev + step) % 360.0f;
                sweptLo = prev;
                sweptHi = step;
            }
        }
        beam.ticks++;

        boolean sendNow = mca ? beam.ticks % MCA_SEND_INTERVAL == 0 : beam.ticks % p.refreshTicks == 0;
        if (!mca && !sendNow) return null;

        List<VehicleRadarPacket.Contact> out = mca ? beam.pending : new ArrayList<>();
        double ox = vehicle.getX();
        double oy = vehicle.getY();
        double oz = vehicle.getZ();

        for (Air a : air) {
            Entity e = a.entity();
            if (e == vehicle || e.getRootVehicle() == vehicle || e.onGround()) continue;
            double dx = e.getX() - ox;
            double dy = e.getY() - oy;
            double dz = e.getZ() - oz;
            double reach = p.range * VehicleRadarProfiles.INSTANCE.signatureFor(e.getType());
            if (dx * dx + dy * dy + dz * dz > reach * reach) continue;

            float bearing = (float) Math.toDegrees(Math.atan2(dx, -dz));
            if (bearing < 0) bearing += 360.0f;
            float rel = Mth.wrapDegrees(bearing - nose);
            if (cone && Math.abs(rel) > p.halfAngle) continue;

            if (mca) {
                if (cone) {
                    if (rel < sweptLo || rel > sweptHi) continue;
                } else {
                    float delta = bearing - sweptLo;
                    if (delta < 0) delta += 360.0f;
                    if (delta >= sweptHi) continue;
                }
                if (p.minAltitude > 0) {
                    int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(e.getX()), Mth.floor(e.getZ()));
                    if (e.getY() - ground < p.minAltitude) continue;
                }
            }

            out.add(new VehicleRadarPacket.Contact(e.getId(), a.type(), e.getX(), (float) e.getY(), e.getZ(),
                    e.getYRot(), com.synarsis.airtacticalarsenal.radar.RadarContacts.isFriendly(viewer.getTeam(), viewer, e)));
        }

        if (!sendNow) return null;
        List<VehicleRadarPacket.Contact> contacts = new ArrayList<>(out);
        if (mca) beam.pending.clear();
        return new VehicleRadarPacket(true, p.kind, p.shape, p.range, p.halfAngle, p.sweepTicks, p.refreshTicks,
                beam.angle, beam.dir, !mca, contacts);
    }
}
