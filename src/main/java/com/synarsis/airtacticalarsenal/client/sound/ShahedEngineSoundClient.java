package com.synarsis.airtacticalarsenal.client.sound;

import com.synarsis.airtacticalarsenal.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShahedEngineSoundClient extends AbstractTickableSoundInstance implements CrashModulated {
    private final int entityId;
    private final Vec3 startPos;
    private final BlockPos targetPos;
    private final String diveMode;
    private boolean shouldStop = false;
    private boolean exploded = false;
    private boolean stopped = false;
    private long startTime;
    private long creationTime;
    private Vec3 explosionPos = null;
    private long explosionTime = 0L;
    private boolean entityRemovedLocally = false;
    private float pitchAtExplosion = 1.0f;
    private float volumeAtExplosion = 1.0f;
    private static final double CRUISE_SPEED = 1.5;
    private double diveDistance;
    private double diveTransitionDistance;
    private double modeSwitchDistance;
    private float currentPitch = 1.0f;
    private float targetPitch = 1.0f;
    private float currentVolumeMultiplier = 1.0f;
    private float targetVolumeMultiplier = 1.0f;
    private int divingTicks = 0;
    private int maxDivingTicks;

    private boolean inModeSwitchPhase = false;
    private int modeSwitchTicks = 0;
    private static final int MODE_SWITCH_DURATION_TICKS = 30;
    private boolean modeSwitchComplete = false;

    private Vec3 currentDronePos;
    private Vec3 lastDirection = new Vec3(1, 0, 0);
    private float localDiveProgress = 0.0f;
    private boolean isLateJoin = false;

    private int ticksAlive = 0;
    private int ticksEntityNotFound = 0;
    private static final int MAX_LIFETIME_TICKS = 2 * 60 * 60 * 20;
    private static final int MAX_ENTITY_NOT_FOUND_TICKS = 200; 

    private Vec3 lastServerPos = null;
    private Vec3 inferredVelocity = null;

    private final float volumeScale;
    private final float pitchScale;
    private final com.synarsis.airtacticalarsenal.api.DroneVariantsClient.EngineSound custom;

    public ShahedEngineSoundClient(int entityId, Vec3 currentPos, BlockPos targetPos, String diveMode, int engineSound) {
        this(entityId, currentPos, targetPos, diveMode, engineSound, null);
    }

    public ShahedEngineSoundClient(int entityId, Vec3 currentPos, BlockPos targetPos, String diveMode, int engineSound,
                                   com.synarsis.airtacticalarsenal.api.DroneVariantsClient.EngineSound custom) {
        super(custom != null ? custom.loop().get()
                        : engineSound == 1 ? ModSounds.SHAHED_ENGINE_2.get() : ModSounds.SHAHED_ENGINE.get(),
                SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.custom = custom;
        this.volumeScale = 1.0f;
        this.pitchScale = custom != null ? custom.pitch() : 1.0f;
        this.entityId = entityId;
        this.startPos = currentPos;
        this.currentDronePos = currentPos;
        this.targetPos = targetPos;
        this.diveMode = diveMode;
        this.looping = true;
        this.delay = 0;
        this.volume = 3.0f;
        this.pitch = 1.0f;

        this.attenuation = custom != null ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR;
        this.creationTime = System.currentTimeMillis();
        this.startTime = this.creationTime;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            double distToStart = mc.player.position().distanceTo(currentPos);
            if (distToStart > 100) {
                this.isLateJoin = true;
                int soundDelayTicks = (int) (distToStart / 17.0);
                this.startTime = this.creationTime - (soundDelayTicks * 50L);
            }
        }

        if (diveMode.equals("high")) {
            this.diveDistance = 500.0;
            this.diveTransitionDistance = 530.0;
            this.modeSwitchDistance = 600.0;
            this.maxDivingTicks = 200;
        } else {
            this.diveDistance = 150.0;
            this.diveTransitionDistance = 180.0;
            this.modeSwitchDistance = 220.0;
            this.maxDivingTicks = 100;
        }

        this.x = currentPos.x;
        this.y = currentPos.y;
        this.z = currentPos.z;

        Vec3 targetVec = Vec3.atCenterOf(targetPos);
        Vec3 dir = targetVec.subtract(currentPos);
        if (dir.lengthSqr() > 0.01) {
            this.lastDirection = dir.normalize();
        }

        this.lastServerPos = currentPos;
        this.inferredVelocity = this.lastDirection.scale(CRUISE_SPEED);
    }

    public void updateDronePosition(Vec3 serverPos) {

        if (this.lastServerPos != null) {
            Vec3 diff = serverPos.subtract(this.lastServerPos);
            double distMoved = diff.length();

            if (distMoved > 1.0 && distMoved < 200.0) {

                this.inferredVelocity = diff.scale(1.0 / 40.0);
            }
        }
        this.lastServerPos = serverPos;

        double dist = this.currentDronePos.distanceTo(serverPos);
        if (dist > 15.0) {
            this.currentDronePos = serverPos;
        } else {
            this.currentDronePos = this.currentDronePos.lerp(serverPos, 0.3);
        }

        this.ticksEntityNotFound = 0;

        this.x = this.currentDronePos.x;
        this.y = this.currentDronePos.y;
        this.z = this.currentDronePos.z;
    }

    public void start() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getSoundManager() != null) {
            mc.getSoundManager().play(this);
        }
    }

    public int getEntityId() {
        return this.entityId;
    }

    public boolean isExploded() {
        return this.exploded;
    }

    @Override
    public double getX() {
        return this.x;
    }

    @Override
    public double getY() {
        return this.y;
    }

    @Override
    public double getZ() {
        return this.z;
    }

    @Override
    public void tick() {
        if (this.stopped) {
            return;
        }
        this.ticksAlive++;
        if (this.ticksAlive > MAX_LIFETIME_TICKS) {
            this.forceStop();
            return;
        }
        if (this.shouldStop && !this.exploded) {
            this.volume = Math.max(0.0f, this.volume - 0.1f);
            if (this.volume <= 0.0f) {
                this.stopSoundCompletely();
            }
            return;
        }
        if (this.exploded) {
            this.handleExplosionSound();
            return;
        }
        this.updatePosition();
    }

    private void stopSoundCompletely() {
        this.stopped = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getSoundManager() != null) {
            mc.getSoundManager().stop(this);
        }
    }

    private void handleExplosionSound() {
        long currentTime = System.currentTimeMillis();
        long timeSinceExplosion = currentTime - this.explosionTime;

        long fadeTimeMs = 1500;

        if (timeSinceExplosion > fadeTimeMs) {
            this.forceStop();
            return;
        }

        float fadeProgress = (float) timeSinceExplosion / (float) fadeTimeMs;
        fadeProgress = Math.min(1.0f, Math.max(0.0f, fadeProgress));

        this.pitch = this.pitchAtExplosion * (1.0f - fadeProgress * 0.3f) * this.pitchScale;
        this.volume = this.volumeAtExplosion * (1.0f - fadeProgress) * 0.5f;

        if (this.volume < 0.01f) {
            this.forceStop();
        }
    }

    private void forceStop() {
        this.volume = 0.0f;
        this.stopped = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getSoundManager() != null) {
            mc.getSoundManager().stop(this);
        }
    }

    private void updatePosition() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            this.stopSound();
            return;
        }
        LocalPlayer player = mc.player;
        Vec3 targetVec = Vec3.atCenterOf(this.targetPos);

        if (mc.level != null) {
            net.minecraft.world.entity.Entity entity = mc.level.getEntity(this.entityId);
            if (entity instanceof com.synarsis.airtacticalarsenal.entity.ShahedEntity crashing
                    && !entity.isRemoved() && crashing.isCrashing()) {
                this.ticksEntityNotFound = 0;
                tickCrashing(crashing, player);
                return;
            }
            if (entity != null && !entity.isRemoved()) {
                this.ticksEntityNotFound = 0;
                this.entityRemovedLocally = false;
                Vec3 realPos = entity.position();
                this.currentDronePos = this.currentDronePos.lerp(realPos, 0.7);
                this.x = this.currentDronePos.x;
                this.y = this.currentDronePos.y;
                this.z = this.currentDronePos.z;
            } else {
                this.ticksEntityNotFound++;
                if (this.ticksEntityNotFound > MAX_ENTITY_NOT_FOUND_TICKS) {
                    this.forceStop();
                    return;
                }
            }
        }

        double horizontalDist = Math.sqrt(
                Math.pow(targetVec.x - this.currentDronePos.x, 2.0) +
                        Math.pow(targetVec.z - this.currentDronePos.z, 2.0));

        boolean entityActuallyDiving = false;
        if (mc.level != null) {
            net.minecraft.world.entity.Entity entity = mc.level.getEntity(this.entityId);
            if (entity instanceof com.synarsis.airtacticalarsenal.entity.ShahedEntity shahed) {
                entityActuallyDiving = shahed.getIsDiving();
            }
        }

        float targetProgress = 0.0f;
        if (entityActuallyDiving && horizontalDist < this.diveTransitionDistance) {
            targetProgress = horizontalDist < this.diveDistance ? 1.0f
                    : 1.0f - (float) ((horizontalDist - this.diveDistance)
                            / (this.diveTransitionDistance - this.diveDistance));
        }
        this.localDiveProgress += (targetProgress - this.localDiveProgress) * 0.15f;
        this.localDiveProgress = Math.max(0.0f, Math.min(1.0f, this.localDiveProgress));

        boolean hasEntity = (this.ticksEntityNotFound == 0 && !this.entityRemovedLocally);

        if (hasEntity) {
            Vec3 toTarget = targetVec.subtract(this.currentDronePos);
            Vec3 moveDirection;
            if (toTarget.lengthSqr() < 4.0) {
                moveDirection = this.lastDirection;
            } else if (this.localDiveProgress < 0.01f) {
                Vec3 horizontalTarget = new Vec3(targetVec.x, this.currentDronePos.y, targetVec.z);
                Vec3 diff = horizontalTarget.subtract(this.currentDronePos);
                moveDirection = diff.lengthSqr() > 0.01 ? diff.normalize() : toTarget.normalize();
            } else {
                Vec3 horizontalTarget = new Vec3(targetVec.x, this.currentDronePos.y, targetVec.z);
                Vec3 horizontalDiff = horizontalTarget.subtract(this.currentDronePos);
                Vec3 horizontalDir = horizontalDiff.lengthSqr() > 0.01 ? horizontalDiff.normalize()
                        : this.lastDirection;
                Vec3 directDir = toTarget.lengthSqr() > 0.01 ? toTarget.normalize() : this.lastDirection;
                moveDirection = new Vec3(
                        horizontalDir.x + (directDir.x - horizontalDir.x) * this.localDiveProgress,
                        horizontalDir.y + (directDir.y - horizontalDir.y) * this.localDiveProgress,
                        horizontalDir.z + (directDir.z - horizontalDir.z) * this.localDiveProgress);
                if (moveDirection.lengthSqr() > 0.01) {
                    moveDirection = moveDirection.normalize();
                } else {
                    moveDirection = this.lastDirection;
                }
            }
            if (moveDirection.lengthSqr() > 0.01) {
                this.lastDirection = moveDirection;
            }

            double speed = CRUISE_SPEED + 2.1 * this.localDiveProgress;
            this.currentDronePos = this.currentDronePos.add(moveDirection.scale(speed));

            this.inferredVelocity = moveDirection.scale(speed);

        } else {

            if (this.inferredVelocity != null) {
                this.currentDronePos = this.currentDronePos.add(this.inferredVelocity);
            }
        }

        this.x = this.currentDronePos.x;
        this.y = this.currentDronePos.y;
        this.z = this.currentDronePos.z;

        Vec3 playerPos = player.position();
        double dx = this.currentDronePos.x - playerPos.x;
        double dy = (this.currentDronePos.y - playerPos.y) * 0.3;
        double dz = this.currentDronePos.z - playerPos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        float diveProgress = this.localDiveProgress;

        if (entityActuallyDiving && !this.modeSwitchComplete && !this.inModeSwitchPhase) {
            this.inModeSwitchPhase = true;
            this.modeSwitchTicks = 0;
        }

        if (this.inModeSwitchPhase) {
            this.modeSwitchTicks++;
            float switchProgress = (float) this.modeSwitchTicks / (float) MODE_SWITCH_DURATION_TICKS;

            if (switchProgress < 0.5f) {
                float slowdownProgress = switchProgress * 2.0f;
                this.targetPitch = 1.0f - (slowdownProgress * 0.35f);
                this.targetVolumeMultiplier = 1.0f - (slowdownProgress * 0.25f);
            } else {
                float recoveryProgress = (switchProgress - 0.5f) * 2.0f;
                this.targetPitch = 0.65f + (recoveryProgress * 0.35f);
                this.targetVolumeMultiplier = 0.75f + (recoveryProgress * 0.25f);
            }

            if (this.modeSwitchTicks >= MODE_SWITCH_DURATION_TICKS) {
                this.inModeSwitchPhase = false;
                this.modeSwitchComplete = true;
            }
        } else if (this.modeSwitchComplete && diveProgress > 0.01f) {
            this.divingTicks = Math.min(this.divingTicks + 2, this.maxDivingTicks);
            float timeProgress = (float) this.divingTicks / (float) this.maxDivingTicks;
            this.targetPitch = 1.0f + timeProgress * 0.8f;
            this.targetVolumeMultiplier = 1.0f + timeProgress * 0.83f;
        } else if (!this.inModeSwitchPhase && !this.modeSwitchComplete) {
            this.divingTicks = Math.max(this.divingTicks - 1, 0);
            this.targetPitch = 1.0f;
            this.targetVolumeMultiplier = 1.0f;
        }

        float pitchChangeRate = this.inModeSwitchPhase ? 0.012f : 0.006f;
        if (this.currentPitch < this.targetPitch) {
            this.currentPitch = Math.min(this.currentPitch + pitchChangeRate, this.targetPitch);
        } else if (this.currentPitch > this.targetPitch) {
            this.currentPitch = Math.max(this.currentPitch - pitchChangeRate, this.targetPitch);
        }

        float volumeChangeRate = this.inModeSwitchPhase ? 0.02f : 0.015f;
        if (this.currentVolumeMultiplier < this.targetVolumeMultiplier) {
            this.currentVolumeMultiplier = Math.min(this.currentVolumeMultiplier + volumeChangeRate,
                    this.targetVolumeMultiplier);
        } else if (this.currentVolumeMultiplier > this.targetVolumeMultiplier) {
            this.currentVolumeMultiplier = Math.max(this.currentVolumeMultiplier - volumeChangeRate,
                    this.targetVolumeMultiplier);
        }

        this.pitch = this.currentPitch * this.pitchScale;
        float baseVolume = this.calculateVolume(distance);
        this.volume = baseVolume * this.currentVolumeMultiplier;

        if (this.currentDronePos.distanceTo(targetVec) < 2.0) {
            this.markExploded(this.currentDronePos);
        }
    }

    private int crashTicks = 0;

    private void tickCrashing(com.synarsis.airtacticalarsenal.entity.ShahedEntity shahed, LocalPlayer player) {
        this.crashTicks++;
        this.currentDronePos = shahed.position();
        this.x = this.currentDronePos.x;
        this.y = this.currentDronePos.y;
        this.z = this.currentDronePos.z;
        double distance = player.position().distanceTo(this.currentDronePos);
        float t = this.crashTicks;
        float factor;
        float pitch;
        switch (shahed.getCrashMode()) {
            case ENGINE_OUT -> {
                factor = Math.max(0.0f, 1.0f - t / 60.0f) * (0.85f + 0.15f * (float) Math.sin(t * 0.45f));
                pitch = Math.max(0.7f, this.currentPitch - t * 0.004f);
            }
            case SPIN -> {
                factor = Math.max(0.3f, 1.0f - t / 80.0f);
                pitch = this.currentPitch + Math.min(0.08f, t * 0.002f) + 0.03f * (float) Math.sin(t * 0.3f);
            }
            default -> {
                factor = Math.max(0.0f, 1.0f - t / 20.0f);
                pitch = Math.max(0.5f, this.currentPitch - t * 0.02f);
            }
        }
        this.crashVolume = factor;
        this.crashPitch = this.currentPitch > 0.01f ? pitch / this.currentPitch : 1.0f;
        this.pitch = pitch * this.pitchScale;
        this.volume = calculateVolume(distance) * factor;
    }

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

    private float calculateVolume(double distance) {
        if (this.custom != null) {
            return this.custom.gainAt(distance);
        }
        return baseVolume(distance) * this.volumeScale;
    }

    private float baseVolume(double distance) {
        if (distance >= 500.0) {
            return 0.0f;
        }
        if (distance < 30.0) {
            return 16.0f;
        }
        if (distance < 70.0) {
            float t = (float) ((distance - 30.0) / 40.0);
            return 16.0f - t * 10.0f;
        }
        if (distance < 150.0) {
            float t = (float) ((distance - 70.0) / 80.0);
            return 6.0f - t * 3.6f;
        }
        if (distance < 250.0) {
            float t = (float) ((distance - 150.0) / 100.0);
            return 2.4f - t * 1.4f;
        }
        if (distance < 350.0) {
            float t = (float) ((distance - 250.0) / 100.0);
            return 1.0f - t * 0.6f;
        }
        if (distance < 400.0) {
            float t = (float) ((distance - 350.0) / 50.0);
            return 0.4f - t * 0.24f;
        }
        float t = (float) ((distance - 400.0) / 100.0);
        return 0.16f * (1.0f - t);
    }

    public void markExploded(Vec3 position) {
        if (this.exploded || this.stopped) {
            return;
        }

        this.pitchAtExplosion = this.currentPitch;
        this.volumeAtExplosion = Math.min(this.volume, 3.0f);
        this.exploded = true;
        this.explosionPos = position;
        this.explosionTime = System.currentTimeMillis();

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            double dist = mc.player.position().distanceTo(position);
            if (dist < 50.0) {
                this.forceStop();
            }
        }
    }

    public void stopSound() {
        if (!this.exploded) {
            this.shouldStop = true;
        }
    }

    public void discard() {
        this.forceStop();
    }

    public void markEntityRemovedLocally() {
        this.entityRemovedLocally = true;
    }

    @Override
    public boolean isStopped() {
        return this.stopped;
    }
}
