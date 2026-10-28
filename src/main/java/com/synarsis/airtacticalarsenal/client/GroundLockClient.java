package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.api.DroneVariants;
import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import com.synarsis.airtacticalarsenal.network.GroundLockPacket;
import com.synarsis.airtacticalarsenal.network.NetworkHandler;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GroundLockClient {
    private static final double CONE_DEGREES = 45.0;
    private static final int CANDIDATE = 0xFFFFDD55;
    private static final int LOCK = 0xFFFF3333;

    private static int candidateId = -1;
    private static int lockedId = -1;
    private static int lockedDroneId = -1;
    private static boolean wasDown;

    private GroundLockClient() {
    }

    public static ShahedEntity lockDrone() {
        ShahedEntity drone = StrikeCameraClient.viewed();
        if (drone == null || !drone.hasCamera() || DroneVariants.interceptor(DroneVariants.kindOf(drone))) {
            return null;
        }
        return drone.isSteerable() || StrikeAimClient.selectMode() ? drone : null;
    }

    public static int lockedId() {
        return lockedId;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ShahedEntity drone = lockDrone();
        boolean down = drone != null && mc.screen == null && StrikeCameraClient.diveKey().isDown();
        boolean pressed = down && !wasDown;
        wasDown = down;
        if (drone == null) {
            candidateId = -1;
            lockedId = -1;
            lockedDroneId = -1;
            return;
        }
        if (lockedDroneId != drone.getId()) {
            lockedId = -1;
            lockedDroneId = drone.getId();
        }
        if (lockedId >= 0 && !drone.isSteerable() && StrikeAimClient.selectedEntityId() != lockedId) {
            lockedId = -1;
        }
        if (lockedId >= 0) {
            Entity target = mc.level != null ? mc.level.getEntity(lockedId) : null;
            if (target == null || !target.isAlive()) {
                lockedId = -1;
            }
        }
        Entity candidate = lockedId >= 0 ? null : findCandidate(drone);
        candidateId = candidate != null ? candidate.getId() : -1;
        if (!pressed) {
            return;
        }
        if (lockedId >= 0) {
            release(drone);
            return;
        }
        if (candidate == null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F, 0.5F));
            return;
        }
        lockedId = candidate.getId();
        if (drone.isSteerable()) {
            NetworkHandler.sendToServer(new GroundLockPacket(drone.getId(), candidate.getId()));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 2.0F, 0.6F));
        } else {
            StrikeAimClient.lockEntity(drone, candidate);
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS || event.getKey() != GLFW.GLFW_KEY_R
                || Minecraft.getInstance().screen != null || lockedId < 0) {
            return;
        }
        ShahedEntity drone = lockDrone();
        if (drone != null && drone.isSteerable()) {
            release(drone);
        } else {
            lockedId = -1;
        }
    }

    private static void release(ShahedEntity drone) {
        lockedId = -1;
        if (drone.isSteerable()) {
            NetworkHandler.sendToServer(new GroundLockPacket(drone.getId(), -1));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 1.2F, 0.6F));
        } else {
            StrikeAimClient.resume(drone);
        }
    }

    public static boolean isGroundTarget(Entity entity, Entity drone) {
        if (!CameraTargets.targetable(entity, drone) || entity instanceof ShahedEntity
                || entity instanceof com.synarsis.airtacticalarsenal.entity.OrlanEntity) {
            return false;
        }
        return ShahedEntity.onGroundTarget(entity);
    }

    private static Entity findCandidate(ShahedEntity drone) {
        Vec3 origin = CameraTargets.origin();
        Vec3 look = Vec3.directionFromRotation(OrlanCameraHandler.getCameraPitch(), OrlanCameraHandler.getCameraYaw());
        double bestCos = Math.cos(Math.toRadians(CONE_DEGREES));
        Entity best = null;
        for (Entity entity : CameraTargets.visibleTargets()) {
            if (!isGroundTarget(entity, drone)) {
                continue;
            }
            Vec3 to = entity.getBoundingBox().getCenter().subtract(origin);
            if (to.lengthSqr() < 1.0E-4) {
                continue;
            }
            double cos = to.normalize().dot(look);
            if (cos > bestCos) {
                bestCos = cos;
                best = entity;
            }
        }
        return best;
    }

    public static void render(GuiGraphics g, Entity drone, int width, int height, float partial) {
        if (lockDrone() != drone) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (candidateId >= 0) {
            Entity candidate = mc.level.getEntity(candidateId);
            if (candidate != null) {
                frame(g, drone, candidate, width, height, partial, CANDIDATE, false);
            }
        }
        if (lockedId >= 0 && ((ShahedEntity) drone).isSteerable()) {
            Entity target = mc.level.getEntity(lockedId);
            if (target != null) {
                frame(g, drone, target, width, height, partial, LOCK, true);
            }
        }
    }

    private static void frame(GuiGraphics g, Entity drone, Entity target, int width, int height, float partial,
                              int color, boolean distance) {
        Vec3 center = CameraTargets.lerpedCenter(target, partial);
        double[] xy = CameraTargets.project(center, width, height);
        if (xy == null) {
            return;
        }
        int x = (int) xy[0];
        int y = (int) xy[1];
        int h = CameraTargetOverlay.squareSize(target, drone) / 2 + 4;
        int len = Math.max(3, h / 2);
        g.fill(x - h, y - h, x - h + len, y - h + 1, color);
        g.fill(x - h, y - h, x - h + 1, y - h + len, color);
        g.fill(x + h - len + 1, y - h, x + h + 1, y - h + 1, color);
        g.fill(x + h, y - h, x + h + 1, y - h + len, color);
        g.fill(x - h, y + h, x - h + len, y + h + 1, color);
        g.fill(x - h, y + h - len + 1, x - h + 1, y + h + 1, color);
        g.fill(x + h - len + 1, y + h, x + h + 1, y + h + 1, color);
        g.fill(x + h, y + h - len + 1, x + h + 1, y + h + 1, color);
        if (distance) {
            Minecraft mc = Minecraft.getInstance();
            String text = Math.round(drone.position().distanceTo(center)) + " m";
            g.drawString(mc.font, text, x - mc.font.width(text) / 2, y + h + 3, color, true);
        }
    }
}
