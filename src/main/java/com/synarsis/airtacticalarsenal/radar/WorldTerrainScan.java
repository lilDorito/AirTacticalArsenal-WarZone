package com.synarsis.airtacticalarsenal.radar;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WorldTerrainScan {

    private static final Logger LOGGER = LoggerFactory.getLogger("ATA-TerrainScan");
    private static final long BUDGET_MS = 25;

    private static WorldTerrainScan active;

    private final ArrayDeque<int[]> tiles;
    private final int total;
    private final boolean overwrite;
    private final CommandSourceStack source;
    private MapScanJob job;
    private int jobTx, jobTz;
    private int done;
    private int lastReported;

    private WorldTerrainScan(List<int[]> tiles, boolean overwrite, CommandSourceStack source) {
        this.tiles = new ArrayDeque<>(tiles);
        this.total = tiles.size();
        this.overwrite = overwrite;
        this.source = source;
    }

    public static synchronized boolean isActive() {
        return active != null;
    }

    public static synchronized int percent() {
        return active == null ? 100 : active.done * 100 / Math.max(1, active.total);
    }

    public static synchronized int start(int size, int centerX, int centerZ, boolean overwrite, CommandSourceStack source) {
        int half = size / 2;
        int minTx = TerrainTileStore.tileOf(centerX - half), maxTx = TerrainTileStore.tileOf(centerX - half + size - 1);
        int minTz = TerrainTileStore.tileOf(centerZ - half), maxTz = TerrainTileStore.tileOf(centerZ - half + size - 1);
        List<int[]> list = new ArrayList<>();
        for (int tz = minTz; tz <= maxTz; tz++) {
            for (int tx = minTx; tx <= maxTx; tx++) list.add(new int[]{tx, tz});
        }
        int ctx = TerrainTileStore.tileOf(centerX), ctz = TerrainTileStore.tileOf(centerZ);
        list.sort(Comparator.comparingInt(t -> (t[0] - ctx) * (t[0] - ctx) + (t[1] - ctz) * (t[1] - ctz)));
        active = new WorldTerrainScan(list, overwrite, source);
        LOGGER.info("[ATA] Terrain scan started: {} tiles ({}x{} blocks around {}, {})", list.size(), size, size, centerX, centerZ);
        return list.size();
    }

    public static synchronized boolean cancel() {
        if (active == null) return false;
        active = null;
        LOGGER.info("[ATA] Terrain scan cancelled");
        return true;
    }

    public static synchronized boolean tick(ServerLevel level) {
        if (active == null) return false;
        active.tick0(level);
        return true;
    }

    private void tick0(ServerLevel level) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < BUDGET_MS) {
            if (job == null) {
                int[] t = tiles.poll();
                if (t == null) {
                    finish(level);
                    return;
                }
                if (!overwrite && TerrainTileStore.has(level, t[0], t[1])) {
                    done++;
                    continue;
                }
                jobTx = t[0];
                jobTz = t[1];
                job = new MapScanJob(TerrainTileStore.TILE_PX, TerrainTileStore.STEP,
                        jobTx * TerrainTileStore.TILE_BLOCKS, jobTz * TerrainTileStore.TILE_BLOCKS, false, true);
            }
            long left = BUDGET_MS - (System.currentTimeMillis() - start);
            if (!job.tick(level, Math.max(1, left))) return;
            TerrainTileStore.put(level, jobTx, jobTz, job.finish());
            job = null;
            done++;
            report();
        }
    }

    private void report() {
        int pct = done * 100 / Math.max(1, total);
        if (pct / 10 > lastReported / 10) {
            lastReported = pct;
            LOGGER.info("[ATA] Terrain scan {}% ({}/{} tiles)", pct, done, total);
            if (source != null) {
                source.sendSuccess(() -> Component.translatable("command.ata.terrain.progress", pct, done, total), false);
            }
        }
    }

    private void finish(ServerLevel level) {
        active = null;
        LOGGER.info("[ATA] Terrain scan complete: {} tiles", total);
        if (source != null) {
            source.sendSuccess(() -> Component.translatable("command.ata.terrain.done", total), true);
        }
        ServerTerrainScanner.rebuildAll(level);
        ServerRouteMapScanner.rebuildAll(level);
    }
}
