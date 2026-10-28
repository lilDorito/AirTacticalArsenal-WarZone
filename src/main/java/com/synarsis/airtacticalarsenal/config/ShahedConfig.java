package com.synarsis.airtacticalarsenal.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ShahedConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue TARGET_Y_LEVEL;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FORBIDDEN_ZONES;
    public static final ForgeConfigSpec.BooleanValue BLACKLIST_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHITELIST_ZONES;
    public static final ForgeConfigSpec.BooleanValue WHITELIST_ENABLED;
    public static final ForgeConfigSpec.BooleanValue EXPLOSION_PARTICLES_ENABLED;
    public static final ForgeConfigSpec.IntValue EXPLOSION_PARTICLE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue EXPLOSION_BREAKS_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue CUSTOM_EXPLOSION_SOUNDS;
    public static final ForgeConfigSpec.BooleanValue LOGGING_ENABLED;

    public static final ForgeConfigSpec.DoubleValue SHAHED_CRUISE_SPEED;
    public static final ForgeConfigSpec.DoubleValue SHAHED_DIVE_SPEED;

    public static final ForgeConfigSpec.BooleanValue SHAHED_SHOOTDOWN_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SHAHED_MAX_HEALTH;
    public static final ForgeConfigSpec.DoubleValue SHAHED_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SHAHED_ENGINE_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SHAHED_WARHEAD_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SHAHED_MIN_CALIBER_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue SHAHED_SMOKE_ON_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue SHAHED_CRITICAL_DETONATION;
    public static final ForgeConfigSpec.BooleanValue DRONE_CRASH_ON_SHOOTDOWN;
    public static final ForgeConfigSpec.BooleanValue SHAHED_CRASH_DETONATION;
    public static final ForgeConfigSpec.DoubleValue AIRBURST_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue FLAK_HIT_DAMAGE_PERCENT;
    public static final ForgeConfigSpec.DoubleValue SHOT_DETONATION_CHANCE;
    public static final ForgeConfigSpec.DoubleValue ISKANDER_LAUNCH_SPEED;
    public static final ForgeConfigSpec.DoubleValue ISKANDER_CRUISE_SPEED;
    public static final ForgeConfigSpec.DoubleValue ISKANDER_TERMINAL_SPEED;
    public static final ForgeConfigSpec.IntValue ISKANDER_PREPARE_TICKS;
    public static final ForgeConfigSpec.IntValue ISKANDER_LAUNCH_TICKS;
    public static final ForgeConfigSpec.DoubleValue ISKANDER_KVO_DEVIATION;
    public static final ForgeConfigSpec.IntValue ISKANDER_MIN_DISTANCE;
    public static final ForgeConfigSpec.IntValue ISKANDER_MAX_RANGE;
    public static final ForgeConfigSpec.IntValue ISKANDER_MAX_FLIGHT_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue ISKANDER_DIVE_ANGLE;
    public static final ForgeConfigSpec.DoubleValue ISKANDER_CRATER_RADIUS;
    public static final ForgeConfigSpec.DoubleValue SHAHED_CRATER_RADIUS;
    public static final ForgeConfigSpec.IntValue SHAHED_MAX_ROUTE_DISTANCE;
    public static final ForgeConfigSpec.IntValue TABLET_LAUNCHER_RANGE;
    public static final ForgeConfigSpec.IntValue DRONE_CAMERA_CHUNK_RADIUS;
    public static final ForgeConfigSpec.IntValue ORLAN_MAX_ROUTE_DISTANCE;
    public static final ForgeConfigSpec.IntValue ORLAN_FLIGHT_TIME_MINUTES;

    public static final ForgeConfigSpec.DoubleValue ISKANDER_PLAYER_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ISKANDER_ENTITY_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SHAHED_EXPLOSION_PLAYER_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SHAHED_EXPLOSION_ENTITY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SHAHED_CORE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SHAHED_CORE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue SHAHED_DAMAGE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue ISKANDER_CORE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue ISKANDER_CORE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue ISKANDER_DAMAGE_RADIUS;
    public static final ForgeConfigSpec.BooleanValue VANILLA_EXPLOSION_HURTS_ENTITIES;
    public static final ForgeConfigSpec.IntValue RADAR_MIN_ALTITUDE;
    public static final ForgeConfigSpec.IntValue RADAR_RANGE;
    public static final ForgeConfigSpec.IntValue CONFIG_VERSION;
    public static final ForgeConfigSpec.BooleanValue RADAR_REQUIRES_OPEN_SKY;
    public static final ForgeConfigSpec.BooleanValue VEHICLE_RADARS;
    public static final ForgeConfigSpec.BooleanValue RADAR_DARK_BACKGROUND;
    public static final ForgeConfigSpec.IntValue MAP_REFRESH_MINUTES;
    public static final ForgeConfigSpec.BooleanValue MAP_READ_SAVED_CHUNKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RADAR_PLANE_IDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RADAR_HELICOPTER_IDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RADAR_DRONE_IDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RADAR_MISSILE_IDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RADAR_EXCLUDED_IDS;

    public static boolean isInForbiddenZone(int x, int z) {
        boolean inWhitelist = false;
        boolean inBlacklist = false;

        if (WHITELIST_ENABLED.get()) {
            for (String zoneStr : WHITELIST_ZONES.get()) {
                ForbiddenZone zone = ForbiddenZone.parse(zoneStr);
                if (zone != null && zone.contains(x, z)) {
                    inWhitelist = true;
                    break;
                }
            }
            if (!inWhitelist) {
                return true;
            }
        }

        if (BLACKLIST_ENABLED.get()) {
            for (String zoneStr : FORBIDDEN_ZONES.get()) {
                ForbiddenZone zone = ForbiddenZone.parse(zoneStr);
                if (zone != null && zone.contains(x, z)) {
                    inBlacklist = true;
                    break;
                }
            }
            if (inBlacklist) {
                return true;
            }
        }

        return false;
    }

    public static boolean areExplosionParticlesEnabled() {
        return EXPLOSION_PARTICLES_ENABLED.get();
    }

    public static void setExplosionParticlesEnabled(boolean enabled) {
        EXPLOSION_PARTICLES_ENABLED.set(enabled);
    }

    public static int getParticleMultiplier() {
        return EXPLOSION_PARTICLE_MULTIPLIER.get();
    }

    public static void setParticleMultiplier(int multiplier) {
        EXPLOSION_PARTICLE_MULTIPLIER.set(Math.max(0, Math.min(500, multiplier)));
    }

    public static boolean doExplosionsBreakBlocks() {
        return EXPLOSION_BREAKS_BLOCKS.get();
    }

    public static void setExplosionsBreakBlocks(boolean breaks) {
        EXPLOSION_BREAKS_BLOCKS.set(breaks);
    }

    public static boolean areCustomExplosionSoundsEnabled() {
        return CUSTOM_EXPLOSION_SOUNDS.get();
    }

    public static void setCustomExplosionSoundsEnabled(boolean enabled) {
        CUSTOM_EXPLOSION_SOUNDS.set(enabled);
    }

    public static boolean isLoggingEnabled() {
        return LOGGING_ENABLED.get();
    }

    public static void setLoggingEnabled(boolean enabled) {
        LOGGING_ENABLED.set(enabled);
    }

    public static double getShahedCruiseSpeed() { return SHAHED_CRUISE_SPEED.get(); }
    public static double getShahedDiveSpeed() { return SHAHED_DIVE_SPEED.get(); }

    public static boolean isShahedShootdownEnabled() { return SHAHED_SHOOTDOWN_ENABLED.get(); }
    public static double getShahedMaxHealth() { return SHAHED_MAX_HEALTH.get(); }
    public static double getShahedDamageMultiplier() { return SHAHED_DAMAGE_MULTIPLIER.get(); }
    public static double getShahedEngineDamageMultiplier() { return SHAHED_ENGINE_DAMAGE_MULTIPLIER.get(); }
    public static double getShahedWarheadDamageMultiplier() { return SHAHED_WARHEAD_DAMAGE_MULTIPLIER.get(); }
    public static double getShahedMinCaliberDamage() { return SHAHED_MIN_CALIBER_DAMAGE.get(); }
    public static boolean isShahedSmokeOnDamageEnabled() { return SHAHED_SMOKE_ON_DAMAGE.get(); }
    public static boolean isShahedCriticalDetonationEnabled() { return SHAHED_CRITICAL_DETONATION.get(); }
    public static boolean isDroneCrashOnShootdown() { return DRONE_CRASH_ON_SHOOTDOWN.get(); }
    public static boolean isShahedCrashDetonation() { return SHAHED_CRASH_DETONATION.get(); }
    public static double getAirburstDamageMultiplier() { return AIRBURST_DAMAGE_MULTIPLIER.get(); }
    public static double getFlakHitDamagePercent() { return FLAK_HIT_DAMAGE_PERCENT.get(); }
    public static double getShotDetonationChance() { return SHOT_DETONATION_CHANCE.get(); }

    public static double getIskanderLaunchSpeed() { return ISKANDER_LAUNCH_SPEED.get(); }
    public static double getIskanderCruiseSpeed() { return ISKANDER_CRUISE_SPEED.get(); }
    public static double getIskanderTerminalSpeed() { return ISKANDER_TERMINAL_SPEED.get(); }
    public static int getIskanderPrepareTicks() { return ISKANDER_PREPARE_TICKS.get(); }
    public static int getIskanderLaunchTicks() { return ISKANDER_LAUNCH_TICKS.get(); }
    public static double getIskanderKvoDeviation() { return ISKANDER_KVO_DEVIATION.get(); }
    public static int getIskanderMinDistance() { return ISKANDER_MIN_DISTANCE.get(); }
    public static int getIskanderMaxRange() { return ISKANDER_MAX_RANGE.get(); }
    public static int getIskanderMaxFlightHeight() { return ISKANDER_MAX_FLIGHT_HEIGHT.get(); }
    public static double getIskanderDiveAngle() { return ISKANDER_DIVE_ANGLE.get(); }
    public static double getIskanderCraterRadius() { return ISKANDER_CRATER_RADIUS.get(); }
    public static double getShahedCraterRadius() { return SHAHED_CRATER_RADIUS.get(); }
    public static int getShahedMaxRouteDistance() { return SHAHED_MAX_ROUTE_DISTANCE.get(); }
    public static int getTabletLauncherRange() { return TABLET_LAUNCHER_RANGE.get(); }
    public static int getDroneCameraChunkRadius() { return DRONE_CAMERA_CHUNK_RADIUS.get(); }
    public static int getOrlanMaxRouteDistance() { return ORLAN_MAX_ROUTE_DISTANCE.get(); }
    public static int getOrlanFlightTimeMinutes() { return ORLAN_FLIGHT_TIME_MINUTES.get(); }

    public static double getIskanderPlayerDamageMultiplier()  { return ISKANDER_PLAYER_DAMAGE_MULTIPLIER.get(); }
    public static double getIskanderEntityDamageMultiplier()  { return ISKANDER_ENTITY_DAMAGE_MULTIPLIER.get(); }
    public static double getShahedExplosionPlayerMultiplier() { return SHAHED_EXPLOSION_PLAYER_MULTIPLIER.get(); }
    public static double getShahedExplosionEntityMultiplier() { return SHAHED_EXPLOSION_ENTITY_MULTIPLIER.get(); }
    public static double getShahedCoreDamage()     { return SHAHED_CORE_DAMAGE.get(); }
    public static double getShahedCoreRadius()     { return SHAHED_CORE_RADIUS.get(); }
    public static double getShahedDamageRadius()   { return SHAHED_DAMAGE_RADIUS.get(); }
    public static double getIskanderCoreDamage()   { return ISKANDER_CORE_DAMAGE.get(); }
    public static double getIskanderCoreRadius()   { return ISKANDER_CORE_RADIUS.get(); }
    public static double getIskanderDamageRadius() { return ISKANDER_DAMAGE_RADIUS.get(); }
    public static boolean doesVanillaExplosionHurtEntities() { return VANILLA_EXPLOSION_HURTS_ENTITIES.get(); }

    public static float explosionFalloff(double distance, double coreDamage, double coreRadius, double damageRadius) {
        if (distance >= damageRadius) return 0.0f;
        if (distance <= coreRadius) return (float) coreDamage;
        double span = Math.max(0.001, damageRadius - coreRadius);
        return (float) (coreDamage * (1.0 - (distance - coreRadius) / span));
    }

    public static int getRadarMinAltitude() { return RADAR_MIN_ALTITUDE.get(); }
    public static int getRadarRange() { return RADAR_RANGE.get(); }
    public static boolean isRadarOpenSkyRequired() { return RADAR_REQUIRES_OPEN_SKY.get(); }
    public static boolean areVehicleRadarsEnabled() { return VEHICLE_RADARS.get(); }
    public static boolean isRadarDarkBackground() { return RADAR_DARK_BACKGROUND.get(); }
    public static int getMapRefreshMinutes() { return MAP_REFRESH_MINUTES.get(); }
    public static boolean isMapReadSavedChunks() { return MAP_READ_SAVED_CHUNKS.get(); }
    public static List<? extends String> getRadarPlaneIds()      { return RADAR_PLANE_IDS.get(); }
    public static List<? extends String> getRadarHelicopterIds() { return RADAR_HELICOPTER_IDS.get(); }
    public static List<? extends String> getRadarDroneIds()      { return RADAR_DRONE_IDS.get(); }
    public static List<? extends String> getRadarMissileIds()    { return RADAR_MISSILE_IDS.get(); }
    public static List<? extends String> getRadarExcludedIds()   { return RADAR_EXCLUDED_IDS.get(); }

    public static boolean isBlacklistEnabled() {
        return BLACKLIST_ENABLED.get();
    }

    public static void setBlacklistEnabled(boolean enabled) {
        BLACKLIST_ENABLED.set(enabled);
    }

    public static boolean isWhitelistEnabled() {
        return WHITELIST_ENABLED.get();
    }

    public static void setWhitelistEnabled(boolean enabled) {
        WHITELIST_ENABLED.set(enabled);
    }

    public static void reload() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("ata-common.toml");
        CommentedFileConfig fileConfig = CommentedFileConfig.builder(configPath).sync().build();
        fileConfig.load();
        SPEC.setConfig(fileConfig);
    }

    static {
        BUILDER.push("Target Settings");
        TARGET_Y_LEVEL = BUILDER.comment("Target Y coordinate (attack height)").defineInRange("targetYLevel", 64, -64, 320);
        BUILDER.pop();
        BUILDER.push("Blacklist Zones");
        BLACKLIST_ENABLED = BUILDER.comment("Enable blacklist (forbidden zones)").define("blacklistEnabled", false);
        BUILDER.comment("Forbidden zones for attacks", "Format: \"x1,z1,x2,z2\" where x1,z1 - first corner, x2,z2 - second corner", "Example: \"0,0,100,100\" forbids area from 0,0 to 100,100");
        FORBIDDEN_ZONES = BUILDER.defineList("forbiddenZones", Arrays.asList(), obj -> obj instanceof String);
        BUILDER.pop();
        BUILDER.push("Whitelist Zones");
        WHITELIST_ENABLED = BUILDER.comment("Enable whitelist (allowed zones)", "If enabled, attacks are ONLY allowed inside whitelist zones").define("whitelistEnabled", false);
        BUILDER.comment("Allowed zones for attacks", "Format: \"x1,z1,x2,z2\" where x1,z1 - first corner, x2,z2 - second corner", "Attacks are ONLY allowed inside these zones (if whitelist is enabled)");
        WHITELIST_ZONES = BUILDER.defineList("whitelistZones", Arrays.asList(), obj -> obj instanceof String);
        BUILDER.pop();
        BUILDER.push("Explosion Effects");
        EXPLOSION_PARTICLES_ENABLED = BUILDER.comment("Enable explosion particles").define("explosionParticlesEnabled", true);
        EXPLOSION_PARTICLE_MULTIPLIER = BUILDER.comment("Particle amount multiplier (0-500, 100 = default amount)").defineInRange("explosionParticleMultiplier", 100, 0, 500);
        EXPLOSION_BREAKS_BLOCKS = BUILDER.comment("Explosion breaks blocks").define("explosionBreaksBlocks", true);
        CUSTOM_EXPLOSION_SOUNDS = BUILDER.comment("Use custom explosion sounds").define("customExplosionSounds", true);
        BUILDER.pop();
        BUILDER.push("Debug");
        CONFIG_VERSION = BUILDER.comment("Internal: used to update old default values. Do not change.")
            .defineInRange("configVersion", 0, 0, 1000);
        LOGGING_ENABLED = BUILDER.comment("Enable launch logging to console").define("loggingEnabled", false);
        BUILDER.pop();

        BUILDER.push("Shahed Settings");
        SHAHED_CRUISE_SPEED = BUILDER.comment("Cruise speed in blocks/tick (30 blocks/sec = 1.5)").defineInRange("shahedCruiseSpeed", 1.5, 0.5, 10.0);
        SHAHED_DIVE_SPEED = BUILDER.comment("Dive speed in blocks/tick (72 blocks/sec = 3.6)").defineInRange("shahedDiveSpeed", 3.6, 1.0, 15.0);
        SHAHED_CRATER_RADIUS = BUILDER.comment(
                "Radius (blocks) of the round crater a Shahed digs (needs explosionBreaksBlocks = true).",
                "Default 5 (~9 wide, ~3-4 deep); 0 = no crater (explosion effects only).")
                .defineInRange("shahedCraterRadius", 5.0, 0.0, 20.0);
        SHAHED_MAX_ROUTE_DISTANCE = BUILDER.comment(
                "Maximum Shahed route length in blocks, measured from the launcher along all waypoints.",
                "Enforced by the server; players get this value when they join (route screen shows it).",
                "The route map grows with the larger of this and orlanMaxRouteDistance (coarser when bigger).")
                .defineInRange("shahedMaxRouteDistance", 10000, 1000, 30000);
        TABLET_LAUNCHER_RANGE = BUILDER.comment(
                "How close (in blocks) a player must be to a Strike or Recon UAV launcher to save a route",
                "or launch from a tablet. Sent to players on join.")
                .defineInRange("tabletLauncherRange", 100, 30, 250);
        DRONE_CAMERA_CHUNK_RADIUS = BUILDER.comment(
                "Minimum chunks around a drone sent to the player watching its camera (16 = 256 blocks). A player with a",
                "higher render distance gets that many, up to 32. People, mobs and vehicles are synced within the same distance.")
                .defineInRange("droneCameraChunkRadius", 16, 16, 32);
        BUILDER.pop();

        BUILDER.push("Orlan Settings");
        ORLAN_MAX_ROUTE_DISTANCE = BUILDER.comment(
                "Maximum Orlan-10 route length in blocks, measured from the launcher along all waypoints",
                "(also applies to route changes during flight). Enforced by the server, sent to players on join.",
                "Keep it reachable within orlanFlightTimeMinutes: 10,000 blocks out (~7 min at 24 b/s)",
                "plus the way back (~8.3 min at 20 b/s) is ~15 min; 30,000 blocks needs ~46 min.")
                .defineInRange("orlanMaxRouteDistance", 10000, 1000, 30000);
        ORLAN_FLIGHT_TIME_MINUTES = BUILDER.comment(
                "Orlan-10 flight time in minutes. When it runs out the drone stops its mission and flies home.",
                "Default 30: a full 10,000-block route leaves ~15 min of patrol at the last waypoint.",
                "Raise it together with orlanMaxRouteDistance (about 60 for 30,000 blocks).")
                .defineInRange("orlanFlightTimeMinutes", 30, 5, 180);
        BUILDER.pop();

        BUILDER.push("Anti-Air Defense (TacZ Integration)");
        BUILDER.comment("Settings for shooting down Shahed drones with TacZ weapons");
        SHAHED_SHOOTDOWN_ENABLED = BUILDER.comment("Enable shooting down Shaheds with TacZ weapons").define("shahedShootdownEnabled", true);
        SHAHED_MAX_HEALTH = BUILDER.comment("Maximum health of Shahed drone (100 = default)").defineInRange("shahedMaxHealth", 100.0, 10.0, 1000.0);
        SHAHED_DAMAGE_MULTIPLIER = BUILDER.comment("Global damage multiplier for all weapons (1.0 = normal)").defineInRange("shahedDamageMultiplier", 1.0, 0.1, 10.0);
        SHAHED_ENGINE_DAMAGE_MULTIPLIER = BUILDER.comment("Damage multiplier for hits to engine (rear part)").defineInRange("shahedEngineDamageMultiplier", 1.5, 1.0, 5.0);
        SHAHED_WARHEAD_DAMAGE_MULTIPLIER = BUILDER.comment("Damage multiplier for hits to warhead (front part) - may cause detonation!").defineInRange("shahedWarheadDamageMultiplier", 2.0, 1.0, 10.0);
        SHAHED_MIN_CALIBER_DAMAGE = BUILDER.comment("Minimum damage threshold - shots below this are ignored (filters weak weapons)").defineInRange("shahedMinCaliberDamage", 2.0, 0.0, 20.0);
        SHAHED_SMOKE_ON_DAMAGE = BUILDER.comment("Show smoke effects when Shahed is damaged").define("shahedSmokeOnDamage", true);
        SHAHED_CRITICAL_DETONATION = BUILDER.comment("Warhead hits at low HP can cause premature detonation").define("shahedCriticalDetonation", true);
        DRONE_CRASH_ON_SHOOTDOWN = BUILDER.comment(
                "Shot-down Shaheds and Orlans fall out of the sky instead of exploding in the air:",
                "engine hits make them glide down, a lost wing sends them into a spin, other kills make them tumble.",
                "Engine and wings also fail on their own before the drone's health runs out.")
                .define("droneCrashOnShootdown", true);
        SHAHED_CRASH_DETONATION = BUILDER.comment("A crashing Shahed's warhead explodes when it hits the ground")
                .define("shahedCrashDetonation", true);
        AIRBURST_DAMAGE_MULTIPLIER = BUILDER.comment(
                "A single hit dealing at least this many times the drone's max health (a missile, a direct flak burst)",
                "blows it apart in the air instead of bringing it down")
                .defineInRange("airburstDamageMultiplier", 3.0, 1.0, 100.0);
        FLAK_HIT_DAMAGE_PERCENT = BUILDER.comment(
                "SuperbWarfare anti-air shells and the laser tower delete any projectile they touch. Shaheds and Orlans",
                "take this much damage per touch instead (percent of max health), so it takes several hits to bring",
                "one down. Direct hits from other SuperbWarfare cannon shells (AP, HE...) always bring it down.")
                .defineInRange("flakHitDamagePercent", 15.0, 1.0, 100.0);
        SHOT_DETONATION_CHANCE = BUILDER.comment(
                "Chance (percent) that a Shahed blows up in the air the moment it is shot down, instead of falling.",
                "A Shahed that is already falling blows up when it is hit again.")
                .defineInRange("shahedShotDetonationChance", 15.0, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.push("Iskander Settings");
        ISKANDER_LAUNCH_SPEED = BUILDER.comment("Vertical speed during launch phase (blocks/tick)").defineInRange("iskanderLaunchSpeed", 2.5, 0.3, 8.0);
        ISKANDER_CRUISE_SPEED = BUILDER.comment("Horizontal speed during cruise/ballistic phase (blocks/tick, 4.0 = 80 blocks/sec)").defineInRange("iskanderCruiseSpeed", 4.0, 0.5, 30.0);
        ISKANDER_TERMINAL_SPEED = BUILDER.comment("Speed during terminal dive phase (blocks/tick)").defineInRange("iskanderTerminalSpeed", 5.0, 1.0, 40.0);
        ISKANDER_PREPARE_TICKS = BUILDER.comment("Preparation time before launch (ticks, 20 = 1 second). Default 140 = 7 seconds").defineInRange("iskanderPrepareTicks", 140, 0, 300);
        ISKANDER_LAUNCH_TICKS = BUILDER.comment("Duration of vertical launch phase (ticks, 20 = 1 sec). Default 60 = 3 seconds").defineInRange("iskanderLaunchTicks", 60, 20, 300);
        ISKANDER_KVO_DEVIATION = BUILDER.comment("Circular Error Probable - target deviation in blocks").defineInRange("iskanderKvoDeviation", 3.0, 0.0, 50.0);
        ISKANDER_MIN_DISTANCE = BUILDER.comment("Minimum launch distance for Iskander in blocks").defineInRange("iskanderMinDistance", 1000, 100, 2000);
        ISKANDER_MAX_RANGE = BUILDER.comment("Maximum launch distance for Iskander in blocks").defineInRange("iskanderMaxRange", 5000, 500, 20000);
        ISKANDER_MAX_FLIGHT_HEIGHT = BUILDER.comment("Maximum flight height in blocks absolute Y coordinate (e.g. 600 means missile apex will not exceed Y=600)").defineInRange("iskanderMaxFlightHeight", 600, 100, 1500);
        ISKANDER_DIVE_ANGLE = BUILDER.comment("Terminal dive angle in degrees (70-85 recommended)").defineInRange("iskanderDiveAngle", 75.0, 45.0, 89.0);
        ISKANDER_CRATER_RADIUS = BUILDER.comment(
                "Radius (blocks) of the round crater an Iskander digs (needs explosionBreaksBlocks = true).",
                "The bowl is this radius wide and ~70% of it deep; 0 = no crater (explosion effects only).",
                "Default 11 (~20 wide, ~7-8 deep): ~2x the Shahed, matching a ~10x heavier warhead.")
                .defineInRange("iskanderCraterRadius", 11.0, 0.0, 20.0);
        BUILDER.pop();

        BUILDER.push("Explosion Damage");
        BUILDER.comment(
            "Entity damage from Shahed and Iskander warheads.",
            "Damage is full inside coreRadius, then falls off linearly to 0 at damageRadius.",
            "With SuperbWarfare installed, Shahed and Iskander count as aerial bombs against vehicle armor",
            "(tag superbwarfare:aerial_bomb), so a vehicle's own damage modifiers decide how much it takes,",
            "exactly as for SuperbWarfare bombs. Defaults match SuperbWarfare: Shahed = medium bomb (Mk-82) 650 / 22 blocks,",
            "Iskander = large bomb (FAB-500) 1600 / 40 blocks. A direct Shahed hit destroys a tank."
        );
        SHAHED_CORE_DAMAGE   = BUILDER.comment("Shahed: damage at the epicenter (650 = SuperbWarfare Mk-82 medium bomb)").defineInRange("shahedCoreDamage", 650.0, 0.0, 5000.0);
        SHAHED_CORE_RADIUS   = BUILDER.comment("Shahed: radius (blocks) of full damage").defineInRange("shahedCoreRadius", 4.0, 0.0, 64.0);
        SHAHED_DAMAGE_RADIUS = BUILDER.comment("Shahed: radius (blocks) where damage reaches 0").defineInRange("shahedDamageRadius", 22.0, 1.0, 64.0);
        ISKANDER_CORE_DAMAGE   = BUILDER.comment("Iskander: damage at the epicenter (1600 = SuperbWarfare FAB-500 / Mk-84 class)").defineInRange("iskanderCoreDamage", 1600.0, 0.0, 5000.0);
        ISKANDER_CORE_RADIUS   = BUILDER.comment("Iskander: radius (blocks) of full damage").defineInRange("iskanderCoreRadius", 6.0, 0.0, 64.0);
        ISKANDER_DAMAGE_RADIUS = BUILDER.comment("Iskander: radius (blocks) where damage reaches 0").defineInRange("iskanderDamageRadius", 40.0, 1.0, 64.0);
        ISKANDER_PLAYER_DAMAGE_MULTIPLIER  = BUILDER.comment("Iskander: extra multiplier vs players").defineInRange("iskanderPlayerDamageMultiplier",  1.0, 0.0, 20.0);
        ISKANDER_ENTITY_DAMAGE_MULTIPLIER  = BUILDER.comment("Iskander: extra multiplier vs all non-player entities (mobs AND vehicles)").defineInRange("iskanderEntityDamageMultiplier",  1.0, 0.0, 20.0);
        SHAHED_EXPLOSION_PLAYER_MULTIPLIER  = BUILDER.comment("Shahed: extra multiplier vs players").defineInRange("shahedExplosionPlayerMultiplier",  1.0, 0.0, 20.0);
        SHAHED_EXPLOSION_ENTITY_MULTIPLIER  = BUILDER.comment("Shahed: extra multiplier vs all non-player entities (mobs AND vehicles)").defineInRange("shahedExplosionEntityMultiplier",  1.0, 0.0, 20.0);
        VANILLA_EXPLOSION_HURTS_ENTITIES = BUILDER.comment(
            "If false (recommended), the vanilla block-breaking blast of Shahed/Iskander does NOT damage entities,",
            "so the numbers above are the only damage dealt. If true, vanilla TNT-style damage (~170+ at the center) is added on top.")
            .define("vanillaExplosionHurtsEntities", false);
        BUILDER.pop();

        BUILDER.push("Radar");
        RADAR_MIN_ALTITUDE = BUILDER.comment(
            "Minimum absolute Y level for a contact to show on the Air Defense Radar.",
            "Anything flying lower is below the radar horizon. Applies to every contact, missiles included.")
            .defineInRange("radarMinAltitude", 90, -64, 2000);
        RADAR_RANGE = BUILDER.comment(
            "Detection radius of the Air Defense Radar in blocks. Players get it when they join.",
            "The radar terrain map covers this radius; larger values make the map coarser.")
            .defineInRange("radarRange", 1500, 256, 5000);
        RADAR_REQUIRES_OPEN_SKY = BUILDER.comment(
            "If true, the radar needs open sky: no blocks at all (glass, leaves, water included) above the 5x5 area",
            "centred on it. Otherwise it shows WEAK SIGNAL with an empty map and its redstone output is 0.")
            .define("radarRequiresOpenSky", true);
        VEHICLE_RADARS = BUILDER.comment(
            "Radars on board SuperbWarfare and addon vehicles (fighters, attack helicopters, AA vehicles).",
            "Which vehicle gets which radar is set by data files in data/ata/vehicle_radars (datapacks can change them).")
            .define("vehicleRadars", true);
        RADAR_DARK_BACKGROUND = BUILDER.comment(
            "If true, the radar screen shows a plain dark background instead of the biome/terrain map.",
            "The terrain around radars is then never scanned, which also saves server work.",
            "Players see the change the next time they open a radar.")
            .define("radarDarkBackground", false);
        MAP_READ_SAVED_CHUNKS = BUILDER.comment(
            "Radar and Shahed/Orlan route maps show the real terrain. If true, the first scan of a map also",
            "reads the chunks saved on disk (e.g. a world pregenerated with Chunky), in the background.",
            "The first scan takes longer (minutes for a big route map), then the map is cached.",
            "If false, only the chunks loaded at that moment are used; everything else shows biome colours.")
            .define("mapReadSavedChunks", true);
        MAP_REFRESH_MINUTES = BUILDER.comment(
            "Every this many minutes the maps are updated with the chunks the server has loaded at that",
            "moment (around players etc.), so they show new buildings and craters. 0 = never.")
            .defineInRange("mapRefreshMinutes", 10, 0, 1440);
        BUILDER.comment(
            "Extra entity ids (\"modid:entity\") to show on the radar with a forced type.",
            "Aircraft from SuperbWarfare and its addons (AshVehicle etc.) are detected automatically;",
            "these lists are for anything that is not, and override the automatic type.");
        RADAR_PLANE_IDS = BUILDER.comment("Shown as PLN (plane)")
            .defineList("radarPlaneIds", Arrays.asList("superbwarfare:tom_6", "ashvehicle:tom_7"), obj -> obj instanceof String);
        RADAR_HELICOPTER_IDS = BUILDER.comment("Shown as HEL (helicopter)")
            .defineList("radarHelicopterIds", Arrays.asList(), obj -> obj instanceof String);
        RADAR_DRONE_IDS = BUILDER.comment("Extra entities shown as UAV. Shahed, Orlan, MQ-9 Reaper and X-47B are detected automatically")
            .defineList("radarDroneIds", Arrays.asList(), obj -> obj instanceof String);
        RADAR_MISSILE_IDS = BUILDER.comment("Shown as MSL (missiles; SuperbWarfare and AshVehicle missiles and the Iskander are detected automatically)")
            .defineList("radarMissileIds", Arrays.asList(), obj -> obj instanceof String);
        RADAR_EXCLUDED_IDS = BUILDER.comment("Never shown on the radar")
            .defineList("radarExcludedIds", Arrays.asList(), obj -> obj instanceof String);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static class ForbiddenZone {
        public final int x1;
        public final int z1;
        public final int x2;
        public final int z2;

        public ForbiddenZone(int x1, int z1, int x2, int z2) {
            this.x1 = Math.min(x1, x2);
            this.z1 = Math.min(z1, z2);
            this.x2 = Math.max(x1, x2);
            this.z2 = Math.max(z1, z2);
        }

        public boolean contains(int x, int z) {
            return x >= this.x1 && x <= this.x2 && z >= this.z1 && z <= this.z2;
        }

        public static ForbiddenZone parse(String str) {
            try {
                String[] parts = str.split(",");
                if (parts.length == 4) {
                    return new ForbiddenZone(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim()),
                            Integer.parseInt(parts[3].trim())
                    );
                }
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
    }
}
