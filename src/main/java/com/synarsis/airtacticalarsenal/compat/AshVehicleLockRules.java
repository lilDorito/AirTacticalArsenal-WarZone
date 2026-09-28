package com.synarsis.airtacticalarsenal.compat;

import com.synarsis.airtacticalarsenal.entity.IskanderEntity;
import com.synarsis.airtacticalarsenal.entity.OrlanEntity;
import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import net.minecraft.world.entity.Entity;

public final class AshVehicleLockRules {

    private AshVehicleLockRules() {}

    public static Boolean decide(Entity entity) {
        if (entity instanceof IskanderEntity) {
            return Boolean.FALSE;
        }
        if (entity instanceof ShahedEntity shahed) {
            return shahed.isAlive() && shahed.isPickable();
        }
        if (entity instanceof OrlanEntity orlan) {
            return orlan.isAlive();
        }
        return null;
    }
}
