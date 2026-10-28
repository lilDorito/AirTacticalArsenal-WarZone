package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.config.AtaClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CameraTargetOverlay {
    private static final int WHITE = 0xDDFFFFFF;
    private static final int GREEN = 0xDD55FF55;
    private static final int LOCK = 0xFFFF3333;

    private CameraTargetOverlay() {
    }

    public static void render(GuiGraphics g, int width, int height, Entity drone) {
        Minecraft mc = Minecraft.getInstance();
        float partial = mc.getFrameTime();
        int locked = OrlanCameraHandler.isLocked() ? OrlanCameraHandler.getLockEntityId() : -1;
        boolean boxes = drone instanceof com.synarsis.airtacticalarsenal.entity.ShahedEntity && StrikeAimClient.aimDrone() == drone;
        if (AtaClientConfig.TARGET_SQUARES.get()) {
            for (Entity entity : CameraTargets.visibleTargets()) {
                if (!entity.isAlive() || entity.getId() == locked) {
                    continue;
                }
                double[] xy = CameraTargets.project(CameraTargets.lerpedCenter(entity, partial), width, height);
                if (xy == null) {
                    continue;
                }
                int size = squareSize(entity, drone);
                square(g, (int) xy[0], (int) xy[1], size, CameraTargets.friendly(entity) ? GREEN : WHITE);
            }
        }
        if (boxes) {
            StrikeAimClient.renderSelection(g, drone, width, height, partial);
            StrikeAimClient.renderDrag(g, width, height);
        } else if (locked >= 0 && mc.level != null) {
            Entity target = mc.level.getEntity(locked);
            if (target != null && target.isAlive()) {
                Vec3 center = CameraTargets.lerpedCenter(target, partial);
                double[] xy = CameraTargets.project(center, width, height);
                if (xy != null) {
                    int x = (int) xy[0];
                    int y = (int) xy[1];
                    int size = squareSize(target, drone) + 6;
                    corners(g, x, y, size, LOCK);
                    String distance = Math.round(drone.position().distanceTo(center)) + " m";
                    g.drawString(mc.font, distance, x - mc.font.width(distance) / 2, y + size / 2 + 3, LOCK, true);
                }
            }
        }
        GroundLockClient.render(g, drone, width, height, partial);
        com.synarsis.airtacticalarsenal.api.DroneVariantsClient.CameraOverlay extra =
                com.synarsis.airtacticalarsenal.api.DroneVariantsClient.cameraOverlay(drone.getType());
        if (extra != null) {
            extra.render(g, drone, width, height, partial);
        }
    }

    static int squareSize(Entity entity, Entity drone) {
        double distance = Math.max(1.0, entity.distanceTo(drone));
        double extent = Math.max(entity.getBbWidth(), entity.getBbHeight());
        double f = CameraTargets.focal(Minecraft.getInstance().getWindow().getGuiScaledHeight());
        int size = (int) Math.round(extent * f / distance) + 4;
        return Math.max(8, Math.min(48, size));
    }

    private static void square(GuiGraphics g, int x, int y, int size, int color) {
        int h = size / 2;
        g.fill(x - h, y - h, x + h + 1, y - h + 1, color);
        g.fill(x - h, y + h, x + h + 1, y + h + 1, color);
        g.fill(x - h, y - h, x - h + 1, y + h + 1, color);
        g.fill(x + h, y - h, x + h + 1, y + h + 1, color);
    }

    private static void corners(GuiGraphics g, int x, int y, int size, int color) {
        int h = size / 2;
        int len = Math.max(3, size / 4);
        g.fill(x - h, y - h, x - h + len, y - h + 1, color);
        g.fill(x - h, y - h, x - h + 1, y - h + len, color);
        g.fill(x + h - len + 1, y - h, x + h + 1, y - h + 1, color);
        g.fill(x + h, y - h, x + h + 1, y - h + len, color);
        g.fill(x - h, y + h, x - h + len, y + h + 1, color);
        g.fill(x - h, y + h - len + 1, x - h + 1, y + h + 1, color);
        g.fill(x + h - len + 1, y + h, x + h + 1, y + h + 1, color);
        g.fill(x + h, y + h - len + 1, x + h + 1, y + h + 1, color);
    }
}
