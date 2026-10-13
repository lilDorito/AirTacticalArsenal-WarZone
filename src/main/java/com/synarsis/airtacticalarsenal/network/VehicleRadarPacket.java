package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.radar.vehicle.VehicleRadarProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class VehicleRadarPacket {

    public static final class Contact {
        public final int id;
        public final byte type;
        public final double x;
        public final float y;
        public final double z;
        public final float heading;
        public final boolean friendly;

        public Contact(int id, byte type, double x, float y, double z, float heading, boolean friendly) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.heading = heading;
            this.friendly = friendly;
        }
    }

    public final boolean active;
    public final VehicleRadarProfile.Kind kind;
    public final VehicleRadarProfile.Shape shape;
    public final int range;
    public final int halfAngle;
    public final int sweepTicks;
    public final int refreshTicks;
    public final float beam;
    public final int beamDir;
    public final boolean full;
    public final List<Contact> contacts;

    public static VehicleRadarPacket off() {
        return new VehicleRadarPacket(false, VehicleRadarProfile.Kind.MSA, VehicleRadarProfile.Shape.ROUND, 0, 0, 1, 1,
                0, 1, true, List.of());
    }

    public VehicleRadarPacket(boolean active, VehicleRadarProfile.Kind kind, VehicleRadarProfile.Shape shape, int range,
                              int halfAngle, int sweepTicks, int refreshTicks, float beam, int beamDir, boolean full,
                              List<Contact> contacts) {
        this.active = active;
        this.kind = kind;
        this.shape = shape;
        this.range = range;
        this.halfAngle = halfAngle;
        this.sweepTicks = sweepTicks;
        this.refreshTicks = refreshTicks;
        this.beam = beam;
        this.beamDir = beamDir >= 0 ? 1 : -1;
        this.full = full;
        this.contacts = contacts;
    }

    public VehicleRadarPacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
        if (!this.active) {
            this.kind = VehicleRadarProfile.Kind.MSA;
            this.shape = VehicleRadarProfile.Shape.ROUND;
            this.range = 0;
            this.halfAngle = 0;
            this.sweepTicks = 1;
            this.refreshTicks = 1;
            this.beam = 0;
            this.beamDir = 1;
            this.full = true;
            this.contacts = List.of();
            return;
        }
        this.kind = buf.readBoolean() ? VehicleRadarProfile.Kind.AESA : VehicleRadarProfile.Kind.MSA;
        this.shape = buf.readBoolean() ? VehicleRadarProfile.Shape.CONE : VehicleRadarProfile.Shape.ROUND;
        this.range = buf.readVarInt();
        this.halfAngle = buf.readVarInt();
        this.sweepTicks = buf.readVarInt();
        this.refreshTicks = buf.readVarInt();
        this.beam = buf.readFloat();
        this.beamDir = buf.readBoolean() ? 1 : -1;
        this.full = buf.readBoolean();
        int n = Math.min(buf.readVarInt(), 512);
        List<Contact> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(new Contact(buf.readVarInt(), buf.readByte(), buf.readDouble(), buf.readFloat(), buf.readDouble(),
                    buf.readFloat(), buf.readBoolean()));
        }
        this.contacts = list;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.active);
        if (!this.active) return;
        buf.writeBoolean(this.kind == VehicleRadarProfile.Kind.AESA);
        buf.writeBoolean(this.shape == VehicleRadarProfile.Shape.CONE);
        buf.writeVarInt(this.range);
        buf.writeVarInt(this.halfAngle);
        buf.writeVarInt(this.sweepTicks);
        buf.writeVarInt(this.refreshTicks);
        buf.writeFloat(this.beam);
        buf.writeBoolean(this.beamDir > 0);
        buf.writeBoolean(this.full);
        int n = Math.min(this.contacts.size(), 512);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            Contact c = this.contacts.get(i);
            buf.writeVarInt(c.id);
            buf.writeByte(c.type);
            buf.writeDouble(c.x);
            buf.writeFloat(c.y);
            buf.writeDouble(c.z);
            buf.writeFloat(c.heading);
            buf.writeBoolean(c.friendly);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(this)));
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(VehicleRadarPacket packet) {
        com.synarsis.airtacticalarsenal.client.radar.VehicleRadarClient.accept(packet);
    }
}
