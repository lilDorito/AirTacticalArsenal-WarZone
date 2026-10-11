package com.synarsis.airtacticalarsenal.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class MissileProximityFuse {

    private static final double RADIUS = 4.0;
    private static final double SEARCH = 14.0;
    private static final Map<Class<?>, Optional<Method>> TARGET_GETTER = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Optional<Method>> EXPLODE = new ConcurrentHashMap<>();

    private MissileProximityFuse() {}

    public static Projectile find(Entity self) {
        if (self.level().isClientSide) return null;
        String uuid = self.getStringUUID();
        Vec3 center = self.getBoundingBox().getCenter();
        for (Projectile p : self.level().getEntitiesOfClass(Projectile.class, self.getBoundingBox().inflate(SEARCH),
                e -> e != self && e.isAlive())) {
            if (!uuid.equals(targetOf(p))) continue;
            Vec3 pos = p.position();
            Vec3 v = p.getDeltaMovement();
            double d = Math.min(segmentDistance(pos.subtract(v), pos, center), segmentDistance(pos, pos.add(v), center));
            if (d <= RADIUS) return p;
        }
        return null;
    }

    public static void detonate(Projectile missile) {
        Method m = EXPLODE.computeIfAbsent(missile.getClass(), c -> {
            try {
                return Optional.of(c.getMethod("causeExplode", Vec3.class));
            } catch (Throwable t) {
                return Optional.empty();
            }
        }).orElse(null);
        if (m != null) {
            try {
                m.invoke(missile, missile.position());
            } catch (Throwable ignored) {
            }
        }
        missile.discard();
    }

    private static String targetOf(Projectile p) {
        Method m = TARGET_GETTER.computeIfAbsent(p.getClass(), c -> {
            try {
                Method g = c.getMethod("getTargetUUID");
                return g.getReturnType() == String.class ? Optional.of(g) : Optional.empty();
            } catch (Throwable t) {
                return Optional.empty();
            }
        }).orElse(null);
        if (m == null) return null;
        try {
            return (String) m.invoke(p);
        } catch (Throwable t) {
            return null;
        }
    }

    private static double segmentDistance(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.subtract(a);
        double len = ab.lengthSqr();
        double t = len < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, p.subtract(a).dot(ab) / len));
        return a.add(ab.scale(t)).distanceTo(p);
    }
}
