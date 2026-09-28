package com.synarsis.airtacticalarsenal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LanguagePolicyPacket {
    private final boolean allowClientLanguage;

    public LanguagePolicyPacket(boolean allowClientLanguage) {
        this.allowClientLanguage = allowClientLanguage;
    }

    public LanguagePolicyPacket(FriendlyByteBuf buf) {
        this.allowClientLanguage = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(allowClientLanguage);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(allowClientLanguage)));
        ctx.setPacketHandled(true);
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(boolean allow) {
        com.synarsis.airtacticalarsenal.client.lang.AtaLanguage.setServerAllowsClientLanguage(allow);
    }
}
