package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.api.DroneVariantsClient;
import com.synarsis.airtacticalarsenal.entity.OrlanEntity;
import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import com.synarsis.airtacticalarsenal.entity.UavLauncherEntity;
import com.synarsis.airtacticalarsenal.api.DroneVariants;
import com.synarsis.airtacticalarsenal.entity.crash.CrashDynamics;
import com.synarsis.airtacticalarsenal.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class JetExhaustClient {
    private static final double MAX_DISTANCE_SQR = 320.0 * 320.0;
    private static Boolean ashFlame;

    private JetExhaustClient() {
    }

    private static boolean ashFlame() {
        if (ashFlame == null) {
            ashFlame = ModList.get().isLoaded("ashvehicle");
        }
        return ashFlame;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !DroneVariantsClient.hasJetExhausts()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        if (LAUNCH_START.size() > 64) {
            LAUNCH_START.clear();
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof UavLauncherEntity launcher) {
                tickLauncher(mc, launcher, camera);
                continue;
            }
            if (!(entity instanceof ShahedEntity) && !(entity instanceof OrlanEntity)) {
                continue;
            }
            if (OrlanCameraHandler.isCameraActive() && OrlanCameraHandler.getLinkedOrlanId() == entity.getId()) {
                continue;
            }
            if (entity.distanceToSqr(camera) > MAX_DISTANCE_SQR || !engineRunning(entity)) {
                continue;
            }
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (key == null) {
                continue;
            }
            List<DroneVariantsClient.JetExhaust> exhausts = DroneVariantsClient.jetExhausts(key.toString());
            for (DroneVariantsClient.JetExhaust exhaust : exhausts) {
                emit(mc, entity, exhaust);
            }
        }
    }

    private static boolean engineRunning(Entity entity) {
        if (entity instanceof ShahedEntity strike) {
            if (strike.isOnPaintStand() || strike.getFlightPhase() == ShahedEntity.FlightPhase.WARMUP) {
                return false;
            }
            return !strike.isCrashing() || strike.getCrashMode() == CrashDynamics.Mode.SPIN;
        }
        OrlanEntity recon = (OrlanEntity) entity;
        if (recon.getFlightPhase() == OrlanEntity.FlightPhase.LANDED) {
            return false;
        }
        return !recon.isCrashing() || recon.getCrashMode() == CrashDynamics.Mode.SPIN;
    }

    private static final java.util.Map<Integer, Long> LAUNCH_START = new java.util.HashMap<>();
    private static final float LAUNCH_RAMP_TICKS = 100.0f;

    private static void tickLauncher(Minecraft mc, UavLauncherEntity launcher, Vec3 camera) {
        if (!launcher.isLaunching() || !launcher.hasDrone()) {
            LAUNCH_START.remove(launcher.getId());
            return;
        }
        if (launcher.distanceToSqr(camera) > MAX_DISTANCE_SQR) {
            return;
        }
        List<DroneVariantsClient.JetExhaust> exhausts =
                DroneVariantsClient.jetExhausts(DroneVariants.entityTypeKey(launcher.getDroneKind()));
        if (exhausts.isEmpty()) {
            return;
        }
        long now = launcher.level().getGameTime();
        long start = LAUNCH_START.computeIfAbsent(launcher.getId(), id -> now);
        float progress = Math.min(1.0f, (now - start) / LAUNCH_RAMP_TICKS);
        float scale = 0.15f + 0.85f * progress * progress;
        Vec3 origin = launcher.railLaunchPos();
        float yaw = -launcher.getYRot();
        float pitch = launcher.railPitchDeg();
        for (DroneVariantsClient.JetExhaust exhaust : exhausts) {
            emit(mc, origin, yaw, pitch, Vec3.ZERO, exhaust, scale, launcher.level().random);
        }
    }

    private static void emit(Minecraft mc, Entity entity, DroneVariantsClient.JetExhaust exhaust) {
        emit(mc, entity.position(), entity.getYRot(), entity.getXRot(), entity.getDeltaMovement(), exhaust, 1.0f,
                entity.level().random);
    }

    private static void emit(Minecraft mc, Vec3 position, float yawDeg, float pitchDeg, Vec3 motion,
                             DroneVariantsClient.JetExhaust exhaust, float scale, RandomSource random) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        Vec3 forward = new Vec3(Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = new Vec3(Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3 up = right.cross(forward).scale(-1.0);
        Vec3 n = exhaust.nozzle();
        Vec3 origin = position.add(right.scale(n.x)).add(up.scale(n.y)).add(forward.scale(n.z));
        Vec3 back = forward.scale(-0.35 * scale);
        boolean ash = ashFlame();
        ParticleOptions flame = ash
                ? (exhaust.large() ? ModParticles.JET_FLAME_LARGE.get() : ModParticles.JET_FLAME.get())
                : ParticleTypes.FLAME;
        int count = Math.max(1, Math.round((exhaust.large() ? 4 : 3) * scale));
        for (int i = 0; i < count; i++) {
            double along = i / (double) count;
            Vec3 at = origin.subtract(motion.scale(along));
            double jitter = exhaust.large() ? 0.12 : 0.07;
            com.synarsis.airtacticalarsenal.particle.JetFlameParticle.nextScale = scale;
            mc.level.addParticle(flame,
                    at.x + (random.nextDouble() - 0.5) * jitter,
                    at.y + (random.nextDouble() - 0.5) * jitter,
                    at.z + (random.nextDouble() - 0.5) * jitter,
                    motion.x + back.x, motion.y + back.y, motion.z + back.z);
            com.synarsis.airtacticalarsenal.particle.JetFlameParticle.nextScale = 1.0f;
        }
        if (!ash && random.nextFloat() < 0.5f * scale) {
            mc.level.addParticle(ParticleTypes.SMALL_FLAME, origin.x, origin.y, origin.z,
                    motion.x + back.x * 1.5, motion.y + back.y * 1.5, motion.z + back.z * 1.5);
        }
    }
}
