package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.radar.vehicle.VehicleRadarProfile;
import com.synarsis.airtacticalarsenal.radar.vehicle.VehicleRadarProfiles;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class VehicleRadarSyncPacket {
    private final Map<ResourceLocation, VehicleRadarProfile> radars;
    private final Map<ResourceLocation, Float> signatures;

    public VehicleRadarSyncPacket(Map<ResourceLocation, VehicleRadarProfile> radars, Map<ResourceLocation, Float> signatures) {
        this.radars = radars;
        this.signatures = signatures;
    }

    public VehicleRadarSyncPacket(FriendlyByteBuf buf) {
        int radarCount = buf.readVarInt();
        this.radars = new HashMap<>();
        for (int i = 0; i < radarCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            VehicleRadarProfile.Kind kind = buf.readEnum(VehicleRadarProfile.Kind.class);
            VehicleRadarProfile.Shape shape = buf.readEnum(VehicleRadarProfile.Shape.class);
            this.radars.put(id, new VehicleRadarProfile(kind, shape, buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        int signatureCount = buf.readVarInt();
        this.signatures = new HashMap<>();
        for (int i = 0; i < signatureCount; i++) {
            this.signatures.put(buf.readResourceLocation(), buf.readFloat());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(radars.size());
        for (Map.Entry<ResourceLocation, VehicleRadarProfile> entry : radars.entrySet()) {
            VehicleRadarProfile p = entry.getValue();
            buf.writeResourceLocation(entry.getKey());
            buf.writeEnum(p.kind);
            buf.writeEnum(p.shape);
            buf.writeVarInt(p.range);
            buf.writeVarInt(p.halfAngle);
            buf.writeVarInt(p.sweepTicks);
            buf.writeVarInt(p.refreshTicks);
            buf.writeVarInt(p.minAltitude);
        }
        buf.writeVarInt(signatures.size());
        for (Map.Entry<ResourceLocation, Float> entry : signatures.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeFloat(entry.getValue());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> VehicleRadarProfiles.setClientData(radars, signatures));
        ctx.get().setPacketHandled(true);
    }
}
