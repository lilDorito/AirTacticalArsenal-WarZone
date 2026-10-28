package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.config.AtaClientConfig;
import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import com.synarsis.airtacticalarsenal.network.CameraLookPacket;
import com.synarsis.airtacticalarsenal.network.DroneSteerPacket;
import com.synarsis.airtacticalarsenal.network.NetworkHandler;
import com.synarsis.airtacticalarsenal.network.StrikeAimPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class StrikeAimClient {
    private static final double RAY_LENGTH = 1300.0;
    private static final int ATTACK = 0xFFFF3333;
    private static final int ORBIT = 0xFFFFDD55;

    private static int flashTicks;
    private static boolean flashOrbit;
    private static final Set<Integer> MANUAL = new HashSet<>();
    private static int selectionDroneId = -1;
    private static int selectionEntityId = -1;
    private static Vec3 selectionPoint;
    private static double selectionW;
    private static double selectionH;
    private static boolean selectionOrbit;
    private static int ticks;
    private static final float MAX_ROLL = 40.0f;
    private static final float ROLL_TURN = 0.08f;
    private static float roll;
    private static final double CLICK_PIXELS = 5.0;
    private static boolean dragging;
    private static int dragButton = -1;
    private static float anchorYaw;
    private static float anchorPitch;
    private static double boxX;
    private static double boxY;

    private StrikeAimClient() {
    }

    public static boolean autopilot(ShahedEntity drone) {
        return drone != null && drone.isSteerable() && !MANUAL.contains(drone.getId());
    }

    public static boolean selectMode() {
        return AtaClientConfig.STRIKE_AIM.get() == AtaClientConfig.StrikeAim.SELECT;
    }

    public static ShahedEntity aimDrone() {
        ShahedEntity drone = StrikeCameraClient.viewed();
        return drone != null && drone.hasCamera() && !drone.isSteerable() && selectMode() ? drone : null;
    }

    public static ShahedEntity steeredDrone() {
        ShahedEntity drone = StrikeCameraClient.viewed();
        return drone != null && drone.isSteerable() ? drone : null;
    }

    public static boolean flashOrbit() {
        return flashOrbit;
    }

    public static int flashTicks() {
        return flashTicks;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || !OrlanCameraHandler.isCameraActive()) {
            return;
        }
        int button = event.getButton();
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }
        event.setCanceled(true);
        ShahedEntity drone = aimDrone();
        if (drone == null) {
            cancelDrag();
            return;
        }
        if (event.getAction() == GLFW.GLFW_PRESS && !dragging) {
            dragging = true;
            dragButton = button;
            anchorYaw = mc.player.getYRot();
            anchorPitch = mc.player.getXRot();
            boxX = 0.0;
            boxY = 0.0;
        } else if (event.getAction() == GLFW.GLFW_RELEASE && dragging && button == dragButton) {
            finishDrag(drone);
        }
    }

    public static void beginCameraFrame(net.minecraft.world.entity.player.Player player) {
        if (!dragging) {
            return;
        }
        if (aimDrone() == null) {
            cancelDrag();
            return;
        }
        float dYaw = net.minecraft.util.Mth.wrapDegrees(player.getYRot() - anchorYaw);
        float dPitch = player.getXRot() - anchorPitch;
        Minecraft mc = Minecraft.getInstance();
        double f = CameraTargets.focal(mc.getWindow().getGuiScaledHeight());
        boxX += Math.toRadians(dYaw) * f;
        boxY += Math.toRadians(dPitch) * f;
        double maxX = mc.getWindow().getGuiScaledWidth() * 0.45;
        double maxY = mc.getWindow().getGuiScaledHeight() * 0.45;
        boxX = net.minecraft.util.Mth.clamp(boxX, -maxX, maxX);
        boxY = net.minecraft.util.Mth.clamp(boxY, -maxY, maxY);
        player.setYRot(anchorYaw);
        player.setXRot(anchorPitch);
        player.yRotO = anchorYaw;
        player.xRotO = anchorPitch;
    }

    private static long lastTurnNanos;

    public static void applyRollTurn(net.minecraft.world.entity.player.Player player, float partialTick) {
        long now = System.nanoTime();
        long last = lastTurnNanos;
        lastTurnNanos = now;
        Minecraft mc = Minecraft.getInstance();
        ShahedEntity steered = steeredDrone();
        if (steered == null || mc.screen != null || autopilot(steered) || last == 0L) {
            return;
        }
        float current = OrlanCameraHandler.getCameraRoll(partialTick);
        if (current == 0.0f) {
            return;
        }
        double ticks = Math.min(0.25, (now - last) / 1.0E9) * 20.0;
        float turn = (float) (current * ROLL_TURN * ticks);
        player.setYRot(player.getYRot() + turn);
        player.yRotO += turn;
    }

    public static void endCameraFrame(net.minecraft.world.entity.player.Player player) {
        if (dragging) {
            anchorYaw = player.getYRot();
            anchorPitch = player.getXRot();
        }
    }

    public static boolean isDragging() {
        return dragging;
    }

    private static void cancelDrag() {
        dragging = false;
        dragButton = -1;
        boxX = 0.0;
        boxY = 0.0;
    }

    private static void finishDrag(ShahedEntity drone) {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        boolean orbit = dragButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        boolean rect = Math.hypot(boxX, boxY) > CLICK_PIXELS;
        double cx = width * 0.5 + (rect ? boxX * 0.5 : 0.0);
        double cy = height * 0.5 + (rect ? boxY * 0.5 : 0.0);
        double boxW = rect ? net.minecraft.util.Mth.clamp(Math.abs(boxX), 10.0, width * 0.65) : 14.0;
        double boxH = rect ? net.minecraft.util.Mth.clamp(Math.abs(boxY), 10.0, height * 0.65) : 12.0;
        cancelDrag();
        select(drone, orbit, cx, cy, boxW, boxH, !rect);
    }

    public static void renderDrag(GuiGraphics g, int width, int height) {
        if (!dragging) {
            return;
        }
        int cx = width / 2;
        int cy = height / 2;
        int x0 = (int) Math.round(Math.min(cx, cx + boxX));
        int x1 = (int) Math.round(Math.max(cx, cx + boxX));
        int y0 = (int) Math.round(Math.min(cy, cy + boxY));
        int y1 = (int) Math.round(Math.max(cy, cy + boxY));
        if (x1 - x0 < 2 && y1 - y0 < 2) {
            return;
        }
        int color = dragButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT ? ORBIT : 0xFFFFFFFF;
        outline(g, x0, y0, x1, y1, color);
        g.fill(x0 + 1, y0 + 1, x1, y1, 0x18FFFFFF);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS || Minecraft.getInstance().screen != null) {
            return;
        }
        ShahedEntity steered = steeredDrone();
        if (steered != null && event.getKey() == GLFW.GLFW_KEY_SPACE) {
            toggleAutopilot(steered);
            return;
        }
        ShahedEntity drone = aimDrone();
        if (drone == null || dragging) {
            return;
        }
        if (event.getKey() == GLFW.GLFW_KEY_R) {
            resume(drone);
        } else if (event.getKey() == GLFW.GLFW_KEY_SPACE) {
            OrlanCameraHandler.releaseFollow();
        }
    }

    public static void toggleAutopilot(ShahedEntity steered) {
        if (autopilot(steered)) {
            if (MANUAL.size() > 256) {
                MANUAL.clear();
            }
            MANUAL.add(steered.getId());
        } else {
            MANUAL.remove(steered.getId());
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                autopilot(steered) ? SoundEvents.NOTE_BLOCK_BASS.value() : SoundEvents.NOTE_BLOCK_PLING.value(), 1.4F, 0.6F));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ticks++;
        if (flashTicks > 0) {
            flashTicks--;
        }
        Minecraft mc = Minecraft.getInstance();
        if (OrlanCameraHandler.isCameraActive() && mc.player != null) {
            NetworkHandler.sendToServer(new CameraLookPacket(mc.player.getYRot(), mc.player.getXRot()));
        }
        ShahedEntity steered = steeredDrone();
        float targetRoll = 0.0f;
        if (steered != null && mc.player != null && mc.screen == null && !autopilot(steered)) {
            if (mc.options.keyLeft.isDown()) {
                targetRoll -= MAX_ROLL;
            }
            if (mc.options.keyRight.isDown()) {
                targetRoll += MAX_ROLL;
            }
        }
        roll += (targetRoll - roll) * 0.15f;
        if (Math.abs(roll) < 0.05f) {
            roll = 0.0f;
        }
        OrlanCameraHandler.setCameraRoll(steered != null ? roll : 0.0f);
        if (steered == null) {
            roll = 0.0f;
        } else if (mc.player != null && mc.screen == null && !autopilot(steered)) {
            NetworkHandler.sendToServer(new DroneSteerPacket(steered.getId(),
                    OrlanCameraHandler.getCameraYaw(), OrlanCameraHandler.getCameraPitch(), roll));
        }
        ShahedEntity aim = aimDrone();
        if (aim == null || aim.getId() != selectionDroneId) {
            clearSelection();
        }
        if (dragging && (aim == null || mc.screen != null)) {
            cancelDrag();
        }
    }

    public static void clearSelection() {
        selectionDroneId = -1;
        selectionEntityId = -1;
        selectionPoint = null;
    }

    public static Entity pick(double cx, double cy, double halfW, double halfH, boolean click, ShahedEntity drone) {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        float partial = mc.getFrameTime();
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        int inside = 0;
        Set<UUID> seen = new HashSet<>();
        for (Entity entity : CameraTargets.visibleTargets()) {
            if (!entity.isAlive() || !seen.add(entity.getUUID())) {
                continue;
            }
            double[] xy = CameraTargets.project(CameraTargets.lerpedCenter(entity, partial), width, height);
            if (xy == null) {
                continue;
            }
            double reachX = halfW;
            double reachY = halfH;
            if (click) {
                double half = CameraTargetOverlay.squareSize(entity, drone) * 0.5 + 2.0;
                reachX = Math.max(reachX, half);
                reachY = Math.max(reachY, half);
            }
            double dx = Math.abs(xy[0] - cx);
            double dy = Math.abs(xy[1] - cy);
            if (dx > reachX || dy > reachY) {
                continue;
            }
            inside++;
            double distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        if (!click && inside > 1) {
            return null;
        }
        return best;
    }

    public static void select(ShahedEntity drone, boolean orbit, double cx, double cy, double boxW, double boxH, boolean click) {
        Entity selected = pick(cx, cy, boxW * 0.5, boxH * 0.5, click, drone);
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int action = orbit ? StrikeAimPacket.ORBIT : StrikeAimPacket.STRIKE;
        Vec3 origin = CameraTargets.origin();
        Vec3 point;
        if (selected != null) {
            point = selected.getBoundingBox().getCenter();
            NetworkHandler.sendToServer(new StrikeAimPacket(drone.getId(), action, selected.getId(), point, origin, Vec3.ZERO));
            OrlanCameraHandler.followTarget(point, selected.getId());
        } else {
            Vec3 ray = CameraTargets.ray(cx, cy, width, height);
            point = CameraTargets.clipBlock(origin, ray, RAY_LENGTH, drone);
            NetworkHandler.sendToServer(new StrikeAimPacket(drone.getId(), action, -1, point, origin, ray));
            if (point != null) {
                OrlanCameraHandler.followTarget(point, -1);
            } else {
                OrlanCameraHandler.releaseFollow();
                clearSelection();
            }
        }
        if (point != null || selected != null) {
            selectionDroneId = drone.getId();
            selectionEntityId = selected != null ? selected.getId() : -1;
            selectionPoint = point;
            selectionW = boxW;
            selectionH = boxH;
            selectionOrbit = orbit;
        }
        flashTicks = 20;
        flashOrbit = orbit;
        StrikeCameraClient.markSent(orbit ? null : point);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), orbit ? 1.5F : 2.0F, 0.6F));
    }

    public static void lockEntity(ShahedEntity drone, Entity target) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 point = target.getBoundingBox().getCenter();
        NetworkHandler.sendToServer(new StrikeAimPacket(drone.getId(), StrikeAimPacket.STRIKE, target.getId(), point,
                CameraTargets.origin(), Vec3.ZERO));
        OrlanCameraHandler.followTarget(point, target.getId());
        int size = CameraTargetOverlay.squareSize(target, drone) + 6;
        selectionDroneId = drone.getId();
        selectionEntityId = target.getId();
        selectionPoint = point;
        selectionW = size;
        selectionH = size;
        selectionOrbit = false;
        flashTicks = 20;
        flashOrbit = false;
        StrikeCameraClient.markSent(point);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 2.0F, 0.6F));
    }

    public static void resume(ShahedEntity drone) {
        NetworkHandler.sendToServer(new StrikeAimPacket(drone.getId(), StrikeAimPacket.RESUME, -1, null, null, null));
        OrlanCameraHandler.releaseFollow();
        StrikeCameraClient.markSent(null);
        clearSelection();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 1.2F, 0.6F));
    }

    public static int selectedEntityId() {
        return selectionEntityId;
    }

    public static void renderSelection(GuiGraphics g, Entity drone, int width, int height, float partial) {
        if (selectionPoint == null || drone.getId() != selectionDroneId) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Vec3 point = selectionPoint;
        if (selectionEntityId >= 0 && mc.level != null) {
            Entity target = mc.level.getEntity(selectionEntityId);
            if (target != null && target.isAlive()) {
                point = CameraTargets.lerpedCenter(target, partial);
                selectionPoint = point;
            }
        }
        double[] xy = CameraTargets.project(point, width, height);
        if (xy == null) {
            return;
        }
        int jitterX = ticks / 5 * 37 % 3 - 1;
        int jitterY = ticks / 7 * 13 % 3 - 1;
        int x = (int) Math.round(xy[0]) + jitterX;
        int y = (int) Math.round(xy[1]) + jitterY;
        int hw = (int) Math.round(selectionW * 0.5);
        int hh = (int) Math.round(selectionH * 0.5);
        int color = selectionOrbit ? ORBIT : ATTACK;
        outline(g, x - hw, y - hh, x + hw, y + hh, color);
        String distance = Math.round(drone.position().distanceTo(point)) + " m";
        g.drawString(mc.font, distance, x - mc.font.width(distance) / 2, y + hh + 3, color, true);
    }

    public static void outline(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1 + 1, y0 + 1, color);
        g.fill(x0, y1, x1 + 1, y1 + 1, color);
        g.fill(x0, y0, x0 + 1, y1 + 1, color);
        g.fill(x1, y0, x1 + 1, y1 + 1, color);
    }
}
