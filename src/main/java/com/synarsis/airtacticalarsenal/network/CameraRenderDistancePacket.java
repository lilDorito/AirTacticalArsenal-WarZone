package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.entity.DroneCameraLink;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CameraRenderDistancePacket {
    private final int chunks;

    public CameraRenderDistancePacket(int chunks) {
        this.chunks = chunks;
    }

    public CameraRenderDistancePacket(FriendlyByteBuf buf) {
        this.chunks = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(chunks);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                DroneCameraLink.setRenderDistance(player.getUUID(), chunks);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
