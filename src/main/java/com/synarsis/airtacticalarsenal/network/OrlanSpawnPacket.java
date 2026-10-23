package com.synarsis.airtacticalarsenal.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OrlanSpawnPacket {
    private final int entityId;
    private final double x, y, z;
    private final double motionX, motionY, motionZ;
    private final BlockPos target;
    private final int phaseOrdinal;
    private final int remainingTicks;
    private final String typeKey;

    public OrlanSpawnPacket(int entityId, Vec3 pos, Vec3 motion, BlockPos target, int phaseOrdinal, int remainingTicks) {
        this(entityId, pos, motion, target, phaseOrdinal, remainingTicks, "");
    }

    public OrlanSpawnPacket(int entityId, Vec3 pos, Vec3 motion, BlockPos target, int phaseOrdinal, int remainingTicks, String typeKey) {
        this.typeKey = typeKey == null ? "" : typeKey;
        this.entityId = entityId;
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.motionX = motion.x;
        this.motionY = motion.y;
        this.motionZ = motion.z;
        this.target = target;
        this.phaseOrdinal = phaseOrdinal;
        this.remainingTicks = remainingTicks;
    }

    public OrlanSpawnPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.motionX = buf.readDouble();
        this.motionY = buf.readDouble();
        this.motionZ = buf.readDouble();
        this.target = buf.readBlockPos();
        this.phaseOrdinal = buf.readInt();
        this.remainingTicks = buf.readInt();
        this.typeKey = buf.readUtf(256);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(motionX);
        buf.writeDouble(motionY);
        buf.writeDouble(motionZ);
        buf.writeBlockPos(target);
        buf.writeInt(phaseOrdinal);
        buf.writeInt(remainingTicks);
        buf.writeUtf(typeKey, 256);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(entityId, x, y, z, motionX, motionY, motionZ, phaseOrdinal, remainingTicks, typeKey));
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(int entityId, double x, double y, double z, double motionX, double motionY, double motionZ, int phaseOrdinal, int remainingTicks, String typeKey) {
        com.synarsis.airtacticalarsenal.network.ClientPacketHandler.handleOrlanSpawn(entityId, x, y, z, motionX, motionY, motionZ, phaseOrdinal, remainingTicks, typeKey);
    }
}