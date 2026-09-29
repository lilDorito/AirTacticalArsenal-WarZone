package com.synarsis.airtacticalarsenal.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenRadarScreenPacket {
    private final BlockPos pos;
    private final boolean darkBackground;

    public OpenRadarScreenPacket(BlockPos pos, boolean darkBackground) {
        this.pos = pos;
        this.darkBackground = darkBackground;
    }

    public OpenRadarScreenPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.darkBackground = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeBoolean(this.darkBackground);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(this.pos, this.darkBackground));
        });
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(BlockPos pos, boolean darkBackground) {
        com.synarsis.airtacticalarsenal.client.gui.ShahedRadarScreen.open(pos, darkBackground);
    }
}
