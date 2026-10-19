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
