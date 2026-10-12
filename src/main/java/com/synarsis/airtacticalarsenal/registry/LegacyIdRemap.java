package com.synarsis.airtacticalarsenal.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;

import java.util.Map;

@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyIdRemap {

    private static final Map<String, String> RENAMED = Map.of(
            "shahed_launcher", "strike_uav_launcher",
            "control_tablet", "strike_uav_tablet",
            "orlan_launcher", "recon_uav_launcher",
            "orlan_tablet", "recon_uav_tablet",
            "launcher", "missile_launchpad",
            "unified_terminal", "missile_terminal",
            "launch_interface_cable", "missile_link_cable");

    private LegacyIdRemap() {}

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        remap(event, Registries.ITEM, ForgeRegistries.ITEMS);
        remap(event, Registries.BLOCK, ForgeRegistries.BLOCKS);
        remap(event, Registries.ENTITY_TYPE, ForgeRegistries.ENTITY_TYPES);
        remap(event, Registries.BLOCK_ENTITY_TYPE, ForgeRegistries.BLOCK_ENTITY_TYPES);
    }

    private static <T> void remap(MissingMappingsEvent event, ResourceKey<? extends Registry<T>> key,
                                  IForgeRegistry<T> registry) {
        for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings(key, "ata")) {
            String target = RENAMED.get(mapping.getKey().getPath());
            if (target == null) continue;
            T value = registry.getValue(new ResourceLocation("ata", target));
            if (value != null) mapping.remap(value);
        }
    }
}
