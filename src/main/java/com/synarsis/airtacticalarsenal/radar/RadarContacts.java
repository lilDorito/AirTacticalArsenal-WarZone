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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private static boolean onTeam(Team team, Entity e) {
        return team != null && e != null && e.getTeam() != null && e.getTeam().isAlliedTo(team);
    }
}
