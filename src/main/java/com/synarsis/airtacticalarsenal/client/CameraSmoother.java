package com.synarsis.airtacticalarsenal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Iterator;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CameraSmoother {
    private static final double TICK_NS = 50_000_000.0;
    private static final double DELAY_TICKS = 2.0;
    private static final double MAX_AHEAD_TICKS = 3.0;
    private static final ArrayDeque<Sample> SAMPLES = new ArrayDeque<>();
    private static int entityId = -1;
    private static double offset = Double.NaN;
    private static Entity applied;
    private static double savedX, savedY, savedZ, savedXo, savedYo, savedZo;

    private record Sample(long tick, double x, double y, double z) {
    }

    private CameraSmoother() {
    }

    public static void sample(int id, long tick, double x, double y, double z) {
        if (!OrlanCameraHandler.isCameraActive() || OrlanCameraHandler.getLinkedOrlanId() != id) {
            return;
        }
        Sample last = SAMPLES.peekLast();
        if (id != entityId || (last != null && (tick < last.tick() - 40 || tick > last.tick() + 200))) {
            SAMPLES.clear();
            offset = Double.NaN;
            entityId = id;
            last = null;
        }
        if (last != null && tick <= last.tick()) {
            if (tick == last.tick()) {
                SAMPLES.pollLast();
                SAMPLES.addLast(new Sample(tick, x, y, z));
            }
            return;
        }
        SAMPLES.addLast(new Sample(tick, x, y, z));
        while (SAMPLES.size() > 60) {
            SAMPLES.pollFirst();
        }
        double observed = tick - System.nanoTime() / TICK_NS;
        if (Double.isNaN(offset) || Math.abs(observed - offset) > 20.0) {
            offset = observed;
        } else {
            offset += (observed - offset) * 0.05;
        }
    }

    private static Vec3 at(long now) {
        if (SAMPLES.size() < 2 || Double.isNaN(offset)) {
            return null;
        }
        Sample first = SAMPLES.peekFirst();
        Sample last = SAMPLES.peekLast();
        double t = now / TICK_NS + offset - DELAY_TICKS;
        if (t > last.tick() + MAX_AHEAD_TICKS) {
            offset -= t - (last.tick() + MAX_AHEAD_TICKS);
            t = last.tick() + MAX_AHEAD_TICKS;
        }
        if (t <= first.tick()) {
            return new Vec3(first.x(), first.y(), first.z());
        }
        if (t >= last.tick()) {
            Iterator<Sample> it = SAMPLES.descendingIterator();
            it.next();
            Sample prev = it.next();
            double span = last.tick() - prev.tick();
            double ahead = (t - last.tick()) / span;
            return new Vec3(last.x() + (last.x() - prev.x()) * ahead,
                    last.y() + (last.y() - prev.y()) * ahead,
                    last.z() + (last.z() - prev.z()) * ahead);
        }
        Sample a = first;
        for (Sample b : SAMPLES) {
            if (b.tick() >= t) {
                double f = b.tick() == a.tick() ? 1.0 : (t - a.tick()) / (double) (b.tick() - a.tick());
                return new Vec3(a.x() + (b.x() - a.x()) * f, a.y() + (b.y() - a.y()) * f, a.z() + (b.z() - a.z()) * f);
            }
            a = b;
        }
        return new Vec3(last.x(), last.y(), last.z());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.START) {
            applied = null;
            if (!OrlanCameraHandler.isCameraActive() || mc.level == null) {
                SAMPLES.clear();
                entityId = -1;
                offset = Double.NaN;
                return;
            }
            Entity linked = OrlanCameraHandler.getLinkedDrone();
            if (linked != null && linked.isAlive() && mc.getCameraEntity() != linked) {
                mc.setCameraEntity(linked);
            }
            Entity drone = mc.getCameraEntity();
            if (drone == null || drone.getId() != entityId) {
                return;
            }
            Vec3 pos = at(System.nanoTime());
            if (pos == null || pos.distanceToSqr(drone.position()) > 64.0 * 64.0) {
                return;
            }
            savedX = drone.getX();
            savedY = drone.getY();
            savedZ = drone.getZ();
            savedXo = drone.xo;
            savedYo = drone.yo;
            savedZo = drone.zo;
            drone.setPosRaw(pos.x, pos.y, pos.z);
            drone.xo = pos.x;
            drone.yo = pos.y;
            drone.zo = pos.z;
            applied = drone;
        } else {
            if (applied != null) {
                Entity drone = applied;
                applied = null;
                drone.setPosRaw(savedX, savedY, savedZ);
                drone.xo = savedXo;
                drone.yo = savedYo;
                drone.zo = savedZo;
            }
            DroneChunkCache.processFrame();
        }
    }
}
