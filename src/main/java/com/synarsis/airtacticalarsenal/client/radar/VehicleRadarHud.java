package com.synarsis.airtacticalarsenal.client.radar;

import com.synarsis.airtacticalarsenal.config.AtaClientConfig;
import com.synarsis.airtacticalarsenal.radar.vehicle.VehicleRadarProfile;
import com.synarsis.airtacticalarsenal.util.Tr;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public final class VehicleRadarHud {

    public static final IGuiOverlay OVERLAY = (gui, g, partialTick, width, height) -> render(g, partialTick, width, height);

    private static final int BG = 0xD8020a04;
    private static final int EDGE = 0xFF2fe860;
    private static final int GRID = 0x582fe860;
    private static final int BEAM = 0xE84cff7a;
    private static final int TEXT = 0xFF4cff7a;
    private static final int TEXT_DIM = 0xC02fb050;
    private static final int OWN = 0xFFb8ffcc;
    private static final int MARGIN = 6;
    private static final int LEGEND_W = 30;
    private static final float TEXT_SCALE = 0.6f;
    private static final int GLOW_LINES = 10;

    private static final byte[] TYPES = {4, 3, 5, 1};
    private static final String[] TAGS = {"PLN", "HEL", "UAV", "MSL"};

    private VehicleRadarHud() {}

    static int colorOf(byte type) {
        return switch (type) {
            case 1 -> 0xFFFFE62A;
            case 3 -> 0xFF40c0ff;
            case 4 -> 0xFFff8040;
            case 5 -> 0xFFFF2A2A;
            default -> 0xFFe04040;
        };
    }

    private static void render(GuiGraphics g, float pt, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (!VehicleRadarClient.isActive() || mc.player == null || mc.options.hideGui) return;
        if (!AtaClientConfig.VEHICLE_RADAR_HUD.get()) return;
        Entity vehicle = mc.player.getRootVehicle();
        if (vehicle == mc.player) return;

        int size = AtaClientConfig.VEHICLE_RADAR_SIZE.get();
        boolean cone = VehicleRadarClient.shape == VehicleRadarProfile.Shape.CONE;
        int half = VehicleRadarClient.halfAngle;
        int r;
        int w;
        int h;
        if (cone) {
            r = Math.round(size * 0.8f);
            h = r;
            w = half >= 90 ? 2 * r : 2 * (int) Math.ceil(r * Math.sin(Math.toRadians(half))) + 2;
        } else {
            r = size / 2;
            w = 2 * r + 1;
            h = 2 * r + 1;
        }

        AtaClientConfig.HudPosition pos = AtaClientConfig.VEHICLE_RADAR_POSITION.get();
        boolean right = pos == AtaClientConfig.HudPosition.RIGHT_CENTER || pos == AtaClientConfig.HudPosition.TOP_RIGHT
                || pos == AtaClientConfig.HudPosition.BOTTOM_RIGHT;
        int total = w + 4 + LEGEND_W;
        int x0 = right ? sw - MARGIN - total : MARGIN;
        int y0 = switch (pos) {
            case TOP_LEFT, TOP_RIGHT -> MARGIN;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> sh - h - MARGIN - 30;
            default -> (sh - h) / 2;
        };
        int scopeX = right ? x0 + LEGEND_W + 4 : x0;
        int legendX = right ? x0 : x0 + w + 4;

        Vec3 own = vehicle.getPosition(pt);
        float yaw = Mth.rotLerp(pt, vehicle.yRotO, vehicle.getYRot());
        double fx = -Mth.sin(yaw * Mth.DEG_TO_RAD);
        double fz = Mth.cos(yaw * Mth.DEG_TO_RAD);
        float nose = (float) Math.toDegrees(Math.atan2(fx, -fz));
        double scale = (double) r / Math.max(1, VehicleRadarClient.range);
        boolean msa = VehicleRadarClient.kind == VehicleRadarProfile.Kind.MSA;

        if (cone) {
            int ax = scopeX + w / 2;
            int ay = y0 + h;
            RadarDraw.fan(g, ax, ay, r, half, BG);
            RadarDraw.arc(g, ax, ay, r / 3.0, -half, half, GRID);
            RadarDraw.arc(g, ax, ay, r * 2 / 3.0, -half, half, GRID);
            RadarDraw.ray(g, ax, ay, 0, r, GRID);
            if (half > 35) {
                RadarDraw.ray(g, ax, ay, -half / 2.0, r, GRID);
                RadarDraw.ray(g, ax, ay, half / 2.0, r, GRID);
            }
            RadarDraw.ray(g, ax, ay, -half, r, EDGE);
            RadarDraw.ray(g, ax, ay, half, r, EDGE);
            RadarDraw.arc(g, ax, ay, r, -half, half, EDGE);

            if (msa) {
                float b = VehicleRadarClient.beamAt(pt);
                int dir = VehicleRadarClient.beamDir;
                for (int i = GLOW_LINES; i >= 1; i--) {
                    float a = b - dir * i * 2.0f;
                    if (Math.abs(a) > half) continue;
                    int alpha = (int) (0x60 * (1.0f - (float) i / (GLOW_LINES + 1)));
                    RadarDraw.ray(g, ax, ay, a, r, (alpha << 24) | (BEAM & 0xFFFFFF));
                }
                RadarDraw.ray(g, ax, ay, b, r, BEAM);
            }

            double rx = -Mth.cos(yaw * Mth.DEG_TO_RAD);
            double rz = -Mth.sin(yaw * Mth.DEG_TO_RAD);
            for (VehicleRadarClient.Blip blip : VehicleRadarClient.BLIPS.values()) {
                double dx = blip.x - own.x;
                double dz = blip.z - own.z;
                double fwd = dx * fx + dz * fz;
                double side = dx * rx + dz * rz;
                double dist = Math.sqrt(fwd * fwd + side * side);
                if (dist > VehicleRadarClient.range) continue;
                double ang = Math.toDegrees(Math.atan2(side, fwd));
                if (Math.abs(ang) > half) continue;
                int sx = ax + (int) Math.round(side * scale);
                int sy = ay - (int) Math.round(fwd * scale);
                drawBlip(g, blip, sx, sy, headingBearing(blip.heading) - nose, msa);
            }
            RadarDraw.dot(g, ax, ay - 1, OWN);
        } else {
            int cx = scopeX + r;
            int cy = y0 + r;
            float rotation = AtaClientConfig.ROUND_RADAR_HEADING_UP.get() ? nose : 0.0f;
            RadarDraw.disc(g, cx, cy, r, BG);
            RadarDraw.arc(g, cx, cy, r / 3.0, 0, 360, GRID);
            RadarDraw.arc(g, cx, cy, r * 2 / 3.0, 0, 360, GRID);
            for (int i = 0; i < 4; i++) {
                RadarDraw.ray(g, cx, cy, i * 90 - rotation, r, GRID);
            }
            RadarDraw.arc(g, cx, cy, r, 0, 360, EDGE);

            if (msa) {
                float b = VehicleRadarClient.beamAt(pt) - rotation;
                for (int i = GLOW_LINES; i >= 1; i--) {
                    int alpha = (int) (0x60 * (1.0f - (float) i / (GLOW_LINES + 1)));
                    RadarDraw.ray(g, cx, cy, b - i * 3.0f, r, (alpha << 24) | (BEAM & 0xFFFFFF));
                }
                RadarDraw.ray(g, cx, cy, b, r, BEAM);
            }

            for (VehicleRadarClient.Blip blip : VehicleRadarClient.BLIPS.values()) {
                double dx = blip.x - own.x;
                double dz = blip.z - own.z;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > VehicleRadarClient.range) continue;
                double ang = Math.toRadians(Math.toDegrees(Math.atan2(dx, -dz)) - rotation);
                int sx = cx + (int) Math.round(Math.sin(ang) * dist * scale);
                int sy = cy - (int) Math.round(Math.cos(ang) * dist * scale);
                drawBlip(g, blip, sx, sy, headingBearing(blip.heading) - rotation, msa);
            }

            RadarDraw.ray(g, cx, cy, nose - rotation, 5, OWN);
            RadarDraw.dot(g, cx, cy, OWN);
            Font font = mc.font;
            text(g, font, "N", cx + (int) Math.round(Math.sin(Math.toRadians(-rotation)) * (r - 5)) - 1,
                    cy - (int) Math.round(Math.cos(Math.toRadians(-rotation)) * (r - 5)) - 2, TEXT_DIM);
        }

        renderLegend(g, mc.font, legendX, y0, msa);
    }

    private static float headingBearing(float targetYaw) {
        double vx = -Mth.sin(targetYaw * Mth.DEG_TO_RAD);
        double vz = Mth.cos(targetYaw * Mth.DEG_TO_RAD);
        return (float) Math.toDegrees(Math.atan2(vx, -vz));
    }

    private static void drawBlip(GuiGraphics g, VehicleRadarClient.Blip blip, int x, int y, float headingScreen, boolean msa) {
        int alpha = 255;
        if (msa) {
            float fade = Math.min(1.0f, blip.age / (float) Math.max(1, VehicleRadarClient.paintPeriod()));
            alpha = 255 - (int) (170 * fade);
        }
        int color = (alpha << 24) | (colorOf(blip.type) & 0xFFFFFF);
        RadarDraw.ray(g, x, y, headingScreen, 6, color);
        RadarDraw.blip(g, x, y, color, blip.friendly);
    }

    private static void renderLegend(GuiGraphics g, Font font, int x, int y, boolean msa) {
        int line = Math.round(10 * TEXT_SCALE) + 1;
        text(g, font, msa ? "MSA" : "AESA", x, y, TEXT);
        text(g, font, Tr.s("gui.ata.radar.meters", VehicleRadarClient.range), x, y + line, TEXT_DIM);
        int ly = y + line * 2 + 3;
        for (int i = 0; i < TYPES.length; i++) {
            RadarDraw.blip(g, x + 2, ly + 2, colorOf(TYPES[i]), false);
            text(g, font, TAGS[i], x + 7, ly, TEXT_DIM);
            ly += line;
        }
        RadarDraw.blip(g, x + 2, ly + 2, TEXT, true);
        text(g, font, Tr.s("gui.ata.vehicle_radar.team"), x + 7, ly, TEXT_DIM);
    }

    private static void text(GuiGraphics g, Font font, String s, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }
}
