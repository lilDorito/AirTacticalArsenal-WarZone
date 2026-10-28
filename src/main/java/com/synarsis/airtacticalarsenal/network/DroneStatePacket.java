package com.synarsis.airtacticalarsenal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DroneStatePacket {
    private final int entityId;
    private final long tick;
    private final double x;
    private final double y;
    private final double z;

    public DroneStatePacket(int entityId, long tick, double x, double y, double z) {
        this.entityId = entityId;
        this.tick = tick;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public DroneStatePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.tick = buf.readVarLong();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public static void send(ServerPlayer viewer, Entity drone) {
        NetworkHandler.sendToPlayer(viewer, new DroneStatePacket(drone.getId(), drone.level().getGameTime(),
                drone.getX(), drone.getY(), drone.getZ()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarLong(tick);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(entityId, tick, x, y, z)));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(int entityId, long tick, double x, double y, double z) {
        com.synarsis.airtacticalarsenal.client.CameraSmoother.sample(entityId, tick, x, y, z);
    }
}
