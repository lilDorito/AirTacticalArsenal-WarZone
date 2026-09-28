package com.synarsis.airtacticalarsenal.event;

import com.synarsis.airtacticalarsenal.entity.IskanderEntity;
import com.synarsis.airtacticalarsenal.network.IskanderSpawnPacket;
import com.synarsis.airtacticalarsenal.network.NetworkHandler;
import com.synarsis.airtacticalarsenal.radar.ServerRouteMapScanner;
import com.synarsis.airtacticalarsenal.radar.ServerTerrainScanner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerForgeEvents {

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof IskanderEntity iskander
                && event.getEntity() instanceof ServerPlayer serverPlayer
                && iskander.isLegitSpawn) {
            NetworkHandler.sendToPlayer(serverPlayer, new IskanderSpawnPacket(
                    iskander.getId(),
                    iskander.position(),
                    iskander.getTargetPos(),
                    iskander.getFlightPhase(),
                    iskander.getFlightTicks(),
                    null,
                    iskander.getStartPos(),
                    iskander.getLaunchEndPos(),
                    iskander.getFinalTargetPos()
            ));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToPlayer(serverPlayer, new com.synarsis.airtacticalarsenal.network.LanguagePolicyPacket(
                    com.synarsis.airtacticalarsenal.config.ShahedConfig.isClientLanguageAllowed()));
            NetworkHandler.sendToPlayer(serverPlayer, new com.synarsis.airtacticalarsenal.network.RouteLimitsPacket(
                    com.synarsis.airtacticalarsenal.config.ShahedConfig.getShahedMaxRouteDistance(),
                    com.synarsis.airtacticalarsenal.config.ShahedConfig.getOrlanMaxRouteDistance()));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerLevel overworld = server.overworld();
        if (overworld != null) {

            long terrainOrder = ServerTerrainScanner.getFirstBuildingOrder();
            long routeOrder = ServerRouteMapScanner.getFirstBuildingOrder();
            if (terrainOrder <= routeOrder) {
                if (!ServerTerrainScanner.tickAll(overworld)) {
                    ServerRouteMapScanner.tickAll(overworld);
                }
            } else {
                if (!ServerRouteMapScanner.tickAll(overworld)) {
                    ServerTerrainScanner.tickAll(overworld);
                }
            }
        }
    }
}
