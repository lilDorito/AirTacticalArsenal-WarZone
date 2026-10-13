package com.synarsis.airtacticalarsenal.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AtaClientConfig {

    public enum HudPosition { LEFT_CENTER, TOP_LEFT, BOTTOM_LEFT, RIGHT_CENTER, TOP_RIGHT, BOTTOM_RIGHT }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue VEHICLE_RADAR_HUD;
    public static final ForgeConfigSpec.EnumValue<HudPosition> VEHICLE_RADAR_POSITION;
    public static final ForgeConfigSpec.IntValue VEHICLE_RADAR_SIZE;
    public static final ForgeConfigSpec.BooleanValue ROUND_RADAR_HEADING_UP;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("VehicleRadar");
        VEHICLE_RADAR_HUD = builder.comment("Show the radar panel while riding a vehicle that has a radar (toggle key: I by default)")
                .define("showVehicleRadar", true);
        VEHICLE_RADAR_POSITION = builder.comment("Where the radar panel sits on the screen")
                .defineEnum("vehicleRadarPosition", HudPosition.LEFT_CENTER);
        VEHICLE_RADAR_SIZE = builder.comment("Size of the radar panel in GUI pixels (height of the scope)")
                .defineInRange("vehicleRadarPanelSize", 73, 40, 260);
        ROUND_RADAR_HEADING_UP = builder.comment("Round radars (AA vehicles): false = north up, true = vehicle heading up")
                .define("roundRadarHeadingUp", false);
        builder.pop();
        SPEC = builder.build();
    }

    private AtaClientConfig() {}
}
