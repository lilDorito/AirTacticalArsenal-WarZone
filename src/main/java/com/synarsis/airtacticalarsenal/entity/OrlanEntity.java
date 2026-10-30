package com.synarsis.airtacticalarsenal.entity;

import com.synarsis.airtacticalarsenal.chunk.MissileChunkManager;
import com.synarsis.airtacticalarsenal.compat.CreateCompat;
import com.synarsis.airtacticalarsenal.config.ShahedConfig;
import com.synarsis.airtacticalarsenal.entity.crash.CrashDynamics;
import com.synarsis.airtacticalarsenal.entity.crash.CrashEffects;
import com.synarsis.airtacticalarsenal.network.NetworkHandler;
import com.synarsis.airtacticalarsenal.network.OrlanSpawnPacket;
import com.synarsis.airtacticalarsenal.network.OrlanCameraViewPacket;
import com.synarsis.airtacticalarsenal.network.OrlanCameraForceExitPacket;
import com.synarsis.airtacticalarsenal.network.OrlanRemovePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OrlanEntity extends Projectile implements GeoEntity {

    private static final Map<Integer, OrlanEntity> activeOrlanRegistry = new java.util.concurrent.ConcurrentHashMap<>();

    public static List<OrlanEntity> getActiveOrlansByOwner(UUID ownerUUID) {
        List<OrlanEntity> result = new ArrayList<>();
        String ownerStr = ownerUUID.toString();
        for (OrlanEntity orlan : activeOrlanRegistry.values()) {
            if (orlan.isAlive() && ownerStr.equals(orlan.getOwnerUUIDString())) {
                result.add(orlan);
            }
        }
        return result;
    }

    public static List<OrlanEntity> getActiveOrlansWithCamera() {
        List<OrlanEntity> result = new ArrayList<>();
        for (OrlanEntity orlan : activeOrlanRegistry.values()) {
            if (orlan.isAlive() && !orlan.getLinkedPlayerUUID().isEmpty()) {
                result.add(orlan);
            }
        }
        return result;
    }

    public static void clearRegistry() {
        activeOrlanRegistry.clear();
        PARKED.clear();
    }

    public record LandedDrone(UUID uuid, String owner, String nameKey, String kind,
                              net.minecraft.resources.ResourceKey<Level> dimension, int x, int z) {
    }

    private static final Map<UUID, LandedDrone> PARKED = new java.util.concurrent.ConcurrentHashMap<>();

    public static List<LandedDrone> parkedByOwner(Level level, UUID owner) {
        List<LandedDrone> result = new ArrayList<>();
        String ownerStr = owner.toString();
        for (LandedDrone drone : PARKED.values()) {
            if (drone.dimension().equals(level.dimension()) && ownerStr.equals(drone.owner())) {
                result.add(drone);
            }
        }
        return result;
    }

    public enum FlightPhase {
        LAUNCHING, 
        CLIMBING, 
        CRUISING, 
        PATROLLING, 
        RETURNING, 
        LANDING, 
        LANDED 
    }

    private static final double LAUNCH_INITIAL_SPEED = 0.9;
    private static final double LAUNCH_ACCELERATION = 0.02;
    private static final double LAUNCH_MAX_SPEED = 1.2;
    private static final double LAUNCH_ANGLE_DEG = 20.0;
    private static final double RAIL_ANGLE_DEG = 11.0;
    private static final double PATH_ANGLE_RATE_DEG = 0.3;

    private static final double CLIMB_ACCELERATION = 0.03;
    private static final double CLIMB_ANGLE_DEG = 25.0;
    private static final double CLIMB_DISTANCE = 300.0;

    private static final double CRUISE_SPEED = 1.2;
    private static final double HEIGHT_CORRECTION_FACTOR = 0.1;

    private static final double PATROL_ALTITUDE = 200.0;
    private static final double PATROL_RADIUS = 50.0;
    private static final double PATROL_ENTRY_DISTANCE = 60.0;
    private static final double PATROL_SPEED = 0.8;

    private static final double TURN_RATE = 2.0;

    private static final float DEFAULT_MAX_HEALTH = 80.0f;

    private static final double RETURN_SPEED = 1.0;

    private static final EntityDataAccessor<BlockPos> TARGET_POS = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> FLIGHT_PHASE_ID = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SYNC_X_ROT = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNC_Y_ROT = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CRASH_MODE = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SYNC_ROLL = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.INT);

    public int getSkin() {
        return this.entityData.get(SKIN);
    }

    public void setSkin(int skin) {
        this.entityData.set(SKIN, Math.max(0, skin));
    }

    private final CrashDynamics crash = new CrashDynamics();
    private final com.synarsis.airtacticalarsenal.entity.crash.Durability durability =
            new com.synarsis.airtacticalarsenal.entity.crash.Durability();
    private boolean discardOnLoad = false;
    private int clientCrashTicks = 0;
    private float roll;
    private float rollO;
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_DAMAGED = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<String> LINKED_PLAYER = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> OWNER_UUID_DATA = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Integer> FLIGHT_TIMER = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.INT);

    private static final EntityDataAccessor<BlockPos> LAUNCHER_POS_DATA = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.BLOCK_POS);

    private static final EntityDataAccessor<Float> TARGET_ALTITUDE_DATA = SynchedEntityData.defineId(OrlanEntity.class,
            EntityDataSerializers.FLOAT);

    private FlightPhase flightPhase = FlightPhase.LAUNCHING;
    private int phaseTicks = 0;
    private double currentSpeed = 0;
    private double launchStartY = 0;
    private Vec3 launchStartPos = Vec3.ZERO;
    private Vec3 launchDirection = Vec3.ZERO;
    private double targetAltitude = this.patrolAltitude();
    private Vec3 lastDirection = new Vec3(0, 0, 1);

    private double patrolAngle = 0;
    private boolean patrolInitialized = false;

    private List<BlockPos> waypoints = null;
    private int currentWaypointIndex = 0;

    private BlockPos launcherPos = BlockPos.ZERO;

    private Vec3 landingTarget = null;

    private int syncTickCounter = 0;
    private static final int SYNC_INTERVAL = 20;
    private boolean initialPacketSent = false;
    private boolean destroyed = false;

    private int localFlightTimer = flightTicksMax();

    private float maxHealth = this.maxHealthValue();

    private double lerpX, lerpY, lerpZ;
    private float lerpYRot, lerpXRot;
    private int lerpSteps = 0;

    private double savedPlayerX, savedPlayerY, savedPlayerZ;
    private float savedPlayerYRot, savedPlayerXRot;

    private Vec3 cameraPhysicsPos = null;
    private float cameraPlayerFallDistance = 0.0f;

    private ChunkPos lastSentChunkPos = null;

    private final CameraEntityStream trackedCameraEntities = new CameraEntityStream();
    private final EntityDataMirror cameraSelfData = new EntityDataMirror();

    private boolean rotationSynced = true;
    private double pathAngleDeg = RAIL_ANGLE_DEG;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OrlanEntity(EntityType<? extends OrlanEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public boolean isLegitSpawn = false;

    public double plannedCruiseSpeed() {
        return cruiseSpeed();
    }

    protected double cruiseSpeed() {
        return CRUISE_SPEED;
    }

    protected double patrolSpeed() {
        return PATROL_SPEED;
    }

    protected double patrolAltitude() {
        return PATROL_ALTITUDE;
    }

    protected double returnSpeed() {
        return RETURN_SPEED;
    }

    protected double turnRate() {
        return TURN_RATE;
    }

    public float maxHealthValue() {
        return DEFAULT_MAX_HEALTH;
    }

    protected int flightTicksMax() {
        return getMaxFlightTicks();
    }

    public ItemStack createDroneItem() {
        return new ItemStack(com.synarsis.airtacticalarsenal.item.ModItems.ORLAN.get());
    }

    public String getDroneNameKey() {
        return "drone.ata.orlan";
    }

    public Component getDroneName() {
        return Component.translatable(this.getDroneNameKey());
    }

    public OrlanEntity(Level level, Vec3 startPos, BlockPos targetPos, float launcherYaw, BlockPos launcherPos) {
        this(ModEntities.ORLAN.get(), level, startPos, targetPos, launcherYaw, launcherPos);
    }

    protected OrlanEntity(EntityType<? extends OrlanEntity> type, Level level, Vec3 startPos, BlockPos targetPos, float launcherYaw, BlockPos launcherPos) {
        this(type, level);
        this.setPos(startPos);
        this.launcherPos = launcherPos != null ? launcherPos
                : new BlockPos((int) startPos.x, (int) startPos.y, (int) startPos.z);
        this.entityData.set(LAUNCHER_POS_DATA, this.launcherPos);
        this.entityData.set(TARGET_POS, targetPos);

        this.flightPhase = FlightPhase.LAUNCHING;
        this.phaseTicks = 0;
        this.currentSpeed = LAUNCH_INITIAL_SPEED;
        this.launchStartY = startPos.y;
        this.launchStartPos = startPos;
        this.targetAltitude = this.patrolAltitude();

        if (!Float.isNaN(launcherYaw)) {
            Vec3 dir = Vec3.directionFromRotation(0, launcherYaw).normalize();
            this.launchDirection = new Vec3(dir.x, 0, dir.z).normalize();
            this.lastDirection = this.launchDirection;
        } else {
            Vec3 targetVec = Vec3.atCenterOf(targetPos);
            Vec3 horizontalDir = new Vec3(targetVec.x - startPos.x, 0, targetVec.z - startPos.z);
            if (horizontalDir.lengthSqr() > 1.0E-4) {
                this.launchDirection = horizontalDir.normalize();
                this.lastDirection = horizontalDir.normalize();
            } else {
                this.launchDirection = new Vec3(0, 0, 1);
                this.lastDirection = new Vec3(0, 0, 1);
            }
        }

        this.setYRot((float) Math.toDegrees(Math.atan2(this.launchDirection.x, this.launchDirection.z)));
        this.setXRot((float) RAIL_ANGLE_DEG);
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.entityData.set(FLIGHT_PHASE_ID, this.flightPhase.ordinal());
        this.entityData.set(SYNC_X_ROT, (float) RAIL_ANGLE_DEG);
        this.entityData.set(SYNC_Y_ROT, this.getYRot());
    }

    public void setRailPitch(double degrees) {
        this.pathAngleDeg = degrees;
        this.setXRot((float) degrees);
        this.xRotO = (float) degrees;
        this.entityData.set(SYNC_X_ROT, (float) degrees);
    }

    private static double approachAngle(double current, double target) {
        double diff = target - current;
        if (Math.abs(diff) <= PATH_ANGLE_RATE_DEG) {
            return target;
        }
        return current + Math.signum(diff) * PATH_ANGLE_RATE_DEG;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == SYNC_Y_ROT || key == SYNC_X_ROT) {
            this.rotationSynced = true;
        }
    }

    @Override
    public void recreateFromPacket(net.minecraft.network.protocol.game.ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.rotationSynced = false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 490000.0;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = steps + 1;
    }

    public void setServerPosition(Vec3 pos) {
        if (this.level().isClientSide) {
            this.lerpX = pos.x;
            this.lerpY = pos.y;
            this.lerpZ = pos.z;
            this.lerpSteps = 3;
        }
    }

    private static float lerpRotation(float current, float target, float factor) {
        float diff = target - current;
        while (diff < -180.0F)
            diff += 360.0F;
        while (diff >= 180.0F)
            diff -= 360.0F;
        return current + diff * factor;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET_POS, BlockPos.ZERO);
        this.entityData.define(FLIGHT_PHASE_ID, 0);
        this.entityData.define(SYNC_X_ROT, 0.0f);
        this.entityData.define(SYNC_Y_ROT, 0.0f);
        this.entityData.define(CRASH_MODE, 0);
        this.entityData.define(SYNC_ROLL, 0.0f);
        this.entityData.define(HEALTH, this.maxHealthValue());
        this.entityData.define(IS_DAMAGED, false);
        this.entityData.define(LINKED_PLAYER, "");
        this.entityData.define(OWNER_UUID_DATA, "");
        this.entityData.define(FLIGHT_TIMER, flightTicksMax());
        this.entityData.define(LAUNCHER_POS_DATA, BlockPos.ZERO);
        this.entityData.define(TARGET_ALTITUDE_DATA, (float) this.patrolAltitude());
        this.entityData.define(SKIN, 0);
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && !this.isLegitSpawn) {
            this.removeSelf();
            return;
        }
        if (!this.level().isClientSide && this.tickCount % 3 == 0) {
            com.synarsis.airtacticalarsenal.compat.SuperbWarfareCompat.registerForSbwRadar(this);
        }

        super.tick();

        BlockPos target = this.entityData.get(TARGET_POS);
        Vec3 targetVec = Vec3.atCenterOf(target);
        Vec3 currentPos = this.position();
        double horizontalDist = Math.sqrt(
                Math.pow(targetVec.x - currentPos.x, 2.0) +
                        Math.pow(targetVec.z - currentPos.z, 2.0));

        int syncedPhaseId = this.entityData.get(FLIGHT_PHASE_ID);
        if (this.level().isClientSide) {
            if (syncedPhaseId >= 0 && syncedPhaseId < FlightPhase.values().length) {
                this.flightPhase = FlightPhase.values()[syncedPhaseId];
            }
            this.launcherPos = this.entityData.get(LAUNCHER_POS_DATA);

            if (this.lerpSteps > 0) {
                double dx = (this.lerpX - this.getX()) / this.lerpSteps;
                double dy = (this.lerpY - this.getY()) / this.lerpSteps;
                double dz = (this.lerpZ - this.getZ()) / this.lerpSteps;
                this.setPos(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
                this.lerpSteps--;
            } else {

                Vec3 vel = this.getDeltaMovement();
                if (vel.lengthSqr() > 1.0E-6) {
                    this.setPos(this.getX() + vel.x, this.getY() + vel.y, this.getZ() + vel.z);
                }
            }

            if (isCrashing()) {
                tickCrashClient();
                return;
            }

            float syncedXRot = this.rotationSynced ? this.entityData.get(SYNC_X_ROT) : this.getXRot();
            float syncedYRot = this.rotationSynced ? this.entityData.get(SYNC_Y_ROT) : this.getYRot();
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
            this.setYRot(lerpRotation(this.getYRot(), syncedYRot, 0.5f));
            this.setXRot(lerpRotation(this.getXRot(), syncedXRot, 0.5f));
            this.setYBodyRot(this.getYRot());
            this.setYHeadRot(this.getYRot());
            return;
        }

        if (this.discardOnLoad) {
            this.destroyed = true;
            this.removeSelf();
            return;
        }

        if (isCrashing()) {
            tickCrashServer();
            if (!this.isRemoved()) tickLinkedCamera();
            return;
        }

        net.minecraft.world.entity.projectile.Projectile incoming =
                com.synarsis.airtacticalarsenal.compat.MissileProximityFuse.find(this);
        if (incoming != null) {
            Entity shooter = incoming.getOwner();
            com.synarsis.airtacticalarsenal.compat.MissileProximityFuse.detonate(incoming);
            com.synarsis.airtacticalarsenal.compat.MissileProximityFuse.airBurst(this);
            if (!this.destroyed && !this.isRemoved()) {
                this.hurt(this.damageSources().explosion(incoming, shooter), 1000.0f);
            }
            if (this.destroyed || this.isRemoved()) return;
        }

        Vec3 motion;

        switch (this.flightPhase) {
            case LAUNCHING:
                motion = tickLaunchingPhase(targetVec, currentPos);
                break;
            case CLIMBING:
                motion = tickClimbingPhase(targetVec, currentPos);
                break;
            case CRUISING:
                motion = tickCruisingPhase(targetVec, currentPos, horizontalDist);
                break;
            case PATROLLING:
                motion = tickPatrollingPhase(targetVec, currentPos);
                break;
            case RETURNING:
                motion = tickReturningPhase(currentPos);
                break;
            case LANDING:
                motion = tickLandingPhase(currentPos);
                break;
            case LANDED:
                motion = Vec3.ZERO;
                break;
            default:
                motion = this.lastDirection.scale(this.cruiseSpeed());
        }

        this.phaseTicks++;
        this.setDeltaMovement(motion);

        if (motion.lengthSqr() > 1.0E-4) {
            this.lastDirection = motion.normalize();
        }

        if (motion.lengthSqr() > 1.0E-4) {
            float newYRot = (float) (Math.atan2(motion.x, motion.z) * 57.29577951308232);
            float newXRot;
            if (this.flightPhase == FlightPhase.CRUISING || this.flightPhase == FlightPhase.PATROLLING
                    || this.flightPhase == FlightPhase.RETURNING) {
                newXRot = 0;
            } else {
                newXRot = (float) (Math.atan2(motion.y,
                        Math.sqrt(motion.x * motion.x + motion.z * motion.z)) * 57.29577951308232);
            }
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
            this.setYRot(newYRot);
            this.setXRot(newXRot);
            this.setYBodyRot(newYRot);
            this.setYHeadRot(newYRot);
            this.entityData.set(SYNC_X_ROT, newXRot);
            this.entityData.set(SYNC_Y_ROT, newYRot);
        }

        spawnDamageEffects();

        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.flightPhase == FlightPhase.LANDED) {

                if (MissileChunkManager.isMissileRegistered(this)) {
                    MissileChunkManager.unregisterMissile(this);
                }
            } else if (this.flightPhase == FlightPhase.PATROLLING) {

                MissileChunkManager.updateChunksWithRadius(this, serverLevel, 3);
            } else {

                MissileChunkManager.updateChunksWithRadius(this, serverLevel, 1);
            }
        }

        if (this.flightPhase != FlightPhase.LANDED) {
            if (this.localFlightTimer > 0) {
                this.localFlightTimer--;
            }

            if (this.tickCount % 20 == 0) {
                this.entityData.set(FLIGHT_TIMER, this.localFlightTimer);
            }

            if (this.localFlightTimer <= 0 && this.flightPhase != FlightPhase.RETURNING
                    && this.flightPhase != FlightPhase.LANDING) {
                transitionToPhase(FlightPhase.RETURNING);
            }
        }

        if (this.flightPhase != FlightPhase.LANDED && this.flightPhase != FlightPhase.LAUNCHING) {
            Vec3 newPos = this.position().add(motion);
            BlockPos checkPos = new BlockPos(
                    (int) Math.floor(newPos.x),
                    (int) Math.floor(newPos.y),
                    (int) Math.floor(newPos.z));
            if (!this.level().getBlockState(checkPos).isAir() && !this.level().getBlockState(checkPos).liquid()) {
                this.removeSelf();
                return;
            }
        }

        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        if (!activeOrlanRegistry.containsKey(this.getId())) {
            activeOrlanRegistry.put(this.getId(), this);
            PARKED.remove(this.getUUID());
        }

        this.syncTickCounter++;
        boolean needSync = !this.initialPacketSent || this.syncTickCounter >= SYNC_INTERVAL;
        if (needSync) {
            this.initialPacketSent = true;
            this.syncTickCounter = 0;
            NetworkHandler.sendToDimension(this.level(), new OrlanSpawnPacket(
                    this.getId(), this.position(), this.getDeltaMovement(), target,
                    this.flightPhase.ordinal(), this.getRemainingFlightTicks(), soundTypeKey()));
        }

        tickLinkedCamera();
    }

    private void tickLinkedCamera() {
        String linkedUUID = getLinkedPlayerUUID();
        if (!linkedUUID.isEmpty() && this.level() instanceof ServerLevel srvLevel) {
            try {
                ServerPlayer linkedPlayer = srvLevel.getServer().getPlayerList()
                        .getPlayer(UUID.fromString(linkedUUID));
                if (linkedPlayer != null) {

                    if (!linkedPlayer.isAlive()) {
                        linkedPlayer.setNoGravity(false);
                        linkedPlayer.gameMode.getGameModeForPlayer()
                                .updatePlayerAbilities(linkedPlayer.getAbilities());
                        linkedPlayer.onUpdateAbilities();
                        setLinkedPlayer("");
                        lastSentChunkPos = null;
                        NetworkHandler.sendToPlayer(linkedPlayer, new OrlanCameraForceExitPacket());

                    } else {

                        if (cameraPhysicsPos == null) {
                            cameraPhysicsPos = linkedPlayer.position();
                        }

                        Vec3 vel = linkedPlayer.getDeltaMovement();
                        vel = vel.add(0, -0.08, 0); 

                        linkedPlayer.absMoveTo(cameraPhysicsPos.x, cameraPhysicsPos.y,
                                cameraPhysicsPos.z, linkedPlayer.getYRot(), linkedPlayer.getXRot());
                        linkedPlayer.setDeltaMovement(vel);
                        linkedPlayer.fallDistance = cameraPlayerFallDistance;

                        double prevY = linkedPlayer.getY();
                        linkedPlayer.move(net.minecraft.world.entity.MoverType.SELF, vel);
                        double dY = linkedPlayer.getY() - prevY;

                        linkedPlayer.getAbilities().mayfly = false;
                        linkedPlayer.doCheckFallDamage(0, dY, 0, linkedPlayer.onGround());
                        linkedPlayer.getAbilities().mayfly = true;

                        cameraPhysicsPos = linkedPlayer.position();
                        cameraPlayerFallDistance = linkedPlayer.fallDistance;

                        linkedPlayer.connection.teleport(
                                linkedPlayer.getX(), linkedPlayer.getY(), linkedPlayer.getZ(),
                                linkedPlayer.getYRot(), linkedPlayer.getXRot(),
                                RelativeMovement.ROTATION);

                        cameraSelfData.sync(linkedPlayer, this);
                        if (this.tickCount % 10 == 0) {
                            linkedPlayer.connection.send(new ClientboundTeleportEntityPacket(this));
                            linkedPlayer.connection.send(new ClientboundSetEntityMotionPacket(this));
                        }

                        int cameraChunkRadius = (this.flightPhase == FlightPhase.PATROLLING)
                                ? OrlanCameraViewPacket.PATROLLING_CAMERA_CHUNK_RADIUS
                                : OrlanCameraViewPacket.DRONE_CHUNK_RADIUS;

                        ChunkPos currentChunk = this.chunkPosition();
                        if (lastSentChunkPos == null) {
                            lastSentChunkPos = currentChunk;
                        } else if (!currentChunk.equals(lastSentChunkPos)) {
                            OrlanCameraViewPacket.sendDroneChunksDelta(lastSentChunkPos, currentChunk, linkedPlayer,
                                    srvLevel, cameraChunkRadius);
                            lastSentChunkPos = currentChunk;
                        }

                        com.synarsis.airtacticalarsenal.network.DroneStatePacket.send(linkedPlayer, this);
                        syncNearbyEntities(linkedPlayer, srvLevel);
                    }
                }
            } catch (IllegalArgumentException ignored) {
                setLinkedPlayer("");
            }
        }
    }

    private Vec3 tickLaunchingPhase(Vec3 targetVec, Vec3 currentPos) {
        this.currentSpeed = Math.min(this.currentSpeed + LAUNCH_ACCELERATION, LAUNCH_MAX_SPEED);

        this.pathAngleDeg = approachAngle(this.pathAngleDeg, LAUNCH_ANGLE_DEG);
        double launchAngleRad = Math.toRadians(this.pathAngleDeg);
        double horizontalSpeed = this.currentSpeed * Math.cos(launchAngleRad);
        double verticalSpeed = this.currentSpeed * Math.sin(launchAngleRad);

        Vec3 motion = new Vec3(
                this.launchDirection.x * horizontalSpeed,
                verticalSpeed,
                this.launchDirection.z * horizontalSpeed);

        if (this.currentSpeed >= LAUNCH_MAX_SPEED * 0.8) {
            transitionToPhase(FlightPhase.CLIMBING);
        }

        return motion;
    }

    private Vec3 tickClimbingPhase(Vec3 targetVec, Vec3 currentPos) {
        this.currentSpeed = Math.min(this.currentSpeed + CLIMB_ACCELERATION, this.cruiseSpeed());

        double distanceFromLaunch = Math.sqrt(
                Math.pow(currentPos.x - this.launchStartPos.x, 2) +
                        Math.pow(currentPos.z - this.launchStartPos.z, 2));

        Vec3 horizontalDir = this.launchDirection;
        double remainingHeight = this.targetAltitude - this.getY();
        double climbAngleRad;

        if (remainingHeight > 80) {
            climbAngleRad = Math.toRadians(CLIMB_ANGLE_DEG);
        } else if (remainingHeight > 20) {
            climbAngleRad = Math.toRadians(CLIMB_ANGLE_DEG * (remainingHeight / 80.0));
        } else {
            climbAngleRad = Math.toRadians(3.0);
        }

        this.pathAngleDeg = approachAngle(this.pathAngleDeg, Math.toDegrees(climbAngleRad));
        climbAngleRad = Math.toRadians(this.pathAngleDeg);
        double horizontalSpeed = this.currentSpeed * Math.cos(climbAngleRad);
        double verticalSpeed = this.currentSpeed * Math.sin(climbAngleRad);

        Vec3 motion = new Vec3(
                horizontalDir.x * horizontalSpeed,
                verticalSpeed,
                horizontalDir.z * horizontalSpeed);

        boolean reachedAltitude = this.getY() >= this.patrolAltitude() - 10.0;
        boolean reachedDistance = distanceFromLaunch >= CLIMB_DISTANCE;

        if (reachedAltitude && reachedDistance) {
            transitionToPhase(FlightPhase.CRUISING);
        }

        return motion;
    }

    private Vec3 tickCruisingPhase(Vec3 targetVec, Vec3 currentPos, double horizontalDist) {
        this.currentSpeed = this.cruiseSpeed();

        Vec3 navTarget;
        boolean isFinalLeg;
        if (waypoints != null && !waypoints.isEmpty() && currentWaypointIndex < waypoints.size()) {
            BlockPos wp = waypoints.get(currentWaypointIndex);
            navTarget = new Vec3(wp.getX() + 0.5, this.targetAltitude, wp.getZ() + 0.5);
            isFinalLeg = false;

            Vec3 prevPos = currentWaypointIndex == 0 ? this.launchStartPos
                    : Vec3.atCenterOf(waypoints.get(currentWaypointIndex - 1));
            if (RouteNav.reached(currentPos, this.lastDirection, prevPos, navTarget, 50.0)) {
                currentWaypointIndex++;

            }
        } else {
            navTarget = targetVec;
            isFinalLeg = true;
        }

        Vec3 desiredDir = new Vec3(navTarget.x - currentPos.x, 0, navTarget.z - currentPos.z);
        Vec3 smoothedDir;
        if (desiredDir.lengthSqr() > 1.0E-4) {
            desiredDir = desiredDir.normalize();
            smoothedDir = smoothTurn(this.lastDirection, desiredDir, this.turnRate());
        } else {
            smoothedDir = this.lastDirection;
        }

        double heightDiff = this.targetAltitude - this.getY();
        double verticalCorrection = heightDiff * HEIGHT_CORRECTION_FACTOR;
        verticalCorrection = Math.max(-0.3, Math.min(0.3, verticalCorrection));

        Vec3 motion = new Vec3(
                smoothedDir.x * this.currentSpeed,
                verticalCorrection,
                smoothedDir.z * this.currentSpeed);

        if (isFinalLeg && horizontalDist < PATROL_ENTRY_DISTANCE) {
            transitionToPhase(FlightPhase.PATROLLING);
        }

        return motion;
    }

    private static Vec3 smoothTurn(Vec3 current, Vec3 desired, double maxDegreesPerTick) {
        double currentYaw = Math.atan2(current.x, current.z);
        double desiredYaw = Math.atan2(desired.x, desired.z);
        double diff = desiredYaw - currentYaw;

        while (diff > Math.PI)
            diff -= 2 * Math.PI;
        while (diff < -Math.PI)
            diff += 2 * Math.PI;
        double maxRad = Math.toRadians(maxDegreesPerTick);
        if (Math.abs(diff) > maxRad) {
            diff = Math.signum(diff) * maxRad;
        }
        double newYaw = currentYaw + diff;
        return new Vec3(Math.sin(newYaw), 0, Math.cos(newYaw)).normalize();
    }

    private Vec3 tickPatrollingPhase(Vec3 targetVec, Vec3 currentPos) {
        if (!this.patrolInitialized) {
            double dx = currentPos.x - targetVec.x;
            double dz = currentPos.z - targetVec.z;
            this.patrolAngle = Math.atan2(dz, dx);
            this.patrolInitialized = true;
        }

        double dx = currentPos.x - targetVec.x;
        double dz = currentPos.z - targetVec.z;
        double distFromCenter = Math.sqrt(dx * dx + dz * dz);

        double currentAngle = Math.atan2(dz, dx);

        double tangentX = -Math.sin(currentAngle);
        double tangentZ = Math.cos(currentAngle);

        double radialError = distFromCenter - PATROL_RADIUS;
        double radialWeight = 0;
        if (distFromCenter > 0.1) {

            radialWeight = Math.min(Math.abs(radialError) / PATROL_RADIUS, 0.3);
        }

        double radialX = (distFromCenter > 0.1) ? -dx / distFromCenter : 0;
        double radialZ = (distFromCenter > 0.1) ? -dz / distFromCenter : 0;

        double radialSign = (radialError > 0) ? 1.0 : -1.0;

        double moveX = tangentX * (1.0 - radialWeight) + radialX * radialSign * radialWeight;
        double moveZ = tangentZ * (1.0 - radialWeight) + radialZ * radialSign * radialWeight;

        double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (len > 0.01) {
            moveX /= len;
            moveZ /= len;
        }

        double heightDiff = this.targetAltitude - this.getY();
        double verticalCorrection = heightDiff * HEIGHT_CORRECTION_FACTOR * 0.5;
        verticalCorrection = Math.max(-0.15, Math.min(0.15, verticalCorrection));

        this.patrolAngle = currentAngle;

        return new Vec3(moveX * this.patrolSpeed(), verticalCorrection, moveZ * this.patrolSpeed());
    }

    private Vec3 tickReturningPhase(Vec3 currentPos) {
        Vec3 launcherVec = Vec3.atCenterOf(this.launcherPos);
        Vec3 horizontalToLauncher = new Vec3(launcherVec.x - currentPos.x, 0, launcherVec.z - currentPos.z);
        double horizontalDist = horizontalToLauncher.horizontalDistance();

        double landingTransitionDist = Math.max(CLIMB_DISTANCE, this.targetAltitude * 1.5);
        if (horizontalDist < landingTransitionDist) {
            transitionToPhase(FlightPhase.LANDING);
            return tickLandingPhase(currentPos);
        }

        Vec3 dir = horizontalToLauncher.normalize();

        double heightDiff = this.targetAltitude - this.getY();
        double verticalCorrection = heightDiff * HEIGHT_CORRECTION_FACTOR;
        verticalCorrection = Math.max(-0.3, Math.min(0.3, verticalCorrection));

        return new Vec3(dir.x * this.returnSpeed(), verticalCorrection, dir.z * this.returnSpeed());
    }

    private Vec3 tickLandingPhase(Vec3 currentPos) {
        if (this.landingTarget == null) {
            computeLandingTarget();
        }

        Vec3 horizontalToTarget = new Vec3(
                this.landingTarget.x - currentPos.x, 0, this.landingTarget.z - currentPos.z);
        double horizontalDist = horizontalToTarget.horizontalDistance();

        double heightAboveGround = this.getY() - this.landingTarget.y;

        BlockPos belowPos = this.blockPosition().below();
        net.minecraft.world.level.block.state.BlockState stateBelow = this.level().getBlockState(belowPos);
        boolean solidBelow = !stateBelow.isAir() && stateBelow.isCollisionShapeFullBlock(this.level(), belowPos);
        boolean nearGround = solidBelow && (this.getY() - belowPos.getY()) < 2.5;

        if (nearGround) {
            performLanding();
            return Vec3.ZERO;
        }

        if (heightAboveGround < -10) {
            performLanding();
            return Vec3.ZERO;
        }

        if (horizontalDist < 3.0) {
            double descentSpeed = Math.max(0.15, Math.min(0.5, heightAboveGround * 0.05));
            return new Vec3(0, -descentSpeed, 0);
        }

        Vec3 dir = horizontalToTarget.normalize();

        double requiredAngleRad = (heightAboveGround > 0)
                ? Math.atan2(heightAboveGround, horizontalDist)
                : Math.toRadians(5.0);
        double descentAngleRad = Math.max(Math.toRadians(5.0), Math.min(Math.toRadians(40.0), requiredAngleRad));

        double minLandingSpeed = LAUNCH_INITIAL_SPEED;
        double maxLandingSpeed = this.returnSpeed();
        double speedFactor;
        if (horizontalDist > 100) {
            speedFactor = 1.0;
        } else if (horizontalDist > 20) {
            speedFactor = 0.3 + 0.7 * ((horizontalDist - 20.0) / 80.0);
        } else {
            speedFactor = Math.max(0.2, 0.3 * (horizontalDist / 20.0));
        }
        this.currentSpeed = minLandingSpeed + (maxLandingSpeed - minLandingSpeed) * speedFactor;

        double horizontalSpeed = this.currentSpeed * Math.cos(descentAngleRad);
        double verticalSpeed = -this.currentSpeed * Math.sin(descentAngleRad);

        return new Vec3(
                dir.x * horizontalSpeed,
                verticalSpeed,
                dir.z * horizontalSpeed);
    }

    private void computeLandingTarget() {
        Vec3 launcherVec = Vec3.atCenterOf(this.launcherPos);

        double randomAngle = this.random.nextDouble() * Math.PI * 2.0;
        double randomDist = 10.0 + this.random.nextDouble() * 5.0;
        double dirX = Math.cos(randomAngle);
        double dirZ = Math.sin(randomAngle);

        double targetX = launcherVec.x + dirX * randomDist;
        double targetZ = launcherVec.z + dirZ * randomDist;

        int groundY = findGroundY((int) Math.floor(targetX), (int) Math.floor(targetZ));

        this.landingTarget = new Vec3(targetX, groundY, targetZ);
    }

    private void performLanding() {

        BlockPos snapPos = this.blockPosition();
        for (int y = snapPos.getY(); y >= this.level().getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(snapPos.getX(), y, snapPos.getZ());
            net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(checkPos);
            if (!state.isAir() && state.isCollisionShapeFullBlock(this.level(), checkPos)) {
                this.setPos(this.getX(), y + 1.0, this.getZ());
                break;
            }
        }

        this.setXRot(0);
        this.xRotO = 0;
        this.entityData.set(SYNC_X_ROT, 0f);

        transitionToPhase(FlightPhase.LANDED);
        this.setNoGravity(false);
        this.noPhysics = false;
    }

    private int findGroundY(int x, int z) {

        int startY = Math.min(this.launcherPos.getY() + 60, this.level().getMaxBuildHeight() - 1);
        for (int y = startY; y >= this.level().getMinBuildHeight(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(pos);
            if (!state.isAir() && state.isCollisionShapeFullBlock(this.level(), pos)) {
                return y + 1;
            }
        }
        return this.launcherPos.getY();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (CreateCompat.isCreateFakePlayer(player)) return InteractionResult.FAIL;
        if (this.flightPhase != FlightPhase.LANDED || this.level().isClientSide) {
            return InteractionResult.PASS;
        }

        ItemStack orlanItem = this.createDroneItem();
        if (!player.getInventory().add(orlanItem)) {
            player.drop(orlanItem, false);
        }
        player.displayClientMessage(Component.translatable("message.ata.orlan.picked_up", this.getDroneName()), true);
        this.removeSelf();
        return InteractionResult.SUCCESS;
    }

    void transitionToPhase(FlightPhase newPhase) {
        if (this.flightPhase != newPhase) {
            this.flightPhase = newPhase;
            this.phaseTicks = 0;
            if (newPhase == FlightPhase.LANDING && !this.level().isClientSide) {
                computeLandingTarget();
            }
            if (newPhase != FlightPhase.LANDING && newPhase != FlightPhase.LANDED) {
                this.landingTarget = null;
            }
            if (!this.level().isClientSide) {
                this.entityData.set(FLIGHT_PHASE_ID, newPhase.ordinal());
            }
        }
    }

    public FlightPhase getFlightPhase() {
        return this.flightPhase;
    }

    public float getHealth() {
        return this.entityData.get(HEALTH);
    }

    public void setHealth(float health) {
        float clamped = Math.max(0, Math.min(health, this.maxHealth));
        this.entityData.set(HEALTH, clamped);
        this.entityData.set(IS_DAMAGED, clamped < this.maxHealth);
    }

    public boolean isDamaged() {
        return this.entityData.get(IS_DAMAGED);
    }

    public float getHealthPercent() {
        return this.getHealth() / this.maxHealth;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide)
            return false;
        if (CreateCompat.isCreateDamageSource(source))
            return false;
        if (this.destroyed)
            return false;

        Entity directEntity = source.getDirectEntity();
        if (directEntity instanceof Projectile) {
            if (com.synarsis.airtacticalarsenal.compat.SuperbWarfareCompat.isMissile(directEntity)) {
                com.synarsis.airtacticalarsenal.compat.MissileProximityFuse.airBurst(this);
            }
            boolean heavy = ShahedEntity.isHeavyShell(directEntity) && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
            if (heavy) {
                amount = Math.max(amount, this.maxHealth * 1.5f);
            }
            applyDamage(amount, source.getEntity(), directEntity.position(), false, heavy);
            return true;
        }

        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            applyDamage(amount * 2.0f, source.getEntity(), this.position(), true);
            return true;
        }

        return false;
    }

    public void interceptorHit(float healthFraction, Entity attacker, Vec3 hitPos) {
        if (this.level().isClientSide || this.destroyed) {
            return;
        }
        applyDamage(this.maxHealth * healthFraction, attacker, hitPos, false, true);
    }

    private void applyDamage(float amount, Entity attacker, Vec3 hitPos, boolean explosion) {
        applyDamage(amount, attacker, hitPos, explosion, false);
    }

    private void applyDamage(float amount, Entity attacker, Vec3 hitPos, boolean explosion, boolean heavy) {
        boolean airburst = amount >= this.maxHealth * ShahedConfig.getAirburstDamageMultiplier();
        this.durability.ensure(this.maxHealth, this.random);
        if (isCrashing()) {
            if (airburst) destroyDrone(attacker);
            else if (this.durability.fallingHit()) crashImpact();
            return;
        }
        if (!heavy && !airburst) amount = this.durability.scale(amount);
        boolean canFall = this.durability.hit() || heavy || airburst;
        float newHealth = this.getHealth() - amount;
        if (!canFall) newHealth = Math.max(newHealth, 1.0f);
        this.setHealth(newHealth);
        boolean crashAllowed = ShahedConfig.isDroneCrashOnShootdown() && !airburst
                && this.flightPhase != FlightPhase.LANDED && this.flightPhase != FlightPhase.LAUNCHING
                && this.flightPhase != FlightPhase.LANDING;
        int zone = explosion ? CrashDynamics.ZONE_BODY : crashZoneAt(hitPos);
        switch (zone) {
            case CrashDynamics.ZONE_ENGINE -> this.durability.engineHp -= amount;
            case CrashDynamics.ZONE_WING_LEFT -> this.durability.wingLeftHp -= amount;
            case CrashDynamics.ZONE_WING_RIGHT -> this.durability.wingRightHp -= amount;
            default -> {
            }
        }
        if (!canFall) return;
        if (!crashAllowed) {
            if (this.getHealth() <= 0) destroyDrone(attacker);
            return;
        }
        if (this.getHealth() <= 0) {
            startCrash(CrashDynamics.modeForZone(zone, explosion, this.random), CrashDynamics.sideForZone(zone, this.random));
        } else if (this.durability.wingLeftHp <= 0) {
            startCrash(CrashDynamics.Mode.SPIN, -1);
        } else if (this.durability.wingRightHp <= 0) {
            startCrash(CrashDynamics.Mode.SPIN, 1);
        } else if (this.durability.engineHp <= 0) {
            startCrash(CrashDynamics.Mode.ENGINE_OUT, this.random.nextBoolean() ? 1 : -1);
        }
    }

    public boolean isCrashing() {
        return this.level().isClientSide ? this.entityData.get(CRASH_MODE) != 0 : this.crash.active();
    }

    public CrashDynamics.Mode getCrashMode() {
        return CrashDynamics.Mode.of(this.entityData.get(CRASH_MODE));
    }

    public float getRoll(float partialTick) {
        return this.rollO + net.minecraft.util.Mth.wrapDegrees(this.roll - this.rollO) * partialTick;
    }

    private void startCrash(CrashDynamics.Mode mode, int side) {
        if (this.destroyed || this.crash.active() || this.level().isClientSide) return;
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() < 0.01) motion = this.lastDirection.normalize().scale(this.cruiseSpeed());
        this.crash.start(mode, motion, this.getYRot(), this.getXRot(), side, this.random);
        this.entityData.set(CRASH_MODE, mode.ordinal());
    }

    private void tickCrashServer() {
        Vec3 motion = this.crash.step(this.random);
        this.setDeltaMovement(motion);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setYRot(this.crash.yaw());
        this.setXRot(this.crash.pitch());
        this.rollO = this.roll;
        this.roll = this.crash.roll();
        this.entityData.set(SYNC_Y_ROT, this.crash.yaw());
        this.entityData.set(SYNC_X_ROT, this.crash.pitch());
        this.entityData.set(SYNC_ROLL, this.crash.roll());

        if (this.level() instanceof ServerLevel serverLevel) {
            MissileChunkManager.updateChunksWithRadius(this, serverLevel, 1);
        }

        Vec3 from = this.position();
        Vec3 to = from.add(motion);
        net.minecraft.world.phys.BlockHitResult blockHit = this.level().clip(new net.minecraft.world.level.ClipContext(
                from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.ANY, this));
        if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            this.setPos(blockHit.getLocation());
            crashImpact();
            return;
        }
        this.setPos(to);
        if (to.y < this.level().getMinBuildHeight() || this.crash.ticks() > CrashDynamics.MAX_TICKS) {
            crashImpact();
        }
    }

    private void tickCrashClient() {
        float syncedYRot = this.entityData.get(SYNC_Y_ROT);
        float syncedXRot = this.entityData.get(SYNC_X_ROT);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setYRot(syncedYRot);
        this.setXRot(syncedXRot);
        this.setYBodyRot(syncedYRot);
        this.setYHeadRot(syncedYRot);
        this.rollO = this.roll;
        this.roll = this.entityData.get(SYNC_ROLL);
        this.clientCrashTicks++;
        Vec3 motion = new Vec3(this.getX() - this.xo, this.getY() - this.yo, this.getZ() - this.zo);
        CrashEffects.trail(this.level(), this.position().add(0, 0.3, 0), motion, getCrashMode(),
                this.clientCrashTicks, 1.0f, this.random);
    }

    private void crashImpact() {
        if (this.destroyed) return;
        this.destroyed = true;
        Vec3 pos = this.position();
        this.level().explode(this, pos.x, pos.y, pos.z, 1.2f, Level.ExplosionInteraction.NONE);
        NetworkHandler.sendToDimension(this.level(), new com.synarsis.airtacticalarsenal.network.OrlanCrashPacket(pos));
        if (this.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(pos) > 512.0 * 512.0) continue;
                serverLevel.sendParticles(player, ParticleTypes.EXPLOSION, true, pos.x, pos.y + 0.5, pos.z, 3, 0.6, 0.4, 0.6, 0.0);
                serverLevel.sendParticles(player, ParticleTypes.LARGE_SMOKE, true, pos.x, pos.y + 0.8, pos.z, 40, 1.2, 0.8, 1.2, 0.04);
                serverLevel.sendParticles(player, ParticleTypes.FLAME, true, pos.x, pos.y + 0.4, pos.z, 20, 0.7, 0.4, 0.7, 0.03);
                serverLevel.sendParticles(player, ParticleTypes.CAMPFIRE_COSY_SMOKE, true, pos.x, pos.y + 1.0, pos.z, 12, 0.6, 0.5, 0.6, 0.02);
                serverLevel.sendParticles(player, com.synarsis.airtacticalarsenal.particle.ModParticles.DEBRIS.get(), true,
                        pos.x, pos.y + 0.5, pos.z, 25, 0.5, 0.5, 0.5, 0.4);
            }
        }
        this.removeSelf();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    private boolean selfRemoving = false;
    private int lastFlakTick = -1;

    private void removeSelf() {
        this.selfRemoving = true;
        this.discard();
    }

    private void destroyDrone(Entity attacker) {
        if (this.destroyed)
            return;
        this.destroyed = true;
        spawnShootdownEffects();
        this.removeSelf();
    }

    private void spawnShootdownEffects() {
        Vec3 pos = this.position();

        if (this.level() instanceof ServerLevel serverLevel) {

            for (ServerPlayer player : serverLevel.players()) {
                if (player.position().distanceTo(pos) < 200.0) {
                    serverLevel.sendParticles(player, ParticleTypes.LARGE_SMOKE, true,
                            pos.x, pos.y, pos.z, 20, 1.0, 1.0, 1.0, 0.05);
                    serverLevel.sendParticles(player, ParticleTypes.FLAME, true,
                            pos.x, pos.y, pos.z, 5, 0.5, 0.5, 0.5, 0.02);
                }
            }
        }
    }

    private void spawnDamageEffects() {
        if (!(this.level() instanceof ServerLevel serverLevel))
            return;
        if (!this.isDamaged())
            return;

        Vec3 pos = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 enginePos = motion.lengthSqr() > 0.01 ? pos.subtract(motion.normalize().scale(1.0)) : pos;

        if (this.tickCount % 3 == 0) {
            for (ServerPlayer player : serverLevel.players()) {
                if (player.position().distanceTo(pos) < 200.0) {
                    serverLevel.sendParticles(player, ParticleTypes.SMOKE, true,
                            enginePos.x, enginePos.y, enginePos.z, 1, 0.2, 0.2, 0.2, 0.01);
                }
            }
        }
    }

    public void setLinkedPlayer(String playerUUID) {
        String newVal = playerUUID != null ? playerUUID : "";

        if (newVal.isEmpty() && !this.level().isClientSide) {
            String oldVal = this.entityData.get(LINKED_PLAYER);
            if (!oldVal.isEmpty()) {
                clearCameraTracking(oldVal);
                cameraPhysicsPos = null;
                cameraPlayerFallDistance = 0.0f;
            }
        }
        this.entityData.set(LINKED_PLAYER, newVal);
    }

    public int crashZoneAt(Vec3 hitPos) {
        return CrashDynamics.zoneOf(hitPos.subtract(this.position()), this.lastDirection, 0.8);
    }

    public String soundTypeKey() {
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(this.getType());
        return key != null ? key.toString() : "";
    }

    public String getLinkedPlayerUUID() {
        return this.entityData.get(LINKED_PLAYER);
    }

    public boolean isLinkedToPlayer(UUID playerUUID) {
        String linked = getLinkedPlayerUUID();
        return !linked.isEmpty() && linked.equals(playerUUID.toString());
    }

    public Vec3 getCameraPhysicsPos() {
        return cameraPhysicsPos;
    }

    public float getCameraPlayerFallDistance() {
        return cameraPlayerFallDistance;
    }

    public void savePlayerPosition(ServerPlayer player) {
        this.savedPlayerX = player.getX();
        this.savedPlayerY = player.getY();
        this.savedPlayerZ = player.getZ();
        this.savedPlayerYRot = player.getYRot();
        this.savedPlayerXRot = player.getXRot();
    }

    public void restorePlayerPosition(ServerPlayer player) {
        player.connection.teleport(savedPlayerX, savedPlayerY, savedPlayerZ, savedPlayerYRot, savedPlayerXRot);
    }

    private void syncNearbyEntities(ServerPlayer viewer, ServerLevel level) {
        int chunkRadius = (this.flightPhase == FlightPhase.PATROLLING)
                ? OrlanCameraViewPacket.PATROLLING_CAMERA_CHUNK_RADIUS
                : OrlanCameraViewPacket.DRONE_CHUNK_RADIUS;
        trackedCameraEntities.tick(viewer, level, this, chunkRadius * 16.0,
                e -> e != viewer && e.isAlive() && !(e instanceof net.minecraft.world.entity.Marker)
                        && e.getType().clientTrackingRange() > 0,
                this.tickCount % 3 == 0);
    }

    public void clearCameraTracking(String playerUUID) {
        if (trackedCameraEntities.isEmpty())
            return;
        if (this.level() instanceof ServerLevel srvLevel) {
            try {
                ServerPlayer player = srvLevel.getServer().getPlayerList()
                        .getPlayer(UUID.fromString(playerUUID));
                if (player != null) {
                    trackedCameraEntities.removeAll(player);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        trackedCameraEntities.clear();
    }

    public void setOwnerUUID(String uuid) {
        this.entityData.set(OWNER_UUID_DATA, uuid != null ? uuid : "");
    }

    public String getOwnerUUIDString() {
        return this.entityData.get(OWNER_UUID_DATA);
    }

    public boolean isOwnedBy(Player player) {
        String owner = getOwnerUUIDString();
        return owner.isEmpty() || owner.equals(player.getUUID().toString());
    }

    public int getRemainingFlightTicks() {
        if (!this.level().isClientSide) {
            return this.localFlightTimer;
        }
        return this.entityData.get(FLIGHT_TIMER);
    }

    public void forceReturn() {
        if (!this.level().isClientSide) {
            transitionToPhase(FlightPhase.RETURNING);
        }
    }

    public BlockPos getLauncherPos() {
        return this.launcherPos;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("LegitSpawn", this.isLegitSpawn);
        BlockPos target = this.entityData.get(TARGET_POS);
        tag.putInt("TargetX", target.getX());
        tag.putInt("TargetY", target.getY());
        tag.putInt("TargetZ", target.getZ());
        tag.putInt("FlightPhase", this.flightPhase.ordinal());
        tag.putBoolean("Crashing", this.crash.active());
        tag.putInt("PhaseTicks", this.phaseTicks);
        tag.putDouble("CurrentSpeed", this.currentSpeed);
        tag.putDouble("LaunchStartY", this.launchStartY);
        tag.putDouble("LaunchStartX", this.launchStartPos.x);
        tag.putDouble("LaunchStartPosY", this.launchStartPos.y);
        tag.putDouble("LaunchStartZ", this.launchStartPos.z);
        tag.putDouble("LaunchDirX", this.launchDirection.x);
        tag.putDouble("LaunchDirY", this.launchDirection.y);
        tag.putDouble("LaunchDirZ", this.launchDirection.z);
        tag.putDouble("TargetAltitude", this.targetAltitude);
        tag.putDouble("PatrolAngle", this.patrolAngle);
        tag.putBoolean("PatrolInitialized", this.patrolInitialized);
        tag.putDouble("LastDirX", this.lastDirection.x);
        tag.putDouble("LastDirY", this.lastDirection.y);
        tag.putDouble("LastDirZ", this.lastDirection.z);
        tag.putFloat("Health", this.getHealth());
        tag.putFloat("MaxHealth", this.maxHealth);
        tag.putString("LinkedPlayer", this.getLinkedPlayerUUID());
        tag.putString("OwnerUUID", this.getOwnerUUIDString());
        tag.putInt("AtaSkin", this.getSkin());
        tag.putInt("FlightTimer", this.entityData.get(FLIGHT_TIMER));
        tag.putInt("LauncherPosX", this.launcherPos.getX());
        tag.putInt("LauncherPosY", this.launcherPos.getY());
        tag.putInt("LauncherPosZ", this.launcherPos.getZ());
        if (this.landingTarget != null) {
            tag.putDouble("LandingTargetX", this.landingTarget.x);
            tag.putDouble("LandingTargetY", this.landingTarget.y);
            tag.putDouble("LandingTargetZ", this.landingTarget.z);
        }
        if (this.waypoints != null && !this.waypoints.isEmpty()) {
            net.minecraft.nbt.ListTag wpTag = new net.minecraft.nbt.ListTag();
            for (BlockPos wp : this.waypoints) {
                CompoundTag wpc = new CompoundTag();
                wpc.putInt("x", wp.getX());
                wpc.putInt("y", wp.getY());
                wpc.putInt("z", wp.getZ());
                wpTag.add(wpc);
            }
            tag.put("Waypoints", wpTag);
            tag.putInt("WaypointIndex", this.currentWaypointIndex);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSkin(tag.getInt("AtaSkin"));
        this.discardOnLoad = tag.getBoolean("Crashing");
        if (tag.contains("LegitSpawn")) {
            this.isLegitSpawn = tag.getBoolean("LegitSpawn");
        }
        BlockPos target = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        this.entityData.set(TARGET_POS, target);

        if (tag.contains("FlightPhase")) {
            int phaseId = tag.getInt("FlightPhase");
            if (phaseId >= 0 && phaseId < FlightPhase.values().length) {
                this.flightPhase = FlightPhase.values()[phaseId];
                this.entityData.set(FLIGHT_PHASE_ID, phaseId);
            }
        }
        if (tag.contains("PhaseTicks"))
            this.phaseTicks = tag.getInt("PhaseTicks");
        if (tag.contains("CurrentSpeed"))
            this.currentSpeed = tag.getDouble("CurrentSpeed");
        if (tag.contains("LaunchStartY"))
            this.launchStartY = tag.getDouble("LaunchStartY");
        if (tag.contains("LaunchDirX")) {
            this.launchDirection = new Vec3(
                    tag.getDouble("LaunchDirX"), tag.getDouble("LaunchDirY"), tag.getDouble("LaunchDirZ"));
        }
        if (tag.contains("LaunchStartX")) {
            this.launchStartPos = new Vec3(
                    tag.getDouble("LaunchStartX"), tag.getDouble("LaunchStartPosY"), tag.getDouble("LaunchStartZ"));
        }
        if (tag.contains("TargetAltitude"))
            this.targetAltitude = tag.getDouble("TargetAltitude");
        if (tag.contains("PatrolAngle"))
            this.patrolAngle = tag.getDouble("PatrolAngle");
        if (tag.contains("PatrolInitialized"))
            this.patrolInitialized = tag.getBoolean("PatrolInitialized");
        if (tag.contains("LastDirX")) {
            this.lastDirection = new Vec3(
                    tag.getDouble("LastDirX"), tag.getDouble("LastDirY"), tag.getDouble("LastDirZ"));
        }
        if (tag.contains("Health"))
            this.setHealth(tag.getFloat("Health"));
        if (tag.contains("MaxHealth"))
            this.maxHealth = tag.getFloat("MaxHealth");
        if (tag.contains("LinkedPlayer"))
            this.setLinkedPlayer(tag.getString("LinkedPlayer"));
        if (tag.contains("OwnerUUID"))
            this.setOwnerUUID(tag.getString("OwnerUUID"));
        if (tag.contains("FlightTimer")) {
            this.localFlightTimer = tag.getInt("FlightTimer");
            this.entityData.set(FLIGHT_TIMER, this.localFlightTimer);
        }
        if (tag.contains("LauncherPosX")) {
            this.launcherPos = new BlockPos(
                    tag.getInt("LauncherPosX"), tag.getInt("LauncherPosY"), tag.getInt("LauncherPosZ"));
            this.entityData.set(LAUNCHER_POS_DATA, this.launcherPos);
        }
        if (tag.contains("LandingTargetX")) {
            this.landingTarget = new Vec3(
                    tag.getDouble("LandingTargetX"),
                    tag.getDouble("LandingTargetY"),
                    tag.getDouble("LandingTargetZ"));
        }
        if (tag.contains("Waypoints")) {
            net.minecraft.nbt.ListTag wpTag = tag.getList("Waypoints", 10);
            this.waypoints = new ArrayList<>();
            for (int i = 0; i < wpTag.size(); i++) {
                CompoundTag wpc = wpTag.getCompound(i);
                this.waypoints.add(new BlockPos(wpc.getInt("x"), wpc.getInt("y"), wpc.getInt("z")));
            }
            if (this.waypoints.isEmpty())
                this.waypoints = null;
        }
        if (tag.contains("WaypointIndex"))
            this.currentWaypointIndex = tag.getInt("WaypointIndex");

        if (this.flightPhase == FlightPhase.LANDED) {
            this.setNoGravity(false);
            this.noPhysics = false;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
        controllerRegistrar.add(new AnimationController<>(this, "propeller", 0, this::propellerPredicate));
    }

    private PlayState predicate(AnimationState<OrlanEntity> animationState) {
        FlightPhase phase = getClientFlightPhase();
        if (phase == FlightPhase.LANDED) {
            animationState.getController().setAnimation(RawAnimation.begin().thenLoop("animation.orlan.idle"));
        } else {
            animationState.getController().setAnimation(RawAnimation.begin().thenLoop("animation.orlan.fly"));
        }
        return PlayState.CONTINUE;
    }

    private PlayState propellerPredicate(AnimationState<OrlanEntity> animationState) {
        FlightPhase phase = getClientFlightPhase();
        if (phase == FlightPhase.LANDED) {
            return PlayState.STOP;
        }
        animationState.getController().setAnimation(RawAnimation.begin().thenLoop("animation.orlan.propeller"));
        return PlayState.CONTINUE;
    }

    private FlightPhase getClientFlightPhase() {
        int phaseId = this.entityData.get(FLIGHT_PHASE_ID);
        if (phaseId >= 0 && phaseId < FlightPhase.values().length) {
            return FlightPhase.values()[phaseId];
        }
        return FlightPhase.LAUNCHING;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(2.0, 0.5, 2.0);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason == RemovalReason.DISCARDED && !this.selfRemoving && !this.destroyed
                && this.isLegitSpawn && this.flightPhase != FlightPhase.LANDED) {
            if (this.tickCount != this.lastFlakTick) {
                this.lastFlakTick = this.tickCount;
                Vec3 hit = this.position().add((this.random.nextDouble() - 0.5) * 2.0, this.random.nextDouble() * 0.6,
                        (this.random.nextDouble() - 0.5) * 2.0);
                applyDamage(this.maxHealth * (float) ShahedConfig.getFlakHitDamagePercent() / 100.0f, null, hit, false);
            }
            return;
        }
        if (!this.level().isClientSide && reason == RemovalReason.DISCARDED && !this.selfRemoving && !this.destroyed) {
            this.destroyed = true;
            spawnShootdownEffects();
        }
        if (!this.level().isClientSide) {

            activeOrlanRegistry.remove(this.getId());
            if (reason == RemovalReason.UNLOADED_TO_CHUNK && this.flightPhase == FlightPhase.LANDED
                    && !this.getOwnerUUIDString().isEmpty()) {
                PARKED.put(this.getUUID(), new LandedDrone(this.getUUID(), this.getOwnerUUIDString(), this.getDroneNameKey(),
                        com.synarsis.airtacticalarsenal.api.DroneVariants.kindOf(this), this.level().dimension(),
                        (int) this.getX(), (int) this.getZ()));
            } else {
                PARKED.remove(this.getUUID());
            }

            String linkedUUID = getLinkedPlayerUUID();
            if (!linkedUUID.isEmpty() && this.level() instanceof ServerLevel srvLevel) {
                try {
                    ServerPlayer linkedPlayer = srvLevel.getServer().getPlayerList()
                            .getPlayer(UUID.fromString(linkedUUID));
                    if (linkedPlayer != null) {
                        linkedPlayer.setNoGravity(false);
                        linkedPlayer.gameMode.getGameModeForPlayer()
                                .updatePlayerAbilities(linkedPlayer.getAbilities());
                        linkedPlayer.onUpdateAbilities();
                        if (cameraPhysicsPos != null) {
                            linkedPlayer.connection.teleport(cameraPhysicsPos.x,
                                    cameraPhysicsPos.y, cameraPhysicsPos.z,
                                    linkedPlayer.getYRot(), linkedPlayer.getXRot());
                        }

                        setLinkedPlayer("");
                        OrlanCameraViewPacket.sendPlayerChunks(linkedPlayer, srvLevel);
                        OrlanCameraViewPacket.resyncPlayerEntities(linkedPlayer, srvLevel);

                        NetworkHandler.sendToPlayer(linkedPlayer,
                                new com.synarsis.airtacticalarsenal.network.OrlanCameraForceExitPacket(reason.shouldDestroy()));
                    }
                } catch (IllegalArgumentException ignored) {
                }

                if (!getLinkedPlayerUUID().isEmpty()) {
                    setLinkedPlayer("");
                }
            }
            MissileChunkManager.unregisterMissile(this);
            NetworkHandler.sendToAllPlayers(new OrlanRemovePacket(this.getId()));
        }
        super.remove(reason);
    }

    public BlockPos getTargetPos() {
        return this.entityData.get(TARGET_POS);
    }

    public void setWaypoints(List<BlockPos> waypoints) {
        this.waypoints = waypoints != null && !waypoints.isEmpty() ? new ArrayList<>(waypoints) : null;
        this.currentWaypointIndex = 0;
    }

    public List<BlockPos> getWaypoints() {
        return this.waypoints;
    }

    public static int getMaxFlightTicks() {
        try {
            return com.synarsis.airtacticalarsenal.config.ShahedConfig.getOrlanFlightTimeMinutes() * 60 * 20;
        } catch (IllegalStateException configNotLoaded) {
            return 30 * 60 * 20;
        }
    }

    public double getTargetAltitude() {
        if (this.level().isClientSide) {
            return this.entityData.get(TARGET_ALTITUDE_DATA);
        }
        return this.targetAltitude;
    }

    public void setTargetAltitude(double altitude) {
        this.targetAltitude = Math.max(100.0, Math.min(350.0, altitude));
        this.entityData.set(TARGET_ALTITUDE_DATA, (float) this.targetAltitude);
    }

    public void updateRouteInFlight(BlockPos newTarget, List<BlockPos> newWaypoints) {
        this.entityData.set(TARGET_POS, newTarget);
        this.setWaypoints(newWaypoints);
        this.patrolInitialized = false;

        if (this.flightPhase == FlightPhase.PATROLLING || this.flightPhase == FlightPhase.CRUISING) {
            transitionToPhase(FlightPhase.CRUISING);
        }
    }
}
