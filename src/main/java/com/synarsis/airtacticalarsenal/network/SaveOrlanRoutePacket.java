package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.entity.UavLauncherEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SaveOrlanRoutePacket {

    private final BlockPos launcherPos;
    private final List<BlockPos> waypoints;

    public SaveOrlanRoutePacket(BlockPos launcherPos, List<BlockPos> waypoints) {
        this.launcherPos = launcherPos;
        this.waypoints = waypoints != null ? new ArrayList<>(waypoints) : new ArrayList<>();
    }

    public SaveOrlanRoutePacket(FriendlyByteBuf buf) {
        this.launcherPos = buf.readBlockPos();
        int count = buf.readVarInt();
        this.waypoints = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            this.waypoints.add(buf.readBlockPos());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(launcherPos);
        buf.writeVarInt(waypoints.size());
        for (BlockPos wp : waypoints) {
            buf.writeBlockPos(wp);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            UavLauncherEntity launcher = findLauncher(level, launcherPos);

            if (launcher == null) {
                player.displayClientMessage(Component.translatable("message.ata.launcher.not_found"), true);
                return;
            }

            double playerDistance = player.position().distanceTo(launcher.position());
            if (playerDistance > com.synarsis.airtacticalarsenal.config.ShahedConfig.getTabletLauncherRange()) {
                player.displayClientMessage(Component.translatable("message.ata.launcher.too_far_max", com.synarsis.airtacticalarsenal.config.ShahedConfig.getTabletLauncherRange()), true);
                return;
            }

            if (!com.synarsis.airtacticalarsenal.util.RouteLimits.check(player,
                    com.synarsis.airtacticalarsenal.util.RouteLimits.pathLength(launcher.blockPosition(), waypoints), com.synarsis.airtacticalarsenal.util.RouteLimits.orlanMax(launcher.getDroneKind()))) {
                return;
            }
            launcher.setSavedRoute(waypoints);

            if (waypoints.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.ata.route.cleared"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.ata.route.saved", waypoints.size()), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private UavLauncherEntity findLauncher(ServerLevel level, BlockPos pos) {
        AABB searchBox = new AABB(pos).inflate(2.0);
        List<UavLauncherEntity> launchers = level.getEntitiesOfClass(
                UavLauncherEntity.class, searchBox);
        return launchers.isEmpty() ? null : launchers.get(0);
    }
}
