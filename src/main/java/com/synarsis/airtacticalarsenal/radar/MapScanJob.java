package com.synarsis.airtacticalarsenal.radar;

import com.synarsis.airtacticalarsenal.config.ShahedConfig;
import com.mojang.serialization.Codec;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

final class MapScanJob {

    private static final int MAX_IN_FLIGHT = 64;
    private static final Codec<PalettedContainer<BlockState>> BLOCK_STATES = PalettedContainer.codecRW(
            Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES,
            Blocks.AIR.defaultBlockState());

    private final int size, step, originX, originZ;
    private final int minChunkX, minChunkZ, chunksX, totalChunks;
    private final boolean readDisk;
    private final boolean useTiles;

    final int[] pixels;
    final short[] heights;
    final byte[] kinds;

    private int nextChunk = 0;
    private final List<CompletableFuture<?>> inFlight = new ArrayList<>();
    private final Map<Long, Integer> biomeCache = new ConcurrentHashMap<>();
    private final MapPixelSampler.Sample tmp = new MapPixelSampler.Sample();

    MapScanJob(int size, int step, int originX, int originZ) {
        this(size, step, originX, originZ, true);
    }

    MapScanJob(int size, int step, int originX, int originZ, boolean useTiles) {
        this.useTiles = useTiles;
        this.size = size;
        this.step = step;
        this.originX = originX;
        this.originZ = originZ;
        int maxOffset = (size - 1) * step;
        this.minChunkX = Math.floorDiv(originX, 16);
        this.minChunkZ = Math.floorDiv(originZ, 16);
        this.chunksX = Math.floorDiv(originX + maxOffset, 16) - minChunkX + 1;
        int chunksZ = Math.floorDiv(originZ + maxOffset, 16) - minChunkZ + 1;
        this.totalChunks = chunksX * chunksZ;
        this.readDisk = !useTiles || ShahedConfig.isMapReadSavedChunks();
        this.pixels = new int[size * size];
        this.heights = new short[size * size];
        this.kinds = new byte[size * size];
    }

    float progress() {
        return Math.min(1.0f, (nextChunk - inFlight.size()) / (float) totalChunks);
    }

    boolean tick(ServerLevel level, long budgetMs) {
        long start = System.currentTimeMillis();
        inFlight.removeIf(CompletableFuture::isDone);
        BiomeSource bs = level.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();

        while (nextChunk < totalChunks && System.currentTimeMillis() - start < budgetMs) {
            if (inFlight.size() >= MAX_IN_FLIGHT) break;
            int cx = minChunkX + nextChunk % chunksX;
            int cz = minChunkZ + nextChunk / chunksX;
            nextChunk++;

            LevelChunk live = level.getChunkSource().getChunkNow(cx, cz);
            if (live != null) {
                fillFromColumn(MapPixelSampler.live(live), cx, cz, tmp);
            } else if (useTiles && TerrainTileStore.has(level, TerrainTileStore.tileOf(cx * 16), TerrainTileStore.tileOf(cz * 16))) {
                fillFromTiles(level, cx, cz, bs, sampler);
            } else if (readDisk) {
                inFlight.add(level.getChunkSource().chunkMap.read(new ChunkPos(cx, cz))
                        .thenAcceptAsync(tag -> fillFromSaved(level, tag, cx, cz, bs, sampler), Util.backgroundExecutor())
                        .exceptionally(t -> {
                            fillBiome(cx, cz, bs, sampler);
                            return null;
                        }));
            } else {
                fillBiome(cx, cz, bs, sampler);
            }
        }
        return nextChunk >= totalChunks && inFlight.isEmpty();
    }

    int[] finish() {
        for (int pz = 0; pz < size; pz++) {
            for (int px = 0; px < size; px++) {
                int idx = pz * size + px;
                if (kinds[idx] != MapPixelSampler.KIND_LAND) continue;
                int h = heights[idx];
                int nh = h;
                if (px > 0 && pz > 0) {
                    int n = idx - size - 1;
                    if (kinds[n] == MapPixelSampler.KIND_LAND || kinds[n] == MapPixelSampler.KIND_WATER) nh = heights[n];
                }
                pixels[idx] = MapPixelSampler.shadeLand(pixels[idx], h, nh, step);
            }
        }
        return pixels;
    }

    private int pxFrom(int cx) { return Math.max(0, ceilDiv(cx * 16 - originX, step)); }
    private int pxTo(int cx) { return Math.min(size - 1, Math.floorDiv(cx * 16 + 15 - originX, step)); }
    private int pzFrom(int cz) { return Math.max(0, ceilDiv(cz * 16 - originZ, step)); }
    private int pzTo(int cz) { return Math.min(size - 1, Math.floorDiv(cz * 16 + 15 - originZ, step)); }

