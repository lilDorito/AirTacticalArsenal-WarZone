package com.synarsis.airtacticalarsenal.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenUavTabletPacket {

    public record LauncherInfo(BlockPos pos, float yaw, int role, String nameKey, boolean heavy, List<BlockPos> route, String kind) {
        public boolean hasDrone() {
            return role != 0;
        }

        public boolean hasRoute() {
            return route != null && !route.isEmpty();
        }
    }

    public record FlyingInfo(int entityId, boolean recon, String nameKey, int x, int z, int targetX, int targetZ,
                             int phase, int remainingTicks, boolean camera, String kind) {
    }

    private final List<LauncherInfo> launchers;
    private final List<FlyingInfo> flying;
    private final boolean refresh;

    public OpenUavTabletPacket(List<LauncherInfo> launchers, List<FlyingInfo> flying, boolean refresh) {
        this.launchers = launchers;
        this.flying = flying;
        this.refresh = refresh;
    }

    public OpenUavTabletPacket(FriendlyByteBuf buf) {
        this.refresh = buf.readBoolean();
        int launcherCount = buf.readVarInt();
        this.launchers = new ArrayList<>(launcherCount);
        for (int i = 0; i < launcherCount; i++) {
            BlockPos pos = buf.readBlockPos();
            float yaw = buf.readFloat();
            int role = buf.readVarInt();
            String nameKey = buf.readUtf(128);
            boolean heavy = buf.readBoolean();
            int routeSize = buf.readVarInt();
            List<BlockPos> route = new ArrayList<>(routeSize);
            for (int j = 0; j < routeSize; j++) {
                route.add(buf.readBlockPos());
            }
            String kind = buf.readUtf(256);
            this.launchers.add(new LauncherInfo(pos, yaw, role, nameKey, heavy, route, kind));
        }
        int flyingCount = buf.readVarInt();
        this.flying = new ArrayList<>(flyingCount);
        for (int i = 0; i < flyingCount; i++) {
            this.flying.add(new FlyingInfo(buf.readInt(), buf.readBoolean(), buf.readUtf(128),
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readUtf(256)));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(refresh);
        buf.writeVarInt(launchers.size());
        for (LauncherInfo launcher : launchers) {
            buf.writeBlockPos(launcher.pos());
            buf.writeFloat(launcher.yaw());
            buf.writeVarInt(launcher.role());
            buf.writeUtf(launcher.nameKey(), 128);
            buf.writeBoolean(launcher.heavy());
            buf.writeVarInt(launcher.route().size());
            for (BlockPos wp : launcher.route()) {
                buf.writeBlockPos(wp);
            }
            buf.writeUtf(launcher.kind() == null ? "" : launcher.kind(), 256);
        }
        buf.writeVarInt(flying.size());
        for (FlyingInfo drone : flying) {
            buf.writeInt(drone.entityId());
            buf.writeBoolean(drone.recon());
            buf.writeUtf(drone.nameKey(), 128);
            buf.writeInt(drone.x());
            buf.writeInt(drone.z());
            buf.writeInt(drone.targetX());
            buf.writeInt(drone.targetZ());
            buf.writeVarInt(Math.max(0, drone.phase()));
            buf.writeVarInt(Math.max(0, drone.remainingTicks()));
            buf.writeBoolean(drone.camera());
            buf.writeUtf(drone.kind() == null ? "" : drone.kind(), 256);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(launchers, flying, refresh)));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(List<LauncherInfo> launchers, List<FlyingInfo> flying, boolean refresh) {
        com.synarsis.airtacticalarsenal.client.ClientPacketHandler.openUavTabletScreen(launchers, flying, refresh);
    }
}
