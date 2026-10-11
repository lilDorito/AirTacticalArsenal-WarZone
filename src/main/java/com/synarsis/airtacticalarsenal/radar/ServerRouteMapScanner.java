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
import java.util.Objects;
import java.util.stream.Stream;

public class ServerRouteMapScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger("ATA-RouteMap");
    public static final int CACHE_SIZE = com.synarsis.airtacticalarsenal.util.MapScale.ROUTE_CACHE_SIZE;
    private static final int CACHE_VERSION = 20;
    private static final long MAX_TICK_MS = 25;
    public static final int REUSE_RADIUS = 500;

    private static final Map<String, ServerRouteMapScanner> INSTANCES = new LinkedHashMap<>();

    private final BlockPos centerPos;
    private final int bpp;
    private final int originX, originZ;
    private MapScanJob job;
    private boolean ready = false;
    private long buildStartOrder = 0;
    private volatile byte[] pngBytes;
    private final MapRefresher refresher;

    public ServerRouteMapScanner(BlockPos centerPos) {
        this.centerPos = centerPos;
        this.bpp = com.synarsis.airtacticalarsenal.util.MapScale.serverRouteBpp();
        this.originX = centerPos.getX() - (CACHE_SIZE / 2) * bpp;
        this.originZ = centerPos.getZ() - (CACHE_SIZE / 2) * bpp;
        this.refresher = new MapRefresher(CACHE_SIZE, bpp, originX, originZ);
    }

    public static synchronized ServerRouteMapScanner findNear(BlockPos pos) {
        for (ServerRouteMapScanner scanner : INSTANCES.values()) {
            double dx = scanner.centerPos.getX() - pos.getX();
            double dz = scanner.centerPos.getZ() - pos.getZ();
            if (dx * dx + dz * dz <= (long) REUSE_RADIUS * REUSE_RADIUS) {
                return scanner;
            }
        }
        return null;
    }

    public BlockPos getCenterPos() { return centerPos; }

    private static String filePrefix(int bpp) {
        return "route_v" + CACHE_VERSION + "_b" + bpp + "_";
    }

    public static String key(BlockPos pos) {
        return "route_" + pos.getX() + "_" + pos.getZ();
    }

    public static synchronized ServerRouteMapScanner getOrCreate(BlockPos pos, ServerLevel level) {

        ServerRouteMapScanner near = findNear(pos);
        if (near != null) {
            if (near.bpp == com.synarsis.airtacticalarsenal.util.MapScale.serverRouteBpp()) return near;
            INSTANCES.remove(key(near.centerPos));
        }

        String k = key(pos);
        ServerRouteMapScanner scanner = new ServerRouteMapScanner(pos);
        byte[] cached = scanner.tryLoadFromFile(level);
        if (cached != null) {
            scanner.pngBytes = cached;
            scanner.ready = true;
            INSTANCES.put(k, scanner);
            return scanner;
        }

        BlockPos nearbyCenter = findNearbyDiskCache(pos, level);
        if (nearbyCenter != null) {
            String nk = key(nearbyCenter);

            if (INSTANCES.containsKey(nk)) return INSTANCES.get(nk);
            ServerRouteMapScanner nearbyScanner = new ServerRouteMapScanner(nearbyCenter);
            byte[] nearbyCached = nearbyScanner.tryLoadFromFile(level);
            if (nearbyCached != null) {
                nearbyScanner.pngBytes = nearbyCached;
                nearbyScanner.ready = true;
                INSTANCES.put(nk, nearbyScanner);
                return nearbyScanner;
            }
        }

        scanner.job = new MapScanJob(CACHE_SIZE, scanner.bpp, scanner.originX, scanner.originZ, true, false);
        scanner.buildStartOrder = ++ServerTerrainScanner.GLOBAL_BUILD_ORDER;
        INSTANCES.put(k, scanner);
        return scanner;
    }

    private static BlockPos findNearbyDiskCache(BlockPos pos, ServerLevel level) {
        try {
            Path cacheDir = level.getServer().getWorldPath(LevelResource.ROOT).resolve("ata_radar_cache");
            if (!Files.exists(cacheDir)) return null;
            String prefix = filePrefix(com.synarsis.airtacticalarsenal.util.MapScale.serverRouteBpp());
            try (Stream<Path> stream = Files.list(cacheDir)) {
                return stream
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.startsWith(prefix) && name.endsWith(".png"))
                    .map(name -> {

                        String coords = name.substring(prefix.length(), name.length() - 4); 
                        int sep = coords.lastIndexOf('_');
                        if (sep <= 0) return null;
                        try {
                            int cx = Integer.parseInt(coords.substring(0, sep));
                            int cz = Integer.parseInt(coords.substring(sep + 1));
                            double dx = cx - pos.getX();
                            double dz = cz - pos.getZ();
                            if (dx * dx + dz * dz <= (long) REUSE_RADIUS * REUSE_RADIUS) {
                                return new BlockPos(cx, 0, cz);
                            }
                        } catch (NumberFormatException ignored) {}
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            }
        } catch (Exception e) {
            LOGGER.warn("[ATA] Failed to search nearby disk cache for route map", e);
            return null;
        }
    }

    public static synchronized void remove(BlockPos pos) {
        ServerRouteMapScanner near = findNear(pos);
        if (near != null) {
            INSTANCES.remove(key(near.centerPos));
        }
    }

    public boolean isReady() { return ready; }
    public boolean isBuilding() { return job != null; }
    public float getProgress() { return job != null ? job.progress() : 1.0f; }
    public byte[] getPngBytes() { return pngBytes; }

    public static synchronized int getQueuePosition(BlockPos pos) {
        ServerRouteMapScanner target = findNear(pos);
        if (target == null || !target.isBuilding()) return 0;
        int position = 1;
        for (ServerRouteMapScanner s : INSTANCES.values()) {
            if (s.isBuilding() && s.buildStartOrder < target.buildStartOrder) position++;
        }
        position += ServerTerrainScanner.countBuildingBefore(target.buildStartOrder);
        return position;
    }

    public static synchronized int countBuildingBefore(long order) {
        int c = 0;
        for (ServerRouteMapScanner s : INSTANCES.values()) {
            if (s.isBuilding() && s.buildStartOrder < order) c++;
        }
        return c;
    }

    public static synchronized long getFirstBuildingOrder() {
        for (ServerRouteMapScanner s : INSTANCES.values()) {
            if (s.isBuilding()) return s.buildStartOrder;
        }
        return Long.MAX_VALUE;
    }

    public static synchronized boolean tickAll(ServerLevel level) {
        for (ServerRouteMapScanner scanner : INSTANCES.values()) {
            if (scanner.isBuilding()) {
                scanner.tick(level);
                return true;
            }
        }
        return false;
    }

    public static synchronized void rebuildAll(ServerLevel level) {
        for (ServerRouteMapScanner s : INSTANCES.values()) {
            s.ready = false;
            s.refresher.cancel();
            s.job = new MapScanJob(CACHE_SIZE, s.bpp, s.originX, s.originZ, true, false);
            s.buildStartOrder = ++ServerTerrainScanner.GLOBAL_BUILD_ORDER;
        }
    }

    public static synchronized boolean tickRefresh(ServerLevel level) {
        ServerRouteMapScanner due = null;
        for (ServerRouteMapScanner scanner : INSTANCES.values()) {
            if (!scanner.ready) continue;
            if (scanner.refresher.isActive()) {
                byte[] png = scanner.refresher.tick(level, MAX_TICK_MS);
                if (png != null) {
                    scanner.pngBytes = png;
                    scanner.saveToDisk(level);
                }
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

    public void tick(ServerLevel level) {
        if (ready || job == null) return;
        if (!job.tick(level, MAX_TICK_MS)) return;
        int[] pixels = job.finish();
        job = null;
        ready = true;
        pngBytes = MapRefresher.encode(pixels, CACHE_SIZE);
        saveToDisk(level);
        refresher.markFresh();
        LOGGER.info("[ATA] Route map ready for {}", key(centerPos));
    }

    private Path getCacheDir(ServerLevel level) {
        Path worldPath = level.getServer().getWorldPath(LevelResource.ROOT);
        return worldPath.resolve("ata_radar_cache");
    }

    private byte[] tryLoadFromFile(ServerLevel level) {
        try {
            Path file = getCacheDir(level).resolve(filePrefix(bpp) + centerPos.getX() + "_" + centerPos.getZ() + ".png");
            if (Files.exists(file)) {
                LOGGER.info("[ATA] Loading cached route map from {}", file);
                return Files.readAllBytes(file);
            }
        } catch (Exception e) {
            LOGGER.warn("[ATA] Failed to load cached route map", e);
        }
        return null;
    }

    private void saveToDisk(ServerLevel level) {
        byte[] png = pngBytes;
        if (png == null) return;
        try {
            Path dir = getCacheDir(level);
            Files.createDirectories(dir);
            Path file = dir.resolve(filePrefix(bpp) + centerPos.getX() + "_" + centerPos.getZ() + ".png");
            Files.write(file, png);
            for (int v = 1; v < CACHE_VERSION; v++) {
                Files.deleteIfExists(dir.resolve("route_v" + v + "_" + centerPos.getX() + "_" + centerPos.getZ() + ".png"));
            }
        } catch (Exception e) {
            LOGGER.error("[ATA] Failed to save route map", e);
        }
    }
}
