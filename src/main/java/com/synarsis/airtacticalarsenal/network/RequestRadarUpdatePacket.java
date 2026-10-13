package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.config.ShahedConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
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

            double top = Math.max(level.getMaxBuildHeight(), ShahedConfig.getIskanderMaxFlightHeight()) + 64;
            AABB searchBox = new AABB(
                radarCenter.x - ShahedConfig.getRadarRange(), level.getMinBuildHeight(), radarCenter.z - ShahedConfig.getRadarRange(),
                radarCenter.x + ShahedConfig.getRadarRange(), top, radarCenter.z + ShahedConfig.getRadarRange()
            );

            List<RadarUpdatePacket.TargetData> targets = new ArrayList<>();
            int minAltitudeCfg = ShahedConfig.getRadarMinAltitude();
            if (!com.synarsis.airtacticalarsenal.block.ShahedRadarBlock.hasOpenSky(level, radarPos)) {
                NetworkHandler.sendToPlayer(player, new RadarUpdatePacket(radarPos, targets, minAltitudeCfg, false));
                return;
            }
            double rangeSquared = (double) ShahedConfig.getRadarRange() * ShahedConfig.getRadarRange();
            int minAltitude = ShahedConfig.getRadarMinAltitude();

            for (Entity entity : level.getEntities(null, searchBox)) {
                byte type = com.synarsis.airtacticalarsenal.radar.RadarContacts.classify(entity);
                if (type < 0) continue;

                double dx = entity.getX() - radarCenter.x;
                double dz = entity.getZ() - radarCenter.z;
                if (dx * dx + dz * dz > rangeSquared) continue;

                if (entity.getY() < minAltitude) continue;

                Vec3 delta = entity.getDeltaMovement();
                float speed = (float) Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                targets.add(new RadarUpdatePacket.TargetData(
                        entity.getId(), type, entity.position(), entity.getYRot(), speed,
                        com.synarsis.airtacticalarsenal.radar.RadarContacts.isFriendly(player.getTeam(), player, entity)
                ));
            }

            NetworkHandler.sendToPlayer(player, new RadarUpdatePacket(radarPos, targets, minAltitude, true));
        });
        return true;
    }
}
