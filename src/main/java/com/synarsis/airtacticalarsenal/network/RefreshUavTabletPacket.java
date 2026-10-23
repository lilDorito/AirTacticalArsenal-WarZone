package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.item.UavTabletItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RefreshUavTabletPacket {

    public RefreshUavTabletPacket() {
    }

    public RefreshUavTabletPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ItemStack tablet = UavTabletItem.heldTablet(player);
            if (tablet != null) {
                UavTabletItem.open(player, tablet, true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
