package com.synarsis.airtacticalarsenal.radar;

import com.synarsis.airtacticalarsenal.compat.SuperbWarfareCompat;
import com.synarsis.airtacticalarsenal.entity.IskanderEntity;
import com.synarsis.airtacticalarsenal.entity.OrlanEntity;
import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.scores.Team;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class RadarContacts {

    private static final Map<EntityType<?>, Byte> TYPE_CACHE = new ConcurrentHashMap<>();
    private static long cacheStamp;

    private RadarContacts() {}

    public static byte classify(Entity e) {
        if (e == null || e.isRemoved()) return -1;
        if (e instanceof ShahedEntity shahed) return isShahedAirborne(shahed) ? SuperbWarfareCompat.TYPE_SW_DRONE : -1;
        if (e instanceof OrlanEntity orlan) return isOrlanAirborne(orlan) ? SuperbWarfareCompat.TYPE_SW_DRONE : -1;
        if (e instanceof IskanderEntity) return SuperbWarfareCompat.TYPE_MISSILE;
        if (e instanceof Player || e instanceof ItemEntity || e instanceof ExperienceOrb) return -1;
        long now = System.currentTimeMillis();
        if (now - cacheStamp > 10_000L) {
            cacheStamp = now;
            TYPE_CACHE.clear();
        }
        Byte cached = TYPE_CACHE.get(e.getType());
        if (cached != null) return cached;
        byte type = SuperbWarfareCompat.getRadarType(e);
        TYPE_CACHE.put(e.getType(), type);
        return type;
    }

    private static boolean isShahedAirborne(ShahedEntity shahed) {
        ShahedEntity.FlightPhase phase = shahed.getFlightPhase();
        return phase == ShahedEntity.FlightPhase.LAUNCHING || phase == ShahedEntity.FlightPhase.CLIMBING
                || phase == ShahedEntity.FlightPhase.CRUISING || phase == ShahedEntity.FlightPhase.DIVING
                || shahed.isCrashing();
    }

    private static boolean isOrlanAirborne(OrlanEntity orlan) {
        return orlan.getFlightPhase() != OrlanEntity.FlightPhase.LANDED;
    }

    public static boolean isFriendly(Team team, Entity viewer, Entity e) {
        if (team == null && viewer == null) return false;
        if (onTeam(team, e)) return true;
        for (Entity passenger : e.getIndirectPassengers()) {
            if (passenger == viewer || onTeam(team, passenger)) return true;
        }
        if (e instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner != null && (owner == viewer || onTeam(team, owner))) return true;
        }
        return false;
    }

    public static boolean isFriendlyToOwner(ServerLevel level, String ownerName, Entity e) {
        if (ownerName == null || ownerName.isEmpty() || e == null) return false;
        Team team = level.getScoreboard().getPlayersTeam(ownerName);
        MinecraftServer server = level.getServer();
        ServerPlayer owner = server.getPlayerList().getPlayerByName(ownerName);
        if (isFriendly(team, owner, e)) return true;
        if (isOwnersName(ownerName, team, level, e)) return true;
        for (Entity passenger : e.getIndirectPassengers()) {
            if (isOwnersName(ownerName, team, level, passenger)) return true;
        }
        Entity shooter = e instanceof Projectile projectile ? projectile.getOwner() : null;
        if (shooter != null) {
            if (isOwnersName(ownerName, team, level, shooter)) return true;
            for (Entity passenger : shooter.getIndirectPassengers()) {
                if (isOwnersName(ownerName, team, level, passenger)) return true;
            }
            Entity root = shooter.getRootVehicle();
            if (root != shooter && isFriendly(team, owner, root)) return true;
        }
        UUID ownerId = storedOwner(e);
        if (ownerId != null) {
            String name = nameOf(server, ownerId);
            if (name != null && sameSide(ownerName, team, level, name)) return true;
        }
        return false;
    }

    public static void forEachInRange(ServerLevel level, Vec3 center, double range, double minY, Consumer<Entity> action) {
        double rangeSq = range * range;
        for (Entity e : level.getAllEntities()) {
            if (e == null || e.isRemoved()) continue;
            double dx = e.getX() - center.x;
            double dz = e.getZ() - center.z;
            if (dx * dx + dz * dz > rangeSq) continue;
            if (e.getY() < minY) continue;
            action.accept(e);
        }
    }

    private static boolean isOwnersName(String ownerName, Team team, ServerLevel level, Entity e) {
        if (!(e instanceof Player player)) return false;
        return sameSide(ownerName, team, level, player.getScoreboardName());
    }

    private static boolean sameSide(String ownerName, Team team, ServerLevel level, String name) {
        if (ownerName.equals(name)) return true;
        if (team == null) return false;
        Team other = level.getScoreboard().getPlayersTeam(name);
        return other != null && other.isAlliedTo(team);
    }

    private static UUID storedOwner(Entity e) {
        if (e instanceof OrlanEntity orlan) {
            try {
                String s = orlan.getOwnerUUIDString();
                if (s != null && !s.isEmpty()) return UUID.fromString(s);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (e instanceof Projectile && ((Projectile) e).getOwner() == null) {
            try {
                CompoundTag tag = e.saveWithoutId(new CompoundTag());
                if (tag.hasUUID("Owner")) return tag.getUUID("Owner");
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String nameOf(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) return online.getScoreboardName();
        if (server.getProfileCache() == null) return null;
        Optional<GameProfile> profile = server.getProfileCache().get(id);
        return profile.map(GameProfile::getName).orElse(null);
    }

    private static boolean onTeam(Team team, Entity e) {
        return team != null && e != null && e.getTeam() != null && e.getTeam().isAlliedTo(team);
    }
}
