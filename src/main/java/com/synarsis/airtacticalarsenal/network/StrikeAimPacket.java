package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StrikeAimPacket {
    public static final int STRIKE = 0;
    public static final int ORBIT = 1;
    public static final int RESUME = 2;

    private final int droneId;
    private final int action;
    private final int entityId;
    private final boolean hasPoint;
    private final Vec3 point;
    private final Vec3 origin;
    private final Vec3 direction;

    public StrikeAimPacket(int droneId, int action, int entityId, Vec3 point, Vec3 origin, Vec3 direction) {
        this.droneId = droneId;
        this.action = action;
        this.entityId = entityId;
        this.hasPoint = point != null;
        this.point = point != null ? point : Vec3.ZERO;
        this.origin = origin != null ? origin : Vec3.ZERO;
        this.direction = direction != null ? direction : Vec3.ZERO;
    }

    public StrikeAimPacket(FriendlyByteBuf buf) {
        this.droneId = buf.readInt();
        this.action = buf.readByte();
        this.entityId = buf.readInt();
        this.hasPoint = buf.readBoolean();
        this.point = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.origin = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.direction = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(droneId);
        buf.writeByte(action);
        buf.writeInt(entityId);
        buf.writeBoolean(hasPoint);
        buf.writeDouble(point.x);
        buf.writeDouble(point.y);
        buf.writeDouble(point.z);
        buf.writeDouble(origin.x);
        buf.writeDouble(origin.y);
        buf.writeDouble(origin.z);
        buf.writeDouble(direction.x);
        buf.writeDouble(direction.y);
        buf.writeDouble(direction.z);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            if (!(level.getEntity(droneId) instanceof ShahedEntity drone) || !drone.hasCamera()) {
                return;
            }
            if (!drone.cameraLink().isLinkedTo(player.getUUID())) {
                return;
            }
            if (action == RESUME) {
                if (drone.commandResume() == ShahedEntity.AttackResult.OK) {
                    player.displayClientMessage(Component.translatable("message.ata.strike.resumed"), true);
                }
                return;
            }
            Entity target = entityId >= 0 ? level.getEntity(entityId) : null;
            if (target != null && (!target.isAlive() || target == drone || target instanceof Projectile)) {
                target = null;
            }
            Vec3 aim;
            if (target != null) {
                aim = target.getBoundingBox().getCenter();
            } else if (hasPoint) {
                aim = point;
            } else {
                aim = null;
            }
            if (aim == null) {
                if (direction.lengthSqr() > 1.0E-6
                        && drone.commandHeading(direction, action == ORBIT) == ShahedEntity.AttackResult.OK) {
                    player.displayClientMessage(Component.translatable(action == ORBIT
                            ? "message.ata.strike.heading_orbit" : "message.ata.strike.heading"), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.ata.strike.no_surface"), true);
                }
                return;
            }
            double dx = aim.x - drone.getX();
            double dz = aim.z - drone.getZ();
            if (dx * dx + dz * dz > StrikeRetargetPacket.MAX_RETARGET_DISTANCE * StrikeRetargetPacket.MAX_RETARGET_DISTANCE) {
                player.displayClientMessage(Component.translatable("message.ata.strike.retarget_too_far",
                        (int) StrikeRetargetPacket.MAX_RETARGET_DISTANCE), true);
                return;
            }
            ShahedEntity.AttackResult result = action == ORBIT ? drone.commandOrbit(aim, target) : drone.commandStrike(aim, target);
            if (result == ShahedEntity.AttackResult.OK) {
                String key = action == ORBIT
                        ? (target != null ? "message.ata.strike.orbit_target" : "message.ata.strike.orbit_point")
                        : (target != null ? "message.ata.strike.strike_target" : "message.ata.strike.retargeted");
                if (target != null) {
                    player.displayClientMessage(Component.translatable(key, target.getDisplayName()), true);
                } else {
                    player.displayClientMessage(Component.translatable(key, (int) aim.x, (int) aim.y, (int) aim.z), true);
                }
            } else if (result == ShahedEntity.AttackResult.TOO_LATE) {
                player.displayClientMessage(Component.translatable("message.ata.strike.too_late"), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
