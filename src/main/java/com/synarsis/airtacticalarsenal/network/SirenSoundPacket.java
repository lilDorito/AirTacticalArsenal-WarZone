package com.synarsis.airtacticalarsenal.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SirenSoundPacket {
    public static final int PLAY = 0;
    public static final int STOP = 1;
    public static final int ALL_CLEAR = 2;

    private final BlockPos pos;
    private final int mode;

    public SirenSoundPacket(BlockPos pos) {
        this(pos, PLAY);
    }

    public SirenSoundPacket(BlockPos pos, boolean stop) {
        this(pos, stop ? STOP : PLAY);
    }

    public SirenSoundPacket(BlockPos pos, int mode) {
        this.pos = pos;
        this.mode = mode;
    }

    public SirenSoundPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.mode = buf.readByte();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeByte(this.mode);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> handleClient());
        });
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        if (this.mode == STOP) {
            com.synarsis.airtacticalarsenal.client.sound.SirenSoundInstance.stopSiren(this.pos);
        } else if (this.mode == ALL_CLEAR) {
            com.synarsis.airtacticalarsenal.client.sound.SirenSoundInstance.playAllClear(this.pos);
        } else {
            com.synarsis.airtacticalarsenal.client.sound.SirenSoundInstance.playSiren(this.pos);
        }
    }
}
