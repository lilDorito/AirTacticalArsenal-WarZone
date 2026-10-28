package com.synarsis.airtacticalarsenal.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AtaClientConfig {

    public enum HudPosition { LEFT_CENTER, TOP_LEFT, BOTTOM_LEFT, RIGHT_CENTER, TOP_RIGHT, BOTTOM_RIGHT }

    public enum StrikeAim { SELECT, KEY }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue VEHICLE_RADAR_HUD;
    public static final ForgeConfigSpec.EnumValue<HudPosition> VEHICLE_RADAR_POSITION;
    public static final ForgeConfigSpec.IntValue VEHICLE_RADAR_SIZE;
    public static final ForgeConfigSpec.BooleanValue ROUND_RADAR_HEADING_UP;
    public static final ForgeConfigSpec.EnumValue<StrikeAim> STRIKE_AIM;
    public static final ForgeConfigSpec.BooleanValue TARGET_SQUARES;
    public static final ForgeConfigSpec.DoubleValue DOPPLER_STRENGTH;
    public static final ForgeConfigSpec.BooleanValue SOUND_DELAY;

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
        builder.push("DroneCamera");
        STRIKE_AIM = builder.comment("Strike drone camera aiming. SELECT: the mouse turns the camera; click to dive on what is in the middle, hold a button and move the mouse to draw a box (left: dive, right: circle over it), R to go back to the route. KEY: hold R to lock, press the dive key (X) to dive")
                .defineEnum("strikeAim", StrikeAim.SELECT);
        TARGET_SQUARES = builder.comment("Mark people, mobs and vehicles the drone camera can see with squares")
                .define("targetSquares", true);
        builder.pop();
        builder.push("DroneSound");
        DOPPLER_STRENGTH = builder.comment("Doppler pitch shift of drone engines (0 = off, 1 = physical)")
                .defineInRange("dopplerStrength", 1.0, 0.0, 2.0);
        SOUND_DELAY = builder.comment("Hear distant drones with the delay of sound travelling through air")
                .define("soundTravelDelay", true);
        builder.pop();
        SPEC = builder.build();
    }

    private AtaClientConfig() {}
}
