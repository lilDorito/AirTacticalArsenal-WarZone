package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.entity.OrlanEntity;
import com.synarsis.airtacticalarsenal.entity.UavLauncherEntity;
import com.synarsis.airtacticalarsenal.util.LauncherZoneValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class LaunchFromReconUavTabletPacket {

    private final BlockPos launcherPos;
    private final int targetX;
    private final int targetZ;

    public LaunchFromReconUavTabletPacket(BlockPos launcherPos, int targetX, int targetZ) {
        this.launcherPos = launcherPos;
        this.targetX = targetX;
        this.targetZ = targetZ;
    }

    public LaunchFromReconUavTabletPacket(FriendlyByteBuf buf) {
        this.launcherPos = buf.readBlockPos();
        this.targetX = buf.readInt();
        this.targetZ = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(launcherPos);
        buf.writeInt(targetX);
        buf.writeInt(targetZ);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null)
                return;

            ServerLevel level = player.serverLevel();

            UavLauncherEntity launcher = findLauncher(level, launcherPos);

            if (launcher == null) {
                player.displayClientMessage(Component.translatable("message.ata.launcher.not_found"), true);
                return;
            }

            if (!launcher.hasReconDrone()) {
                player.displayClientMessage(Component.translatable("message.ata.launcher.no_orlan"), true);
                return;
            }

            double playerDistance = player.position().distanceTo(launcher.position());
            if (playerDistance > com.synarsis.airtacticalarsenal.config.ShahedConfig.getTabletLauncherRange()) {
                player.displayClientMessage(Component.translatable("message.ata.launcher.too_far_max", com.synarsis.airtacticalarsenal.config.ShahedConfig.getTabletLauncherRange()), true);
                return;
            }

            BlockPos targetPos;
            java.util.List<BlockPos> waypoints = null;

            if (launcher.hasSavedRoute()) {
                java.util.List<BlockPos> route = launcher.getSavedRoute();

                targetPos = route.get(route.size() - 1);

                if (route.size() > 1) {
                    waypoints = new java.util.ArrayList<>(route.subList(0, route.size() - 1));
                }
            } else {
                targetPos = new BlockPos(targetX, 64, targetZ);
            }

            BlockPos firstTarget = (waypoints != null && !waypoints.isEmpty()) ? waypoints.get(0) : targetPos;
            double firstDistance = Math.sqrt(
                    Math.pow(firstTarget.getX() - launcher.blockPosition().getX(), 2) +
                            Math.pow(firstTarget.getZ() - launcher.blockPosition().getZ(), 2));
            if (firstDistance < 600) {
                player.displayClientMessage(Component.translatable("message.ata.orlan.first_point_too_close"),
                        true);
                return;
            }

            float launcherYaw = launcher.getYRot();
            if (!LauncherZoneValidator.isTargetInValidZone(launcher.blockPosition(), launcherYaw, firstTarget)) {
                double angle = LauncherZoneValidator.calculateDeviationAngle(
                        Vec3.atCenterOf(launcher.blockPosition()), launcherYaw, Vec3.atCenterOf(firstTarget));
                player.displayClientMessage(Component.translatable("message.ata.launch.out_of_zone",
                        String.format("%.0f", angle), String.format("%.0f", LauncherZoneValidator.MAX_DEVIATION_ANGLE)), true);
                return;
            }

            if (!level.getFluidState(launcher.blockPosition()).isEmpty()) {
                player.displayClientMessage(Component.translatable("message.ata.launch.underwater"), true);
                return;
            }

            java.util.List<BlockPos> fullRoute = new java.util.ArrayList<>();
            if (waypoints != null) fullRoute.addAll(waypoints);
            fullRoute.add(targetPos);
            if (!com.synarsis.airtacticalarsenal.util.RouteLimits.check(player,
                    com.synarsis.airtacticalarsenal.util.RouteLimits.pathLength(launcher.blockPosition(), fullRoute), com.synarsis.airtacticalarsenal.util.RouteLimits.orlanMax(launcher.getDroneKind()))) {
                return;
            }

            if (launcher.isLaunching()) {
                return;
            }
            launcher.startReconLaunch(player, targetPos, waypoints);
            player.displayClientMessage(Component.translatable("message.ata.launch.success"), true);
        });
        ctx.get().setPacketHandled(true);
    }

    private UavLauncherEntity findLauncher(ServerLevel level, BlockPos pos) {
        AABB searchBox = new AABB(pos).inflate(2.0);
        List<UavLauncherEntity> launchers = level.getEntitiesOfClass(
                UavLauncherEntity.class, searchBox);
        if (!launchers.isEmpty()) {
            return launchers.get(0);
        }
        return null;
    }
}
