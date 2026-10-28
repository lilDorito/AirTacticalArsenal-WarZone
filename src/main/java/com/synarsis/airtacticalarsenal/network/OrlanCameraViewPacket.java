package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.entity.CameraTrackingGuard;

import com.synarsis.airtacticalarsenal.entity.OrlanEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class OrlanCameraViewPacket {
    private final int orlanEntityId;
    private final boolean connect;

    public static final int DRONE_CHUNK_RADIUS = 5;

    public static final int PATROLLING_CAMERA_CHUNK_RADIUS = 10;

    public OrlanCameraViewPacket(int orlanEntityId, boolean connect) {
        this.orlanEntityId = orlanEntityId;
        this.connect = connect;
    }

    public OrlanCameraViewPacket(FriendlyByteBuf buf) {
        this.orlanEntityId = buf.readInt();
        this.connect = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(orlanEntityId);
        buf.writeBoolean(connect);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(orlanEntityId);
            if (!connect && !isLinkedTo(entity, player)) {
                entity = findLinkedDrone(player, level);
                if (entity == null) {
                    if (!PENDING_RETURN.containsKey(player.getUUID())) {
                        sendPlayerChunks(player, level);
                        resyncPlayerEntities(player, level);
                    }
                    return;
                }
            }

            if (entity instanceof com.synarsis.airtacticalarsenal.entity.ShahedEntity strike) {
                if (!strike.hasCamera()) {
                    return;
                }
                if (connect) {
                    if (strike.getOwner() == null || strike.getOwner().getUUID().equals(player.getUUID())) {
                        strike.cameraLink().connect(player);
                    }
                } else {
                    strike.cameraLink().disconnect(player, false);
                }
                return;
            }
            if (entity instanceof OrlanEntity orlan) {
                if (connect) {
                    orlan.setLinkedPlayer(player.getUUID().toString());

                    player.setNoGravity(true);

                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();

                    player.connection.send(orlan.getAddEntityPacket());
                    List<SynchedEntityData.DataValue<?>> dataValues = orlan.getEntityData().getNonDefaultValues();
                    if (dataValues != null) {
                        player.connection.send(new ClientboundSetEntityDataPacket(orlan.getId(), dataValues));
                    }
                    player.connection.send(new ClientboundTeleportEntityPacket(orlan));

                    int connectRadius = (orlan.getFlightPhase() == OrlanEntity.FlightPhase.PATROLLING)
                            ? PATROLLING_CAMERA_CHUNK_RADIUS : DRONE_CHUNK_RADIUS;
                    sendDroneChunks(orlan, player, level, connectRadius);
                } else {
                    if (orlan.isLinkedToPlayer(player.getUUID())) {

                        Vec3 physPos = orlan.getCameraPhysicsPos();
                        float physFall = orlan.getCameraPlayerFallDistance();
                        orlan.setLinkedPlayer("");
                        player.setNoGravity(false);

                        player.gameMode.getGameModeForPlayer()
                                .updatePlayerAbilities(player.getAbilities());
                        player.onUpdateAbilities();
                        if (physPos != null) {
                            player.connection.teleport(physPos.x, physPos.y, physPos.z,
                                    player.getYRot(), player.getXRot());
                        }
                        player.fallDistance = physFall;

                        sendPlayerChunks(player, level);
                        resyncPlayerEntities(player, level);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static boolean isLinkedTo(Entity entity, ServerPlayer player) {
        if (entity instanceof com.synarsis.airtacticalarsenal.entity.ShahedEntity strike) {
            return strike.hasCamera() && strike.cameraLink().isLinkedTo(player.getUUID());
        }
        return entity instanceof OrlanEntity orlan && orlan.isLinkedToPlayer(player.getUUID());
    }

    private static Entity findLinkedDrone(ServerPlayer player, ServerLevel level) {
        Entity strike = com.synarsis.airtacticalarsenal.entity.CameraTrackingGuard.strikeDroneOf(player.getUUID());
        if (strike != null && strike.level() == level && isLinkedTo(strike, player)) {
            return strike;
        }
        for (OrlanEntity orlan : OrlanEntity.getActiveOrlansWithCamera()) {
            if (orlan.level() == level && orlan.isLinkedToPlayer(player.getUUID())) {
                return orlan;
            }
        }
        return null;
    }

    public static void sendDroneChunks(OrlanEntity orlan, ServerPlayer player, ServerLevel level) {
        sendDroneChunks(orlan, player, level, DRONE_CHUNK_RADIUS);
    }

    public static void sendDroneChunks(OrlanEntity orlan, ServerPlayer player, ServerLevel level, int radius) {
        ChunkPos droneChunk = orlan.chunkPosition();
        ChunkPos outside = new ChunkPos(droneChunk.x + 4 * radius + 1, droneChunk.z + 4 * radius + 1);
        DroneQueue queue = DRONE_QUEUE.get(player.getUUID());
        if (queue != null) {
            queue.pending.clear();
        }
        sendDroneChunksDelta(outside, droneChunk, player, level, radius);
    }

    private static void sendDroneChunk(ServerPlayer player, DroneQueue queue, LevelChunk chunk, ServerLevel level) {
        NetworkHandler.sendToPlayer(player, new DroneChunkPacket(queue.center.x, queue.center.z, queue.radius,
                chunk == null ? null : new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null)));
    }

    public static void sendDroneChunksDelta(ChunkPos oldCenter, ChunkPos newCenter, ServerPlayer player, ServerLevel level) {
        sendDroneChunksDelta(oldCenter, newCenter, player, level, DRONE_CHUNK_RADIUS);
    }

    public static void sendDroneChunksDelta(ChunkPos oldCenter, ChunkPos newCenter, ServerPlayer player, ServerLevel level, int r) {
        cancelPendingReturn(player);
        DroneQueue queue = DRONE_QUEUE.get(player.getUUID());
        if (queue != null && queue.level != level) {
            queue.releaseAll();
            queue = null;
        }
        if (queue == null) {
            queue = new DroneQueue(level);
            DRONE_QUEUE.put(player.getUUID(), queue);
        }
        queue.center = newCenter;
        queue.radius = r;
        sendDroneChunk(player, queue, null, level);
        DroneQueue q = queue;
        queue.pending.removeIf(key -> !q.inRange(key));
        queue.requested.removeIf(key -> {
            if (q.inRange(key)) {
                return false;
            }
            q.release(key);
            return true;
        });
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int cx = newCenter.x + dx;
                int cz = newCenter.z + dz;
                if (Math.abs(cx - oldCenter.x) <= r && Math.abs(cz - oldCenter.z) <= r) {
                    continue;
                }
                queue.pending.add(ChunkPos.asLong(cx, cz));
            }
        }
    }

    private static final int DRONE_CHUNKS_PER_TICK = 8;
    private static final int DRONE_REQUESTS_PER_TICK = 16;
    private static final int DRONE_REQUESTS_OUTSTANDING = 48;
    private static final net.minecraft.server.level.TicketType<ChunkPos> CAMERA_TICKET =
            net.minecraft.server.level.TicketType.create("ata_drone_camera", java.util.Comparator.comparingLong(ChunkPos::toLong), 100);
    private static final java.util.Map<java.util.UUID, DroneQueue> DRONE_QUEUE = new java.util.concurrent.ConcurrentHashMap<>();

    private static final class DroneQueue {
        final ServerLevel level;
        final java.util.Set<Long> pending = new java.util.HashSet<>();
        final java.util.Set<Long> requested = new java.util.HashSet<>();
        ChunkPos center;
        int radius;

        DroneQueue(ServerLevel level) {
            this.level = level;
        }

        boolean inRange(long key) {
            return Math.abs(ChunkPos.getX(key) - center.x) <= radius && Math.abs(ChunkPos.getZ(key) - center.z) <= radius;
        }

        void request(long key) {
            ChunkPos pos = new ChunkPos(key);
            level.getChunkSource().addRegionTicket(CAMERA_TICKET, pos, 0, pos);
        }

        void release(long key) {
            ChunkPos pos = new ChunkPos(key);
            level.getChunkSource().removeRegionTicket(CAMERA_TICKET, pos, 0, pos);
        }

        void releaseAll() {
            for (long key : requested) {
                release(key);
            }
            requested.clear();
            pending.clear();
        }
    }

    public static void clearDroneQueue(ServerPlayer player) {
        DroneQueue queue = DRONE_QUEUE.remove(player.getUUID());
        if (queue != null) {
            queue.releaseAll();
        }
    }

    private static void tickDroneQueues(net.minecraft.server.MinecraftServer server) {
        if (DRONE_QUEUE.isEmpty()) return;
        java.util.Iterator<java.util.Map.Entry<java.util.UUID, DroneQueue>> it = DRONE_QUEUE.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            DroneQueue queue = entry.getValue();
            if (player == null || queue.center == null || player.serverLevel() != queue.level) {
                queue.releaseAll();
                it.remove();
                continue;
            }
            if (server.getTickCount() % 40 == 0) {
                for (long key : queue.requested) {
                    queue.request(key);
                }
            }
            if (queue.pending.isEmpty()) {
                continue;
            }
            ServerLevel level = queue.level;
            ChunkPos center = queue.center;
            java.util.List<Long> order = new java.util.ArrayList<>(queue.pending);
            order.sort(java.util.Comparator.comparingInt(key ->
                    Math.max(Math.abs(ChunkPos.getX(key) - center.x), Math.abs(ChunkPos.getZ(key) - center.z))));
            int sent = 0;
            int requests = 0;
            for (long key : order) {
                if (sent >= DRONE_CHUNKS_PER_TICK && requests >= DRONE_REQUESTS_PER_TICK) {
                    break;
                }
                LevelChunk loaded = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
                if (loaded != null) {
                    if (sent < DRONE_CHUNKS_PER_TICK) {
                        sendDroneChunk(player, queue, loaded, level);
                        queue.pending.remove(key);
                        if (queue.requested.remove(key)) {
                            queue.release(key);
                        }
                        sent++;
                    }
                    continue;
                }
                if (!queue.requested.contains(key) && requests < DRONE_REQUESTS_PER_TICK
                        && queue.requested.size() < DRONE_REQUESTS_OUTSTANDING) {
                    queue.requested.add(key);
                    queue.request(key);
                    requests++;
                }
            }
        }
    }

    public static void sendPlayerChunks(ServerPlayer player, ServerLevel level) {
        clearDroneQueue(player);
        cancelPendingReturn(player);
        NetworkHandler.sendToPlayer(player, new DroneChunkPacket(0, 0, -1, null));
        ChunkPos pc = player.chunkPosition();
        player.connection.send(new ClientboundSetChunkCacheCenterPacket(pc.x, pc.z));
        player.connection.teleport(player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
    }

    private static final int IMMEDIATE_RETURN_RADIUS = 2;
    private static final int RETURN_CHUNKS_PER_TICK = 40;
    private static final java.util.Map<java.util.UUID, java.util.ArrayDeque<ChunkPos>> PENDING_RETURN =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final java.util.Set<java.util.UUID> PENDING_RESYNC =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void cancelPendingReturn(ServerPlayer player) {
        PENDING_RETURN.remove(player.getUUID());
        PENDING_RESYNC.remove(player.getUUID());
    }

    public static void tickPendingReturns(net.minecraft.server.MinecraftServer server) {
        tickDroneQueues(server);
        if (PENDING_RETURN.isEmpty()) return;
        java.util.Iterator<java.util.Map.Entry<java.util.UUID, java.util.ArrayDeque<ChunkPos>>> it = PENDING_RETURN.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                PENDING_RESYNC.remove(entry.getKey());
                it.remove();
                continue;
            }
            ServerLevel level = player.serverLevel();
            boolean resync = PENDING_RESYNC.contains(entry.getKey());
            java.util.ArrayDeque<ChunkPos> queue = entry.getValue();
            for (int i = 0; i < RETURN_CHUNKS_PER_TICK && !queue.isEmpty(); i++) {
                ChunkPos c = queue.poll();
                ChunkAccess ca = level.getChunk(c.x, c.z, ChunkStatus.FULL, false);
                if (ca instanceof LevelChunk lc) {
                    player.connection.send(new ClientboundLevelChunkWithLightPacket(lc, level.getLightEngine(), null, null));
                    if (resync) {
                        syncEntitiesIn(player, level, new AABB(c.getMinBlockX(), level.getMinBuildHeight(), c.getMinBlockZ(),
                                c.getMaxBlockX() + 1, level.getMaxBuildHeight(), c.getMaxBlockZ() + 1));
                    }
                }
            }
            if (queue.isEmpty()) {
                PENDING_RESYNC.remove(entry.getKey());
                it.remove();
            }
        }
    }

    public static void resyncPlayerEntities(ServerPlayer player, ServerLevel level) {
        if (PENDING_RETURN.containsKey(player.getUUID())) {
            ChunkPos pc = player.chunkPosition();
            int r = IMMEDIATE_RETURN_RADIUS;
            syncEntitiesIn(player, level, new AABB(
                    (pc.x - r) * 16.0, level.getMinBuildHeight(), (pc.z - r) * 16.0,
                    (pc.x + r + 1) * 16.0, level.getMaxBuildHeight(), (pc.z + r + 1) * 16.0));
            PENDING_RESYNC.add(player.getUUID());
            return;
        }
        int viewDist = level.getServer().getPlayerList().getViewDistance();
        double range = viewDist * 16.0;
        syncEntitiesIn(player, level, new AABB(
                player.getX() - range, player.getY() - 128, player.getZ() - range,
                player.getX() + range, player.getY() + 128, player.getZ() + range
        ));
    }

    private static void syncEntitiesIn(ServerPlayer player, ServerLevel level, AABB box) {
        List<Entity> entities = level.getEntities(player, box, e -> e.isAlive());
        for (Entity e : entities) {
            Packet<?> add;
            try {
                add = e.getAddEntityPacket();
            } catch (RuntimeException ex) {
                continue;
            }
            if (add == null) {
                continue;
            }
            player.connection.send(add);
            var dataValues = e.getEntityData().getNonDefaultValues();
            if (dataValues != null) {
                player.connection.send(new ClientboundSetEntityDataPacket(e.getId(), dataValues));
            }
            player.connection.send(new ClientboundTeleportEntityPacket(e));
            if (e.getDeltaMovement().lengthSqr() > 0.001) {
                player.connection.send(new ClientboundSetEntityMotionPacket(e));
            }
        }
    }
}
