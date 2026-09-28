package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.compat.SuperbWarfareCompat;
import com.synarsis.airtacticalarsenal.config.ShahedConfig;
import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import com.synarsis.airtacticalarsenal.entity.IskanderEntity;
import com.synarsis.airtacticalarsenal.entity.OrlanEntity;
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
    private static final int RADAR_RANGE = 1500;

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
                radarCenter.x - RADAR_RANGE, level.getMinBuildHeight(), radarCenter.z - RADAR_RANGE,
                radarCenter.x + RADAR_RANGE, top, radarCenter.z + RADAR_RANGE
            );

            List<RadarUpdatePacket.TargetData> targets = new ArrayList<>();
            int minAltitudeCfg = ShahedConfig.getRadarMinAltitude();
            if (!com.synarsis.airtacticalarsenal.block.ShahedRadarBlock.hasOpenSky(level, radarPos)) {
                NetworkHandler.sendToPlayer(player, new RadarUpdatePacket(radarPos, targets, minAltitudeCfg, false));
                return;
            }
            double rangeSquared = (double) RADAR_RANGE * RADAR_RANGE;
            int minAltitude = ShahedConfig.getRadarMinAltitude();

            for (Entity entity : level.getEntities(null, searchBox)) {
                byte type = -1;
                if (entity instanceof ShahedEntity) type = 0;
                else if (entity instanceof IskanderEntity) type = 1;
                else if (entity instanceof OrlanEntity) type = 2;
                else {
                    byte swType = SuperbWarfareCompat.getRadarType(entity);
                    if (swType >= 0) type = swType;
                }
                if (type < 0) continue;

                double dx = entity.getX() - radarCenter.x;
                double dz = entity.getZ() - radarCenter.z;
                if (dx * dx + dz * dz > rangeSquared) continue;

                if (type != 1 && entity.getY() < minAltitude) continue;

                Vec3 delta = entity.getDeltaMovement();
                float speed = (float) Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                targets.add(new RadarUpdatePacket.TargetData(
                        entity.getId(), type, entity.position(), entity.getYRot(), speed
                ));
            }

            NetworkHandler.sendToPlayer(player, new RadarUpdatePacket(radarPos, targets, minAltitude, true));
        });
        return true;
    }
}