    private void fillFromColumn(MapPixelSampler.Column col, int cx, int cz, MapPixelSampler.Sample s) {
        for (int pz = pzFrom(cz), pzEnd = pzTo(cz); pz <= pzEnd; pz++) {
            for (int px = pxFrom(cx), pxEnd = pxTo(cx); px <= pxEnd; px++) {
                int idx = pz * size + px;
                MapPixelSampler.sampleColumn(col, originX + px * step, originZ + pz * step, s);
                pixels[idx] = s.color;
                heights[idx] = (short) s.height;
                kinds[idx] = s.kind;
            }
        }
    }

    private void fillFromTiles(ServerLevel level, int cx, int cz, BiomeSource bs, Climate.Sampler sampler) {
        for (int pz = pzFrom(cz), pzEnd = pzTo(cz); pz <= pzEnd; pz++) {
            for (int px = pxFrom(cx), pxEnd = pxTo(cx); px <= pxEnd; px++) {
                int idx = pz * size + px;
                int wx = originX + px * step, wz = originZ + pz * step;
                int c = TerrainTileStore.sample(level, wx, wz);
                if (c == 0) {
                    pixels[idx] = MapPixelSampler.biomeColor(wx, wz, bs, sampler, biomeCache);
                    kinds[idx] = MapPixelSampler.KIND_BIOME;
                } else {
                    pixels[idx] = c;
                    kinds[idx] = MapPixelSampler.KIND_FINAL;
                }
            }
        }
    }

    private void fillBiome(int cx, int cz, BiomeSource bs, Climate.Sampler sampler) {
        for (int pz = pzFrom(cz), pzEnd = pzTo(cz); pz <= pzEnd; pz++) {
            for (int px = pxFrom(cx), pxEnd = pxTo(cx); px <= pxEnd; px++) {
                int idx = pz * size + px;
                pixels[idx] = MapPixelSampler.biomeColor(originX + px * step, originZ + pz * step, bs, sampler, biomeCache);
                kinds[idx] = MapPixelSampler.KIND_BIOME;
            }
        }
    }

    private void fillFromSaved(ServerLevel level, Optional<CompoundTag> maybeTag, int cx, int cz,
                               BiomeSource bs, Climate.Sampler sampler) {
        MapPixelSampler.Column col = null;
        try {
            if (maybeTag.isPresent()) col = savedColumn(level, maybeTag.get());
        } catch (Exception ignored) {
            col = null;
        }
        if (col == null) {
            fillBiome(cx, cz, bs, sampler);
            return;
        }
        try {
            fillFromColumn(col, cx, cz, new MapPixelSampler.Sample());
        } catch (Exception e) {
            fillBiome(cx, cz, bs, sampler);
        }
    }

    private static MapPixelSampler.Column savedColumn(ServerLevel level, CompoundTag tag) {
        int current = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        if (NbtUtils.getDataVersion(tag, -1) < current) {
            tag = level.getChunkSource().chunkMap.upgradeChunkTag(level.dimension(),
                    () -> level.getServer().overworld().getDataStorage(), tag,
                    level.getChunkSource().getGenerator().getTypeNameForDataFixer());
        }
        if (!tag.getString("Status").endsWith("full")) return null;
        CompoundTag hm = tag.getCompound("Heightmaps");
        if (!hm.contains("WORLD_SURFACE", Tag.TAG_LONG_ARRAY)) return null;

        int minY = level.getMinBuildHeight();
        int bits = Mth.ceillog2(level.getHeight() + 1);
        SimpleBitStorage surface = new SimpleBitStorage(bits, 256, hm.getLongArray("WORLD_SURFACE"));

        Map<Integer, CompoundTag> sectionTags = new HashMap<>();
        ListTag sections = tag.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < sections.size(); i++) {
            CompoundTag sec = sections.getCompound(i);
            if (sec.contains("block_states", Tag.TAG_COMPOUND)) sectionTags.put((int) sec.getByte("Y"), sec.getCompound("block_states"));
        }
        Map<Integer, PalettedContainer<BlockState>> decoded = new HashMap<>();
        BlockState air = Blocks.AIR.defaultBlockState();

        return new MapPixelSampler.Column() {
            @Override public int minY() { return minY; }
            @Override public int surfaceY(int wx, int wz) {
                return minY + surface.get((wx & 15) + (wz & 15) * 16) - 1;
            }
            @Override public BlockState state(int wx, int y, int wz) {
                int sy = y >> 4;
                PalettedContainer<BlockState> c = decoded.get(sy);
                if (c == null) {
                    if (decoded.containsKey(sy)) return air;
                    CompoundTag st = sectionTags.get(sy);
                    c = st == null ? null : BLOCK_STATES.parse(NbtOps.INSTANCE, st).result().orElse(null);
                    decoded.put(sy, c);
                    if (c == null) return air;
                }
                return c.get(wx & 15, y & 15, wz & 15);
            }
        };
    }

    private static int ceilDiv(int a, int b) {
        return -Math.floorDiv(-a, b);
    }
}
