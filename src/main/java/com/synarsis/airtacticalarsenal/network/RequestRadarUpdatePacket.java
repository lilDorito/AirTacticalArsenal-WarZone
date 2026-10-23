package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.config.ShahedConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RequestRadarUpdatePacket {
    private final BlockPos radarPos;

    public RequestRadarUpdatePacket(BlockPos radarPos) {
        this.radarPos = radarPos;
    }

    public RequestRadarUpdatePacket(FriendlyByteBuf buf) {
        this.radarPos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.radarPos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            Vec3 radarCenter = Vec3.atCenterOf(radarPos);

            int alarm = 0;
            boolean canToggle = false;
            if (level.getBlockEntity(radarPos) instanceof com.synarsis.airtacticalarsenal.block.entity.ShahedRadarBlockEntity radar) {
                alarm = RadarUpdatePacket.alarmState(radar.isManualMode(), radar.isManualAlert(), radar.isRelayed(level.getGameTime()));
                canToggle = radar.canControl(player);
            }

            List<RadarUpdatePacket.TargetData> targets = new ArrayList<>();
            int minAltitude = ShahedConfig.getRadarMinAltitude();
            if (!com.synarsis.airtacticalarsenal.block.ShahedRadarBlock.hasOpenSky(level, radarPos)) {
                NetworkHandler.sendToPlayer(player, new RadarUpdatePacket(radarPos, targets, minAltitude, false, alarm, canToggle));
                return;
            }
            String viewer = player.getScoreboardName();
            com.synarsis.airtacticalarsenal.radar.RadarContacts.forEachInRange(level, radarCenter, ShahedConfig.getRadarRange(), minAltitude, entity -> {
                byte type = com.synarsis.airtacticalarsenal.radar.RadarContacts.classify(entity);
                if (type < 0) return;
                Vec3 delta = entity.getDeltaMovement();
                float speed = (float) Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                targets.add(new RadarUpdatePacket.TargetData(
                        entity.getId(), type, entity.position(), entity.getYRot(), speed,
                        com.synarsis.airtacticalarsenal.radar.RadarContacts.isFriendlyToOwner(level, viewer, entity)
                ));
            });

            NetworkHandler.sendToPlayer(player, new RadarUpdatePacket(radarPos, targets, minAltitude, true, alarm, canToggle));
        });
        return true;
    }
}
