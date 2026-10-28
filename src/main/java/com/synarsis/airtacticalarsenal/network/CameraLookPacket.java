package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.entity.CameraTrackingGuard;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CameraLookPacket {
    private final float yaw;
    private final float pitch;

    public CameraLookPacket(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public CameraLookPacket(FriendlyByteBuf buf) {
        this.yaw = buf.readFloat();
        this.pitch = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
                return;
            }
            if (CameraTrackingGuard.viewedDrone(player) == null) {
                return;
            }
            float clampedPitch = Mth.clamp(pitch, -90.0F, 90.0F);
            player.setYRot(yaw);
            player.setXRot(clampedPitch);
            player.setYHeadRot(yaw);
            player.yRotO = yaw;
            player.xRotO = clampedPitch;
        });
        ctx.get().setPacketHandled(true);
    }
}
