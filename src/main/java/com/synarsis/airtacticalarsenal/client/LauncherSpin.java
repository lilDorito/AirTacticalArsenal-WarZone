package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.entity.UavLauncherEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class LauncherSpin {
    public static final float RAMP_TICKS = 80.0f;
    private static final Map<Integer, Long> START = new HashMap<>();

    private LauncherSpin() {
    }

    public static float angle(UavLauncherEntity launcher, float partial, float topRadPerTick) {
        if (!launcher.isLaunching()) {
            START.remove(launcher.getId());
            return 0.0f;
        }
        if (START.size() > 64) {
            START.clear();
        }
        long start = START.computeIfAbsent(launcher.getId(), id -> launcher.level().getGameTime());
        float t = launcher.level().getGameTime() - start + partial;
        if (t < RAMP_TICKS) {
            return topRadPerTick * t * t / (2.0f * RAMP_TICKS);
        }
        return topRadPerTick * RAMP_TICKS / 2.0f + topRadPerTick * (t - RAMP_TICKS);
    }
}
