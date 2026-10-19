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
