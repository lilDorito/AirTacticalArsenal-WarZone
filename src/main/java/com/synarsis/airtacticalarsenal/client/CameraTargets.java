package com.synarsis.airtacticalarsenal.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CameraTargets {
    public static final double RANGE = 400.0;
    private static final int MAX_TARGETS = 64;

    private static double fov = 70.0;
    private static List<Entity> visible = Collections.emptyList();
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
    private static final Matrix4f INVERSE = new Matrix4f();
    private static Vec3 cameraPos = Vec3.ZERO;
    private static boolean captured;

    private CameraTargets() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (OrlanCameraHandler.isCameraActive() && event.usedConfiguredFov()) {
            fov = event.getFOV();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        if (!OrlanCameraHandler.isCameraActive()) {
            captured = false;
            return;
        }
        VIEW.set(event.getPoseStack().last().pose());
        PROJECTION.set(event.getProjectionMatrix());
        PROJECTION.mul(VIEW, VIEW_PROJECTION);
        VIEW_PROJECTION.invert(INVERSE);
        cameraPos = event.getCamera().getPosition();
        captured = true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Entity drone = OrlanCameraHandler.isCameraActive() ? OrlanCameraHandler.getLinkedDrone() : null;
        if (drone == null) {
            if (!visible.isEmpty()) {
                visible = Collections.emptyList();
            }
            return;
        }
        visible = scan(drone);
    }

    public static List<Entity> visibleTargets() {
        return visible;
    }

    public static double focal(int height) {
        if (captured && PROJECTION.m11() > 1.0E-4F) {
            return height * 0.5 * PROJECTION.m11();
        }
        return height / (2.0 * Math.tan(Math.toRadians(fov) / 2.0));
    }

    public static Vec3 origin() {
        if (captured) {
            return cameraPos;
        }
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    public static double[] project(Vec3 world, int width, int height) {
        if (captured) {
            Vector4f clip = new Vector4f((float) (world.x - cameraPos.x), (float) (world.y - cameraPos.y),
                    (float) (world.z - cameraPos.z), 1.0F);
            VIEW_PROJECTION.transform(clip);
            if (clip.w() <= 0.05F) {
                return null;
            }
            double nx = clip.x() / clip.w();
            double ny = clip.y() / clip.w();
            return new double[]{(nx * 0.5 + 0.5) * width, (0.5 - ny * 0.5) * height};
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 rel = world.subtract(camera.getPosition());
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();
        double z = rel.x * look.x() + rel.y * look.y() + rel.z * look.z();
        if (z <= 0.05) {
            return null;
        }
        double x = rel.x * left.x() + rel.y * left.y() + rel.z * left.z();
        double y = rel.x * up.x() + rel.y * up.y() + rel.z * up.z();
        double f = focal(height);
        return new double[]{width * 0.5 - x * f / z, height * 0.5 - y * f / z};
    }

    public static Vec3 ray(double sx, double sy, int width, int height) {
        if (captured) {
            float nx = (float) (sx / width * 2.0 - 1.0);
            float ny = (float) (1.0 - sy / height * 2.0);
            Vector4f far = new Vector4f(nx, ny, 0.5F, 1.0F);
            INVERSE.transform(far);
            if (Math.abs(far.w()) > 1.0E-9F) {
                Vec3 direction = new Vec3(far.x() / far.w(), far.y() / far.w(), far.z() / far.w());
                if (direction.lengthSqr() > 1.0E-9) {
                    return direction.normalize();
                }
            }
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();
        double f = focal(height);
        double a = -(sx - width * 0.5) / f;
        double b = -(sy - height * 0.5) / f;
        return new Vec3(look.x() + left.x() * a + up.x() * b,
                look.y() + left.y() * a + up.y() * b,
                look.z() + left.z() * a + up.z() * b).normalize();
    }

    public static Vec3 lerpedCenter(Entity entity, float partialTick) {
        double x = Mth.lerp(partialTick, entity.xo, entity.getX());
        double y = Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() * 0.5;
        double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
        return new Vec3(x, y, z);
    }

    public static boolean targetable(Entity entity, Entity drone) {
        Minecraft mc = Minecraft.getInstance();
        return entity != drone
                && entity != mc.player
                && !entity.isRemoved()
                && entity.isAlive()
                && !entity.isSpectator()
                && entity.isPickable()
                && !(entity instanceof Projectile);
    }

    public static boolean friendly(Entity entity) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        if (entity == player || entity.isAlliedTo(player)) {
            return true;
        }
        for (Entity passenger : entity.getPassengers()) {
            if (passenger == player || passenger.isAlliedTo(player)) {
                return true;
            }
        }
        return false;
    }

    public static Vec3 clipBlock(Vec3 start, Vec3 direction, double length, Entity drone) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        Vec3 end = start.add(direction.scale(length));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, drone));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : null;
    }

    private static List<Entity> scan(Entity drone) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return Collections.emptyList();
        }
        Vec3 eye = drone.getEyePosition();
        List<Entity> found = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (Entity entry : mc.level.entitiesForRendering()) {
            Entity entity = entry.getRootVehicle();
            if (!seen.add(entity.getUUID()) || !targetable(entity, drone)) {
                continue;
            }
            if (entity.distanceToSqr(drone) > RANGE * RANGE) {
                continue;
            }
            found.add(entity);
        }
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(drone)));
        List<Entity> result = new ArrayList<>();
        for (Entity entity : found) {
            if (result.size() >= MAX_TARGETS) {
                break;
            }
            Vec3 center = entity.getBoundingBox().getCenter();
            BlockHitResult hit = mc.level.clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, drone));
            if (hit.getType() == HitResult.Type.MISS || entity.getBoundingBox().inflate(0.1).contains(hit.getLocation())) {
                result.add(entity);
            }
        }
        return result;
    }
}
