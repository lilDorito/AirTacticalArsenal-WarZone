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
public class OrlanEngineSoundClient extends AbstractTickableSoundInstance implements CrashModulated {

    private final int entityId;
    private boolean stopped = false;
    private int ticksNotFound = 0;
    private static final int MAX_NOT_FOUND = 100;

    private final float volumeScale;
    private final float pitchScale;
    private final com.synarsis.airtacticalarsenal.api.DroneVariantsClient.EngineSound custom;

    public OrlanEngineSoundClient(int entityId, double x, double y, double z) {
        this(entityId, x, y, z, null);
    }

    public OrlanEngineSoundClient(int entityId, double x, double y, double z,
                                  com.synarsis.airtacticalarsenal.api.DroneVariantsClient.EngineSound custom) {
        super(custom != null ? custom.loop().get() : ModSounds.ORLAN_ENGINE.get(), SoundSource.NEUTRAL,
                SoundInstance.createUnseededRandom());
        this.custom = custom;
        this.volumeScale = 1.0f;
        this.pitchScale = custom != null ? custom.pitch() : 1.0f;
        this.entityId = entityId;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.5f;
        this.pitch = 1.0f;
        this.attenuation = custom != null ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    private int crashTicks = 0;
    private float crashVolume = 1.0f;
    private float crashPitch = 1.0f;

    @Override
    public float crashVolumeFactor() {
        return crashVolume;
    }

    @Override
    public float crashPitchFactor() {
        return crashPitch;
    }

    public int getEntityId() { return entityId; }

    public void start() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getSoundManager() != null) {
            mc.getSoundManager().play(this);
        }
    }

    public void updateFromPacket(double px, double py, double pz) {
        this.x = px;
        this.y = py;
        this.z = pz;
        this.ticksNotFound = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            double dist = mc.player.position().distanceTo(new Vec3(px, py, pz));
            updateVolumeByDistance(dist);
        }
    }

    @Override
    public void tick() {
        if (stopped) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) {
            forceStop();
            return;
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity != null && !entity.isRemoved()) {
            ticksNotFound = 0;
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
            double dist = mc.player.position().distanceTo(entity.position());
            updateVolumeByDistance(dist);
            if (entity instanceof com.synarsis.airtacticalarsenal.entity.OrlanEntity orlan && orlan.isCrashing()) {
                crashTicks++;
                float vf;
                float pf;
                switch (orlan.getCrashMode()) {
                    case ENGINE_OUT -> {
                        vf = Math.max(0.0f, 1.0f - crashTicks / 50.0f);
                        pf = Math.max(0.7f, 1.0f - crashTicks * 0.005f);
                    }
                    case SPIN -> {
                        vf = Math.max(0.3f, 1.0f - crashTicks / 80.0f);
                        pf = 1.0f + Math.min(0.08f, crashTicks * 0.002f) + 0.03f * (float) Math.sin(crashTicks * 0.3f);
                    }
                    default -> {
                        vf = Math.max(0.0f, 1.0f - crashTicks / 20.0f);
                        pf = Math.max(0.5f, 1.0f - crashTicks * 0.02f);
                    }
                }
                crashVolume = vf;
                crashPitch = pf;
                this.volume *= vf;
                this.pitch = pf * this.pitchScale;
            }
        } else {
            ticksNotFound++;
            if (ticksNotFound > MAX_NOT_FOUND) {
                forceStop();
            }
        }
    }

    private void updateVolumeByDistance(double dist) {
        if (this.custom != null) {
            this.volume = this.custom.gainAt(dist);
            this.pitch = this.pitchScale;
            return;
        }
        if (dist < 30) this.volume = 3.0f;
        else if (dist < 80) this.volume = 2.0f;
        else if (dist < 150) this.volume = 1.2f;
        else if (dist < 300) this.volume = 0.5f;
        else this.volume = 0.15f;
        this.volume *= this.volumeScale;
        this.pitch = this.pitchScale;
    }

    public void forceStop() {
        this.stopped = true;
        this.volume = 0.0f;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getSoundManager() != null) {
            mc.getSoundManager().stop(this);
        }
    }

    public void stopSound() { forceStop(); }

    @Override
    public boolean isStopped() { return stopped; }
}
