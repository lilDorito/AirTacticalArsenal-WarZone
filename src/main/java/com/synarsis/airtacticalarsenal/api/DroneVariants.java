package com.synarsis.airtacticalarsenal.api;

import com.synarsis.airtacticalarsenal.entity.OrlanEntity;
import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public final class DroneVariants {
    private static final Map<String, StrikeFactory> STRIKE = new ConcurrentHashMap<>();
    private static final Map<String, ReconFactory> RECON = new ConcurrentHashMap<>();
    private static final Map<String, String> NAMES = new ConcurrentHashMap<>();
    private static final Map<String, LauncherInteraction> INTERACTIONS = new ConcurrentHashMap<>();
    private static final Map<String, UavWeight> ITEM_WEIGHTS = new ConcurrentHashMap<>();
    private static final Map<EntityType<?>, UavWeight> ENTITY_WEIGHTS = new ConcurrentHashMap<>();
    private static final Map<String, String> ENTITY_TYPES = new ConcurrentHashMap<>();

    private DroneVariants() {
    }

    @FunctionalInterface
    public interface StrikeFactory {
        ShahedEntity create(ServerLevel level, Vec3 launchPos, BlockPos target, String diveMode, float launcherYaw);
    }

    @FunctionalInterface
    public interface ReconFactory {
        OrlanEntity create(ServerLevel level, Vec3 launchPos, BlockPos target, float launcherYaw, BlockPos launcherPos, ItemStack source);
    }

    @FunctionalInterface
    public interface LauncherInteraction {
        InteractionResult interact(Entity launcher, Player player, InteractionHand hand);
    }

    public static void registerName(Item item, String translationKey) {
        NAMES.put(key(item), translationKey);
    }

    public static void registerLauncherInteraction(Item item, LauncherInteraction interaction) {
        INTERACTIONS.put(key(item), interaction);
    }

    public static String nameKey(String kind, String fallback) {
        String name = kind == null || kind.isEmpty() ? null : NAMES.get(kind);
        return name != null ? name : fallback;
    }

    public static LauncherInteraction interaction(String kind) {
        return kind == null || kind.isEmpty() ? null : INTERACTIONS.get(kind);
    }

    public static void registerStrike(Item item, StrikeFactory factory) {
        STRIKE.put(key(item), factory);
    }

    public static void registerRecon(Item item, ReconFactory factory) {
        RECON.put(key(item), factory);
    }

    public static void registerWeight(Item item, EntityType<?> type, UavWeight weight) {
        ITEM_WEIGHTS.put(key(item), weight);
        ENTITY_WEIGHTS.put(type, weight);
        ENTITY_KINDS.put(type, key(item));
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (typeId != null) {
            ENTITY_TYPES.put(key(item), typeId.toString());
        }
    }

    private static final Map<EntityType<?>, String> ENTITY_KINDS = new ConcurrentHashMap<>();
    private static final java.util.Set<String> JET_BLAST = ConcurrentHashMap.newKeySet();

    public static void registerJetBlast(Item item) {
        JET_BLAST.add(key(item));
    }

    public static boolean jetBlast(String kind) {
        return kind != null && JET_BLAST.contains(kind);
    }

    private static final java.util.Set<String> INTERCEPTORS = ConcurrentHashMap.newKeySet();

    public static void registerInterceptor(Item item) {
        INTERCEPTORS.add(key(item));
    }

    public static boolean interceptor(String kind) {
        return kind != null && INTERCEPTORS.contains(kind);
    }
    private static final Map<String, java.util.function.DoubleSupplier> ROUTE_SCALES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> SKINS = new ConcurrentHashMap<>();

    public static void registerRouteScale(Item item, java.util.function.DoubleSupplier scale) {
        ROUTE_SCALES.put(key(item), scale);
    }

    public static double routeScale(String kind) {
        java.util.function.DoubleSupplier scale = kind == null || kind.isEmpty() ? null : ROUTE_SCALES.get(kind);
        if (scale == null) {
            return 1.0;
        }
        double value = scale.getAsDouble();
        return value > 0.0 ? value : 1.0;
    }

    public static Map<String, Double> routeScales() {
        Map<String, Double> scales = new java.util.HashMap<>();
        for (String kind : ROUTE_SCALES.keySet()) {
            scales.put(kind, routeScale(kind));
        }
        return scales;
    }

    public static String kindOf(Entity drone) {
        return drone == null ? "" : ENTITY_KINDS.getOrDefault(drone.getType(), "");
    }

    public static void registerSkins(Item item, int count) {
        if (count > 1) {
            SKINS.put(key(item), count);
        }
    }

    public static int skinCount(String kind) {
        return kind == null || kind.isEmpty() ? 1 : SKINS.getOrDefault(kind, 1);
    }

    private static final Map<String, net.minecraft.world.phys.Vec3> RAIL_MOUNTS = new ConcurrentHashMap<>();

    public static void registerRailMount(Item item, UavWeight launcherWeight, net.minecraft.world.phys.Vec3 mount) {
        RAIL_MOUNTS.put(key(item) + "@" + launcherWeight.name(), mount);
    }

    public static net.minecraft.world.phys.Vec3 railMount(String kind, UavWeight launcherWeight) {
        return kind == null || kind.isEmpty() ? null : RAIL_MOUNTS.get(kind + "@" + launcherWeight.name());
    }

    public static java.util.Collection<String> registeredEntityTypeKeys() {
        return java.util.Collections.unmodifiableCollection(new java.util.LinkedHashSet<>(ENTITY_TYPES.values()));
    }

    public static String entityTypeKey(String kind) {
        return kind == null || kind.isEmpty() ? "" : ENTITY_TYPES.getOrDefault(kind, "");
    }

    public static UavWeight weightOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        UavWeight weight = ITEM_WEIGHTS.get(key(stack.getItem()));
        if (weight != null) {
            return weight;
        }
        if (stack.getItem() instanceof com.synarsis.airtacticalarsenal.item.ShahedItem) {
            return UavWeight.HEAVY;
        }
        if (stack.getItem() instanceof com.synarsis.airtacticalarsenal.item.OrlanItem) {
            return UavWeight.LIGHT;
        }
        if (isStrikeVariant(stack)) {
            return UavWeight.HEAVY;
        }
        if (isReconVariant(stack)) {
            return UavWeight.LIGHT;
        }
        return null;
    }

    public static UavWeight weightOf(Entity drone) {
        UavWeight weight = ENTITY_WEIGHTS.get(drone.getType());
        if (weight != null) {
            return weight;
        }
        return drone instanceof OrlanEntity ? UavWeight.LIGHT : UavWeight.HEAVY;
    }

    public static String key(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? "" : id.toString();
    }

    public static boolean isStrikeVariant(ItemStack stack) {
        return !stack.isEmpty() && STRIKE.containsKey(key(stack.getItem()));
    }

    public static boolean isReconVariant(ItemStack stack) {
        return !stack.isEmpty() && RECON.containsKey(key(stack.getItem()));
    }

    public static StrikeFactory strike(String kind) {
        return kind == null || kind.isEmpty() ? null : STRIKE.get(kind);
    }

    public static ReconFactory recon(String kind) {
        return kind == null || kind.isEmpty() ? null : RECON.get(kind);
    }

    public static ItemStack stackOf(String kind) {
        if (kind == null || kind.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation id = ResourceLocation.tryParse(kind);
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
