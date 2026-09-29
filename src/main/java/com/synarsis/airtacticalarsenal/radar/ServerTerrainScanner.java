package com.synarsis.airtacticalarsenal.radar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServerTerrainScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger("ATA-RadarMap");
    private static final int CACHE_SIZE = 1536;
    private static final int CACHE_BPP = 2;
    private static final int CACHE_VERSION = 17;
    private static final long MAX_TICK_MS = 25;

    private static final Map<String, ServerTerrainScanner> INSTANCES = new LinkedHashMap<>();

    public static long GLOBAL_BUILD_ORDER = 0;

    private final BlockPos radarPos;
    private final int originX, originZ;
    private int buildRow = -1;
    private int buildCol = 0;
    private boolean ready = false;
    private long buildStartOrder = 0;
    private int[] pixels;
    private volatile byte[] pngBytes;
    private final MapRefresher refresher;

    private final Map<Long, Integer> biomeColorCache = new HashMap<>();

    public ServerTerrainScanner(BlockPos radarPos) {
        this.radarPos = radarPos;
        this.originX = radarPos.getX() - (CACHE_SIZE / 2) * CACHE_BPP;
        this.originZ = radarPos.getZ() - (CACHE_SIZE / 2) * CACHE_BPP;
        this.pixels = new int[CACHE_SIZE * CACHE_SIZE];
        this.refresher = new MapRefresher(CACHE_SIZE, CACHE_BPP, originX, originZ);
    }

    public static String key(BlockPos pos) {
        return pos.getX() + "_" + pos.getZ();
    }

    public static synchronized ServerTerrainScanner getOrCreate(BlockPos pos, ServerLevel level) {
        String k = key(pos);
        ServerTerrainScanner scanner = INSTANCES.get(k);
        if (scanner != null) return scanner;

        scanner = new ServerTerrainScanner(pos);
        byte[] cached = scanner.tryLoadFromFile(level);
        if (cached != null) {
            scanner.pngBytes = cached;
            scanner.ready = true;
            scanner.pixels = null;
        } else {
            scanner.buildRow = 0;
            scanner.buildStartOrder = ++GLOBAL_BUILD_ORDER;
        }
        INSTANCES.put(k, scanner);
        return scanner;
    }

    public static synchronized void remove(BlockPos pos) {
        INSTANCES.remove(key(pos));
    }

    public static synchronized void removeAndInvalidate(BlockPos pos, ServerLevel level) {
        INSTANCES.remove(key(pos));
        if (level != null) {
            try {
                Path worldPath = level.getServer().getWorldPath(LevelResource.ROOT);
                Path file = worldPath.resolve("ata_radar_cache")
                        .resolve("radar_v" + CACHE_VERSION + "_" + pos.getX() + "_" + pos.getZ() + ".png");
                Files.deleteIfExists(file);
            } catch (Exception e) {
                LOGGER.warn("[ATA] Failed to delete radar cache for {}", key(pos));
            }
        }
    }

    public boolean isReady() { return ready; }
    public boolean isBuilding() { return buildRow >= 0; }
    public float getProgress() { return buildRow >= 0 ? (float)(buildRow * CACHE_SIZE + buildCol) / (CACHE_SIZE * CACHE_SIZE) : 1.0f; }
    public byte[] getPngBytes() { return pngBytes; }

    public static synchronized int getQueuePosition(BlockPos pos) {
        String targetKey = key(pos);
        ServerTerrainScanner target = INSTANCES.get(targetKey);
        if (target == null || !target.isBuilding()) return 0;
        int position = 1;
        for (ServerTerrainScanner s : INSTANCES.values()) {
            if (s.isBuilding() && s.buildStartOrder < target.buildStartOrder) position++;
        }
        position += ServerRouteMapScanner.countBuildingBefore(target.buildStartOrder);
        return position;
    }

    public static synchronized int countBuildingBefore(long order) {
        int c = 0;
        for (ServerTerrainScanner s : INSTANCES.values()) {
            if (s.isBuilding() && s.buildStartOrder < order) c++;
        }
        return c;
    }

    public static synchronized long getFirstBuildingOrder() {
        for (ServerTerrainScanner s : INSTANCES.values()) {
            if (s.isBuilding()) return s.buildStartOrder;
        }
        return Long.MAX_VALUE;
    }

    public static synchronized boolean tickAll(ServerLevel level) {
        for (ServerTerrainScanner scanner : INSTANCES.values()) {
            if (scanner.isBuilding()) {
                scanner.tick(level);
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean tickRefresh(ServerLevel level) {
        ServerTerrainScanner due = null;
        for (ServerTerrainScanner scanner : INSTANCES.values()) {
            if (!scanner.ready) continue;
            if (scanner.refresher.isActive()) {
                scanner.tickRefresh0(level);
                return true;
            }
            if (due == null && scanner.refresher.isDue()) due = scanner;
        }
        if (due != null) {
            due.refresher.start(due.pngBytes);
            return true;
        }
        return false;
    }

    private void tickRefresh0(ServerLevel level) {
        byte[] png = refresher.tick(level, MAX_TICK_MS);
        if (png != null) {
            pngBytes = png;
            saveToDisk(level);
        }
    }

    public void tick(ServerLevel level) {
        if (ready || buildRow < 0) return;

        long start = System.currentTimeMillis();
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        BiomeSource bs = level.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        int pixelsDone = 0;

        while (buildRow < CACHE_SIZE) {
            int wx = originX + buildCol * CACHE_BPP;
            int wz = originZ + buildRow * CACHE_BPP;
            pixels[buildRow * CACHE_SIZE + buildCol] =
                    MapPixelSampler.sample(level, wx, wz, CACHE_BPP, mPos, bs, sampler, biomeColorCache);
            buildCol++;
            pixelsDone++;
            if (buildCol >= CACHE_SIZE) {
                buildCol = 0;
                buildRow++;
            }
            if ((pixelsDone & 31) == 0 && System.currentTimeMillis() - start > MAX_TICK_MS) break;
        }

        if (buildRow >= CACHE_SIZE) {
            buildRow = -1;
            ready = true;
            pngBytes = MapRefresher.encode(pixels, CACHE_SIZE);
            saveToDisk(level);
            biomeColorCache.clear();
            pixels = null;
            refresher.markFresh();
            LOGGER.info("[ATA] Radar map ready for {}", key(radarPos));
        }
    }

    private Path getCacheDir(ServerLevel level) {
        Path worldPath = level.getServer().getWorldPath(LevelResource.ROOT);
        return worldPath.resolve("ata_radar_cache");
    }

    private byte[] tryLoadFromFile(ServerLevel level) {
        try {
            Path file = getCacheDir(level).resolve("radar_v" + CACHE_VERSION + "_" + radarPos.getX() + "_" + radarPos.getZ() + ".png");
            if (Files.exists(file)) {
                LOGGER.info("[ATA] Loading cached radar map from {}", file);
                return Files.readAllBytes(file);
            }
        } catch (Exception e) {
            LOGGER.warn("[ATA] Failed to load cached radar map", e);
        }
        return null;
    }

    private void saveToDisk(ServerLevel level) {
        byte[] png = pngBytes;
        if (png == null) return;
        try {
            Path dir = getCacheDir(level);
            Files.createDirectories(dir);
            Path file = dir.resolve("radar_v" + CACHE_VERSION + "_" + radarPos.getX() + "_" + radarPos.getZ() + ".png");
            Files.write(file, png);
            for (int v = 1; v < CACHE_VERSION; v++) {
                Files.deleteIfExists(dir.resolve("radar_v" + v + "_" + radarPos.getX() + "_" + radarPos.getZ() + ".png"));
            }
        } catch (Exception e) {
            LOGGER.error("[ATA] Failed to save radar map", e);
        }
    }

    public static int getCacheSize() { return CACHE_SIZE; }
    public static int getCacheBpp() { return CACHE_BPP; }
}
