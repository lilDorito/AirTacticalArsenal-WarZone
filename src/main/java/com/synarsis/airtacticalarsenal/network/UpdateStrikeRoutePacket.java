package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import com.synarsis.airtacticalarsenal.util.RouteLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class UpdateStrikeRoutePacket {
    private final int droneId;
    private final List<BlockPos> points;

    public UpdateStrikeRoutePacket(int droneId, List<BlockPos> points) {
        this.droneId = droneId;
        this.points = points != null ? new ArrayList<>(points) : new ArrayList<>();
    }

    public UpdateStrikeRoutePacket(FriendlyByteBuf buf) {
        this.droneId = buf.readVarInt();
        int count = Math.min(64, buf.readVarInt());
        this.points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            this.points.add(buf.readBlockPos());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(droneId);
        buf.writeVarInt(points.size());
        for (BlockPos p : points) {
            buf.writeBlockPos(p);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            Entity entity = player.serverLevel().getEntity(droneId);
            if (!(entity instanceof ShahedEntity drone) || !drone.hasCamera()) {
                player.displayClientMessage(Component.translatable("message.ata.strike.reroute_lost"), true);
                return;
            }
            Entity owner = drone.getOwner();
            if (owner == null || !owner.getUUID().equals(player.getUUID())) {
                return;
            }
            if (!drone.canReroute()) {
                player.displayClientMessage(Component.translatable("message.ata.strike.reroute_busy"), true);
                return;
            }
            if (points.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.ata.route.empty"), true);
                return;
            }
            double length = RouteLimits.pathLength(drone.blockPosition(), points);
            if (!RouteLimits.check(player, length, drone.remainingRoute())) {
                return;
            }
            drone.updateRouteInFlight(points);
            player.displayClientMessage(Component.translatable("message.ata.strike.rerouted", points.size()), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
