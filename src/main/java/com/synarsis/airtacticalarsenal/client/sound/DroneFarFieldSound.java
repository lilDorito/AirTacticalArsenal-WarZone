package com.synarsis.airtacticalarsenal.client.sound;

import com.synarsis.airtacticalarsenal.api.DroneVariantsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DroneFarFieldSound extends AbstractTickableSoundInstance {
    private static final float SPEED_OF_SOUND = 17.0f;

    private final TickableSoundInstance host;
    private int age;
    private final DroneVariantsClient.FarSound spec;

    private DroneFarFieldSound(TickableSoundInstance host, DroneVariantsClient.FarSound spec) {
        super(spec.loop().get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.host = host;
        this.spec = spec;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
        this.pitch = 1.0f;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = host.getX();
        this.y = host.getY();
        this.z = host.getZ();
    }

    public static void attach(TickableSoundInstance host, String typeKey) {
        DroneVariantsClient.FarSound spec = DroneVariantsClient.farSound(typeKey);
        if (spec == null || host == null) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(new DroneFarFieldSound(host, spec));
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        age++;
        if (mc.player == null || host.isStopped() || (age > 40 && !mc.getSoundManager().isActive(host))) {
            this.volume = 0.0f;
            stop();
            return;
        }
        this.x = host.getX();
        this.y = host.getY();
        this.z = host.getZ();
        double distance = Math.sqrt(mc.player.distanceToSqr(this.x, this.y, this.z));
        float fadeIn = (float) Mth.clamp((distance - spec.near()) / Math.max(1.0, spec.near()), 0.0, 1.0);
        float fadeOut = (float) Mth.clamp((spec.range() - distance) / (spec.range() * 0.25), 0.0, 1.0);
        float falloff = (float) Mth.clamp(1.0 - distance / spec.range(), 0.0, 1.0);
        this.volume = spec.volume() * fadeIn * fadeOut * (0.35f + 0.65f * falloff);
        this.pitch = Mth.clamp(1.0f - (float) (distance / spec.range()) * 0.15f, 0.8f, 1.0f);
        if (host instanceof CrashModulated crash) {
            this.volume *= crash.crashVolumeFactor();
            this.pitch *= crash.crashPitchFactor();
        }
        if (host instanceof DopplerSource doppler) {
            this.pitch *= doppler.dopplerFactor();
        }
    }
}
