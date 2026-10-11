package com.synarsis.airtacticalarsenal.radar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServerTerrainScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger("ATA-RadarMap");
    private static final int CACHE_SIZE = com.synarsis.airtacticalarsenal.util.MapScale.RADAR_CACHE_SIZE;
    private static final int CACHE_VERSION = 20;
    private static final long MAX_TICK_MS = 25;

    private static final Map<String, ServerTerrainScanner> INSTANCES = new LinkedHashMap<>();

    public static long GLOBAL_BUILD_ORDER = 0;

    private final BlockPos radarPos;
    private final int bpp;
    private final int originX, originZ;
    private MapScanJob job;
    private boolean ready = false;
    private long buildStartOrder = 0;
    private volatile byte[] pngBytes;
    private final MapRefresher refresher;

    public ServerTerrainScanner(BlockPos radarPos) {
        this.radarPos = radarPos;
        this.bpp = com.synarsis.airtacticalarsenal.util.MapScale.serverRadarBpp();
        this.originX = radarPos.getX() - (CACHE_SIZE / 2) * bpp;
        this.originZ = radarPos.getZ() - (CACHE_SIZE / 2) * bpp;
        this.refresher = new MapRefresher(CACHE_SIZE, bpp, originX, originZ);
    }

    public static String key(BlockPos pos) {
        return pos.getX() + "_" + pos.getZ();
    }

    public static synchronized ServerTerrainScanner getOrCreate(BlockPos pos, ServerLevel level) {
        String k = key(pos);
        ServerTerrainScanner scanner = INSTANCES.get(k);
        if (scanner != null && scanner.bpp == com.synarsis.airtacticalarsenal.util.MapScale.serverRadarBpp()) return scanner;

        scanner = new ServerTerrainScanner(pos);
        byte[] cached = scanner.tryLoadFromFile(level);
        if (cached != null) {
            scanner.pngBytes = cached;
            scanner.ready = true;
        } else {
            scanner.job = new MapScanJob(CACHE_SIZE, scanner.bpp, scanner.originX, scanner.originZ, true, com.synarsis.airtacticalarsenal.config.ShahedConfig.isMapReadSavedChunks());
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
                Path dir = worldPath.resolve("ata_radar_cache");
                String suffix = "_" + pos.getX() + "_" + pos.getZ() + ".png";
                try (java.util.stream.Stream<Path> files = Files.list(dir)) {
                    for (Path f : (Iterable<Path>) files::iterator) {
                        String n = f.getFileName().toString();
                        if (n.startsWith("radar_v") && n.endsWith(suffix)) Files.deleteIfExists(f);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[ATA] Failed to delete radar cache for {}", key(pos));
            }
        }
    }

    public boolean isReady() { return ready; }
    public boolean isBuilding() { return job != null; }
    public float getProgress() { return job != null ? job.progress() : 1.0f; }
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

    public static synchronized void rebuildAll(ServerLevel level) {
        for (ServerTerrainScanner s : INSTANCES.values()) {
            s.ready = false;
            s.refresher.cancel();
            s.job = new MapScanJob(CACHE_SIZE, s.bpp, s.originX, s.originZ, true, com.synarsis.airtacticalarsenal.config.ShahedConfig.isMapReadSavedChunks());
            s.buildStartOrder = ++GLOBAL_BUILD_ORDER;
        }
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
        if (ready || job == null) return;
        if (!job.tick(level, MAX_TICK_MS)) return;
        int[] pixels = job.finish();
        job = null;
        ready = true;
        pngBytes = MapRefresher.encode(pixels, CACHE_SIZE);
        saveToDisk(level);
        refresher.markFresh();
        LOGGER.info("[ATA] Radar map ready for {}", key(radarPos));
    }

    private Path getCacheDir(ServerLevel level) {
        Path worldPath = level.getServer().getWorldPath(LevelResource.ROOT);
        return worldPath.resolve("ata_radar_cache");
    }

    private byte[] tryLoadFromFile(ServerLevel level) {
        try {
            Path file = getCacheDir(level).resolve(fileName());
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
            Path file = dir.resolve(fileName());
            Files.write(file, png);
            for (int v = 1; v < CACHE_VERSION; v++) {
                Files.deleteIfExists(dir.resolve("radar_v" + v + "_" + radarPos.getX() + "_" + radarPos.getZ() + ".png"));
            }
        } catch (Exception e) {
            LOGGER.error("[ATA] Failed to save radar map", e);
        }
    }

    private String fileName() {
        return "radar_v" + CACHE_VERSION + "_b" + bpp + "_" + radarPos.getX() + "_" + radarPos.getZ() + ".png";
    }

    public static int getCacheSize() { return CACHE_SIZE; }
}
