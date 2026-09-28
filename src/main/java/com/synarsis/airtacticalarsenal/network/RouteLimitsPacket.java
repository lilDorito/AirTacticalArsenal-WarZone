package com.synarsis.airtacticalarsenal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RouteLimitsPacket {
    private final int shahedMax;
    private final int orlanMax;

    public RouteLimitsPacket(int shahedMax, int orlanMax) {
        this.shahedMax = shahedMax;
        this.orlanMax = orlanMax;
    }

    public RouteLimitsPacket(FriendlyByteBuf buf) {
        this.shahedMax = buf.readVarInt();
        this.orlanMax = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(shahedMax);
        buf.writeVarInt(orlanMax);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(shahedMax, orlanMax)));
        ctx.setPacketHandled(true);
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(int shahedMax, int orlanMax) {
        com.synarsis.airtacticalarsenal.client.ClientRouteLimits.set(shahedMax, orlanMax);
    }
}
