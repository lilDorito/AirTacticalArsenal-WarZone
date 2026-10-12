package com.synarsis.airtacticalarsenal.client.sound;

import com.synarsis.airtacticalarsenal.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IskanderFlightSoundClient extends AbstractTickableSoundInstance {
    public static final double REACH = 1500.0;
    private static final double FALLOFF = 2.0;
    private static final float MAX_GAIN = 0.6f;
    private static final int FADE_IN_TICKS = 60;
    private static final int FADE_OUT_TICKS = 8;
    private static final int STALE_TICKS = 40;

    private final int entityId;
    private Vec3 pos;
    private Vec3 velocity = Vec3.ZERO;
    private int age;
    private int sinceUpdate;
    private int stopAt = -1;
    private boolean stopped;

    public IskanderFlightSoundClient(int entityId, Vec3 pos) {
        super(ModSounds.ISKANDER_FLIGHT.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.entityId = entityId;
        this.pos = pos;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
        this.pitch = 1.0f;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }

    public int getEntityId() {
        return entityId;
    }

    public void start() {
        Minecraft.getInstance().getSoundManager().play(this);
    }

    public void update(Vec3 serverPos) {
        Vec3 step = serverPos.subtract(pos);
        if (step.lengthSqr() < 400.0) velocity = step;
        pos = serverPos;
        sinceUpdate = 0;
    }

    public void finish(Vec3 at) {
        if (stopAt >= 0 || stopped) return;
        if (at != null) pos = at;
        Minecraft mc = Minecraft.getInstance();
        Vec3 listener = listener(mc);
        int delayTicks = listener == null ? 0 : (int) (listener.distanceTo(pos) / 17.0);
        stopAt = age + delayTicks;
        velocity = Vec3.ZERO;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (stopped) return;
        age++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            halt();
            return;
        }
        if (stopAt < 0) {
            Entity e = mc.level.getEntity(entityId);
            if (e != null && !e.isRemoved()) {
                pos = e.position();
            } else if (sinceUpdate > 0) {
                pos = pos.add(velocity);
            }
            if (sinceUpdate > STALE_TICKS) finish(null);
        }
        sinceUpdate++;
        x = pos.x;
        y = pos.y;
        z = pos.z;

        Vec3 listener = listener(mc);
        double d = listener == null ? REACH : listener.distanceTo(pos);
        float gain = d >= REACH ? 0f : MAX_GAIN * (float) Math.pow(1.0 - d / REACH, FALLOFF);
        gain *= Math.min(1f, age / (float) FADE_IN_TICKS);
        if (stopAt >= 0 && age >= stopAt) {
            int t = age - stopAt;
            if (t >= FADE_OUT_TICKS) {
                halt();
                return;
            }
            gain *= 1f - t / (float) FADE_OUT_TICKS;
        }
        volume = gain;
    }

    private static Vec3 listener(Minecraft mc) {
        if (mc.cameraEntity != null) return mc.cameraEntity.position();
        return mc.player != null ? mc.player.position() : null;
    }

    private void halt() {
        stopped = true;
        volume = 0f;
        stop();
    }

    @Override
    public boolean isStopped() {
        return stopped || super.isStopped();
    }
}
