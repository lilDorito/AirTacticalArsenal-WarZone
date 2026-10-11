package com.synarsis.airtacticalarsenal.client.sound;

import com.synarsis.airtacticalarsenal.entity.ShahedLauncherEntity;
import com.synarsis.airtacticalarsenal.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class LauncherSpinUpSound extends AbstractTickableSoundInstance {
    private static final Map<Integer, LauncherSpinUpSound> ACTIVE = new HashMap<>();
    private static final int RAMP_TICKS = 100;
    private static final int FADE_TICKS = 6;

    private final ShahedLauncherEntity launcher;
    private int age;
    private int fade = -1;

    public static void ensure(ShahedLauncherEntity launcher) {
        LauncherSpinUpSound current = ACTIVE.get(launcher.getId());
        if (current != null && !current.isStopped()) return;
        LauncherSpinUpSound sound = new LauncherSpinUpSound(launcher);
        ACTIVE.put(launcher.getId(), sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private LauncherSpinUpSound(ShahedLauncherEntity launcher) {
        super(launcher.getEngineSound() == 1 ? ModSounds.SHAHED_ENGINE_2.get() : ModSounds.SHAHED_ENGINE.get(),
                SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.launcher = launcher;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.1f;
        this.pitch = 0.5f;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.x = launcher.getX();
        this.y = launcher.getY() + 1.5;
        this.z = launcher.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        age++;
        if (fade < 0 && (launcher.isRemoved() || !launcher.isLaunching())) fade = 0;
        float t = Math.min(1f, age / (float) RAMP_TICKS);
        float gain = 0.1f + 0.9f * t;
        this.pitch = 0.5f + 0.5f * t;
        if (fade >= 0) {
            fade++;
            gain *= Math.max(0f, 1f - fade / (float) FADE_TICKS);
            if (fade >= FADE_TICKS) {
                ACTIVE.remove(launcher.getId(), this);
                this.volume = 0f;
                stop();
                return;
            }
        }
        this.volume = gain;
        this.x = launcher.getX();
        this.y = launcher.getY() + 1.5;
        this.z = launcher.getZ();
    }
}
