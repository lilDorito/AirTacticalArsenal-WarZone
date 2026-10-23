package com.synarsis.airtacticalarsenal.item;

import com.synarsis.airtacticalarsenal.ShahedMod;
import com.synarsis.airtacticalarsenal.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ShahedMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ATA_TAB = CREATIVE_MODE_TABS.register("ata_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.MISSILE_TERMINAL.get()))
                    .title(Component.literal("Air Tactical Arsenal WarZone"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.SHAHED_RADAR.get());
                        output.accept(ModBlocks.SIREN.get());
                        output.accept(ModBlocks.MISSILE_TERMINAL.get());
                        output.accept(ModBlocks.MISSILE_LAUNCHPAD.get());
                        output.accept(ModItems.ISKANDER.get());
                        output.accept(ModItems.SHAHED.get());
                        output.accept(ModItems.HEAVY_UAV_LAUNCHER.get());
                        output.accept(ModItems.UAV_TABLET.get());
                        output.accept(ModItems.MISSILE_LINK_CABLE.get());
                        output.accept(ModItems.PAINT_STAND.get());

                        ItemStack fullSprayCan = new ItemStack(ModItems.SPRAY_CAN.get());
                        SprayCanItem.setCharge(fullSprayCan, SprayCanItem.MAX_CHARGE);
                        output.accept(fullSprayCan);
                        output.accept(ModItems.LIGHT_UAV_LAUNCHER.get());
                        output.accept(ModItems.ORLAN.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
