package com.synarsis.airtacticalarsenal.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class MapBorder {
    private static final int SHADE = 0xB0000000;
    private static final int LINE = 0xFFFF5050;

    private MapBorder() {
    }

    public static WorldBorder border() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getWorldBorder() : null;
    }

    public static double clampX(double x) {
        WorldBorder border = border();
        return border == null ? x : Mth.clamp(x, border.getMinX(), border.getMaxX() - 1.0);
    }

    public static double clampZ(double z) {
        WorldBorder border = border();
        return border == null ? z : Mth.clamp(z, border.getMinZ(), border.getMaxZ() - 1.0);
    }

    public static void shade(GuiGraphics g, int left, int top, int right, int bottom,
                             double centerX, double centerY, double viewX, double viewZ, double scale) {
        WorldBorder border = border();
        if (border == null || scale <= 0.0) {
            return;
        }
        double minX = centerX + (border.getMinX() - viewX) / scale;
        double maxX = centerX + (border.getMaxX() - viewX) / scale;
        double minY = centerY + (border.getMinZ() - viewZ) / scale;
        double maxY = centerY + (border.getMaxZ() - viewZ) / scale;
        int x0 = (int) Mth.clamp(minX, left, right);
        int x1 = (int) Mth.clamp(maxX, left, right);
        int y0 = (int) Mth.clamp(minY, top, bottom);
        int y1 = (int) Mth.clamp(maxY, top, bottom);
        if (y0 > top) {
            g.fill(left, top, right, y0, SHADE);
        }
        if (y1 < bottom) {
            g.fill(left, y1, right, bottom, SHADE);
        }
        if (x0 > left) {
            g.fill(left, y0, x0, y1, SHADE);
        }
        if (x1 < right) {
            g.fill(x1, y0, right, y1, SHADE);
        }
        if (minY >= top && minY < bottom && x1 > x0) {
            g.fill(x0, y0, x1, y0 + 1, LINE);
        }
        if (maxY > top && maxY <= bottom && x1 > x0) {
            g.fill(x0, y1 - 1, x1, y1, LINE);
        }
        if (minX >= left && minX < right && y1 > y0) {
            g.fill(x0, y0, x0 + 1, y1, LINE);
        }
        if (maxX > left && maxX <= right && y1 > y0) {
            g.fill(x1 - 1, y0, x1, y1, LINE);
        }
    }
}
