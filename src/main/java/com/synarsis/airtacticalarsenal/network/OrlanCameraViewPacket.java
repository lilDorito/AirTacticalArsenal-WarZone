package com.synarsis.airtacticalarsenal.network;

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
        cancelPendingReturn(player);
        ChunkPos droneChunk = orlan.chunkPosition();
        player.connection.send(new ClientboundSetChunkCacheCenterPacket(droneChunk.x, droneChunk.z));

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = droneChunk.x + dx;
                int cz = droneChunk.z + dz;

                ChunkAccess ca = level.getChunk(cx, cz, ChunkStatus.FULL, true);
                if (ca instanceof LevelChunk lc) {
                    player.connection.send(new ClientboundLevelChunkWithLightPacket(
                            lc, level.getLightEngine(), null, null));
                }
            }
        }
    }

    public static void sendDroneChunksDelta(ChunkPos oldCenter, ChunkPos newCenter, ServerPlayer player, ServerLevel level) {
        sendDroneChunksDelta(oldCenter, newCenter, player, level, DRONE_CHUNK_RADIUS);
    }

    public static void sendDroneChunksDelta(ChunkPos oldCenter, ChunkPos newCenter, ServerPlayer player, ServerLevel level, int r) {
        cancelPendingReturn(player);
        player.connection.send(new ClientboundSetChunkCacheCenterPacket(newCenter.x, newCenter.z));

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int cx = newCenter.x + dx;
                int cz = newCenter.z + dz;

                if (Math.abs(cx - oldCenter.x) <= r && Math.abs(cz - oldCenter.z) <= r) {
                    continue;
                }
                ChunkAccess ca = level.getChunk(cx, cz, ChunkStatus.FULL, true);
                if (ca instanceof LevelChunk lc) {
                    player.connection.send(new ClientboundLevelChunkWithLightPacket(
                            lc, level.getLightEngine(), null, null));
                }
            }
        }
    }

    public static void sendPlayerChunks(ServerPlayer player, ServerLevel level) {
        ChunkPos pc = player.chunkPosition();

        player.connection.send(new ClientboundSetChunkCacheCenterPacket(pc.x, pc.z));

        int viewDist = level.getServer().getPlayerList().getViewDistance();
        java.util.List<ChunkPos> later = new java.util.ArrayList<>();
        for (int dx = -viewDist; dx <= viewDist; dx++) {
            for (int dz = -viewDist; dz <= viewDist; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) <= IMMEDIATE_RETURN_RADIUS) {
                    ChunkAccess ca = level.getChunk(pc.x + dx, pc.z + dz, ChunkStatus.FULL, false);
                    if (ca instanceof LevelChunk lc) {
                        player.connection.send(new ClientboundLevelChunkWithLightPacket(
                                lc, level.getLightEngine(), null, null));
                    }
                } else {
                    later.add(new ChunkPos(pc.x + dx, pc.z + dz));
                }
            }
        }
        later.sort(java.util.Comparator.comparingInt(c -> Math.max(Math.abs(c.x - pc.x), Math.abs(c.z - pc.z))));
        PENDING_RETURN.put(player.getUUID(), new java.util.ArrayDeque<>(later));

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
