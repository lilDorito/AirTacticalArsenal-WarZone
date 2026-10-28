package com.synarsis.airtacticalarsenal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DroneChunkPacket {
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final ClientboundLevelChunkWithLightPacket chunk;

    public DroneChunkPacket(int centerX, int centerZ, int radius, ClientboundLevelChunkWithLightPacket chunk) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.chunk = chunk;
    }

    public DroneChunkPacket(FriendlyByteBuf buf) {
        this.centerX = buf.readInt();
        this.centerZ = buf.readInt();
        this.radius = buf.readVarInt();
        this.chunk = buf.readBoolean() ? new ClientboundLevelChunkWithLightPacket(buf) : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(centerX);
        buf.writeInt(centerZ);
        buf.writeVarInt(radius);
        buf.writeBoolean(chunk != null);
        if (chunk != null) {
            chunk.write(buf);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(centerX, centerZ, radius, chunk)));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(int centerX, int centerZ, int radius, ClientboundLevelChunkWithLightPacket chunk) {
        com.synarsis.airtacticalarsenal.client.DroneChunkCache.accept(centerX, centerZ, radius, chunk);
    }
}
