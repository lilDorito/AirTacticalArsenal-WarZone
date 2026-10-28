package com.synarsis.airtacticalarsenal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShahedCrashSoundPacket {
    private final int entityId;
    private final Vec3 position;
    private final int mode;

    public ShahedCrashSoundPacket(int entityId, Vec3 position, int mode) {
        this.entityId = entityId;
        this.position = position;
        this.mode = mode;
    }

    public ShahedCrashSoundPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.mode = buf.readByte();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
        buf.writeByte(mode);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(entityId, position, mode)));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(int entityId, Vec3 position, int mode) {
        ClientPacketHandler.handleShahedCrashSound(entityId, position, mode);
    }
}
