package com.synarsis.airtacticalarsenal.radar;

import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class TerrainTileStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("ATA-TerrainTiles");

    public static final int TILE_BLOCKS = 512;
    public static final int STEP = 2;
    public static final int TILE_PX = TILE_BLOCKS / STEP;
    private static final int FORMAT = 1;
    private static final int MAX_CACHED = 64;
    private static final long REFRESH_BUDGET_MS = 10;

    private static Set<Long> existing;
    private static Path dir;
    private static final Map<Long, int[]> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, int[]> eldest) {
            return size() > MAX_CACHED && !DIRTY.contains(eldest.getKey());
        }
    };
    private static final Set<Long> DIRTY = new HashSet<>();

    private static long lastRefreshMs = System.currentTimeMillis();
    private static ArrayDeque<Long> refreshQueue;

    private TerrainTileStore() {}

    public static long key(int tx, int tz) {
        return ChunkPos.asLong(tx, tz);
    }

    public static int tileOf(int blockCoord) {
        return Math.floorDiv(blockCoord, TILE_BLOCKS);
    }

    public static synchronized void reset() {
        existing = null;
        dir = null;
        CACHE.clear();
        DIRTY.clear();
        refreshQueue = null;
    }

    private static Path dir(ServerLevel level) {
        if (dir == null) dir = level.getServer().getWorldPath(LevelResource.ROOT).resolve("ata_map");
        return dir;
    }

    private static Path file(ServerLevel level, int tx, int tz) {
        return dir(level).resolve("tile_v" + FORMAT + "_" + tx + "_" + tz + ".png");
    }

    private static Set<Long> existing(ServerLevel level) {
        if (existing == null) {
            existing = ConcurrentHashMap.newKeySet();
            Path d = dir(level);
            String prefix = "tile_v" + FORMAT + "_";
            if (Files.isDirectory(d)) {
                try (Stream<Path> files = Files.list(d)) {
                    files.map(p -> p.getFileName().toString())
                            .filter(n -> n.startsWith(prefix) && n.endsWith(".png"))
                            .forEach(n -> {
                                String[] parts = n.substring(prefix.length(), n.length() - 4).split("_");
                                if (parts.length != 2) return;
                                try {
                                    existing.add(key(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
                                } catch (NumberFormatException ignored) {
                                }
                            });
                } catch (Exception e) {
                    LOGGER.warn("[ATA] Failed to list terrain tiles", e);
                }
            }
        }
        return existing;
    }

    public static synchronized int count(ServerLevel level) {
        return existing(level).size();
    }

    public static synchronized boolean has(ServerLevel level, int tx, int tz) {
        return existing(level).contains(key(tx, tz));
    }

    public static synchronized int[] get(ServerLevel level, int tx, int tz) {
        long k = key(tx, tz);
        int[] px = CACHE.get(k);
        if (px != null) return px;
        if (!existing(level).contains(k)) return null;
        try {
            px = MapRefresher.decode(Files.readAllBytes(file(level, tx, tz)), TILE_PX);
        } catch (Exception e) {
            px = null;
        }
        if (px == null) {
            existing(level).remove(k);
            return null;
        }
        CACHE.put(k, px);
        return px;
    }

    public static int sample(ServerLevel level, int wx, int wz) {
        int[] px = get(level, tileOf(wx), tileOf(wz));
        if (px == null) return 0;
        int lx = Math.floorMod(wx, TILE_BLOCKS) / STEP;
        int lz = Math.floorMod(wz, TILE_BLOCKS) / STEP;
        return px[lz * TILE_PX + lx];
    }

    public static synchronized void put(ServerLevel level, int tx, int tz, int[] pixels) {
        long k = key(tx, tz);
        CACHE.put(k, pixels);
        existing(level).add(k);
        save(level, tx, tz, pixels.clone());
    }

    private static void save(ServerLevel level, int tx, int tz, int[] pixels) {
        Path target = file(level, tx, tz);
        Util.backgroundExecutor().execute(() -> {
            byte[] png = MapRefresher.encode(pixels, TILE_PX);
            if (png == null) return;
            try {
                Files.createDirectories(target.getParent());
                Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
                Files.write(tmp, png);
                Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                LOGGER.error("[ATA] Failed to save terrain tile {} {}", tx, tz, e);
            }
        });
    }

    public static synchronized boolean tickRefresh(ServerLevel level) {
        int minutes = com.synarsis.airtacticalarsenal.config.ShahedConfig.getMapRefreshMinutes();
        if (refreshQueue == null) {
            if (minutes <= 0 || System.currentTimeMillis() - lastRefreshMs < minutes * 60_000L) return false;
            if (existing(level).isEmpty()) {
                lastRefreshMs = System.currentTimeMillis();
                return false;
            }
            refreshQueue = new ArrayDeque<>();
            Set<Long> seen = new HashSet<>();
            int radius = level.getServer().getPlayerList().getViewDistance();
            for (ServerPlayer p : level.players()) {
                ChunkPos c = p.chunkPosition();
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        long ck = ChunkPos.asLong(c.x + dx, c.z + dz);
                        if (seen.add(ck)) refreshQueue.add(ck);
                    }
                }
            }
            return true;
        }

        long start = System.currentTimeMillis();
        MapPixelSampler.Sample tmp = new MapPixelSampler.Sample();
        while (!refreshQueue.isEmpty() && System.currentTimeMillis() - start < REFRESH_BUDGET_MS) {
            long ck = refreshQueue.poll();
            int cx = ChunkPos.getX(ck), cz = ChunkPos.getZ(ck);
            int tx = Math.floorDiv(cx * 16, TILE_BLOCKS), tz = Math.floorDiv(cz * 16, TILE_BLOCKS);
            if (!existing(level).contains(key(tx, tz))) continue;
            if (level.getChunkSource().getChunkNow(cx, cz) == null) continue;
            int[] px = get(level, tx, tz);
            if (px == null) continue;
            int ox = tx * TILE_BLOCKS, oz = tz * TILE_BLOCKS;
            boolean changed = false;
            for (int wz = cz * 16; wz < cz * 16 + 16; wz += STEP) {
                for (int wx = cx * 16; wx < cx * 16 + 16; wx += STEP) {
                    int c = MapPixelSampler.sampleLoaded(level, wx, wz, STEP, tmp);
                    int idx = ((wz - oz) / STEP) * TILE_PX + (wx - ox) / STEP;
                    if (c != 0 && px[idx] != c) {
                        px[idx] = c;
                        changed = true;
                    }
                }
            }
            if (changed) DIRTY.add(key(tx, tz));
        }
        if (refreshQueue.isEmpty()) {
            for (long k : DIRTY) {
                int[] px = CACHE.get(k);
                if (px != null) save(level, ChunkPos.getX(k), ChunkPos.getZ(k), px.clone());
            }
            DIRTY.clear();
            refreshQueue = null;
            lastRefreshMs = System.currentTimeMillis();
        }
        return true;
    }
}
