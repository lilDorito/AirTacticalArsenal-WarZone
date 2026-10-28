package com.synarsis.airtacticalarsenal.client.sound;

import com.synarsis.airtacticalarsenal.config.AtaClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SoundPropagation {
    public static final double SPEED = 17.15;
    private static final int HISTORY = 160;
    private static final double MAX_SOURCE_SPEED = SPEED * 0.8;
    private static final float MIN_FACTOR = 0.75f;
    private static final float MAX_FACTOR = 1.4f;

    private final Vec3[] ring = new Vec3[HISTORY];
    private int head = -1;
    private int size;
    private Vec3 heard;
    private Vec3 heardVelocity = Vec3.ZERO;
    private int delayTicks;
    private float factor = 1.0f;

    public void push(Vec3 position) {
        if (position == null) {
            return;
        }
        head = (head + 1) % HISTORY;
        ring[head] = position;
        if (size < HISTORY) {
            size++;
        }
    }

    private Vec3 at(int ticksAgo) {
        return ring[((head - ticksAgo) % HISTORY + HISTORY) % HISTORY];
    }

    public static Vec3 listener() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameRenderer.getMainCamera().getPosition();
    }

    private static Vec3 listenerVelocity() {
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        return camera != null ? camera.getDeltaMovement() : Vec3.ZERO;
    }

    public void resolve() {
        if (size == 0) {
            heard = null;
            factor = 1.0f;
            return;
        }
        Vec3 listener = listener();
        int k = 0;
        if (AtaClientConfig.SOUND_DELAY.get()) {
            while (k < size - 1 && k * SPEED < listener.distanceTo(at(k))) {
                k++;
            }
        }
        Vec3 point = at(k);
        if (k > 0) {
            Vec3 newer = at(k - 1);
            double ahead = listener.distanceTo(newer) - (k - 1) * SPEED;
            double behind = listener.distanceTo(point) - k * SPEED;
            double span = ahead - behind;
            double t = span > 1.0E-6 ? ahead / span : 1.0;
            point = newer.lerp(point, Math.max(0.0, Math.min(1.0, t)));
        }
        heard = point;
        delayTicks = k;
        int newerIndex = Math.max(0, k - 1);
        int olderIndex = Math.min(size - 1, k + 1);
        int ticks = olderIndex - newerIndex;
        heardVelocity = ticks > 0 ? at(newerIndex).subtract(at(olderIndex)).scale(1.0 / ticks) : Vec3.ZERO;
        factor = computeFactor(listener);
    }

    private float computeFactor(Vec3 listener) {
        double strength = AtaClientConfig.DOPPLER_STRENGTH.get();
        if (strength <= 0.0 || heard == null) {
            return 1.0f;
        }
        Vec3 toListener = listener.subtract(heard);
        double distance = toListener.length();
        if (distance < 1.0) {
            return 1.0f;
        }
        Vec3 direction = toListener.scale(1.0 / distance);
        double source = Math.max(-MAX_SOURCE_SPEED, Math.min(MAX_SOURCE_SPEED, heardVelocity.dot(direction)));
        double observer = Math.max(-MAX_SOURCE_SPEED, Math.min(MAX_SOURCE_SPEED, -listenerVelocity().dot(direction)));
        double raw = (SPEED + observer) / (SPEED - source);
        double shifted = 1.0 + (raw - 1.0) * strength;
        return (float) Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, shifted));
    }

    public Vec3 heard(Vec3 fallback) {
        return heard != null ? heard : fallback;
    }

    public int delayTicks() {
        return delayTicks;
    }

    public float factor() {
        return factor;
    }
}
