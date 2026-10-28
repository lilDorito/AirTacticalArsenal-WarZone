package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GroundLockPacket {
    private final int droneId;
    private final int targetId;

    public GroundLockPacket(int droneId, int targetId) {
        this.droneId = droneId;
        this.targetId = targetId;
    }

    public GroundLockPacket(FriendlyByteBuf buf) {
        this.droneId = buf.readVarInt();
        this.targetId = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(droneId);
        buf.writeInt(targetId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (!(player.serverLevel().getEntity(droneId) instanceof ShahedEntity drone) || !drone.isSteerable()
                    || !drone.hasCamera() || !drone.cameraLink().isLinkedTo(player.getUUID())) {
                return;
            }
            if (targetId < 0) {
                drone.setSteerLock(null);
                return;
            }
            Entity target = player.serverLevel().getEntity(targetId);
            if (target == null || target == drone || !target.isAlive() || !ShahedEntity.onGroundTarget(target)
                    || target.distanceToSqr(drone) > 1500.0 * 1500.0) {
                return;
            }
            drone.setSteerLock(target.getRootVehicle());
            player.displayClientMessage(Component.translatable("message.ata.strike.ground_locked", target.getDisplayName()), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
