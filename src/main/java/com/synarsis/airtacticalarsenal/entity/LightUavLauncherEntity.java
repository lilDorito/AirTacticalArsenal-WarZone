package com.synarsis.airtacticalarsenal.entity;

import com.synarsis.airtacticalarsenal.api.UavWeight;
import com.synarsis.airtacticalarsenal.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class LightUavLauncherEntity extends UavLauncherEntity {

    public LightUavLauncherEntity(EntityType<? extends LightUavLauncherEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public UavWeight weight() {
        return UavWeight.LIGHT;
    }

    @Override
    protected Item launcherItem() {
        return ModItems.LIGHT_UAV_LAUNCHER.get();
    }

    @Override
    protected int defaultRole() {
        return ROLE_RECON;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.5, 0.5, 1.5);
    }
}
