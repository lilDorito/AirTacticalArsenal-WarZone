package com.synarsis.airtacticalarsenal.network;

import com.synarsis.airtacticalarsenal.item.UavTabletItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RemoveLauncherFromTabletPacket {

    private final BlockPos launcherPos;

    public RemoveLauncherFromTabletPacket(BlockPos launcherPos) {
        this.launcherPos = launcherPos;
    }

    public RemoveLauncherFromTabletPacket(FriendlyByteBuf buf) {
        this.launcherPos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(launcherPos);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack tablet = UavTabletItem.heldTablet(player);
            if (tablet == null) {
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.getItem() instanceof UavTabletItem) {
                        tablet = stack;
                        break;
                    }
                }
            }
            if (tablet != null && UavTabletItem.removeLinkedLauncher(tablet, launcherPos)) {
                player.displayClientMessage(Component.translatable("message.ata.tablet.launcher_unlinked"), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
