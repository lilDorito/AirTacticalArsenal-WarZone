package com.synarsis.airtacticalarsenal.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger("ATA-Config");
    private static final int CURRENT = 4;

    private ConfigMigration() {}

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != ShahedConfig.SPEC) return;
        if (ShahedConfig.CONFIG_VERSION.get() >= CURRENT) return;
        int version = ShahedConfig.CONFIG_VERSION.get();
        int changed = 0;
        if (version < 2) {
            changed += bump(ShahedConfig.SHAHED_CORE_DAMAGE, 60.0, 160.0);
            changed += bump(ShahedConfig.ISKANDER_CORE_DAMAGE, 160.0, 1600.0);
            changed += bump(ShahedConfig.ISKANDER_CORE_RADIUS, 5.0, 6.0);
            changed += bump(ShahedConfig.ISKANDER_DAMAGE_RADIUS, 26.0, 40.0);
        }
        if (version < 3) {
            changed += bump(ShahedConfig.SHAHED_CORE_DAMAGE, 160.0, 650.0);
            changed += bump(ShahedConfig.SHAHED_CORE_RADIUS, 3.0, 4.0);
            changed += bump(ShahedConfig.SHAHED_DAMAGE_RADIUS, 14.0, 22.0);
        }
        if (version >= 2 && version < 4) {
            changed += bump(ShahedConfig.SHAHED_MAX_ROUTE_DISTANCE, 20000, 10000);
            changed += bump(ShahedConfig.ORLAN_MAX_ROUTE_DISTANCE, 20000, 10000);
            changed += bump(ShahedConfig.ORLAN_FLIGHT_TIME_MINUTES, 45, 30);
        }
        ShahedConfig.CONFIG_VERSION.set(CURRENT);
        ShahedConfig.SPEC.save();
        if (changed > 0) {
            LOGGER.info("[ATA] Updated {} settings in ata-common.toml from old defaults to the current defaults", changed);
        }
    }

    private static int bump(ForgeConfigSpec.DoubleValue value, double oldDefault, double newDefault) {
        if (Math.abs(value.get() - oldDefault) > 1e-6) return 0;
        value.set(newDefault);
        return 1;
    }

    private static int bump(ForgeConfigSpec.IntValue value, int oldDefault, int newDefault) {
        if (value.get() != oldDefault) return 0;
        value.set(newDefault);
        return 1;
    }
}
