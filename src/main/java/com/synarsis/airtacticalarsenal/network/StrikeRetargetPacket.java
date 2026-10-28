package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StrikeRetargetPacket {
    public static final double MAX_RETARGET_DISTANCE = 1300.0;
    private static final double RAY_LENGTH = 1300.0;

    private final int droneId;
    private final boolean ray;
    private final BlockPos target;
    private final Vec3 origin;
    private final Vec3 direction;

    public StrikeRetargetPacket(int droneId, BlockPos target) {
        this.droneId = droneId;
        this.ray = false;
        this.target = target;
        this.origin = Vec3.ZERO;
        this.direction = Vec3.ZERO;
    }

    public StrikeRetargetPacket(int droneId, Vec3 origin, Vec3 direction) {
        this.droneId = droneId;
        this.ray = true;
        this.target = BlockPos.ZERO;
        this.origin = origin;
        this.direction = direction;
    }

    public StrikeRetargetPacket(FriendlyByteBuf buf) {
        this.droneId = buf.readInt();
        this.ray = buf.readBoolean();
        this.target = buf.readBlockPos();
        this.origin = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.direction = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(droneId);
        buf.writeBoolean(ray);
        buf.writeBlockPos(target);
        buf.writeDouble(origin.x);
        buf.writeDouble(origin.y);
        buf.writeDouble(origin.z);
        buf.writeDouble(direction.x);
        buf.writeDouble(direction.y);
        buf.writeDouble(direction.z);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            if (!(level.getEntity(droneId) instanceof ShahedEntity drone) || !drone.hasCamera()) {
                return;
            }
            if (!drone.cameraLink().isLinkedTo(player.getUUID())) {
                return;
            }
            BlockPos aim = ray ? marchLoaded(level, drone, origin, direction) : target;
            if (aim == null) {
                player.displayClientMessage(Component.translatable("message.ata.strike.no_surface"), true);
                return;
            }
            double dx = aim.getX() + 0.5 - drone.getX();
            double dz = aim.getZ() + 0.5 - drone.getZ();
            if (dx * dx + dz * dz > MAX_RETARGET_DISTANCE * MAX_RETARGET_DISTANCE) {
                player.displayClientMessage(Component.translatable("message.ata.strike.retarget_too_far",
                        (int) MAX_RETARGET_DISTANCE), true);
                return;
            }
            ShahedEntity.AttackResult result = drone.commandAttack(aim);
            if (result == ShahedEntity.AttackResult.OK) {
                player.displayClientMessage(Component.translatable("message.ata.strike.retargeted",
                        aim.getX(), aim.getY(), aim.getZ()), true);
            } else if (result == ShahedEntity.AttackResult.TOO_LATE) {
                player.displayClientMessage(Component.translatable("message.ata.strike.too_late"), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    static BlockPos marchLoaded(ServerLevel level, ShahedEntity drone, Vec3 start, Vec3 dir) {
        if (dir.lengthSqr() < 1.0E-6 || start.distanceToSqr(drone.position()) > 400.0) {
            return null;
        }
        Vec3 step = dir.normalize().scale(0.5);
        Vec3 p = start;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < RAY_LENGTH * 2; i++) {
            p = p.add(step);
            pos.set(p.x, p.y, p.z);
            if (pos.getY() < level.getMinBuildHeight()) {
                return null;
            }
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty())) {
                return pos.immutable();
            }
        }
        return null;
    }
}
