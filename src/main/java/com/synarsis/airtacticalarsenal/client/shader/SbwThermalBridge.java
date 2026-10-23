package com.synarsis.airtacticalarsenal.client.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
public final class SbwThermalBridge {
    private static final String HANDLER = "com.atsuishio.superbwarfare.event.ClientEventHandler";
    private static final ResourceLocation EFFECT = new ResourceLocation("superbwarfare", "shaders/post/night_vision.json");
    private static final int MAX_RESETS = 3;

    private static boolean resolved;
    private static boolean broken;
    private static Field activeField;
    private static Method turnOn;
    private static Method turnOff;
    private static boolean active;
    private static int resets;

    private SbwThermalBridge() {
    }

    public static boolean available() {
        if (broken) {
            return false;
        }
        if (!resolved) {
            resolved = true;
            try {
                if (!ModList.get().isLoaded("superbwarfare")) {
                    broken = true;
                    return false;
                }
                Class<?> handler = Class.forName(HANDLER);
                activeField = handler.getField("activeThermalImaging");
                turnOn = handler.getMethod("turnOnThermalImaging");
                turnOff = handler.getMethod("turnOffThermalImaging");
                if (activeField.getType() != boolean.class
                        || Minecraft.getInstance().getResourceManager().getResource(EFFECT).isEmpty()) {
                    broken = true;
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                broken = true;
            }
        }
        return !broken;
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean enable() {
        if (!available()) {
            return false;
        }
        active = true;
        resets = 0;
        try {
            activeField.setBoolean(null, true);
            turnOn.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail();
            return false;
        }
        return true;
    }

    public static void disable() {
        if (!active) {
            return;
        }
        active = false;
        try {
            activeField.setBoolean(null, false);
            turnOff.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    public static boolean tick() {
        if (!active) {
            return true;
        }
        try {
            if (!activeField.getBoolean(null)) {
                if (++resets > MAX_RESETS) {
                    fail();
                    return false;
                }
                activeField.setBoolean(null, true);
            }
            if (Minecraft.getInstance().gameRenderer.currentEffect() == null) {
                turnOn.invoke(null);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail();
            return false;
        }
        return true;
    }

    private static void fail() {
        disable();
        broken = true;
    }
}
