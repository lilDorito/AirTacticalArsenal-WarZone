package com.synarsis.airtacticalarsenal.entity;

import com.synarsis.airtacticalarsenal.api.DroneVariants;
import com.synarsis.airtacticalarsenal.api.UavWeight;
import com.synarsis.airtacticalarsenal.compat.CreateCompat;
import com.synarsis.airtacticalarsenal.item.ModItems;
import com.synarsis.airtacticalarsenal.item.OrlanItem;
import com.synarsis.airtacticalarsenal.item.ShahedItem;
import com.synarsis.airtacticalarsenal.item.UavTabletItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class UavLauncherEntity extends Entity implements GeoEntity {
    public static final int ROLE_NONE = 0;
    public static final int ROLE_STRIKE = 1;
    public static final int ROLE_RECON = 2;

    private static final EntityDataAccessor<Boolean> DATA_HAS_DRONE = SynchedEntityData
            .defineId(UavLauncherEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_LAUNCHING = SynchedEntityData
            .defineId(UavLauncherEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_SHAHED_COLOR = SynchedEntityData
            .defineId(UavLauncherEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_ENGINE_SOUND = SynchedEntityData
            .defineId(UavLauncherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_DRONE_KIND = SynchedEntityData
            .defineId(UavLauncherEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_DRONE_ROLE = SynchedEntityData
            .defineId(UavLauncherEntity.class, EntityDataSerializers.INT);

    private static final int LAUNCH_DELAY_TICKS = 100;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    private UUID storedDroneUuid;
    private CompoundTag storedVariant = null;
    private BlockPos targetPos = null;
    private String diveMode = "low";
    private String shahedColor = "white";
    private List<BlockPos> routeWaypoints = null;
    private int launchAltitude = 0;
    private List<BlockPos> savedRoute = null;
    private int launchTimer = 0;
    private Player launchingPlayer = null;

    public boolean isLegitSpawn = false;

    protected UavLauncherEntity(EntityType<? extends UavLauncherEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public abstract UavWeight weight();

    protected abstract Item launcherItem();

    protected abstract int defaultRole();

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HAS_DRONE, false);
        this.entityData.define(DATA_IS_LAUNCHING, false);
        this.entityData.define(DATA_SHAHED_COLOR, "white");
        this.entityData.define(DATA_ENGINE_SOUND, 0);
        this.entityData.define(DATA_DRONE_KIND, "");
        this.entityData.define(DATA_DRONE_ROLE, ROLE_NONE);
    }

    public String getDroneKind() {
        return this.entityData.get(DATA_DRONE_KIND);
    }

    private void setDroneKind(String kind) {
        this.entityData.set(DATA_DRONE_KIND, kind == null ? "" : kind);
    }

    public int getDroneRole() {
        return this.entityData.get(DATA_DRONE_ROLE);
    }

    public boolean hasDrone() {
        return this.entityData.get(DATA_HAS_DRONE);
    }

    public boolean hasStrikeDrone() {
        return hasDrone() && getDroneRole() == ROLE_STRIKE;
    }

    public boolean hasReconDrone() {
        return hasDrone() && getDroneRole() == ROLE_RECON;
    }

    public boolean servesStrike() {
        return !hasDrone() || getDroneRole() == ROLE_STRIKE;
    }

    public boolean servesRecon() {
        return !hasDrone() || getDroneRole() == ROLE_RECON;
    }

    public boolean isLaunching() {
        return this.entityData.get(DATA_IS_LAUNCHING);
    }

    public int getEngineSound() {
        return this.entityData.get(DATA_ENGINE_SOUND);
    }

    public String getShahedColor() {
        return this.entityData.get(DATA_SHAHED_COLOR);
    }

    public Component storedDroneName() {
        String fallback = getDroneRole() == ROLE_STRIKE ? "drone.ata.shahed" : "drone.ata.orlan";
        return Component.translatable(DroneVariants.nameKey(getDroneKind(), fallback));
    }

    public void setStoredDroneStack(ItemStack stack) {
        if (hasDrone() && !getDroneKind().isEmpty() && !stack.isEmpty()) {
            storedVariant = stack.copyWithCount(1).save(new CompoundTag());
        }
    }

    public ItemStack getStoredDroneStack() {
        if (!getDroneKind().isEmpty() && storedVariant != null) {
            ItemStack stack = ItemStack.of(storedVariant);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        ItemStack variant = DroneVariants.stackOf(getDroneKind());
        if (!variant.isEmpty()) {
            return variant;
        }
        return new ItemStack(getDroneRole() == ROLE_STRIKE ? ModItems.SHAHED.get() : ModItems.ORLAN.get());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("LegitSpawn")) {
            this.isLegitSpawn = tag.getBoolean("LegitSpawn");
        }
        if (tag.hasUUID("StoredDrone")) {
            storedDroneUuid = tag.getUUID("StoredDrone");
            this.entityData.set(DATA_HAS_DRONE, true);
            this.entityData.set(DATA_DRONE_ROLE, tag.contains("DroneRole") ? tag.getInt("DroneRole") : defaultRole());
        }
        if (tag.contains("TargetX")) {
            targetPos = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        }
        if (tag.contains("DiveMode")) {
            diveMode = tag.getString("DiveMode");
        }
        if (tag.contains("ShahedColor")) {
            shahedColor = tag.getString("ShahedColor");
            this.entityData.set(DATA_SHAHED_COLOR, shahedColor);
        }
        setDroneKind(tag.getString("DroneKind"));
        storedVariant = tag.contains("DroneStack") ? tag.getCompound("DroneStack") : null;
        savedRoute = readRoute(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("LegitSpawn", this.isLegitSpawn);
        if (storedDroneUuid != null) {
            tag.putUUID("StoredDrone", storedDroneUuid);
            tag.putInt("DroneRole", getDroneRole());
        }
        if (targetPos != null) {
            tag.putInt("TargetX", targetPos.getX());
            tag.putInt("TargetY", targetPos.getY());
            tag.putInt("TargetZ", targetPos.getZ());
        }
        tag.putString("DiveMode", diveMode);
        tag.putString("ShahedColor", shahedColor);
        tag.putString("DroneKind", getDroneKind());
        if (storedVariant != null) {
            tag.put("DroneStack", storedVariant.copy());
        }
        if (savedRoute != null && !savedRoute.isEmpty()) {
            ListTag routeTag = new ListTag();
            for (BlockPos wp : savedRoute) {
                CompoundTag wpTag = new CompoundTag();
                wpTag.putInt("x", wp.getX());
                wpTag.putInt("y", wp.getY());
                wpTag.putInt("z", wp.getZ());
                routeTag.add(wpTag);
            }
            tag.put("SavedRoute", routeTag);
        }
    }

    private static List<BlockPos> readRoute(CompoundTag tag) {
        if (!tag.contains("SavedRoute")) {
            return null;
        }
        ListTag routeTag = tag.getList("SavedRoute", 10);
        List<BlockPos> route = new ArrayList<>();
        for (int i = 0; i < routeTag.size(); i++) {
            CompoundTag wp = routeTag.getCompound(i);
            route.add(new BlockPos(wp.getInt("x"), wp.getInt("y"), wp.getInt("z")));
        }
        return route.isEmpty() ? null : route;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && !this.isLegitSpawn) {
            this.discard();
            return;
        }

        if (CreateCompat.isAnyCreateLoaded()) {
            setDeltaMovement(Vec3.ZERO);
        }

        super.tick();

        if (!isNoGravity()) {
            setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
        }
        move(MoverType.SELF, getDeltaMovement());
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
        }

        if (level().isClientSide && isLaunching()) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.synarsis.airtacticalarsenal.client.sound.LauncherSpinUpSound.ensure(this));
        }

        if (launchTimer > 0 && !level().isClientSide) {
            launchTimer--;
            if (launchTimer == 0 && level() instanceof ServerLevel serverLevel) {
                actuallyLaunchStrike(serverLevel);
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (CreateCompat.isCreateFakePlayer(player)) return InteractionResult.FAIL;
        ItemStack held = player.getItemInHand(hand);

        if (held.getItem() instanceof UavTabletItem) {
            if (!level().isClientSide) {
                linkTablet(player, held);
            }
            return InteractionResult.SUCCESS;
        }

        if (!(level() instanceof ServerLevel)) {
            return InteractionResult.PASS;
        }

        if (hasReconDrone() && !(player.isShiftKeyDown() && held.isEmpty())) {
            DroneVariants.LauncherInteraction interaction = DroneVariants.interaction(getDroneKind());
            if (interaction != null) {
                InteractionResult result = interaction.interact(this, player, hand);
                if (result != InteractionResult.PASS) {
                    return result;
                }
            }
        }

        int role = roleOf(held);
        if (role != ROLE_NONE) {
            if (hasDrone()) {
                player.displayClientMessage(Component.translatable("message.ata.launcher.drone_already_mounted"), true);
                return InteractionResult.FAIL;
            }
            UavWeight droneWeight = DroneVariants.weightOf(held);
            if (droneWeight != weight()) {
                player.displayClientMessage(Component.translatable("message.ata.launcher.wrong_weight",
                        held.getHoverName(), Component.translatable(droneWeight == UavWeight.HEAVY
                                ? "item.ata.heavy_uav_launcher" : "item.ata.light_uav_launcher")), true);
                return InteractionResult.FAIL;
            }
            mount(held, role);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            player.displayClientMessage(Component.translatable("message.ata.uav_launcher.mounted", storedDroneName()), true);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (player.isShiftKeyDown() && held.isEmpty() && hasDrone() && !isLaunching()) {
            Component name = storedDroneName();
            ItemStack stack = droneStackForDrop();
            if (!player.addItem(stack)) {
                spawnAtLocation(stack);
            }
            clearStoredDrone();
            targetPos = null;
            player.displayClientMessage(Component.translatable("message.ata.uav_launcher.dismounted", name), true);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return InteractionResult.PASS;
    }

    private void linkTablet(Player player, ItemStack held) {
        if (UavTabletItem.addLinkedLauncher(held, this)) {
            player.displayClientMessage(Component.translatable("message.ata.uav_launcher.linked",
                    UavTabletItem.getLinkedLaunchers(held).size(), UavTabletItem.MAX_LAUNCHERS), true);
        } else if (UavTabletItem.getLinkedLaunchers(held).size() >= UavTabletItem.MAX_LAUNCHERS) {
            player.displayClientMessage(Component.translatable("message.ata.launcher.max_linked", UavTabletItem.MAX_LAUNCHERS), true);
        } else {
            player.displayClientMessage(Component.translatable("message.ata.launcher.already_linked"), true);
        }
    }

    private static int roleOf(ItemStack held) {
        if (held.getItem() instanceof ShahedItem || DroneVariants.isStrikeVariant(held)) {
            return ROLE_STRIKE;
        }
        if (held.getItem() instanceof OrlanItem || DroneVariants.isReconVariant(held)) {
            return ROLE_RECON;
        }
        return ROLE_NONE;
    }

    private void mount(ItemStack held, int role) {
        boolean variant = !(held.getItem() instanceof ShahedItem) && !(held.getItem() instanceof OrlanItem);
        storedDroneUuid = UUID.randomUUID();
        setDroneKind(variant ? DroneVariants.key(held.getItem()) : "");
        storedVariant = variant ? held.copyWithCount(1).save(new CompoundTag()) : null;
        this.shahedColor = "white";
        CompoundTag itemTag = held.getTag();
        if (role == ROLE_STRIKE && itemTag != null) {
            if (itemTag.contains("TargetX")) {
                this.targetPos = new BlockPos(itemTag.getInt("TargetX"), itemTag.getInt("TargetY"), itemTag.getInt("TargetZ"));
                this.diveMode = itemTag.getString("DiveMode");
                if (this.diveMode.isEmpty()) {
                    this.diveMode = "low";
                }
            }
            if (itemTag.contains("ShahedColor")) {
                this.shahedColor = itemTag.getString("ShahedColor");
            }
        }
        this.entityData.set(DATA_SHAHED_COLOR, this.shahedColor);
        this.entityData.set(DATA_DRONE_ROLE, role);
        this.entityData.set(DATA_HAS_DRONE, true);
    }

    public void startLaunchSequence(Player player, BlockPos target, String mode) {
        if (launchTimer > 0 || !hasStrikeDrone()) {
            return;
        }
        this.targetPos = target;
        this.diveMode = mode;
        this.launchingPlayer = player;
        this.launchTimer = LAUNCH_DELAY_TICKS;
        this.entityData.set(DATA_ENGINE_SOUND, this.random.nextInt(2));
        this.entityData.set(DATA_IS_LAUNCHING, true);
    }

    public void startLaunchSequenceWithRoute(Player player, BlockPos target, String mode, List<BlockPos> waypoints) {
        this.routeWaypoints = waypoints != null ? new ArrayList<>(waypoints) : null;
        this.launchAltitude = 0;
        startLaunchSequence(player, target, mode);
    }

    public void startLaunchSequenceWithRoute(Player player, BlockPos target, String mode, List<BlockPos> waypoints,
                                             int altitude) {
        this.routeWaypoints = waypoints != null ? new ArrayList<>(waypoints) : null;
        this.launchAltitude = altitude;
        startLaunchSequence(player, target, mode);
    }

    private void actuallyLaunchStrike(ServerLevel serverLevel) {
        if (targetPos == null || !hasStrikeDrone()) {
            this.entityData.set(DATA_IS_LAUNCHING, false);
            return;
        }

        float launcherYaw = this.getYRot();
        Vec3 launchDirection = Vec3.directionFromRotation(0, launcherYaw).normalize();
        Vec3 launchPos = railLaunchPos();

        DroneVariants.StrikeFactory factory = DroneVariants.strike(getDroneKind());
        ShahedEntity drone = factory != null
                ? factory.create(serverLevel, launchPos, targetPos, diveMode, launcherYaw)
                : new ShahedEntity(serverLevel, launchPos, targetPos, diveMode, launcherYaw);
        if (launchingPlayer != null) {
            drone.setOwner(launchingPlayer);
        }
        drone.setShahedColor(this.shahedColor);
        drone.setEngineSound(getEngineSound());
        if (this.routeWaypoints != null && !this.routeWaypoints.isEmpty()) {
            drone.setWaypoints(this.routeWaypoints);
        }
        if (this.launchAltitude > 0) {
            drone.setTargetAltitude(this.launchAltitude);
        }
        drone.setRailPitch(railPitchDeg());
        drone.isLegitSpawn = true;
        serverLevel.addFreshEntity(drone);

        clearStoredDrone();
        targetPos = null;
        launchingPlayer = null;
        this.routeWaypoints = null;
        this.launchAltitude = 0;
        this.entityData.set(DATA_IS_LAUNCHING, false);
    }

    public Vec3 railLaunchPos() {
        boolean heavy = weight() == UavWeight.HEAVY;
        Vec3 mount = DroneVariants.railMount(getDroneKind(), weight());
        double lift = 0.0;
        if (mount == null) {
            if (getDroneKind().isEmpty()) {
                mount = heavy ? NATIVE_SHAHED_MOUNT : NATIVE_ORLAN_MOUNT;
                lift = heavy ? 0.0 : NATIVE_ORLAN_RENDER_DROP;
            } else {
                Vec3 forward = Vec3.directionFromRotation(0, this.getYRot()).normalize();
                return this.position().add(0, heavy ? 0.9 : 0.85, 0).add(forward.scale(heavy ? 0.4 : 0.65));
            }
        }
        Vec3 pivot = heavy ? HEAVY_RAIL_PIVOT : LIGHT_RAIL_PIVOT;
        double angle = Math.toRadians(railPitchDeg());
        Vec3 rel = mount.subtract(pivot);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        Vec3 rotated = new Vec3(rel.x, rel.y * cos + rel.z * sin, -rel.y * sin + rel.z * cos);
        double scale = heavy ? HEAVY_LAUNCHER_SCALE : 1.0;
        Vec3 local = pivot.add(rotated).scale(scale);
        double yaw = Math.toRadians(-this.getYRot());
        double wx = local.x * Math.cos(yaw) + local.z * Math.sin(yaw);
        double wz = -local.x * Math.sin(yaw) + local.z * Math.cos(yaw);
        return this.position().add(wx, local.y + lift, wz);
    }

    public float railPitchDeg() {
        return weight() == UavWeight.HEAVY ? 13.0f : 9.75f;
    }

    private static final double HEAVY_LAUNCHER_SCALE = 1.2;
    private static final Vec3 HEAVY_RAIL_PIVOT = new Vec3(-0.02405 / 16.0, 21.20946 / 16.0, -10.91532 / 16.0);
    private static final Vec3 LIGHT_RAIL_PIVOT = new Vec3(-2.9625 / 16.0, 12.24982 / 16.0, -17.88756 / 16.0);
    private static final Vec3 NATIVE_SHAHED_MOUNT = new Vec3(0.0, 17.3 / 16.0, 0.7 / 16.0);
    private static final Vec3 NATIVE_ORLAN_MOUNT = new Vec3(0.0, -11.02518 / 16.0, -20.0875 / 16.0);
    private static final double NATIVE_ORLAN_RENDER_DROP = 1.5;

    public void removeDrone() {
        clearStoredDrone();
    }

    public void clearDroneAfterLaunch() {
        clearStoredDrone();
        targetPos = null;
    }

    private void clearStoredDrone() {
        storedDroneUuid = null;
        storedVariant = null;
        this.shahedColor = "white";
        this.entityData.set(DATA_SHAHED_COLOR, "white");
        setDroneKind("");
        this.entityData.set(DATA_DRONE_ROLE, ROLE_NONE);
        this.entityData.set(DATA_HAS_DRONE, false);
    }

    private ItemStack droneStackForDrop() {
        ItemStack stack = getStoredDroneStack();
        if (getDroneRole() == ROLE_STRIKE) {
            CompoundTag tag = stack.getOrCreateTag();
            if (targetPos != null) {
                tag.putInt("TargetX", targetPos.getX());
                tag.putInt("TargetY", targetPos.getY());
                tag.putInt("TargetZ", targetPos.getZ());
                tag.putString("DiveMode", diveMode);
            }
            tag.putString("ShahedColor", this.shahedColor);
        }
        return stack;
    }

    private void dropStoredDroneAsItem() {
        if (!hasDrone()) {
            return;
        }
        spawnAtLocation(droneStackForDrop());
        clearStoredDrone();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !isAlive()) {
            return false;
        }
        if (CreateCompat.isCreateDamageSource(source)) {
            return false;
        }
        dropStoredDroneAsItem();
        spawnAtLocation(new ItemStack(launcherItem()));
        discard();
        return true;
    }

    @Override
    public void remove(RemovalReason reason) {
        dropStoredDroneAsItem();
        super.remove(reason);
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public String getDiveMode() {
        return diveMode;
    }

    public List<BlockPos> getSavedRoute() {
        return savedRoute;
    }

    public void setSavedRoute(List<BlockPos> route) {
        this.savedRoute = route != null && !route.isEmpty() ? new ArrayList<>(route) : null;
    }

    public boolean hasSavedRoute() {
        return savedRoute != null && !savedRoute.isEmpty();
    }
}
