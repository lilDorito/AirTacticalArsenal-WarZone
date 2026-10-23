package com.synarsis.airtacticalarsenal.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class TrClient {
    private TrClient() {}

    static String selectedCode() {
        return Minecraft.getInstance().getLanguageManager().getSelected();
    }
}
