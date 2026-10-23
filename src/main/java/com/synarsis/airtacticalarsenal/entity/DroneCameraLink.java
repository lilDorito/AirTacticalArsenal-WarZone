package com.synarsis.airtacticalarsenal.entity;

import com.synarsis.airtacticalarsenal.network.NetworkHandler;
import com.synarsis.airtacticalarsenal.network.OrlanCameraForceExitPacket;
import com.synarsis.airtacticalarsenal.network.OrlanCameraViewPacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class DroneCameraLink {
    private static final int CHUNK_RADIUS = OrlanCameraViewPacket.PATROLLING_CAMERA_CHUNK_RADIUS;

    private final Entity drone;
    private UUID viewer;
    private Vec3 physicsPos;
    private float fallDistance;
    private ChunkPos lastSentChunk;
    private final Set<Integer> tracked = new HashSet<>();

    public DroneCameraLink(Entity drone) {
        this.drone = drone;
    }

    public boolean isLinked() {
        return viewer != null;
    }

    public boolean isLinkedTo(UUID player) {
        return viewer != null && viewer.equals(player);
    }

    public void connect(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (viewer != null && !viewer.equals(player.getUUID())) {
            ServerPlayer previous = level.getServer().getPlayerList().getPlayer(viewer);
            if (previous != null) {
                disconnect(previous, true);
            }
        }
        viewer = player.getUUID();
        CameraTrackingGuard.setStrikeViewer(viewer, drone);
        physicsPos = player.position();
        fallDistance = player.fallDistance;
        player.setNoGravity(true);
        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();
        player.connection.send(drone.getAddEntityPacket());
        var data = drone.getEntityData().getNonDefaultValues();
        if (data != null) {
            player.connection.send(new ClientboundSetEntityDataPacket(drone.getId(), data));
        }
        player.connection.send(new ClientboundTeleportEntityPacket(drone));
        lastSentChunk = drone.chunkPosition();
        ChunkPos outside = new ChunkPos(lastSentChunk.x + 4 * CHUNK_RADIUS + 1, lastSentChunk.z + 4 * CHUNK_RADIUS + 1);
        OrlanCameraViewPacket.sendDroneChunksDelta(outside, lastSentChunk, player, level, CHUNK_RADIUS);
    }

    public void disconnect(ServerPlayer player, boolean forceExit) {
        if (!isLinkedTo(player.getUUID())) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!tracked.isEmpty()) {
            player.connection.send(new ClientboundRemoveEntitiesPacket(tracked.stream().mapToInt(Integer::intValue).toArray()));
            tracked.clear();
        }
        CameraTrackingGuard.clearStrikeViewer(viewer, drone);
        viewer = null;
        player.setNoGravity(false);
        player.gameMode.getGameModeForPlayer().updatePlayerAbilities(player.getAbilities());
        player.onUpdateAbilities();
        if (physicsPos != null) {
            player.connection.teleport(physicsPos.x, physicsPos.y, physicsPos.z, player.getYRot(), player.getXRot());
        }
        player.fallDistance = fallDistance;
        physicsPos = null;
        lastSentChunk = null;
        OrlanCameraViewPacket.sendPlayerChunks(player, level);
        OrlanCameraViewPacket.resyncPlayerEntities(player, level);
        if (forceExit) {
            NetworkHandler.sendToPlayer(player, new OrlanCameraForceExitPacket());
        }
    }

    public void release(ServerLevel level) {
        if (viewer == null) {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(viewer);
        if (player != null) {
            disconnect(player, true);
        } else {
            CameraTrackingGuard.clearStrikeViewer(viewer, drone);
            viewer = null;
            tracked.clear();
        }
    }

    public void tick(ServerLevel level) {
        if (viewer == null) {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(viewer);
        if (player == null || player.level() != level) {
            CameraTrackingGuard.clearStrikeViewer(viewer, drone);
            viewer = null;
            tracked.clear();
            return;
        }
        if (!player.isAlive()) {
            disconnect(player, true);
            return;
        }
        if (physicsPos == null) {
            physicsPos = player.position();
        }
        Vec3 velocity = player.getDeltaMovement().add(0.0, -0.08, 0.0);
        player.absMoveTo(physicsPos.x, physicsPos.y, physicsPos.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(velocity);
        player.fallDistance = fallDistance;
        double prevY = player.getY();
        player.move(MoverType.SELF, velocity);
        double dy = player.getY() - prevY;
        player.getAbilities().mayfly = false;
        player.doCheckFallDamage(0, dy, 0, player.onGround());
        player.getAbilities().mayfly = true;
        physicsPos = player.position();
        fallDistance = player.fallDistance;
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(),
                RelativeMovement.ROTATION);

        player.connection.send(new ClientboundTeleportEntityPacket(drone));
        if (drone.tickCount % 5 == 0) {
            player.connection.send(new ClientboundSetEntityMotionPacket(drone));
            var data = drone.getEntityData().getNonDefaultValues();
            if (data != null) {
                player.connection.send(new ClientboundSetEntityDataPacket(drone.getId(), data));
            }
        }

        ChunkPos chunk = drone.chunkPosition();
        if (lastSentChunk == null) {
            lastSentChunk = chunk;
        } else if (!chunk.equals(lastSentChunk)) {
            OrlanCameraViewPacket.sendDroneChunksDelta(lastSentChunk, chunk, player, level, CHUNK_RADIUS);
            lastSentChunk = chunk;
        }

        if (drone.tickCount % 3 == 0) {
            syncNearby(player, level);
        }
    }

    private void syncNearby(ServerPlayer player, ServerLevel level) {
        double radius = CHUNK_RADIUS * 16.0;
        AABB box = new AABB(drone.getX() - radius, drone.getY() - 300, drone.getZ() - radius,
                drone.getX() + radius, drone.getY() + 300, drone.getZ() + radius);
        List<Entity> nearby = level.getEntities(drone, box,
                e -> e != player && e.isAlive() && !(e instanceof Marker) && e.getType().clientTrackingRange() > 0);
        Set<Integer> current = new HashSet<>();
        for (Entity e : nearby) {
            int id = e.getId();
            if (!tracked.contains(id)) {
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
                var data = e.getEntityData().getNonDefaultValues();
                if (data != null) {
                    player.connection.send(new ClientboundSetEntityDataPacket(id, data));
                }
            }
            current.add(id);
            player.connection.send(new ClientboundTeleportEntityPacket(e));
            if (e.getDeltaMovement().lengthSqr() > 0.001) {
                player.connection.send(new ClientboundSetEntityMotionPacket(e));
            }
        }
        Set<Integer> gone = new HashSet<>(tracked);
        gone.removeAll(current);
        if (!gone.isEmpty()) {
            player.connection.send(new ClientboundRemoveEntitiesPacket(gone.stream().mapToInt(Integer::intValue).toArray()));
        }
        tracked.clear();
        tracked.addAll(current);
    }
}
