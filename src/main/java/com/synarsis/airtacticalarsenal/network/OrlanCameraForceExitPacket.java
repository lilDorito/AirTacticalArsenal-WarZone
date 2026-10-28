package com.synarsis.airtacticalarsenal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OrlanCameraForceExitPacket {

    private final boolean lost;

    public OrlanCameraForceExitPacket() {
        this(false);
    }

    public OrlanCameraForceExitPacket(boolean lost) {
        this.lost = lost;
    }

    public OrlanCameraForceExitPacket(FriendlyByteBuf buf) {
        this.lost = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(lost);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        boolean signalLost = this.lost;
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (signalLost) {
                    com.synarsis.airtacticalarsenal.client.OrlanCameraHandler.signalLost(false);
                } else {
                    com.synarsis.airtacticalarsenal.client.OrlanCameraHandler.forceStopCameraLocal();
                }
            })
        );
        ctx.get().setPacketHandled(true);
    }
}
