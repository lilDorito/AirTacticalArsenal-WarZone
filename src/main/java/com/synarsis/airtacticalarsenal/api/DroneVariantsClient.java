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
