package com.synarsis.airtacticalarsenal.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AtaItemTooltips {
    private static Boolean shared;

    private AtaItemTooltips() {
    }

    private static boolean sharedHandlerPresent() {
        if (shared == null) {
            boolean found;
            try {
                Class.forName("com.atsuishio.superbwarfare.client.style.ItemTooltips");
                found = true;
            } catch (ClassNotFoundException | LinkageError e) {
                found = false;
            }
            shared = found;
        }
        return shared;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTooltip(ItemTooltipEvent event) {
        if (sharedHandlerPresent() || event.getItemStack().isEmpty()) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null || !"ata".equals(id.getNamespace())) {
            return;
        }
        String base = "tip.ata." + id.getPath();
        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty()) {
            return;
        }
        if (I18n.exists(base)) {
            int at = 1;
            for (String line : I18n.get(base).split("\n")) {
                tooltip.add(Math.min(at++, tooltip.size()), Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
        String controls = base + ".controls";
        if (!I18n.exists(controls)) {
            return;
        }
        if (Screen.hasShiftDown()) {
            for (String line : I18n.get(controls).split("\n")) {
                tooltip.add(controlLine(line));
            }
        } else {
            tooltip.add(controlLine(I18n.get("tip.ata.hold_shift")));
        }
    }

    private static MutableComponent controlLine(String line) {
        int open = line.indexOf('[');
        int close = open < 0 ? -1 : line.indexOf(']', open);
        if (open < 0 || close < 0) {
            return Component.literal(line).withStyle(ChatFormatting.GRAY);
        }
        return Component.literal(line.substring(0, open)).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(line.substring(open, close + 1)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(line.substring(close + 1)).withStyle(ChatFormatting.GRAY));
    }
}
