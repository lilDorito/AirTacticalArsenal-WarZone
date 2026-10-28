package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.client.gui.UavTabletScreen;
import com.synarsis.airtacticalarsenal.network.OpenUavTabletPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void openUavTabletScreen(List<OpenUavTabletPacket.LauncherInfo> launchers,
                                           List<OpenUavTabletPacket.FlyingInfo> flying, boolean refresh) {
        Map<Integer, String> names = new HashMap<>();
        for (OpenUavTabletPacket.FlyingInfo drone : flying) {
            names.put(drone.entityId(), drone.nameKey());
        }
        ClientDroneNames.putAll(names);
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof UavTabletScreen open) {
            open.update(launchers, flying);
        } else if (mc.screen instanceof com.synarsis.airtacticalarsenal.client.gui.StrategicMapScreen map) {
            map.update(launchers, flying);
        } else if (!refresh) {
            mc.setScreen(new UavTabletScreen(launchers, flying));
        }
    }
}
