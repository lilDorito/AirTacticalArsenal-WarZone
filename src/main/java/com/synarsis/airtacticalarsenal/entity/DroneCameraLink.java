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
    private static final java.util.Map<UUID, Integer> RENDER_DISTANCE = new java.util.concurrent.ConcurrentHashMap<>();

    public static void setRenderDistance(UUID player, int chunks) {
        RENDER_DISTANCE.put(player, Math.max(2, Math.min(32, chunks)));
    }

    private int chunkRadius() {
        int min = com.synarsis.airtacticalarsenal.config.ShahedConfig.getDroneCameraChunkRadius();
        int wanted = viewer != null ? RENDER_DISTANCE.getOrDefault(viewer, min) : min;
        return Math.max(min, Math.min(32, wanted));
    }

    private final Entity drone;
    private UUID viewer;
    private Vec3 physicsPos;
    private float fallDistance;
    private ChunkPos lastSentChunk;
    private final CameraEntityStream tracked = new CameraEntityStream();
    private final EntityDataMirror selfData = new EntityDataMirror();

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
        selfData.mark(drone);
        lastSentChunk = drone.chunkPosition();
        ChunkPos outside = new ChunkPos(lastSentChunk.x + 4 * chunkRadius() + 1, lastSentChunk.z + 4 * chunkRadius() + 1);
        OrlanCameraViewPacket.sendDroneChunksDelta(outside, lastSentChunk, player, level, chunkRadius());
    }

    public void disconnect(ServerPlayer player, boolean forceExit) {
        disconnect(player, forceExit, false);
    }

    public void disconnect(ServerPlayer player, boolean forceExit, boolean lost) {
        if (!isLinkedTo(player.getUUID())) {
            return;
        }
        ServerLevel level = player.serverLevel();
        tracked.removeAll(player);
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
            NetworkHandler.sendToPlayer(player, new OrlanCameraForceExitPacket(lost));
        }
    }

    public void release(ServerLevel level) {
        if (viewer == null) {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(viewer);
        if (player != null) {
            disconnect(player, true, true);
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
        com.synarsis.airtacticalarsenal.network.DroneStatePacket.send(player, drone);
        selfData.sync(player, drone);
        if (drone.tickCount % 5 == 0) {
            player.connection.send(new ClientboundSetEntityMotionPacket(drone));
        }

        ChunkPos chunk = drone.chunkPosition();
        if (lastSentChunk == null) {
            lastSentChunk = chunk;
        } else if (!chunk.equals(lastSentChunk)) {
            OrlanCameraViewPacket.sendDroneChunksDelta(lastSentChunk, chunk, player, level, chunkRadius());
            lastSentChunk = chunk;
        }

        tracked.tick(player, level, drone, chunkRadius() * 16.0,
                e -> e != player && e.isAlive() && !(e instanceof Marker) && e.getType().clientTrackingRange() > 0,
                drone.tickCount % 3 == 0);
    }
}
