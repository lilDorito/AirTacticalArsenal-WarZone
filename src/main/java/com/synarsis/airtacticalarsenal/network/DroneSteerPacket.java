package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DroneSteerPacket {
    private final int droneId;
    private final float yaw;
    private final float pitch;
    private final float roll;

    public DroneSteerPacket(int droneId, float yaw, float pitch) {
        this(droneId, yaw, pitch, 0.0f);
    }

    public DroneSteerPacket(int droneId, float yaw, float pitch, float roll) {
        this.droneId = droneId;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }

    public DroneSteerPacket(FriendlyByteBuf buf) {
        this.droneId = buf.readInt();
        this.yaw = buf.readFloat();
        this.pitch = buf.readFloat();
        this.roll = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(droneId);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeFloat(roll);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !Float.isFinite(yaw) || !Float.isFinite(pitch) || !Float.isFinite(roll)) {
                return;
            }
            if (!(player.serverLevel().getEntity(droneId) instanceof ShahedEntity drone) || !drone.isSteerable()) {
                return;
            }
            if (!drone.cameraLink().isLinkedTo(player.getUUID())) {
                return;
            }
            drone.steer(yaw, pitch, roll);
        });
        ctx.get().setPacketHandled(true);
    }
}
