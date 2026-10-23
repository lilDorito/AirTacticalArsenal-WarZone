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

    private static final Map<String, String> RENAMED = Map.ofEntries(
            Map.entry("shahed_launcher", "heavy_uav_launcher"),
            Map.entry("strike_uav_launcher", "heavy_uav_launcher"),
            Map.entry("control_tablet", "uav_tablet"),
            Map.entry("strike_uav_tablet", "uav_tablet"),
            Map.entry("orlan_launcher", "light_uav_launcher"),
            Map.entry("recon_uav_launcher", "light_uav_launcher"),
            Map.entry("orlan_tablet", "uav_tablet"),
            Map.entry("recon_uav_tablet", "uav_tablet"),
            Map.entry("heavy_uav_tablet", "uav_tablet"),
            Map.entry("light_uav_tablet", "uav_tablet"),
            Map.entry("launcher", "missile_launchpad"),
            Map.entry("unified_terminal", "missile_terminal"),
            Map.entry("launch_interface_cable", "missile_link_cable"));

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
