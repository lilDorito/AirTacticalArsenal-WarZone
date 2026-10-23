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
    public static final int TOGGLE_MODE = 0;
    public static final int TOGGLE_ALERT = 1;

    private final BlockPos radarPos;
    private final int action;

    public ToggleRadarAlarmPacket(BlockPos radarPos, int action) {
        this.radarPos = radarPos;
        this.action = action;
    }

    public ToggleRadarAlarmPacket(FriendlyByteBuf buf) {
        this.radarPos = buf.readBlockPos();
        this.action = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(radarPos);
        buf.writeVarInt(action);
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
            if (action == TOGGLE_MODE) {
                radar.setManualMode(!radar.isManualMode());
            } else if (action == TOGGLE_ALERT && radar.isManualMode()) {
                radar.setManualAlert(!radar.isManualAlert());
            }
            if (level.getBlockState(radarPos).getBlock() instanceof ShahedRadarBlock block) {
                block.propagateManualAlert(level, radarPos);
                block.refreshSignal(level, radarPos);
            }
        });
        return true;
    }
}
