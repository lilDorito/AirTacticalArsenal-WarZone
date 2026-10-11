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
    private final int iskanderMin;
    private final int iskanderMax;
    private final int radarRange;

    public RouteLimitsPacket(int shahedMax, int orlanMax, int iskanderMin, int iskanderMax, int radarRange) {
        this.shahedMax = shahedMax;
        this.orlanMax = orlanMax;
        this.iskanderMin = iskanderMin;
        this.iskanderMax = iskanderMax;
        this.radarRange = radarRange;
    }

    public static RouteLimitsPacket fromConfig() {
        return new RouteLimitsPacket(
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getShahedMaxRouteDistance(),
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getOrlanMaxRouteDistance(),
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getIskanderMinDistance(),
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getIskanderMaxRange(),
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getRadarRange());
    }

    public RouteLimitsPacket(FriendlyByteBuf buf) {
        this.shahedMax = buf.readVarInt();
        this.orlanMax = buf.readVarInt();
        this.iskanderMin = buf.readVarInt();
        this.iskanderMax = buf.readVarInt();
        this.radarRange = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(shahedMax);
        buf.writeVarInt(orlanMax);
        buf.writeVarInt(iskanderMin);
        buf.writeVarInt(iskanderMax);
        buf.writeVarInt(radarRange);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(shahedMax, orlanMax, iskanderMin, iskanderMax, radarRange)));
        ctx.setPacketHandled(true);
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(int shahedMax, int orlanMax, int iskanderMin, int iskanderMax, int radarRange) {
        com.synarsis.airtacticalarsenal.client.ClientRouteLimits.set(shahedMax, orlanMax, iskanderMin, iskanderMax, radarRange);
    }
}
