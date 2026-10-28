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
    private final int tabletRange;
    private final java.util.Map<String, Double> scales;

    public RouteLimitsPacket(int shahedMax, int orlanMax, int iskanderMin, int iskanderMax, int radarRange,
                             int tabletRange, java.util.Map<String, Double> scales) {
        this.scales = scales;
        this.shahedMax = shahedMax;
        this.orlanMax = orlanMax;
        this.iskanderMin = iskanderMin;
        this.iskanderMax = iskanderMax;
        this.radarRange = radarRange;
        this.tabletRange = tabletRange;
    }

    public static RouteLimitsPacket fromConfig() {
        return new RouteLimitsPacket(
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getShahedMaxRouteDistance(),
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getOrlanMaxRouteDistance(),
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getIskanderMinDistance(),
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getIskanderMaxRange(),
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getRadarRange(),
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getTabletLauncherRange(),
                com.synarsis.airtacticalarsenal.api.DroneVariants.routeScales());
    }

    public RouteLimitsPacket(FriendlyByteBuf buf) {
        this.shahedMax = buf.readVarInt();
        this.orlanMax = buf.readVarInt();
        this.iskanderMin = buf.readVarInt();
        this.iskanderMax = buf.readVarInt();
        this.radarRange = buf.readVarInt();
        this.tabletRange = buf.readVarInt();
        int count = buf.readVarInt();
        this.scales = new java.util.HashMap<>();
        for (int i = 0; i < count; i++) {
            this.scales.put(buf.readUtf(256), buf.readDouble());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(shahedMax);
        buf.writeVarInt(orlanMax);
        buf.writeVarInt(iskanderMin);
        buf.writeVarInt(iskanderMax);
        buf.writeVarInt(radarRange);
        buf.writeVarInt(tabletRange);
        buf.writeVarInt(scales.size());
        for (java.util.Map.Entry<String, Double> entry : scales.entrySet()) {
            buf.writeUtf(entry.getKey(), 256);
            buf.writeDouble(entry.getValue());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(shahedMax, orlanMax, iskanderMin, iskanderMax, radarRange, tabletRange, scales)));
        ctx.setPacketHandled(true);
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(int shahedMax, int orlanMax, int iskanderMin, int iskanderMax, int radarRange,
                                     int tabletRange, java.util.Map<String, Double> scales) {
        com.synarsis.airtacticalarsenal.client.ClientRouteLimits.set(shahedMax, orlanMax, iskanderMin, iskanderMax, radarRange, tabletRange);
        com.synarsis.airtacticalarsenal.client.ClientRouteLimits.setScales(scales);
    }
}
