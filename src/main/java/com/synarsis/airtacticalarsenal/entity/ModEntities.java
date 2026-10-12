package com.synarsis.airtacticalarsenal.entity;

import com.synarsis.airtacticalarsenal.ShahedMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ShahedMod.MOD_ID);

    public static final RegistryObject<EntityType<ShahedEntity>> SHAHED = ENTITY_TYPES.register("shahed", 
            () -> EntityType.Builder.<ShahedEntity>of(ShahedEntity::new, MobCategory.MISC)
                    .sized(3.0f, 1.5f)
                    .clientTrackingRange(600)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("shahed"));

    public static final RegistryObject<EntityType<IskanderEntity>> ISKANDER = ENTITY_TYPES.register("iskander",
            () -> EntityType.Builder.<IskanderEntity>of(IskanderEntity::new, MobCategory.MISC)
                    .sized(1.0f, 7.5f)
                    .clientTrackingRange(800)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("iskander"));

    public static final RegistryObject<EntityType<StrikeUavLauncherEntity>> STRIKE_UAV_LAUNCHER = ENTITY_TYPES.register("strike_uav_launcher",
            () -> EntityType.Builder.<StrikeUavLauncherEntity>of(StrikeUavLauncherEntity::new, MobCategory.MISC)
                    .sized(2.0f, 1.5f)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .build("strike_uav_launcher"));

    public static final RegistryObject<EntityType<PaintStandEntity>> PAINT_STAND = ENTITY_TYPES.register("paint_stand",
            () -> EntityType.Builder.<PaintStandEntity>of(PaintStandEntity::new, MobCategory.MISC)
                    .sized(1.5f, 1.5f)
                    .clientTrackingRange(64)
                    .updateInterval(3)
                    .build("paint_stand"));

    public static final RegistryObject<EntityType<ReconUavLauncherEntity>> RECON_UAV_LAUNCHER = ENTITY_TYPES.register("recon_uav_launcher",
            () -> EntityType.Builder.<ReconUavLauncherEntity>of(ReconUavLauncherEntity::new, MobCategory.MISC)
                    .sized(1.5f, 1.2f)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .build("recon_uav_launcher"));

    public static final RegistryObject<EntityType<OrlanEntity>> ORLAN = ENTITY_TYPES.register("orlan",
            () -> EntityType.Builder.<OrlanEntity>of(OrlanEntity::new, MobCategory.MISC)
                    .sized(2.5f, 1.0f)
                    .clientTrackingRange(600)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("orlan"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
