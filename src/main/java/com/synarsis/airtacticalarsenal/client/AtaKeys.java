package com.synarsis.airtacticalarsenal.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class AtaKeys {

    public static final KeyMapping VEHICLE_RADAR = new KeyMapping("key.ata.vehicle_radar",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, "key.categories.ata");

    private AtaKeys() {}
}
