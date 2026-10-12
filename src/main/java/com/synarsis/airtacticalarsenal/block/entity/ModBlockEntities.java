package com.synarsis.airtacticalarsenal.block.entity;

import com.synarsis.airtacticalarsenal.ShahedMod;
import com.synarsis.airtacticalarsenal.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ShahedMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<IskanderRocketBlockEntity>> ISKANDER_ROCKET = 
        BLOCK_ENTITIES.register("iskander_rocket", () -> 
            BlockEntityType.Builder.of(IskanderRocketBlockEntity::new, ModBlocks.ISKANDER_ROCKET.get())
                .build(null));

    public static final RegistryObject<BlockEntityType<MissileTerminalBlockEntity>> MISSILE_TERMINAL = 
        BLOCK_ENTITIES.register("missile_terminal", () -> 
            BlockEntityType.Builder.of(MissileTerminalBlockEntity::new, ModBlocks.MISSILE_TERMINAL.get())
                .build(null));

    public static final RegistryObject<BlockEntityType<LoadingHatchBlockEntity>> LOADING_HATCH = 
        BLOCK_ENTITIES.register("loading_hatch", () -> 
            BlockEntityType.Builder.of(LoadingHatchBlockEntity::new, ModBlocks.LOADING_HATCH.get())
                .build(null));

    public static final RegistryObject<BlockEntityType<MissileLaunchpadBlockEntity>> MISSILE_LAUNCHPAD = 
        BLOCK_ENTITIES.register("missile_launchpad", () -> 
            BlockEntityType.Builder.of(MissileLaunchpadBlockEntity::new, ModBlocks.MISSILE_LAUNCHPAD.get())
                .build(null));

    public static final RegistryObject<BlockEntityType<ShahedRadarBlockEntity>> SHAHED_RADAR =
        BLOCK_ENTITIES.register("shahed_radar", () ->
            BlockEntityType.Builder.of(ShahedRadarBlockEntity::new, ModBlocks.SHAHED_RADAR.get())
                .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
