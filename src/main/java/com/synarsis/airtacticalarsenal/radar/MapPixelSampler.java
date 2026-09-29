package com.synarsis.airtacticalarsenal.radar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.Map;

public final class MapPixelSampler {

    public static final byte KIND_BIOME = 0;
    public static final byte KIND_LAND = 1;
    public static final byte KIND_WATER = 2;
    public static final byte KIND_FINAL = 3;

    private static final int WATER_COLOR = 0x3F76E4;
    private static final float LAND_DIM = 0.72f;

    private MapPixelSampler() {}

    public interface Column {
        int minY();
        int surfaceY(int wx, int wz);
        BlockState state(int wx, int y, int wz);
    }

    public static final class Sample {
        public int color;
        public int height;
        public byte kind;
    }

    public static Column live(LevelChunk chunk) {
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        return new Column() {
            @Override public int minY() { return chunk.getMinBuildHeight(); }
            @Override public int surfaceY(int wx, int wz) {
                return chunk.getHeight(Heightmap.Types.WORLD_SURFACE, wx & 15, wz & 15) - 1;
            }
            @Override public BlockState state(int wx, int y, int wz) {
                return chunk.getBlockState(mPos.set(wx, y, wz));
            }
        };
    }

    public static void sampleColumn(Column col, int wx, int wz, Sample out) {
        int minY = col.minY();
        int y = col.surfaceY(wx, wz);
        BlockState st = col.state(wx, y, wz);
        MapColor mc = mapColor(st);
        while (y > minY && mc == MapColor.NONE) {
            y--;
            st = col.state(wx, y, wz);
            mc = mapColor(st);
        }

        if (st.getFluidState().is(FluidTags.WATER)) {
            int depth = 0;
            int wy = y;
            while (wy > minY && depth < 16) {
                if (!col.state(wx, wy, wz).getFluidState().is(FluidTags.WATER)) break;
                depth++;
                wy--;
            }
            float f = 1.0f - Math.min(depth, 16) / 16.0f * 0.55f;
            out.color = scale(WATER_COLOR, f * 0.8f);
            out.height = y + 1;
            out.kind = KIND_WATER;
            return;
        }

        out.color = 0xFF000000 | (mc == MapColor.NONE ? 0x202020 : mc.col);
        out.height = y + 1;
        out.kind = KIND_LAND;
    }

    private static MapColor mapColor(BlockState st) {
        try {
            return st.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (RuntimeException e) {
            return MapColor.STONE;
        }
    }

    public static int shadeLand(int baseColor, int height, int neighbourHeight, int step) {
        float slope = (height - neighbourHeight) * 2.0f / step;
        float shade = Math.max(0.7f, Math.min(1.2f, 1.0f + slope * 0.08f));
        return scale(baseColor, shade * LAND_DIM);
    }

    public static int sampleLoaded(ServerLevel level, int wx, int wz, int step, Sample tmp) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(wx >> 4, wz >> 4);
        if (chunk == null) return 0;
        sampleColumn(live(chunk), wx, wz, tmp);
        if (tmp.kind != KIND_LAND) return tmp.color;
        LevelChunk n = level.getChunkSource().getChunkNow((wx - step) >> 4, (wz - step) >> 4);
        int nh = n == null ? tmp.height
                : n.getHeight(Heightmap.Types.WORLD_SURFACE, (wx - step) & 15, (wz - step) & 15);
        return shadeLand(tmp.color, tmp.height, nh, step);
    }

    public static int biomeColor(int wx, int wz, BiomeSource bs, Climate.Sampler sampler, Map<Long, Integer> cache) {
        long ck = ((long) (wx >> 2) << 32) | ((wz >> 2) & 0xFFFFFFFFL);
        Integer cached = cache.get(ck);
        if (cached != null) return cached;
        int color = biomeOnlyColor(bs.getNoiseBiome(wx >> 2, 16, wz >> 2, sampler));
        cache.put(ck, color);
        return color;
    }

    private static int biomeOnlyColor(Holder<Biome> bh) {
        String n = bh.unwrapKey().map(k -> k.location().getPath()).orElse("");
        Biome bi = bh.value();
        int r, g, b;
        if (n.contains("ocean") || n.contains("river")) {
            int wc = bi.getWaterColor(); r = (wc >> 16) & 0xFF; g = (wc >> 8) & 0xFF; b = wc & 0xFF;
            if (n.contains("deep")) { r = (int) (r * 0.6f); g = (int) (g * 0.6f); b = (int) (b * 0.6f); }
            else { r = (int) (r * 0.75f); g = (int) (g * 0.75f); b = (int) (b * 0.75f); }
        } else if (n.contains("beach") || n.contains("desert")) { r = 190; g = 175; b = 130; }
        else if (n.contains("badlands") || n.contains("eroded")) { r = 180; g = 95; b = 35; }
        else if (n.contains("snowy") || n.contains("frozen") || n.contains("ice")) { r = 200; g = 205; b = 210; }
        else if (n.contains("mushroom")) { r = 130; g = 95; b = 130; }
        else { int gc = grassColor(n); r = (gc >> 16) & 0xFF; g = (gc >> 8) & 0xFF; b = gc & 0xFF; }
        r = (int) (r * 0.65f); g = (int) (g * 0.65f); b = (int) (b * 0.65f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int grassColor(String biomeName) {
        if (biomeName.contains("swamp"))                  return 0x6A7039;
        if (biomeName.contains("jungle"))                 return 0x59C93C;
        if (biomeName.contains("dark_forest"))            return 0x507A32;
        if (biomeName.contains("old_growth_spruce"))      return 0x6B8E52;
        if (biomeName.contains("old_growth_pine"))        return 0x6B8E52;
        if (biomeName.contains("old_growth_birch"))       return 0x7DB05A;
        if (biomeName.contains("birch"))                  return 0x88BB67;
        if (biomeName.contains("flower_forest"))          return 0x79C05A;
        if (biomeName.contains("forest"))                 return 0x79C05A;
        if (biomeName.contains("taiga"))                  return 0x6B8E52;
        if (biomeName.contains("savanna"))                return 0xBFB755;
        if (biomeName.contains("windswept"))              return 0x6B935B;
        if (biomeName.contains("meadow"))                 return 0x83BB6D;
        if (biomeName.contains("cherry"))                 return 0xB6DB61;
        if (biomeName.contains("mangrove"))               return 0x6A7039;
        if (biomeName.contains("stony"))                  return 0x6B935B;
        if (biomeName.contains("grove"))                  return 0x6B8E52;
        if (biomeName.contains("plains"))                 return 0x91BD59;
        if (biomeName.contains("sunflower"))              return 0x91BD59;
        return 0x7ABD52;
    }

    static int scale(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((rgb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
