package com.synarsis.airtacticalarsenal.client.radar;

import com.synarsis.airtacticalarsenal.network.VehicleRadarPacket;
import com.synarsis.airtacticalarsenal.radar.vehicle.VehicleRadarProfile;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class VehicleRadarClient {

    private static final int TIMEOUT_TICKS = 60;

    static final class Blip {
        byte type;
        double x;
        float y;
        double z;
        float heading;
        boolean friendly;
        int age;
    }

    static boolean active;
    static VehicleRadarProfile.Kind kind = VehicleRadarProfile.Kind.MSA;
    static VehicleRadarProfile.Shape shape = VehicleRadarProfile.Shape.CONE;
    static int range = 1000;
    static int halfAngle = 60;
    static int sweepTicks = 40;
    static int refreshTicks = 5;
    static float beam;
    static float beamPrev;
    static int beamDir = 1;
    static final Map<Integer, Blip> BLIPS = new HashMap<>();
    private static int sincePacket;

    private VehicleRadarClient() {}

    public static void accept(VehicleRadarPacket packet) {
        if (!packet.active) {
            reset();
            return;
        }
        boolean fresh = !active || packet.kind != kind || packet.shape != shape || packet.range != range
                || packet.halfAngle != halfAngle;
        active = true;
        kind = packet.kind;
        shape = packet.shape;
        range = packet.range;
        halfAngle = packet.halfAngle;
        sweepTicks = Math.max(1, packet.sweepTicks);
        refreshTicks = Math.max(1, packet.refreshTicks);
        sincePacket = 0;
        if (fresh) {
            BLIPS.clear();
            beamPrev = packet.beam;
            beam = packet.beam;
        }

        if (kind == VehicleRadarProfile.Kind.MSA) {
            beamDir = packet.beamDir;
            beam = packet.beam;
        }

        Set<Integer> seen = packet.full ? new HashSet<>() : null;
        for (VehicleRadarPacket.Contact c : packet.contacts) {
            Blip b = BLIPS.computeIfAbsent(c.id, id -> new Blip());
            b.type = c.type;
            b.x = c.x;
            b.y = c.y;
            b.z = c.z;
            b.heading = c.heading;
            b.friendly = c.friendly;
            b.age = 0;
            if (seen != null) seen.add(c.id);
        }
        if (seen != null) BLIPS.keySet().retainAll(seen);
    }

    public static void tick() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.getRootVehicle() == mc.player
                || ++sincePacket > TIMEOUT_TICKS) {
            reset();
            return;
        }
        beamPrev = beam;
        if (kind == VehicleRadarProfile.Kind.MSA) {
            if (shape == VehicleRadarProfile.Shape.CONE) {
                float step = 2.0f * halfAngle / sweepTicks;
                beam += beamDir * step;
                if (beam > halfAngle) {
                    beam = 2 * halfAngle - beam;
                    beamDir = -1;
                } else if (beam < -halfAngle) {
                    beam = -2 * halfAngle - beam;
                    beamDir = 1;
                }
            } else {
                beam = (beam + 360.0f / sweepTicks) % 360.0f;
            }
        }
        int life = lifetime();
        BLIPS.values().removeIf(b -> ++b.age > life);
    }

    static int paintPeriod() {
        return kind == VehicleRadarProfile.Kind.AESA ? refreshTicks : sweepTicks;
    }

    private static int lifetime() {
        return kind == VehicleRadarProfile.Kind.AESA ? refreshTicks * 3 + 5 : sweepTicks * 2 + 6;
    }

    static float beamAt(float partialTick) {
        if (shape == VehicleRadarProfile.Shape.ROUND) {
            float d = beam - beamPrev;
            if (d < -180) d += 360;
            return beamPrev + d * partialTick;
        }
        return beamPrev + (beam - beamPrev) * partialTick;
    }

    public static boolean isActive() {
        return active;
    }

    public static void reset() {
        active = false;
        BLIPS.clear();
        sincePacket = 0;
    }
}
