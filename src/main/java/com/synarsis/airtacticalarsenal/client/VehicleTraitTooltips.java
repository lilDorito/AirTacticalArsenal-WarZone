package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.api.DroneVariants;
import com.synarsis.airtacticalarsenal.item.IskanderItem;
import com.synarsis.airtacticalarsenal.item.OrlanItem;
import com.synarsis.airtacticalarsenal.item.ShahedItem;
import com.synarsis.airtacticalarsenal.radar.vehicle.VehicleRadarProfile;
import com.synarsis.airtacticalarsenal.radar.vehicle.VehicleRadarProfiles;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class VehicleTraitTooltips {

    private VehicleTraitTooltips() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ResourceLocation id = entityId(event.getItemStack());
        if (id == null) {
            return;
        }
        VehicleRadarProfile radar = VehicleRadarProfiles.clientRadarFor(id);
        if (radar != null) {
            event.getToolTip().add(radarLine(radar).withStyle(ChatFormatting.GRAY));
        }
        float signature = VehicleRadarProfiles.clientSignatureFor(id);
        if (signature < 0.999f) {
            event.getToolTip().add(Component.translatable("tip.ata.stealth",
                    String.valueOf(Math.round(signature * 100.0f))).withStyle(ChatFormatting.GRAY));
        }
    }

    private static net.minecraft.network.chat.MutableComponent radarLine(VehicleRadarProfile radar) {
        String kind = radar.kind == VehicleRadarProfile.Kind.AESA ? "AESA" : "MSA";
        String timing;
        if (radar.kind == VehicleRadarProfile.Kind.AESA) {
            timing = Component.translatable("tip.ata.radar.refresh", number(20.0 / radar.refreshTicks)).getString();
        } else if (radar.shape == VehicleRadarProfile.Shape.ROUND) {
            timing = Component.translatable("tip.ata.radar.turn", number(radar.sweepTicks / 20.0)).getString();
        } else {
            timing = Component.translatable("tip.ata.radar.sweep", number(radar.sweepTicks / 20.0)).getString();
        }
        if (radar.shape == VehicleRadarProfile.Shape.ROUND) {
            return Component.translatable("tip.ata.radar.round", kind, String.valueOf(radar.range), timing);
        }
        return Component.translatable("tip.ata.radar.cone", kind, String.valueOf(radar.range),
                String.valueOf(radar.halfAngle), timing);
    }

    private static String number(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static ResourceLocation entityId(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof ShahedItem) {
            return new ResourceLocation("ata", "shahed");
        }
        if (stack.getItem() instanceof OrlanItem) {
            return new ResourceLocation("ata", "orlan");
        }
        if (stack.getItem() instanceof IskanderItem) {
            return new ResourceLocation("ata", "iskander");
        }
        String variant = DroneVariants.entityTypeKey(DroneVariants.key(stack.getItem()));
        if (!variant.isEmpty()) {
            return ResourceLocation.tryParse(variant);
        }
        if (stack.getItem() instanceof BlockItem) {
            CompoundTag tag = BlockItem.getBlockEntityData(stack);
            if (tag != null && tag.contains("EntityType")) {
                return ResourceLocation.tryParse(tag.getString("EntityType"));
            }
        }
        return null;
    }
}
