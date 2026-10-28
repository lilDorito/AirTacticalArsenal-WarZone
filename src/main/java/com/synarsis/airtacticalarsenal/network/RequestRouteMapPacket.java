package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.radar.ServerRouteMapScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestRouteMapPacket {
    private final BlockPos centerPos;
    private final int bpp;

    public RequestRouteMapPacket(BlockPos centerPos, int bpp) {
        this.centerPos = centerPos;
        this.bpp = bpp;
    }

    public RequestRouteMapPacket(FriendlyByteBuf buf) {
        this.centerPos = buf.readBlockPos();
        this.bpp = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(centerPos);
        buf.writeVarInt(bpp);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();

            int scale = ServerRouteMapScanner.sanitizeBpp(bpp);
            ServerRouteMapScanner scanner = ServerRouteMapScanner.getOrCreate(centerPos, scale, level);
            BlockPos actualCenter = scanner.getCenterPos();

            if (scanner.isReady()) {
                byte[] png = scanner.getPngBytes();
                if (png != null) {
                    sendPngToPlayer(player, png, actualCenter, scale);
                }
            } else {
                int queuePos = ServerRouteMapScanner.getQueuePosition(centerPos, scale);
                if (queuePos > 1) {
                    NetworkHandler.sendToPlayer(player,
                            new RouteMapDataPacket(actualCenter, scale, RouteMapDataPacket.STATUS_QUEUED, (float)(queuePos - 1), null));
                } else {
                    NetworkHandler.sendToPlayer(player,
                            new RouteMapDataPacket(actualCenter, scale, RouteMapDataPacket.STATUS_BUILDING, scanner.getProgress(), null));
                }
            }
        });
        return true;
    }

    private void sendPngToPlayer(ServerPlayer player, byte[] png, BlockPos regionCenter, int scale) {
        int chunkSize = 250000;
        int totalChunks = (png.length + chunkSize - 1) / chunkSize;

        for (int i = 0; i < totalChunks; i++) {
            int offset = i * chunkSize;
            int len = Math.min(chunkSize, png.length - offset);
            byte[] chunk = new byte[len];
            System.arraycopy(png, offset, chunk, 0, len);
            NetworkHandler.sendToPlayer(player,
                    new RouteMapDataPacket(regionCenter, scale, RouteMapDataPacket.STATUS_DATA, i, totalChunks, chunk));
        }
    }
}
