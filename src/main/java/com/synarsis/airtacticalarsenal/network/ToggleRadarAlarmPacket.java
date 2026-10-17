package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.block.ShahedRadarBlock;
import com.synarsis.airtacticalarsenal.block.entity.ShahedRadarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleRadarAlarmPacket {
    private final BlockPos radarPos;

    public ToggleRadarAlarmPacket(BlockPos radarPos) {
        this.radarPos = radarPos;
    }

    public ToggleRadarAlarmPacket(FriendlyByteBuf buf) {
        this.radarPos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(radarPos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            if (player.position().distanceToSqr(Vec3.atCenterOf(radarPos)) > 64.0) return;
            if (!(level.getBlockEntity(radarPos) instanceof ShahedRadarBlockEntity radar)) return;
            if (!radar.canControl(player)) return;
            radar.setAlarmOutput(!radar.isAlarmOutput());
            if (level.getBlockState(radarPos).getBlock() instanceof ShahedRadarBlock block) {
                block.refreshSignal(level, radarPos);
            }
        });
        return true;
    }
}
