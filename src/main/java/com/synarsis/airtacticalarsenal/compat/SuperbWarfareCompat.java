package com.synarsis.airtacticalarsenal.compat;

import com.synarsis.airtacticalarsenal.config.ShahedConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public class SuperbWarfareCompat {

    private static boolean swLoaded = false;
    private static boolean checkedSw = false;
    private static boolean vvpLoaded = false;
    private static boolean checkedVvp = false;

    private static final String SW_PKG  = "com.atsuishio.superbwarfare";
    private static final String VVP_PKG = "tech.vvp.vvp";

    private static final String[] HELI_KEYWORDS  = {"helicopter", "heli", "copter", "rotor"};
    private static final String[] PLANE_KEYWORDS = {"airplane", "aircraft", "fixedwing", "plane", "jet", "bomber", "fighter"};
    private static final String[] DRONE_KEYWORDS = {"drone", "uav", "fpv", "quadrotor"};

    private static final String[] HELI_NAMES  = {
        "ah6", "ah64", "ah1", "mi8", "mi17", "mi24", "mi26", "mi28",
        "uh60", "ch47", "cobra", "nh90", "ka52", "ka50", "hind", "havoc",
        "apache", "blackhawk", "chinook"
    };
    private static final String[] PLANE_NAMES = {
        "a10", "ju87", "tom6", "kv16", "f16", "mig", "su27", "su25",
        "b52", "warthog", "stuka", "thunderbolt"
    };
    private static final String[] DRONE_NAMES = {
        "drone", "uav", "fpv"
    };

    public static final byte TYPE_MISSILE       = 1;
    public static final byte TYPE_SW_HELICOPTER = 3;
    public static final byte TYPE_SW_PLANE      = 4;
    public static final byte TYPE_SW_DRONE      = 5;

    private static final String ASH_PKG = "Aru.Aru.ashvehicle";
    private static final String SW_MISSILE_CLASS = SW_PKG + ".entity.projectile.MissileProjectile";
    private static final java.util.regex.Pattern MISSILE_NAME =
            java.util.regex.Pattern.compile("^(aim|agm|aam|jassm|tomahawk|kh\\d|r\\d\\d).*");
    private static boolean missileClassResolved = false;
    private static Class<?> swMissile;

    public static boolean isLoaded() {
        if (!checkedSw) { swLoaded = ModList.get().isLoaded("superbwarfare"); checkedSw = true; }
        return swLoaded;
    }

    public static boolean isVvpLoaded() {
        if (!checkedVvp) { vvpLoaded = ModList.get().isLoaded("vvp"); checkedVvp = true; }
        return vvpLoaded;
    }

    private static final String SW_AIR_IFACE        = SW_PKG + ".entity.vehicle.base.AirEntity";
    private static final String SW_AIRCRAFT_IFACE   = SW_PKG + ".entity.vehicle.base.AircraftEntity";
    private static final String SW_HELICOPTER_IFACE = SW_PKG + ".entity.vehicle.base.HelicopterEntity";
    private static final String SW_DRONE_CLASS      = SW_PKG + ".entity.vehicle.DroneEntity";
    private static final String SW_VEHICLE_CLASS    = SW_PKG + ".entity.vehicle.base.VehicleEntity";
    private static boolean swClassesResolved = false;
    private static Class<?> swAir, swAircraft, swHelicopter, swDrone, swVehicle;
    private static java.lang.reflect.Method swGetVehicleType;

    private static Class<?> tryLoad(String name) {
        try {
            return Class.forName(name, false, SuperbWarfareCompat.class.getClassLoader());
        } catch (Throwable t) {
            return null;
        }
    }

    private static void resolveSwClasses() {
        if (swClassesResolved) return;
        swClassesResolved = true;
        if (!isLoaded()) return;
        swAir        = tryLoad(SW_AIR_IFACE);
        swAircraft   = tryLoad(SW_AIRCRAFT_IFACE);
        swHelicopter = tryLoad(SW_HELICOPTER_IFACE);
        swDrone      = tryLoad(SW_DRONE_CLASS);
        swVehicle    = tryLoad(SW_VEHICLE_CLASS);
        if (swVehicle != null) {
            try {
                swGetVehicleType = swVehicle.getMethod("getVehicleType");
            } catch (Throwable t) {
                swGetVehicleType = null;
            }
        }
    }

    private static byte classifyByVehicleType(Entity entity) {
        if (swGetVehicleType == null || !swVehicle.isInstance(entity)) return -1;
        Object type;
        try {
            type = swGetVehicleType.invoke(entity);
        } catch (Throwable t) {
            return -1;
        }
        if (!(type instanceof Enum<?> e)) return -1;
        switch (e.name()) {
            case "HELICOPTER": return TYPE_SW_HELICOPTER;
            case "AIRPLANE":
            case "AIRSHIP": return TYPE_SW_PLANE;
            case "DRONE": return TYPE_SW_DRONE;
            case "TANK":
            case "APC":
            case "AA":
            case "CAR":
            case "ARTILLERY":
            case "DEFENSE":
            case "BOAT": return -2;
            default: return -1;
        }
    }

    private static final String[] LARGE_UAV_NAMES = {"reaper", "x-47", "x47", "mq-9", "mq9", "mq_9"};

    private static boolean isLargeUav(String path) {
        String p = path.toLowerCase(java.util.Locale.ROOT);
        for (String n : LARGE_UAV_NAMES) if (p.contains(n)) return true;
        return false;
    }

    public static byte getRadarType(Entity entity) {
        ResourceLocation regKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        if (regKey != null) {
            String id = regKey.toString();
            if (ShahedConfig.getRadarExcludedIds().contains(id))   return -1;
            if (ShahedConfig.getRadarMissileIds().contains(id))    return TYPE_MISSILE;
            if (ShahedConfig.getRadarDroneIds().contains(id) || isLargeUav(regKey.getPath())) return TYPE_SW_DRONE;
            if (ShahedConfig.getRadarHelicopterIds().contains(id)) return TYPE_SW_HELICOPTER;
            if (ShahedConfig.getRadarPlaneIds().contains(id))      return TYPE_SW_PLANE;
        }

        if (entity instanceof net.minecraft.world.entity.projectile.Projectile) {
            return isMissile(entity) ? TYPE_MISSILE : -1;
        }

        if (!isLoaded() && !isVvpLoaded()) return -1;

        if (isLoaded()) {
            resolveSwClasses();
            if (swHelicopter != null && swHelicopter.isInstance(entity)) return TYPE_SW_HELICOPTER;
            if (swAircraft   != null && swAircraft.isInstance(entity))   return TYPE_SW_PLANE;
            if (swDrone      != null && swDrone.isInstance(entity))      return -1;
            if (swAir        != null && swAir.isInstance(entity))        return TYPE_SW_PLANE;
            byte byType = classifyByVehicleType(entity);
            if (byType == TYPE_SW_DRONE) return -1;
            if (byType >= 0) return byType;
            if (byType == -2) return -1;
        }

        String pkg = entity.getClass().getName();
        boolean isSW  = isLoaded()    && pkg.startsWith(SW_PKG);
        boolean isVVP = isVvpLoaded() && pkg.startsWith(VVP_PKG);
        if (!isSW && !isVVP) return -1;

        byte byHierarchy = classifyByHierarchy(entity);
        if (byHierarchy == TYPE_SW_DRONE) return -1;
        if (byHierarchy >= 0) return byHierarchy;

        if (regKey != null) {
            byte byReg = classifyByRegistryPath(regKey.getPath());
            if (byReg == TYPE_SW_DRONE) return -1;
            if (byReg >= 0) return byReg;
        }

        byte byName = classifyByExactName(pkg);
        return byName == TYPE_SW_DRONE ? -1 : byName;
    }

    private static boolean bvrResolved = false;
    private static java.lang.reflect.Method bvrRegister;

    public static void registerForSbwRadar(Entity entity) {
        if (!isLoaded()) return;
        if (!bvrResolved) {
            bvrResolved = true;
            Class<?> handler = tryLoad(SW_PKG + ".tools.ServerSyncedEntityHandler");
            if (handler != null) {
                try {
                    bvrRegister = handler.getMethod("register", Entity.class);
                } catch (Throwable t) {
                    bvrRegister = null;
                }
            }
        }
        if (bvrRegister == null) return;
        ensureSbwScanList();
        try {
            bvrRegister.invoke(null, entity);
        } catch (Throwable t) {
            bvrRegister = null;
        }
    }

    private static final String[] SBW_SCAN_IDS = {"ata:shahed", "ata:orlan", "ata:iskander"};
    private static boolean scanListResolved = false;
    private static net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?> sbwScanList;

    @SuppressWarnings("unchecked")
    private static void ensureSbwScanList() {
        if (!scanListResolved) {
            scanListResolved = true;
            Class<?> cfg = tryLoad(SW_PKG + ".config.server.VehicleConfig");
            if (cfg != null) {
                for (java.lang.reflect.Field f : cfg.getDeclaredFields()) {
                    if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    if (!net.minecraftforge.common.ForgeConfigSpec.ConfigValue.class.isAssignableFrom(f.getType())) continue;
                    try {
                        f.setAccessible(true);
                        Object v = f.get(null);
                        if (v instanceof net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?> cv) {
                            java.util.List<String> path = cv.getPath();
                            if (!path.isEmpty() && "scan_white_list".equals(path.get(path.size() - 1))) {
                                sbwScanList = cv;
                                break;
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        if (sbwScanList == null) return;
        try {
            Object current = sbwScanList.get();
            if (!(current instanceof java.util.List<?> list) || list.contains("ata:*")) return;
            java.util.List<Object> next = null;
            for (String id : SBW_SCAN_IDS) {
                if (list.contains(id)) continue;
                if (next == null) next = new java.util.ArrayList<>(list);
                next.add(id);
            }
            if (next != null) {
                ((net.minecraftforge.common.ForgeConfigSpec.ConfigValue<Object>) sbwScanList).set(next);
            }
        } catch (Throwable t) {
            sbwScanList = null;
        }
    }

    public static boolean isMissile(Entity entity) {
        String own = entity.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        if (own.contains("drone") || own.contains("decoy") || own.contains("flare")) return false;
        if (isLoaded()) {
            if (!missileClassResolved) {
                missileClassResolved = true;
                swMissile = tryLoad(SW_MISSILE_CLASS);
            }
            if (swMissile != null && swMissile.isInstance(entity)) return true;
        }
        boolean known = false;
        String className = entity.getClass().getName();
        if (className.startsWith(SW_PKG) || className.startsWith(ASH_PKG) || className.startsWith(VVP_PKG)) known = true;
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            String sn = clazz.getSimpleName().toLowerCase(java.util.Locale.ROOT);
            if (sn.contains("missile")) return true;
            if (known && MISSILE_NAME.matcher(sn).matches()) return true;
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    private static byte classifyByHierarchy(Entity entity) {
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            String sn = clazz.getSimpleName().toLowerCase();
            for (String kw : HELI_KEYWORDS)  if (sn.contains(kw)) return TYPE_SW_HELICOPTER;
            for (String kw : PLANE_KEYWORDS) if (sn.contains(kw)) return TYPE_SW_PLANE;
            for (String kw : DRONE_KEYWORDS) if (sn.contains(kw)) return TYPE_SW_DRONE;
            clazz = clazz.getSuperclass();
        }
        return -1;
    }

    private static byte classifyByRegistryPath(String path) {
        String p = path.toLowerCase();
        for (String n : HELI_NAMES)  if (p.contains(n)) return TYPE_SW_HELICOPTER;
        for (String n : PLANE_NAMES) if (p.contains(n)) return TYPE_SW_PLANE;
        for (String n : DRONE_NAMES) if (p.contains(n)) return TYPE_SW_DRONE;
        return -1;
    }

    private static byte classifyByExactName(String className) {

        if (className.equals(SW_PKG + ".entity.vehicle.Ah6Entity")  ||
            className.equals(SW_PKG + ".entity.vehicle.Mi28Entity")) return TYPE_SW_HELICOPTER;

        if (className.equals(SW_PKG + ".entity.vehicle.A10Entity")  ||
            className.equals(SW_PKG + ".entity.vehicle.Ju87Entity") ||
            className.equals(SW_PKG + ".entity.vehicle.Tom6Entity") ||
            className.equals(SW_PKG + ".entity.vehicle.Kv16Entity")) return TYPE_SW_PLANE;

        if (className.equals(SW_PKG + ".entity.vehicle.DroneEntity")) return TYPE_SW_DRONE;

        if (className.equals(VVP_PKG + ".entity.vehicle.Ah64Entity") ||
            className.equals(VVP_PKG + ".entity.vehicle.Mi28Entity") ||
            className.equals(VVP_PKG + ".entity.vehicle.Mi24Entity") ||
            className.equals(VVP_PKG + ".entity.vehicle.Mi8Entity")  ||
            className.equals(VVP_PKG + ".entity.vehicle.CobraEntity")||
            className.equals(VVP_PKG + ".entity.vehicle.Nh90Entity")) return TYPE_SW_HELICOPTER;
        return -1;
    }
}
