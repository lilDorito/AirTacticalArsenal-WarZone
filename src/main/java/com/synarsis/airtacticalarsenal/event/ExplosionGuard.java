package com.synarsis.airtacticalarsenal.event;

import com.synarsis.airtacticalarsenal.config.ShahedConfig;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ExplosionGuard {

    private static int depth = 0;

    private ExplosionGuard() {}

    public static void begin() {
        depth++;
    }

    public static void end() {
        if (depth > 0) depth--;
    }

    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        if (depth <= 0) return;
        if (event.getLevel().isClientSide()) return;
        if (ShahedConfig.doesVanillaExplosionHurtEntities()) return;
        event.getAffectedEntities().clear();
    }
}
