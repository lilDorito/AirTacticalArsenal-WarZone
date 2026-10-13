package com.synarsis.airtacticalarsenal.client.radar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class RadarDraw {

    private RadarDraw() {}

    static void line(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        if (dy == 0) {
            g.fill(Math.min(x1, x2), y1, Math.max(x1, x2) + 1, y1 + 1, color);
            return;
        }
        if (dx == 0) {
            g.fill(x1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2) + 1, color);
            return;
        }
        double fx = (double) dx / steps;
        double fy = (double) dy / steps;
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (int) Math.round(fx * i);
            int y = y1 + (int) Math.round(fy * i);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    static void ray(GuiGraphics g, int cx, int cy, double angleDeg, double length, int color) {
        double r = Math.toRadians(angleDeg);
        line(g, cx, cy, cx + (int) Math.round(Math.sin(r) * length), cy - (int) Math.round(Math.cos(r) * length), color);
    }

    static void arc(GuiGraphics g, int cx, int cy, double radius, double fromDeg, double toDeg, int color) {
        int segments = Math.max(6, (int) (Math.abs(toDeg - fromDeg) / 6.0));
        int px = 0;
        int py = 0;
        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(fromDeg + (toDeg - fromDeg) * i / segments);
            int x = cx + (int) Math.round(Math.sin(a) * radius);
            int y = cy - (int) Math.round(Math.cos(a) * radius);
            if (i > 0) line(g, px, py, x, y, color);
            px = x;
            py = y;
        }
    }

    static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r; y++) {
            int hw = (int) Math.sqrt(r * r - y * y);
            g.fill(cx - hw, cy + y, cx + hw + 1, cy + y + 1, color);
        }
    }

    static void fan(GuiGraphics g, int ax, int ay, int r, int halfAngle, int color) {
        double tan = halfAngle >= 90 ? Double.MAX_VALUE : Math.tan(Math.toRadians(halfAngle));
        for (int t = 0; t <= r; t++) {
            double circle = Math.sqrt((double) r * r - (double) t * t);
            int hw = (int) Math.min(circle, t * tan);
            g.fill(ax - hw, ay - t, ax + hw + 1, ay - t + 1, color);
        }
    }

    static void blip(GuiGraphics g, int x, int y, int color, boolean hollow) {
        if (hollow) {
            g.fill(x - 2, y - 2, x + 3, y - 1, color);
            g.fill(x - 2, y + 2, x + 3, y + 3, color);
            g.fill(x - 2, y - 1, x - 1, y + 2, color);
            g.fill(x + 2, y - 1, x + 3, y + 2, color);
        } else {
            g.fill(x - 2, y - 2, x + 3, y + 3, color);
        }
    }

    static void dot(GuiGraphics g, int x, int y, int color) {
        g.fill(x - 1, y - 1, x + 2, y + 2, color);
    }
}
