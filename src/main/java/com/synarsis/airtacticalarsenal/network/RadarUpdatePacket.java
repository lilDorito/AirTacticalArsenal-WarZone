package com.synarsis.airtacticalarsenal.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RadarUpdatePacket {
    private final BlockPos radarPos;
    private final List<TargetData> targets;
    private final int minAltitude;
    private final boolean openSky;
    private final int alarmState;
    private final boolean canToggleAlarm;

    public static class TargetData {
        public final int entityId;
        public final byte type; 
        public final Vec3 pos;
        public final float heading;
        public final float speed;
        public final boolean friendly;

        public TargetData(int entityId, byte type, Vec3 pos, float heading, float speed, boolean friendly) {
            this.entityId = entityId;
            this.type = type;
            this.pos = pos;
            this.heading = heading;
            this.speed = speed;
            this.friendly = friendly;
        }
    }

    public static final int ALARM_MANUAL = 1;
    public static final int ALARM_DECLARED = 2;
    public static final int ALARM_RELAYED = 4;

    public static int alarmState(boolean manual, boolean declared, boolean relayed) {
        return (manual ? ALARM_MANUAL : 0) | (declared ? ALARM_DECLARED : 0) | (relayed ? ALARM_RELAYED : 0);
    }

    public RadarUpdatePacket(BlockPos radarPos, List<TargetData> targets, int minAltitude, boolean openSky,
                             int alarmState, boolean canToggleAlarm) {
        this.radarPos = radarPos;
        this.targets = targets;
        this.minAltitude = minAltitude;
        this.openSky = openSky;
        this.alarmState = alarmState;
        this.canToggleAlarm = canToggleAlarm;
    }

    public RadarUpdatePacket(FriendlyByteBuf buf) {
        this.radarPos = buf.readBlockPos();
        this.minAltitude = buf.readInt();
        this.openSky = buf.readBoolean();
        this.alarmState = buf.readByte();
        this.canToggleAlarm = buf.readBoolean();
        int count = buf.readInt();
        this.targets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            targets.add(new TargetData(
                    buf.readInt(),
                    buf.readByte(),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readBoolean()
            ));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.radarPos);
        buf.writeInt(this.minAltitude);
        buf.writeBoolean(this.openSky);
        buf.writeByte(this.alarmState);
        buf.writeBoolean(this.canToggleAlarm);
        buf.writeInt(this.targets.size());
        for (TargetData t : targets) {
            buf.writeInt(t.entityId);
            buf.writeByte(t.type);
            buf.writeDouble(t.pos.x);
            buf.writeDouble(t.pos.y);
            buf.writeDouble(t.pos.z);
            buf.writeFloat(t.heading);
            buf.writeFloat(t.speed);
            buf.writeBoolean(t.friendly);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(radarPos, targets, minAltitude, openSky, alarmState, canToggleAlarm));
        });
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(BlockPos radarPos, List<TargetData> targets, int minAltitude, boolean openSky,
                                     int alarmState, boolean canToggleAlarm) {
        com.synarsis.airtacticalarsenal.client.gui.ShahedRadarScreen.updateTargets(radarPos, targets, minAltitude, openSky,
                alarmState, canToggleAlarm);
    }
}
