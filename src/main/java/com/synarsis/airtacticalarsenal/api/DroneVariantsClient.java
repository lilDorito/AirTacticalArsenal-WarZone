package com.synarsis.airtacticalarsenal.api;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.synarsis.airtacticalarsenal.entity.OrlanEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class DroneVariantsClient {
    private record DummyKey(EntityType<?> type, int launcher) {
    }

    private static final Map<DummyKey, Entity> LAUNCHER_DUMMIES = new java.util.HashMap<>();

    public static Entity launcherDummy(EntityType<?> type, Entity launcher) {
        DummyKey key = new DummyKey(type, launcher.getId());
        Entity dummy = LAUNCHER_DUMMIES.get(key);
        if (dummy == null || dummy.level() != launcher.level()) {
            if (LAUNCHER_DUMMIES.size() > 256) {
                LAUNCHER_DUMMIES.clear();
            }
            dummy = type.create(launcher.level());
            if (dummy == null) {
                return null;
            }
            LAUNCHER_DUMMIES.put(key, dummy);
        }
        return dummy;
    }

    private static final Map<String, LauncherDisplay> DISPLAYS = new ConcurrentHashMap<>();
    private static final Map<EntityType<?>, CameraPanel> PANELS = new ConcurrentHashMap<>();
    private static final Map<String, EngineSound> ENGINE_SOUNDS = new ConcurrentHashMap<>();
    private static final Map<String, FarSound> FAR_SOUNDS = new ConcurrentHashMap<>();
    private static final Map<String, java.util.function.Supplier<net.minecraft.sounds.SoundEvent>> START_SOUNDS = new ConcurrentHashMap<>();

    private DroneVariantsClient() {
    }

    @FunctionalInterface
    public interface LauncherDisplay {
        void render(Entity launcher, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight);
    }

    @FunctionalInterface
    public interface CameraPanel {
        void render(GuiGraphics graphics, OrlanEntity drone, int left, int right, int top);
    }

    public record EngineSound(java.util.function.Supplier<net.minecraft.sounds.SoundEvent> loop, float volume, float pitch,
                              double range) {
        public float gainAt(double distance) {
            if (distance >= range) {
                return 0.0f;
            }
            double t = 1.0 - distance / range;
            return (float) (volume * Math.pow(t, 1.6));
        }
    }

    public enum SoundClass {
        LIGHT(260.0, 520.0, -21.0f),
        HEAVY(450.0, 900.0, -18.0f),
        JET(600.0, 1300.0, -16.0f);

        private final double near;
        private final double far;
        private final float loudness;

        SoundClass(double near, double far, float loudness) {
            this.near = near;
            this.far = far;
            this.loudness = loudness;
        }

        public double near() {
            return near;
        }

        public double far() {
            return far;
        }

        public float loudness() {
            return loudness;
        }
    }

    public static final float FAR_LAYER_DB = -4.0f;

    public static float levelFor(float measuredLufs, float targetLufs) {
        return (float) Math.pow(10.0, (targetLufs - measuredLufs) / 20.0);
    }

    public static void registerEngineSound(net.minecraft.resources.ResourceLocation entityType,
                                           java.util.function.Supplier<net.minecraft.sounds.SoundEvent> loop,
                                           float measuredLufs, float pitch, SoundClass soundClass) {
        registerEngineSound(entityType, loop, levelFor(measuredLufs, soundClass.loudness()), pitch, soundClass.near());
    }

    public static void registerFarSound(net.minecraft.resources.ResourceLocation entityType,
                                        java.util.function.Supplier<net.minecraft.sounds.SoundEvent> loop,
                                        float measuredLufs, SoundClass soundClass) {
        registerFarSound(entityType, loop, levelFor(measuredLufs, soundClass.loudness() + FAR_LAYER_DB),
                soundClass.near() * 0.4, soundClass.far());
    }

    public static void registerEngineSound(net.minecraft.resources.ResourceLocation entityType,
                                           java.util.function.Supplier<net.minecraft.sounds.SoundEvent> loop,
                                           float volume, float pitch) {
        registerEngineSound(entityType, loop, volume, pitch, 160.0);
    }

    public static void registerEngineSound(net.minecraft.resources.ResourceLocation entityType,
                                           java.util.function.Supplier<net.minecraft.sounds.SoundEvent> loop,
                                           float volume, float pitch, double range) {
        ENGINE_SOUNDS.put(entityType.toString(), new EngineSound(loop, volume, pitch, range));
    }

    public record FarSound(java.util.function.Supplier<net.minecraft.sounds.SoundEvent> loop, float volume, double near, double range) {
    }

    public static void registerFarSound(net.minecraft.resources.ResourceLocation entityType,
                                        java.util.function.Supplier<net.minecraft.sounds.SoundEvent> loop,
                                        float volume, double near, double range) {
        FAR_SOUNDS.put(entityType.toString(), new FarSound(loop, volume, near, range));
    }

    public static FarSound farSound(String entityTypeKey) {
        return entityTypeKey == null || entityTypeKey.isEmpty() ? null : FAR_SOUNDS.get(entityTypeKey);
    }

    public static void registerStartSound(net.minecraft.resources.ResourceLocation entityType,
                                          java.util.function.Supplier<net.minecraft.sounds.SoundEvent> sound) {
        START_SOUNDS.put(entityType.toString(), sound);
    }

    public static java.util.function.Supplier<net.minecraft.sounds.SoundEvent> startSound(String entityTypeKey) {
        return entityTypeKey == null || entityTypeKey.isEmpty() ? null : START_SOUNDS.get(entityTypeKey);
    }

    public static EngineSound engineSound(String entityTypeKey) {
        return entityTypeKey == null || entityTypeKey.isEmpty() ? null : ENGINE_SOUNDS.get(entityTypeKey);
    }

    public record JetExhaust(net.minecraft.world.phys.Vec3 nozzle, boolean large) {
    }

    private static final Map<String, java.util.List<JetExhaust>> JET_EXHAUSTS = new ConcurrentHashMap<>();

    public static void registerJetExhaust(net.minecraft.resources.ResourceLocation entityType,
                                          net.minecraft.world.phys.Vec3 nozzle, boolean large) {
        JET_EXHAUSTS.computeIfAbsent(entityType.toString(), key -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(new JetExhaust(nozzle, large));
    }

    public static java.util.List<JetExhaust> jetExhausts(String entityTypeKey) {
        return entityTypeKey == null || entityTypeKey.isEmpty() ? java.util.List.of()
                : JET_EXHAUSTS.getOrDefault(entityTypeKey, java.util.List.of());
    }

    public static boolean hasJetExhausts() {
        return !JET_EXHAUSTS.isEmpty();
    }

    @FunctionalInterface
    public interface CameraOverlay {
        void render(GuiGraphics graphics, Entity drone, int width, int height, float partialTick);
    }

    private static final Map<EntityType<?>, CameraOverlay> CAMERA_OVERLAYS = new ConcurrentHashMap<>();

    public static void registerCameraOverlay(EntityType<?> type, CameraOverlay overlay) {
        CAMERA_OVERLAYS.put(type, overlay);
    }

    public static CameraOverlay cameraOverlay(EntityType<?> type) {
        return CAMERA_OVERLAYS.get(type);
    }

    private static final java.util.Set<EntityType<?>> NO_CROSSHAIR = ConcurrentHashMap.newKeySet();

    public static void hideCrosshair(EntityType<?> type) {
        NO_CROSSHAIR.add(type);
    }

    public static boolean hidesCrosshair(EntityType<?> type) {
        return NO_CROSSHAIR.contains(type);
    }

    public static void registerCameraPanel(EntityType<?> type, CameraPanel panel) {
        PANELS.put(type, panel);
    }

    public static CameraPanel cameraPanel(EntityType<?> type) {
        return PANELS.get(type);
    }

    public static void registerLauncherDisplay(String kind, LauncherDisplay display) {
        DISPLAYS.put(kind, display);
    }

    public static LauncherDisplay display(String kind) {
        return kind == null || kind.isEmpty() ? null : DISPLAYS.get(kind);
    }
}
