package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.salvo.SalvoScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SalvoPacket {
    private final boolean strike;
    private final List<BlockPos> launchers;
    private final int gapTicks;
    private final int spread;
    private final boolean timeOnTarget;
    private final BlockPos main;

    public SalvoPacket(boolean strike, List<BlockPos> launchers, int gapTicks, int spread, boolean timeOnTarget, BlockPos main) {
        this.main = main;
        this.strike = strike;
        this.launchers = new ArrayList<>(launchers);
        this.gapTicks = gapTicks;
        this.spread = spread;
        this.timeOnTarget = timeOnTarget;
    }

    public SalvoPacket(FriendlyByteBuf buf) {
        this.strike = buf.readBoolean();
        int count = Math.min(SalvoScheduler.MAX_LAUNCHERS, buf.readVarInt());
        this.launchers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            this.launchers.add(buf.readBlockPos());
        }
        this.gapTicks = Math.max(0, Math.min(200, buf.readVarInt()));
        this.spread = Math.max(0, Math.min(50, buf.readVarInt()));
        this.timeOnTarget = buf.readBoolean();
        this.main = buf.readBoolean() ? buf.readBlockPos() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(strike);
        int count = Math.min(SalvoScheduler.MAX_LAUNCHERS, launchers.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buf.writeBlockPos(launchers.get(i));
        }
        buf.writeVarInt(gapTicks);
        buf.writeVarInt(spread);
        buf.writeBoolean(timeOnTarget);
        buf.writeBoolean(main != null);
        if (main != null) {
            buf.writeBlockPos(main);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                SalvoScheduler.schedule(player, strike, launchers, gapTicks, spread, timeOnTarget, main);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
