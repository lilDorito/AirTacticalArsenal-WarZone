package com.synarsis.airtacticalarsenal.entity.crash;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class CrashDynamics {

    public enum Mode {
        NONE, ENGINE_OUT, SPIN, TUMBLE;

        public static Mode of(int ordinal) {
            Mode[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
        }
    }

    public static final int MAX_TICKS = 20 * 45;

    public static final int ZONE_BODY = 0;
    public static final int ZONE_ENGINE = 1;
    public static final int ZONE_WING_LEFT = 2;
    public static final int ZONE_WING_RIGHT = 3;
    public static final int ZONE_NOSE = 4;

    public static int zoneOf(Vec3 localHit, Vec3 direction, double wingThreshold) {
        Vec3 dir = direction.lengthSqr() > 1.0E-6 ? direction.normalize() : new Vec3(0, 0, 1);
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        if (flat.lengthSqr() > 1.0E-4) {
            Vec3 right = new Vec3(-flat.z, 0, flat.x).normalize();
            double lateral = localHit.dot(right);
            if (lateral > wingThreshold) return ZONE_WING_RIGHT;
            if (lateral < -wingThreshold) return ZONE_WING_LEFT;
        }
        double along = localHit.dot(dir);
        if (along < -0.5) return ZONE_ENGINE;
        if (along > 0.5) return ZONE_NOSE;
        return ZONE_BODY;
    }

    public static Mode modeForZone(int zone, boolean explosion, RandomSource random) {
        if (zone == ZONE_ENGINE) return Mode.ENGINE_OUT;
        if (zone == ZONE_WING_LEFT || zone == ZONE_WING_RIGHT) return Mode.SPIN;
        float r = random.nextFloat();
        if (explosion) return r < 0.4f ? Mode.SPIN : r < 0.7f ? Mode.ENGINE_OUT : Mode.TUMBLE;
        return r < 0.5f ? Mode.ENGINE_OUT : Mode.TUMBLE;
    }

    public static int sideForZone(int zone, RandomSource random) {
        if (zone == ZONE_WING_LEFT) return -1;
        if (zone == ZONE_WING_RIGHT) return 1;
        return random.nextBoolean() ? 1 : -1;
    }

    private Mode mode = Mode.NONE;
    private double horizontal;
    private double vertical;
    private Vec3 velocity = Vec3.ZERO;
    private float yaw;
    private float pitch;
    private float roll;
    private float yawRate;
    private float pitchRate;
    private float rollRate;
    private float targetYawRate;
    private int side = 1;
    private int ticks;
    private int spinStyle;
    private float targetPitch;
    private float wobble;

    public Mode mode() {
        return mode;
    }

    public boolean active() {
        return mode != Mode.NONE;
    }

    public int ticks() {
        return ticks;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float roll() {
        return roll;
    }

    public void start(Mode mode, Vec3 motion, float yaw, float pitch, int side, RandomSource random) {
        this.mode = mode;
        this.ticks = 0;
        this.side = side >= 0 ? 1 : -1;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = 0;
        this.horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.vertical = motion.y;
        this.velocity = motion;
        switch (mode) {
            case ENGINE_OUT -> {
                this.yawRate = this.side * (0.4f + random.nextFloat() * 0.8f);
                this.targetYawRate = this.side * (2.0f + random.nextFloat() * 2.5f);
            }
            case SPIN -> {
                this.yawRate = 0;
                this.spinStyle = random.nextInt(3);
                this.wobble = 4.0f + random.nextFloat() * 6.0f;
                switch (this.spinStyle) {
                    case 0 -> {
                        this.targetYawRate = this.side * (7.0f + random.nextFloat() * 6.0f);
                        this.targetPitch = -15.0f - random.nextFloat() * 20.0f;
                        this.rollRate = 0;
                    }
                    case 1 -> {
                        this.targetYawRate = this.side * (1.5f + random.nextFloat() * 3.0f);
                        this.targetPitch = -30.0f - random.nextFloat() * 30.0f;
                        this.rollRate = this.side * (8.0f + random.nextFloat() * 10.0f);
                    }
                    default -> {
                        this.targetYawRate = this.side * (0.5f + random.nextFloat() * 1.5f);
                        this.targetPitch = -65.0f - random.nextFloat() * 20.0f;
                        this.rollRate = sign(random) * (6.0f + random.nextFloat() * 8.0f);
                    }
                }
            }
            case TUMBLE -> {
                this.yawRate = sign(random) * (5.0f + random.nextFloat() * 9.0f);
                this.pitchRate = sign(random) * (7.0f + random.nextFloat() * 12.0f);
                this.rollRate = sign(random) * (9.0f + random.nextFloat() * 16.0f);
                this.velocity = motion.add(random.nextGaussian() * 0.15, 0.1 + random.nextFloat() * 0.2,
                        random.nextGaussian() * 0.15);
            }
            default -> {
            }
        }
    }

    public Vec3 step(RandomSource random) {
        ticks++;
        switch (mode) {
            case ENGINE_OUT -> {
                horizontal += (0.75 - horizontal) * 0.03;
                double sink = -Math.min(0.95, 0.25 + ticks * 0.006);
                vertical += (sink - vertical) * 0.08;
                yawRate += (targetYawRate - yawRate) * 0.01f;
                yaw += yawRate + Mth.sin(ticks * 0.25f) * 0.6f;
                float bank = Mth.clamp(yawRate * 9.0f, -40.0f, 40.0f) + Mth.sin(ticks * 0.37f) * 6.0f;
                roll += (bank - roll) * 0.1f;
                pitch = (float) Math.toDegrees(Math.atan2(vertical, Math.max(horizontal, 0.05)));
                velocity = direction(yaw).scale(horizontal).add(0, vertical, 0);
            }
            case SPIN -> {
                float ramp = Math.min(1.0f, ticks / 30.0f);
                double keep = spinStyle == 1 ? 0.6 : spinStyle == 2 ? 0.2 : 0.35;
                horizontal += (keep - horizontal) * 0.04;
                double fall = spinStyle == 2 ? -1.5 : spinStyle == 1 ? -1.2 : -0.9;
                vertical = Math.max(fall, vertical - 0.03);
                yawRate += (targetYawRate - yawRate) * 0.05f;
                yaw += yawRate;
                if (spinStyle == 0) {
                    float bank = side * 55.0f + Mth.sin(ticks * 0.3f) * wobble;
                    roll += (bank - roll) * 0.08f;
                } else {
                    roll += rollRate * ramp;
                }
                float nose = targetPitch + Mth.sin(ticks * 0.21f) * wobble;
                pitch += (nose - pitch) * 0.06f;
                Vec3 drift = new Vec3(random.nextGaussian() * 0.02, 0, random.nextGaussian() * 0.02);
                Vec3 forward = spinStyle == 1 ? Vec3.directionFromRotation(-pitch, -yaw).scale(0.3) : Vec3.ZERO;
                velocity = direction(yaw).scale(horizontal).add(0, vertical, 0).add(drift).add(forward.x, 0, forward.z);
            }
            case TUMBLE -> {
                velocity = velocity.add(0, -0.05, 0).scale(0.985);
                if (velocity.y < -2.5) velocity = new Vec3(velocity.x, -2.5, velocity.z);
                yaw += yawRate;
                pitch += pitchRate;
                roll += rollRate;
            }
            default -> {
                return Vec3.ZERO;
            }
        }
        yaw = Mth.wrapDegrees(yaw);
        pitch = Mth.wrapDegrees(pitch);
        roll = Mth.wrapDegrees(roll);
        return velocity;
    }

    private static Vec3 direction(float yawDeg) {
        double r = Math.toRadians(yawDeg);
        return new Vec3(Math.sin(r), 0, Math.cos(r));
    }

    private static float sign(RandomSource random) {
        return random.nextBoolean() ? 1.0f : -1.0f;
    }
}
