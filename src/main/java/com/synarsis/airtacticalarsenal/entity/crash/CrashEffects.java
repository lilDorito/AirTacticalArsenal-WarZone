package com.synarsis.airtacticalarsenal.entity.crash;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CrashEffects {

    private CrashEffects() {}

    public static void trail(Level level, Vec3 pos, Vec3 motion, CrashDynamics.Mode mode, int tick, float scale,
                             RandomSource random) {
        Vec3 back = motion.lengthSqr() > 1.0E-4 ? pos.subtract(motion.normalize().scale(scale)) : pos;
        switch (mode) {
            case ENGINE_OUT -> {
                spawn(level, ParticleTypes.LARGE_SMOKE, back, 0.15 * scale, random);
                if (tick % 2 == 0) spawn(level, ParticleTypes.SMOKE, back, 0.2 * scale, random);
                if (tick < 60 && tick % 3 == 0) spawn(level, ParticleTypes.FLAME, back, 0.1 * scale, random);
            }
            case SPIN -> {
                spawn(level, ParticleTypes.LARGE_SMOKE, back, 0.25 * scale, random);
                spawn(level, ParticleTypes.LARGE_SMOKE, pos, 0.4 * scale, random);
                spawn(level, ParticleTypes.FLAME, back, 0.15 * scale, random);
                if (tick % 4 == 0) spawn(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, back, 0.1 * scale, random);
            }
            case TUMBLE -> {
                spawn(level, ParticleTypes.LARGE_SMOKE, pos, 0.4 * scale, random);
                spawn(level, ParticleTypes.FLAME, pos, 0.3 * scale, random);
                if (tick % 3 == 0) spawn(level, ParticleTypes.LAVA, pos, 0.2 * scale, random);
            }
            default -> {
            }
        }
    }

    private static void spawn(Level level, ParticleOptions type, Vec3 at, double spread, RandomSource random) {
        level.addAlwaysVisibleParticle(type, true,
                at.x + (random.nextDouble() - 0.5) * spread * 2,
                at.y + (random.nextDouble() - 0.5) * spread * 2,
                at.z + (random.nextDouble() - 0.5) * spread * 2,
                (random.nextDouble() - 0.5) * 0.02, 0.02 + random.nextDouble() * 0.03,
                (random.nextDouble() - 0.5) * 0.02);
    }
}
