package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.client.gui.ReconUavTabletScreen;
import com.synarsis.airtacticalarsenal.client.gui.StrikeUavTabletScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void openStrikeUavTabletScreen(List<BlockPos> positions, List<Boolean> drones, 
                                               List<BlockPos> targets, List<Float> yawList, Map<Integer, int[]> coords,
                                               List<List<BlockPos>> routes, List<int[]> flyingShaheds) {
        Minecraft.getInstance().setScreen(new StrikeUavTabletScreen(positions, drones, targets, yawList, coords, routes, flyingShaheds));
    }

    public static void openReconUavTabletScreen(List<BlockPos> positions, List<Boolean> drones,
                                              List<Float> yawList, Map<Integer, int[]> coords,
                                              List<int[]> activeOrlans, List<List<BlockPos>> routes) {
        Minecraft.getInstance().setScreen(new ReconUavTabletScreen(positions, drones, yawList, coords, activeOrlans, routes));
    }
}
