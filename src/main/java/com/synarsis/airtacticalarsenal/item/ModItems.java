package com.synarsis.airtacticalarsenal.item;

import com.synarsis.airtacticalarsenal.ShahedMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ShahedMod.MOD_ID);

    public static final RegistryObject<Item> ISKANDER = ITEMS.register("iskander",
            () -> new IskanderItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> SHAHED = ITEMS.register("shahed",
            () -> new ShahedItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)));

    public static final RegistryObject<Item> MISSILE_LINK_CABLE = ITEMS.register("missile_link_cable",
            () -> new MissileLinkCableItem(new Item.Properties()
                    .stacksTo(16)));

    public static final RegistryObject<Item> HEAVY_UAV_LAUNCHER = ITEMS.register("heavy_uav_launcher",
            () -> new HeavyUavLauncherItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> UAV_TABLET = ITEMS.register("uav_tablet",
            () -> new UavTabletItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> PAINT_STAND = ITEMS.register("paint_stand",
            () -> new PaintStandItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SPRAY_CAN = ITEMS.register("spray_can",
            () -> new SprayCanItem(new Item.Properties()
                    .stacksTo(1)));

    public static final RegistryObject<Item> LIGHT_UAV_LAUNCHER = ITEMS.register("light_uav_launcher",
            () -> new LightUavLauncherItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> ORLAN = ITEMS.register("orlan",
            () -> new OrlanItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
