package com.synarsis.airtacticalarsenal.entity;

import com.synarsis.airtacticalarsenal.api.UavWeight;
import com.synarsis.airtacticalarsenal.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class HeavyUavLauncherEntity extends UavLauncherEntity {

    public HeavyUavLauncherEntity(EntityType<? extends HeavyUavLauncherEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public UavWeight weight() {
        return UavWeight.HEAVY;
    }

    @Override
    protected Item launcherItem() {
        return ModItems.HEAVY_UAV_LAUNCHER.get();
    }

    @Override
    protected int defaultRole() {
        return ROLE_STRIKE;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(2.0, 1.0, 2.0);
    }
}
