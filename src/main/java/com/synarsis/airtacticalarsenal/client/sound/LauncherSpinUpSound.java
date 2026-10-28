package com.synarsis.airtacticalarsenal.client.sound;

import com.synarsis.airtacticalarsenal.api.DroneVariants;
import com.synarsis.airtacticalarsenal.api.DroneVariantsClient;
import com.synarsis.airtacticalarsenal.entity.UavLauncherEntity;
import com.synarsis.airtacticalarsenal.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
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
    private static final int FADE_TICKS = 40;

    private final UavLauncherEntity launcher;
    private int age;
    private int fade = -1;

    public static void ensure(UavLauncherEntity launcher) {
        LauncherSpinUpSound current = ACTIVE.get(launcher.getId());
        if (current != null && !current.isStopped()) return;
        String typeKey = DroneVariants.entityTypeKey(launcher.getDroneKind());
        DroneVariantsClient.EngineSound custom = DroneVariantsClient.engineSound(typeKey);
        LauncherSpinUpSound sound = new LauncherSpinUpSound(launcher, custom);
        ACTIVE.put(launcher.getId(), sound);
        Minecraft.getInstance().getSoundManager().play(sound);
        java.util.function.Supplier<net.minecraft.sounds.SoundEvent> start = DroneVariantsClient.startSound(typeKey);
        if (start != null) {
            Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(start.get(), SoundSource.HOSTILE,
                    custom != null ? custom.volume() : 1.0f, 1.0f, SoundInstance.createUnseededRandom(),
                    launcher.getX(), launcher.getY() + 1.5, launcher.getZ()));
        }
    }

    private final float volumeScale;
    private final float pitchScale;

    private LauncherSpinUpSound(UavLauncherEntity launcher, DroneVariantsClient.EngineSound custom) {
        super(custom != null ? custom.loop().get()
                        : launcher.getEngineSound() == 1 ? ModSounds.SHAHED_ENGINE_2.get() : ModSounds.SHAHED_ENGINE.get(),
                SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.volumeScale = custom != null ? custom.volume() : 1.0f;
        this.pitchScale = custom != null ? custom.pitch() : 1.0f;
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
        this.pitch = (0.5f + 0.5f * t) * this.pitchScale;
        if (fade >= 0) {
            fade++;
            float left = Math.max(0f, 1f - fade / (float) FADE_TICKS);
            gain *= left * left;
            if (fade >= FADE_TICKS) {
                ACTIVE.remove(launcher.getId(), this);
                this.volume = 0f;
                stop();
                return;
            }
        }
        this.volume = gain * this.volumeScale;
        if (fade < 0) {
            this.x = launcher.getX();
            this.y = launcher.getY() + 1.5;
            this.z = launcher.getZ();
        }
    }
}
