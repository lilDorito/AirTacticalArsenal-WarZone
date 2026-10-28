package com.synarsis.airtacticalarsenal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class DroneChunkCache extends ClientChunkCache {
    private static final Field SOURCE = ObfuscationReflectionHelper.findField(ClientLevel.class, "f_104559_");
    private static final ArrayDeque<Retired> CLEANUP = new ArrayDeque<>();
    private static DroneChunkCache active;

    private final ClientLevel world;
    private final ClientChunkCache body;
    private final Set<ChunkPos> received = new HashSet<>();
    private final Map<ChunkPos, LevelChunk> borrowed = new ConcurrentHashMap<>();
    private final Map<Long, ClientboundLevelChunkWithLightPacket> pending = new java.util.HashMap<>();
    private boolean receiving;
    private boolean closed;
    private int radius = 8;
    private long center = Long.MIN_VALUE;

    private record Retired(ClientLevel world, LevelChunk chunk) {
    }

    private DroneChunkCache(ClientLevel level) {
        super(level, 8);
        this.world = level;
        this.body = level.getChunkSource();
        install(this);
    }

    public static void accept(int centerX, int centerZ, int radius, ClientboundLevelChunkWithLightPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (radius < 0) {
            closeActive();
            return;
        }
        if (active == null || active.closed || active.world != mc.level) {
            if (active != null) {
                active.retire();
            }
            if (mc.level.getChunkSource() instanceof DroneChunkCache stale) {
                stale.retire();
            }
            active = new DroneChunkCache(mc.level);
        }
        active.receive(centerX, centerZ, radius, packet);
    }

    public static void closeActive() {
        if (active != null) {
            active.retire();
            active = null;
        }
    }

    public static void tickCleanup() {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            CLEANUP.clear();
            active = null;
            return;
        }
        if (active != null && active.world != world) {
            closeActive();
        }
        for (int i = 0; i < 16 && !CLEANUP.isEmpty(); i++) {
            Retired retired = CLEANUP.removeFirst();
            ChunkPos pos = retired.chunk().getPos();
            if (retired.world() == world && world.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false) == null) {
                world.unload(retired.chunk());
                clearLight(world, pos);
            }
        }
    }

    private void install(ClientChunkCache cache) {
        try {
            SOURCE.set(this.world, cache);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot install drone camera terrain cache", e);
        }
    }

    private void receive(int centerX, int centerZ, int requestedRadius, ClientboundLevelChunkWithLightPacket packet) {
        if (this.closed) {
            return;
        }
        int next = Mth.clamp(requestedRadius, 2, 33);
        long nextCenter = ChunkPos.asLong(centerX, centerZ);
        if (next != this.radius || nextCenter != this.center) {
            boolean grew = next > this.radius;
            this.radius = next;
            this.center = nextCenter;
            Iterator<ChunkPos> it = this.received.iterator();
            while (it.hasNext()) {
                ChunkPos old = it.next();
                if (!inRange(old.x, old.z)) {
                    releaseRemote(old);
                    it.remove();
                }
            }
            this.pending.keySet().removeIf(key -> !inRange(ChunkPos.getX(key), ChunkPos.getZ(key)));
            if (grew) {
                super.updateViewRadius(this.radius);
                super.updateViewCenter(centerX, centerZ);
            } else {
                super.updateViewCenter(centerX, centerZ);
                super.updateViewRadius(this.radius);
            }
        }
        if (packet != null && inRange(packet.getX(), packet.getZ())) {
            this.pending.put(ChunkPos.asLong(packet.getX(), packet.getZ()), packet);
        }
    }

    private boolean inRange(int x, int z) {
        return Math.abs(x - ChunkPos.getX(this.center)) <= this.radius && Math.abs(z - ChunkPos.getZ(this.center)) <= this.radius;
    }

    public static void processFrame() {
        if (active == null || active.closed || active.pending.isEmpty()) {
            return;
        }
        long start = System.nanoTime();
        long budget = active.pending.size() > 96 ? 4_000_000L : 2_500_000L;
        do {
            active.applyNearest();
        } while (active != null && !active.closed && !active.pending.isEmpty() && System.nanoTime() - start < budget);
    }

    private void applyNearest() {
        int cx = ChunkPos.getX(this.center);
        int cz = ChunkPos.getZ(this.center);
        long best = 0L;
        int bestDistance = Integer.MAX_VALUE;
        for (long key : this.pending.keySet()) {
            int d = Math.max(Math.abs(ChunkPos.getX(key) - cx), Math.abs(ChunkPos.getZ(key) - cz));
            if (d < bestDistance) {
                bestDistance = d;
                best = key;
                if (d == 0) {
                    break;
                }
            }
        }
        ClientboundLevelChunkWithLightPacket packet = this.pending.remove(best);
        if (packet != null) {
            apply(packet);
        }
    }

    private void apply(ClientboundLevelChunkWithLightPacket packet) {
        int cx = packet.getX();
        int cz = packet.getZ();
        ChunkPos pos = new ChunkPos(cx, cz);
        LevelChunk local = this.body.getChunk(cx, cz, ChunkStatus.FULL, false);
        if (local != null) {
            this.borrowed.put(pos, local);
            this.received.add(pos);
            return;
        }
        this.borrowed.remove(pos);
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        this.receiving = true;
        try {
            packet.handle(Minecraft.getInstance().getConnection());
            this.received.add(pos);
        } finally {
            this.receiving = false;
        }
    }

    private void releaseRemote(ChunkPos pos) {
        LevelChunk retained = this.borrowed.remove(pos);
        super.drop(pos.x, pos.z);
        if (retained != null && this.body.getChunk(pos.x, pos.z, ChunkStatus.FULL, false) == null) {
            this.world.unload(retained);
        }
        if (this.body.getChunk(pos.x, pos.z, ChunkStatus.FULL, false) != null) {
            this.world.onChunkLoaded(pos);
            this.body.getLightEngine().setLightEnabled(pos, true);
        } else {
            clearLight(this.world, pos);
        }
    }

    private static void clearLight(ClientLevel world, ChunkPos pos) {
        world.queueLightUpdate(() -> {
            if (world.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false) == null) {
                LevelLightEngine light = world.getChunkSource().getLightEngine();
                light.setLightEnabled(pos, false);
                for (int y = light.getMinLightSection(); y < light.getMaxLightSection(); y++) {
                    light.queueSectionData(LightLayer.BLOCK, SectionPos.of(pos, y), null);
                    light.queueSectionData(LightLayer.SKY, SectionPos.of(pos, y), null);
                }
                for (int y = world.getMinSection(); y < world.getMaxSection(); y++) {
                    light.updateSectionStatus(SectionPos.of(pos, y), true);
                }
            }
        });
    }

    private void retire() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.world.getChunkSource() == this) {
            install(this.body);
        }
        for (ChunkPos pos : this.received) {
            LevelChunk chunk = this.borrowed.get(pos);
            if (chunk == null) {
                chunk = super.getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
            }
            if (chunk != null && this.body.getChunk(pos.x, pos.z, ChunkStatus.FULL, false) == null) {
                CLEANUP.addLast(new Retired(this.world, chunk));
            }
        }
        this.received.clear();
        this.borrowed.clear();
        this.pending.clear();
    }

    @Override
    public LevelChunk getChunk(int x, int z, ChunkStatus status, boolean required) {
        LevelChunk chunk = this.body.getChunk(x, z, status, false);
        if (chunk == null) {
            chunk = this.borrowed.get(new ChunkPos(x, z));
        }
        return chunk != null ? chunk : super.getChunk(x, z, status, required);
    }

    @Override
    public LevelChunk replaceWithPacketData(int x, int z, FriendlyByteBuf data, CompoundTag heights,
                                            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> tags) {
        if (this.receiving) {
            return super.replaceWithPacketData(x, z, data, heights, tags);
        }
        LevelChunk chunk = this.body.replaceWithPacketData(x, z, data, heights, tags);
        ChunkPos pos = new ChunkPos(x, z);
        if (chunk != null && this.borrowed.containsKey(pos)) {
            this.borrowed.put(pos, chunk);
        }
        return chunk;
    }

    @Override
    public void replaceBiomes(int x, int z, FriendlyByteBuf data) {
        this.body.replaceBiomes(x, z, data);
    }

    @Override
    public void drop(int x, int z) {
        this.body.drop(x, z);
        if (this.borrowed.containsKey(new ChunkPos(x, z)) || super.getChunk(x, z, ChunkStatus.FULL, false) != null) {
            this.world.onChunkLoaded(new ChunkPos(x, z));
            getLightEngine().setLightEnabled(new ChunkPos(x, z), true);
        }
    }

    @Override
    public void updateViewCenter(int x, int z) {
        this.body.updateViewCenter(x, z);
    }

    @Override
    public void updateViewRadius(int viewDistance) {
        this.body.updateViewRadius(viewDistance);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.body.getLightEngine();
    }

    @Override
    public void tick(BooleanSupplier hasTimeLeft, boolean tickChunks) {
        this.body.tick(hasTimeLeft, tickChunks);
    }

    @Override
    public int getLoadedChunksCount() {
        return this.body.getLoadedChunksCount() + super.getLoadedChunksCount();
    }

    @Override
    public String gatherStats() {
        return this.body.gatherStats() + " + drone " + super.gatherStats();
    }
}
