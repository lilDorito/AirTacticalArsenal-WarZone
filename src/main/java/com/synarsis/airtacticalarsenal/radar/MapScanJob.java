package com.synarsis.airtacticalarsenal.radar;

import com.mojang.serialization.Codec;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
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

    MapScanJob(int size, int step, int originX, int originZ, boolean useTiles, boolean readDisk) {
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
        this.readDisk = readDisk;
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
            try {
                tag = level.getChunkSource().chunkMap.upgradeChunkTag(level.dimension(),
                        () -> level.getServer().overworld().getDataStorage(), tag.copy(),
                        level.getChunkSource().getGenerator().getTypeNameForDataFixer());
            } catch (Exception ignored) {
            }
        }

        Map<Integer, PalettedContainer<BlockState>> decoded = new HashMap<>();
        ListTag sections = tag.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < sections.size(); i++) {
            CompoundTag sec = sections.getCompound(i);
            if (!sec.contains("block_states", Tag.TAG_COMPOUND)) continue;
            PalettedContainer<BlockState> c = decodeSection(sec.getCompound("block_states"));
            if (c != null && c.maybeHas(st -> !st.isAir())) decoded.put((int) sec.getByte("Y"), c);
        }
        if (decoded.isEmpty()) return null;

        int[] ys = decoded.keySet().stream().mapToInt(Integer::intValue).sorted().toArray();
        int minY = level.getMinBuildHeight();
        BlockState air = Blocks.AIR.defaultBlockState();

        return new MapPixelSampler.Column() {
            @Override public int minY() { return minY; }
            @Override public int surfaceY(int wx, int wz) {
                for (int i = ys.length - 1; i >= 0; i--) {
                    PalettedContainer<BlockState> c = decoded.get(ys[i]);
                    for (int ly = 15; ly >= 0; ly--) {
                        if (!c.get(wx & 15, ly, wz & 15).isAir()) return ys[i] * 16 + ly;
                    }
                }
                return minY;
            }
            @Override public BlockState state(int wx, int y, int wz) {
                PalettedContainer<BlockState> c = decoded.get(y >> 4);
                return c == null ? air : c.get(wx & 15, y & 15, wz & 15);
            }
        };
    }

    private static PalettedContainer<BlockState> decodeSection(CompoundTag blockStates) {
        PalettedContainer<BlockState> c = BLOCK_STATES.parse(NbtOps.INSTANCE, blockStates).result().orElse(null);
        if (c != null) return c;
        CompoundTag fixed = blockStates.copy();
        ListTag palette = fixed.getList("palette", Tag.TAG_COMPOUND);
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag entry = palette.getCompound(i);
            if (BlockState.CODEC.parse(NbtOps.INSTANCE, entry).result().isPresent()) continue;
            CompoundTag nameOnly = new CompoundTag();
            nameOnly.putString("Name", entry.getString("Name"));
            if (BlockState.CODEC.parse(NbtOps.INSTANCE, nameOnly).result().isEmpty()) {
                nameOnly.putString("Name", "minecraft:stone");
            }
            palette.set(i, nameOnly);
        }
        return BLOCK_STATES.parse(NbtOps.INSTANCE, fixed).result().orElse(null);
    }

    private static int ceilDiv(int a, int b) {
        return -Math.floorDiv(-a, b);
    }
}
